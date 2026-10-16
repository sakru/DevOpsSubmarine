import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

public class Main {

    private static final long FRAME_NANOS = 1_000_000_000L / Game.TICKS_PER_SECOND;

    public static void main(String[] args) {
        Menu menu = new Menu(System.in, System.out);
        try {
            Game.Difficulty difficulty = menu.chooseDifficulty();
            if (difficulty == null) return;
            try (ExplosionWorker explosionWorker = new ExplosionWorker()) {
                menu.showStats(runGame(explosionWorker, difficulty));
            }
        } catch (IOException e) {
            System.err.println("Cannot read menu input: " + e.getMessage());
        }
    }

    private static Game runGame(ExplosionWorker explosionWorker, Game.Difficulty difficulty) {
        Game game = new Game(explosionWorker, difficulty);
        Controller controller = new Controller(game);
        try (Renderer renderer = new Renderer(game)) {
            while (game.isRunning()) {
                long frameStarted = System.nanoTime();
                controller.poll();
                if (controller.quitRequested()) {
                    game.stop(Game.StopReason.QUIT);
                    break;
                }
                if (controller.inputClosed()) {
                    game.stop(Game.StopReason.EOF);
                    break;
                }
                if (renderer.canDrawGame()) {
                    game.update();
                }
                renderer.draw();
                pauseForFrame(frameStarted);
                if (Thread.currentThread().isInterrupted()) {
                    game.stop(Game.StopReason.INTERRUPTED);
                }
            }
        } finally {
            controller.shutdown();
        }
        return game;
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

    Menu(InputStream input, PrintStream output) {
        this.input = input;
        this.output = output;
    }

    Game.Difficulty chooseDifficulty() throws IOException {
        while (true) {
            output.println("\nDevOpsSubmarine\n1: New game\n0: Exit");
            int choice = readChoice();
            if (choice == -1 || choice == '0') return null;
            if (choice != '1') {
                output.println("Choose 1 or 0.");
                continue;
            }
            output.println("\nDifficulty");
            for (Game.Difficulty difficulty : Game.Difficulty.values()) {
                output.println((difficulty.ordinal() + 1) + ": " + difficulty
                        + " (up to " + difficulty.maxSubmarines + " submarines)");
            }
            output.println("0: Back");
            while ((choice = readChoice()) != '0') {
                if (choice == -1) return null;
                if (choice >= '1' && choice <= '3') return Game.Difficulty.values()[choice - '1'];
                output.println("Choose 1, 2, 3 or 0.");
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
        output.println("Level: " + game.getLevel() + "  Hull: " + game.getHull() + "%");
    }
}
