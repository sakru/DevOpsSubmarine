/*
 * Starts each session, keeps frames on schedule and saves the result when a session ends.
 * Menu, below, handles difficulty, captain names and browsing saved results.
 */
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

public class Main {

    private static final long FRAME_NANOS = 1_000_000_000L / Game.TICKS_PER_SECOND;

    public static void main(String[] args) {
        Scores scores;
        try {
            scores = new Scores(Path.of("scores.tsv"));
        } catch (IOException e) {
            System.err.println("Cannot load scores: " + e.getMessage());
            return;
        }
        Reader keyboard = new InputStreamReader(System.in, StandardCharsets.UTF_8);
        try {
            Game.Difficulty difficulty;
            String player;
            try (Renderer renderer = new Renderer(new Game(0L, false))) {
                Menu menu = new Menu(keyboard, System.out, renderer, scores);
                difficulty = menu.chooseDifficulty();
                player = difficulty == null ? null : menu.readPlayer();
            }
            if (player != null) Menu.showStats(runGame(difficulty, scores, player, keyboard), System.out);
        } catch (IOException e) {
            System.err.println("Cannot read menu input: " + e.getMessage());
        }
    }

    private static Game runGame(Game.Difficulty difficulty, Scores scores, String player, Reader keyboard) {
        Controller controller = null;
        Game game;
        boolean restart;
        try {
            do {
                try (ExplosionWorker worker = new ExplosionWorker()) {
                    game = new Game(worker, difficulty);
                    if (controller == null) controller = new Controller(game, keyboard);
                    else controller.startGame(game);
                    try (Renderer renderer = new Renderer(game)) {
                        restart = runSession(game, controller, renderer);
                    }
                    try {
                        scores.add(player, game);
                    } catch (IOException e) {
                        System.err.println("Cannot save this session: " + e.getMessage());
                    }
                }
            } while (restart);
        } finally {
            if (controller != null) {
                controller.shutdown();
                if (controller.inputError() != null) System.err.println("Keyboard input failed: " + controller.inputError());
            }
        }
        return game;
    }

    static boolean runSession(Game game, Controller controller, Renderer renderer) {
        while (!Thread.currentThread().isInterrupted()) {
            long frameStarted = System.nanoTime();
            controller.poll();
            if (controller.quitRequested() || controller.inputClosed()) {
                game.stop(controller.quitRequested() ? Game.StopReason.QUIT : Game.StopReason.EOF);
                break;
            }
            if (controller.restartRequested()) {
                return true;
            }
            if (game.isRunning() && renderer.canDrawGame()) game.update();
            if (game.getStopReason() == Game.StopReason.HULL_BROKEN) {
                renderer.drawMenu("SUBMARINE LOST",
                        game.getSubmarine().isNavigationHit() ? "NAV destroyed" : "Hull breached",
                        "Time: " + game.getSurvivalSeconds() + " s  Distance: " + game.getDistance(),
                        "Kills: " + game.getKills() + "  Crew: " + game.getSubmarine().getCrew().getLiving() + "/100  " + game.getDifficulty(),
                        "1: New game   0: Exit");
            } else {
                renderer.draw();
            }
            pauseForFrame(frameStarted);
        }
        if (Thread.currentThread().isInterrupted()) game.stop(Game.StopReason.INTERRUPTED);
        return false;
    }

    static long remainingFrameNanos(long frameStarted, long now) {
        return Math.max(0, FRAME_NANOS - (now - frameStarted));
    }

    private static void pauseForFrame(long frameStarted) {
        long remaining = remainingFrameNanos(frameStarted, System.nanoTime());
        if (remaining == 0) {
            return;
        }
        try {
            Thread.sleep(remaining / 1_000_000L, (int) (remaining % 1_000_000L));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

class Menu {
    private final Reader input;
    private final PrintStream output;
    private final Renderer renderer;

    private final Scores scores;

    Menu(Reader input, PrintStream output, Renderer renderer, Scores scores) {
        this.input = input;
        this.output = output;
        this.renderer = renderer;
        this.scores = scores;
    }

    private void show(String... lines) {
        if (renderer != null) renderer.drawMenu(lines);
        else for (String line : lines) output.println(line);
    }

    Game.Difficulty chooseDifficulty() throws IOException {
        while (true) {
            show("DevOpsSubmarine", "1: New game", "2: Scores", "3: Find captain",
                    "4: Delete score", "5: Statistics", "0: Exit");
            int choice = readChoice();
            if (choice == -1 || choice == '0') return null;
            if (choice >= '2' && choice <= '5') {
                scoreAction(choice);
                continue;
            }
            if (choice != '1') continue;
            String[] options = new String[Game.Difficulty.values().length + 2];
            options[0] = "Difficulty";
            for (Game.Difficulty difficulty : Game.Difficulty.values()) {
                options[difficulty.ordinal() + 1] = (difficulty.ordinal() + 1) + ": " + difficulty
                        + " (up to " + difficulty.maxSubmarines + " submarines)";
            }
            options[options.length - 1] = "0: Back";
            show(options);
            while ((choice = readChoice()) != '0') {
                if (choice == -1) return null;
                if (choice >= '1' && choice <= '3') return Game.Difficulty.values()[choice - '1'];
                show(options);
            }
        }
    }

    String readPlayer() throws IOException {
        while (true) {
            String name = readText("Captain name (Enter: Captain)");
            if (name == null) return null;
            try {
                return Scores.playerName(name.isEmpty() ? "Captain" : name);
            } catch (IllegalArgumentException e) {
                message("Invalid name", e.getMessage());
            }
        }
    }

    private void scoreAction(int choice) throws IOException {
        try {
            switch (choice) {
                case '2' -> showScores(scores.all());
                case '3' -> {
                    String name = readText("Find captain: name or part of name");
                    if (name != null) showScores(scores.find(name));
                }
                case '4' -> {
                    String id = readText("Delete score: enter ID");
                    if (id != null) message(scores.remove(Long.parseLong(id.strip())) ? "Score deleted" : "Score ID not found");
                }
                case '5' -> {
                    Scores.Summary stats = scores.summary();
                    message("All sessions", "Sessions: " + stats.sessions(), "Kills: " + stats.kills(),
                            "Distance: " + stats.distance(), "Average time: " + Math.round(stats.averageSeconds()) + " s");
                }
                default -> { }
            }
        } catch (NumberFormatException e) {
            message("Invalid ID", "Enter a positive whole number");
        } catch (IllegalArgumentException e) {
            message("Invalid input", e.getMessage());
        } catch (IOException e) {
            message("Scores could not be updated", e.getMessage());
        }
    }

    private void showScores(List<Scores.Score> results) throws IOException {
        if (results.isEmpty()) { message("No matching sessions"); return; }
        int page = 0;
        while (true) {
            int start = page * 3;
            java.util.ArrayList<String> lines = new java.util.ArrayList<>();
            lines.add("ID Captain  K:kills D:distance");
            for (int i = start; i < Math.min(start + 3, results.size()); i++) {
                Scores.Score score = results.get(i);
                lines.add(score.id() + " " + score.player() + " K:" + score.kills() + " D:" + score.distance());
            }
            lines.add("Page " + (page + 1) + "/" + ((results.size() + 2) / 3) + "  N:next P:prev 0:back");
            show(lines.toArray(String[]::new));
            int key = readChoice();
            if (key == '0' || key == -1) return;
            if ((key == 'n' || key == 'N') && start + 3 < results.size()) page++;
            if ((key == 'p' || key == 'P') && page > 0) page--;
        }
    }

    private void message(String... lines) throws IOException {
        String[] panel = java.util.Arrays.copyOf(lines, lines.length + 1);
        panel[lines.length] = "0: Back";
        show(panel);
        int key;
        do { key = readChoice(); } while (key != '0' && key != -1);
    }

    private String readText(String title) throws IOException {
        StringBuilder text = new StringBuilder();
        while (true) {
            show(title, "> " + text, "Enter: confirm  Esc: cancel");
            int key = input.read();
            if (key == -1 || key == 27 || key == 4) return null;
            if (key == '\n' || key == '\r') return text.toString();
            if (key == 8 || key == 127) {
                if (!text.isEmpty()) text.delete(text.offsetByCodePoints(text.length(), -1), text.length());
            } else if (!Character.isISOControl(key) && text.length() < 24) {
                text.append((char) key);
            }
        }
    }

    private int readChoice() throws IOException {
        output.flush();
        int choice;
        do {
            choice = input.read();
        } while (choice == '\n' || choice == '\r');
        return choice == 4 ? -1 : choice;
    }

    static void showStats(Game game, PrintStream output) {
        output.println("\nSession statistics — " + game.getDifficulty());
        output.println(game.getStopMessage());
        output.println("Time: " + game.getSurvivalSeconds() + " s");
        output.println("Distance: " + game.getDistance());
        output.println("Enemies destroyed: " + game.getKills());
        output.println("Crew survived: " + game.getSubmarine().getCrew().getLiving() + "/100");
        output.println("Level: " + game.getLevel() + "  Hull: " + game.getHull() + "%");
    }
}
