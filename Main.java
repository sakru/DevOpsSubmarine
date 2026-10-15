/**
 * Main: (poll → update → render → sleep).
 */
public class Main {

    private static final long FRAME_MS = 1000L / Game.TICKS_PER_SECOND;

    public static void main(String[] args) {
        Game game = new Game();
        Controller controller = new Controller(game);
        try (Renderer renderer = new Renderer(game)) {
            while (game.isRunning()) {
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
                pauseForFrame();
                if (Thread.currentThread().isInterrupted()) {
                    game.stop(Game.StopReason.INTERRUPTED);
                }
            }
        } finally {
            controller.shutdown();
        }
        System.out.println(game.getStopMessage());
    }

    private static void pauseForFrame() {
        try {
            Thread.sleep(FRAME_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
