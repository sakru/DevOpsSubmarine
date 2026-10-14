/**
 * Controller: reads console key input for the game loop.
 *
 * Keys (applied immediately, without pressing Enter, when a real terminal is
 * available):
 *   w  - swim up (depth decreases)
 *   s  - dive down (depth increases)
 *   0  - quit the game
 *
 * Design:
 *  - A dedicated daemon reader thread performs the (blocking) input read and
 *    pushes raw bytes into a thread-safe queue. The game thread calls poll()
 *    once per frame to drain the queue and apply keys, so all game state is
 *    mutated on a single thread (no races) and handling latency is at most one
 *    frame.
 *  - When the input is the interactive console (System.console() != null) the
 *    terminal is switched to raw/cbreak mode so w/s/0 arrive without Enter.
 *    The terminal is restored to its original state on normal shutdown and on
 *    exceptions, and again from a JVM shutdown hook, so it is never left in
 *    raw mode.
 *  - When input is redirected (file/pipe) or the terminal does not support raw
 *    mode, the reader simply consumes bytes as they arrive and records EOF.
 *    The game loop treats EOF (inputClosed) as a stop condition, so redirected
 *    runs and Ctrl-D terminate cleanly.
 *  - Interrupting the reader thread (or shutting the controller down) stops
 *    input handling cleanly without leaving the terminal in a bad state.
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

    private volatile boolean rawMode = false;
    private volatile boolean quitRequested = false;
    private volatile boolean inputClosed = false;
    private volatile boolean shutdownDone = false;

    public Controller(Game game) {
        this(game, System.in);
    }

    /**
     * Testable constructor: reads from the supplied stream. Raw mode is only
     * entered when the stream is the real interactive console.
     */
    public Controller(Game game, InputStream in) {
        this.game = game;
        this.in = in;
        boolean useConsole = (in == System.in && System.console() != null);
        if (useConsole) {
            try {
                System.console().enterRawMode();
                rawMode = true;
            } catch (Throwable t) {
                // Terminal does not support raw mode; keep line/byte mode.
                rawMode = false;
            }
        }
        startReader();
        if (useConsole) {
            // Guarantee terminal restore even on abrupt JVM exit.
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "controller-restore"));
        }
    }

    private void startReader() {
        reader = new Thread(() -> {
            try {
                int b;
                while (!Thread.currentThread().isInterrupted() && (b = in.read()) != EOF) {
                    inputQueue.offer(b);
                }
            } catch (IOException ignored) {
                // I/O error: treat as end of input.
            } finally {
                inputClosed = true;
            }
        }, "controller-input");
        reader.setDaemon(true);
        reader.start();
    }

    /**
     * Drains queued key input and applies it to the game. Non-blocking.
     * Called once per game frame on the game thread.
     */
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
        switch (c) {
            case 'w', 'W' -> game.diveUp();
            case 's', 'S' -> game.diveDown();
            case '0' -> requestQuit();
            default -> {
                // Ignore everything else (Enter, arrows, Ctrl sequences, ...).
            }
        }
    }

    /** True when the user pressed the quit key (0). */
    public boolean quitRequested() {
        return quitRequested;
    }

    /** True once the input stream reached EOF (Ctrl-D / redirected input exhausted). */
    public boolean inputClosed() {
        return inputClosed;
    }

    /** True when raw/cbreak terminal mode is active. */
    public boolean rawMode() {
        return rawMode;
    }

    /** Signals the game loop that the user asked to quit. */
    public void requestQuit() {
        quitRequested = true;
    }

    /**
     * Restores the terminal and stops the reader thread. Idempotent and safe
     * to call from any thread (including a shutdown hook).
     */
    public void shutdown() {
        if (shutdownDone) {
            return;
        }
        shutdownDone = true;
        if (rawMode) {
            try {
                System.console().exitRawMode();
            } catch (Throwable ignored) {
                // Console already gone or not in raw mode; nothing to restore.
            }
            rawMode = false;
        }
        if (reader != null) {
            reader.interrupt();
        }
        inputQueue.clear();
    }

    /**
     * Waits (bounded) for the reader thread to drain the supplied stream.
     * Used by deterministic tests: for an in-memory stream the reader reaches
     * EOF quickly, after which poll() is fully deterministic.
     */
    void awaitDrained(long timeoutMs) throws InterruptedException {
        if (reader != null) {
            reader.join(timeoutMs);
        }
    }
}
