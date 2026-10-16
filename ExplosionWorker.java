import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/** Receives impact positions and returns zones to the game thread. */
public final class ExplosionWorker implements AutoCloseable {

    public record Impact(int x, int y) {}
    public record Zone(int x, int y, int radius) {}

    private final BlockingQueue<Impact> impacts = new LinkedBlockingQueue<>();
    private final BlockingQueue<Zone> zones = new LinkedBlockingQueue<>();
    private final Thread worker;

    public ExplosionWorker() {
        worker = new Thread(this::run, "Взрывы");
        worker.setDaemon(true);
        worker.start();
    }

    public void submit(int x, int y) {
        impacts.add(new Impact(x, y));
    }

    public Zone pollZone() {
        return zones.poll();
    }

    private void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Impact impact = impacts.take();
                int radius = (Weapons.EXPLOSION_MAX_DAMAGE - 1) / Weapons.EXPLOSION_DAMAGE_STEP;
                zones.add(new Zone(impact.x(), impact.y(), radius));
                // TODO: prepare visual cells here; only Renderer writes to the terminal.
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        worker.interrupt();
        try {
            worker.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
