/**
 * Controller:

 */
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Controller {

    private static final int EOF = -1;

    private final Game game;
    private final InputStream in;

    private final ConcurrentLinkedQueue<Integer> inputQueue = new ConcurrentLinkedQueue<>();
    private Thread reader;
    private int escapeState;

    private volatile boolean quitRequested = false;
    private volatile boolean inputClosed = false;
    private volatile boolean shutdownDone = false;

    public Controller(Game game) {
        this(game, System.in);
    }


    public Controller(Game game, InputStream in) {
        this.game = game;
        this.in = in;
        startReader();
    }

    private void startReader() {
        reader = new Thread(() -> {
            try {
                int b;
                while (!Thread.currentThread().isInterrupted() && (b = in.read()) != EOF) {
                    inputQueue.offer(b);
                }
            } catch (IOException ignored) {

            } finally {
                inputClosed = true;
            }
        }, "controller-input");
        reader.setDaemon(true);
        reader.start();
    }


    public void poll() {
        Integer b;
        while ((b = inputQueue.poll()) != null) {
            dispatch(b);
            if (quitRequested) {
                break;
            }
        }
        if (quitRequested) {
            shutdown();
        }
    }

    private void dispatch(int c) {
        if (c == 27) {
            escapeState = 1;
            return;
        }
        if (escapeState == 1) {
            escapeState = 0;
            if (c == '[' || c == 'O') {
                escapeState = 2;
                return;
            }
        }
        if (escapeState == 2) {
            if (c == 'A') {
                game.diveUp();
            } else if (c == 'B') {
                game.diveDown();
            }
            if (c >= 64 && c <= 126) {
                escapeState = 0;
            }
            return;
        }
        switch (c) {
            case 'w', 'W' -> game.diveUp();
            case 's', 'S' -> game.diveDown();
            case ' ', 'f', 'F' -> game.fireTorpedo();
            case '0' -> requestQuit();
            case 4 -> inputClosed = true;
            default -> {
            }
        }
    }

    public boolean quitRequested() {
        return quitRequested;
    }

    public boolean inputClosed() {
        return inputClosed;
    }

    public void requestQuit() {
        quitRequested = true;
    }

    public void shutdown() {
        if (shutdownDone) {
            return;
        }
        shutdownDone = true;
        if (reader != null) {
            reader.interrupt();
        }
        inputQueue.clear();
    }

    void awaitDrained(long timeoutMs) throws InterruptedException {
        if (reader != null) {
            reader.join(timeoutMs);
        }
    }
}
