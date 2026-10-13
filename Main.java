/**
 * Main: (poll → update → render → sleep).
 */
public class Main {

    public static void main(String[] args) {
        Game game = new Game();
        Controller controller = new Controller(game);
        Renderer renderer = new Renderer(game);

        while (game.isRunning()) {
            controller.poll();
            game.update();
            renderer.clearScreen();
            renderer.draw();
            pauseForFrame();
        }
    }

    private static void pauseForFrame() {
        try {
            Thread.sleep(40);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
