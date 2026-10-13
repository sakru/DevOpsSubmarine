/**
 * Controller: reads console key input.
 *
 * Keys:
 *   w  - decrease depth (sub goes up)
 *   s  - increase depth (sub goes down)
 *   0 - quit game
 */
import java.io.IOException;

public class Controller {

    private final GameLoop game;

    public Controller(GameLoop game) {
        this.game = game;
    }

    public void poll() {
        try {
            while (System.in.available() > 0) {
                int c = System.in.read();
                switch (c) {
                    case 'w' -> game.setDepth(game.getDepth() - 1);
                    case 's' -> game.setDepth(game.getDepth() + 1);
                    case '0' -> game.stop();
                }
            }
        } catch (IOException e) {
            game.stop();
        }
    }
}
