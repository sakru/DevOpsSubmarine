/**
 * Controller: reads console key input.
 *
 * Keys:
 *   w  - swim up (depth decreases)
 *   s  - dive down (depth increases)
 *   0  - quit game
 */
import java.io.IOException;

public class Controller {

    private final Game game;

    public Controller(Game game) {
        this.game = game;
    }

    public void poll() {
        try {
            while (System.in.available() > 0) {
                int c = System.in.read();
                switch (c) {
                    case 'w' -> game.diveUp();
                    case 's' -> game.diveDown();
                    case '0' -> game.stop();
                }
            }
        } catch (IOException e) {
            game.stop();
        }
    }
}
