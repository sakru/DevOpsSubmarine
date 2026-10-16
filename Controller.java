import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Controller {

    private static final int EOF = -1;

    private Game game;
    private boolean restartRequested;

    private final ConcurrentLinkedQueue<Integer> inputQueue = new ConcurrentLinkedQueue<>();
    private final Thread reader;
    private int escapeState;

    private volatile boolean quitRequested = false;
    private volatile boolean inputClosed = false;
    private volatile String inputError;
    private volatile boolean stopping;

    public Controller(Game game) {
        this(game, System.in);
    }

    public Controller(Game game, InputStream in) {
        this(game, new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    public Controller(Game game, Reader in) {
        this.game = game;
        reader = new Thread(() -> {
            try {
                int b;
                while (!Thread.currentThread().isInterrupted() && (b = in.read()) != EOF) {
                    inputQueue.offer(b);
                }
            } catch (IOException e) {
                if (!stopping) inputError = e.toString();
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
            if (quitRequested || restartRequested) {
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
            if (game.isRunning() && c == 'A') {
                game.diveUp();
            } else if (game.isRunning() && c == 'B') {
                game.diveDown();
            }
            if (c >= 64 && c <= 126) {
                escapeState = 0;
            }
            return;
        }
        if (!game.isRunning()) {
            if (c == '1') restartRequested = true;
            if (c == '0') requestQuit();
            if (c == 4) inputClosed = true;
            return;
        }
        switch (c) {
            case 'w', 'W' -> game.diveUp();
            case 's', 'S' -> game.diveDown();
            case 'a', 'A' -> game.brake();
            case 'd', 'D' -> game.accelerate();
            case ' ', 'f', 'F' -> game.fireTorpedo();
            case 'r', 'R' -> game.repair();
            case '0' -> requestQuit();
            case 4 -> inputClosed = true;
            default -> {
            }
        }
    }

    public boolean quitRequested() {
        return quitRequested;
    }

    public boolean restartRequested() {
        return restartRequested;
    }

    public void startGame(Game game) {
        this.game = game;
        restartRequested = false;
        escapeState = 0;
        inputQueue.clear();
    }

    public boolean inputClosed() {
        return inputClosed;
    }

    public String inputError() {
        return inputError;
    }

    public void requestQuit() {
        quitRequested = true;
    }

    public void shutdown() {
        stopping = true;
        reader.interrupt();
        inputQueue.clear();
    }

    void awaitDrained(long timeoutMs) throws InterruptedException {
        reader.join(timeoutMs);
    }
}
