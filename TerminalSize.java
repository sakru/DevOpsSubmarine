import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/** Reads the current terminal dimensions without consuming keyboard input. */
public final class TerminalSize {
    public record Size(int columns, int rows) {}

    private final String device;
    private Size size;
    private long nextCheck;

    public TerminalSize() {
        this(System.getProperty("game.terminal", "/dev/tty"),
                new Size(Integer.getInteger("game.columns", Game.FIELD_W + 2),
                        Integer.getInteger("game.rows", Game.FIELD_H + 4)));
    }

    public TerminalSize(String device, Size fallback) {
        this.device = device;
        this.size = fallback;
    }

    public Size current() {
        long now = System.nanoTime();
        if (nextCheck != 0 && now - nextCheck < 0) return size;
        nextCheck = now + 500_000_000L;
        Process process = null;
        try {
            process = new ProcessBuilder("stty", "-F", device, "size")
                    .redirectError(ProcessBuilder.Redirect.DISCARD).start();
            if (process.waitFor(100, TimeUnit.MILLISECONDS) && process.exitValue() == 0) {
                String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.US_ASCII).trim();
                String[] dimensions = output.split("\\s+");
                if (dimensions.length == 2) {
                    int rows = Integer.parseInt(dimensions[0]);
                    int columns = Integer.parseInt(dimensions[1]);
                    if (rows > 0 && columns > 0) size = new Size(columns, rows);
                }
            }
        } catch (IOException | NumberFormatException e) {
            // Keep the last known size when no terminal is available.
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (process != null) process.destroy();
        }
        return size;
    }
}
