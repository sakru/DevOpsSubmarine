import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

public class Main {

    private static final long FRAME_NANOS = 1_000_000_000L / Game.TICKS_PER_SECOND;

    public static void main(String[] args) {
        try {
            Game.Difficulty difficulty;
            try (Renderer renderer = new Renderer(new Game(0L, false))) {
                difficulty = new Menu(System.in, System.out, renderer).chooseDifficulty();
            }
            if (difficulty != null) new Menu(System.in, System.out).showStats(runGame(difficulty));
        } catch (IOException e) {
            System.err.println("Cannot read menu input: " + e.getMessage());
        }
    }

    private static Game runGame(Game.Difficulty difficulty) {
        Controller controller = null;
        Game game;
        boolean restart;
        try {
            do {
                try (ExplosionWorker worker = new ExplosionWorker()) {
                    game = new Game(worker, difficulty);
                    if (controller == null) controller = new Controller(game);
                    else controller.startGame(game);
                    try (Renderer renderer = new Renderer(game)) {
                        restart = runSession(game, controller, renderer);
                    }
                }
            } while (restart);
        } finally {
            if (controller != null) controller.shutdown();
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
    private final InputStream input;
    private final PrintStream output;
    private final Renderer renderer;

    Menu(InputStream input, PrintStream output) {
        this(input, output, null);
    }

    Menu(InputStream input, PrintStream output, Renderer renderer) {
        this.input = input;
        this.output = output;
        this.renderer = renderer;
    }

    private void show(String... lines) {
        if (renderer != null) renderer.drawMenu(lines);
        else for (String line : lines) output.println(line);
    }

    Game.Difficulty chooseDifficulty() throws IOException {
        while (true) {
            show("DevOpsSubmarine", "1: New game", "0: Exit");
            int choice = readChoice();
            if (choice == -1 || choice == '0') return null;
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

    private int readChoice() throws IOException {
        output.flush();
        int choice;
        do {
            choice = input.read();
        } while (choice == '\n' || choice == '\r');
        return choice;
    }

    void showStats(Game game) {
        output.println("\nSession statistics — " + game.getDifficulty());
        output.println(game.getStopMessage());
        output.println("Time: " + game.getSurvivalSeconds() + " s");
        output.println("Distance: " + game.getDistance());
        output.println("Enemies destroyed: " + game.getKills());
        output.println("Crew survived: " + game.getSubmarine().getCrew().getLiving() + "/100");
        output.println("Level: " + game.getLevel() + "  Hull: " + game.getHull() + "%");
    }
}
