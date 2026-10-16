/**
 * Main: (poll → update → render → sleep).
 */
public class Main {

    private static final long FRAME_NANOS = 1_000_000_000L / Game.TICKS_PER_SECOND;

    public static void main(String[] args) {
        try (ExplosionWorker explosionWorker = new ExplosionWorker()) {
            runGame(explosionWorker);
        }
    }

    private static void runGame(ExplosionWorker explosionWorker) {
        Game game = new Game(explosionWorker);
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
                game.update();
                renderer.draw();
                pauseForFrame(frameStarted);
                if (Thread.currentThread().isInterrupted()) {
                    game.stop(Game.StopReason.INTERRUPTED);
                }
            }
        } finally {
            controller.shutdown();
        }
        System.out.println(game.getStopMessage());
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
