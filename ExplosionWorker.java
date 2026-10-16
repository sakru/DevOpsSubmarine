import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/** Receives impact positions and returns zones to the game thread. */
public final class ExplosionWorker implements AutoCloseable {

    public record Impact(int x, int y, int hullX, int hullY, boolean damagesPlayer,
                         long distance, int frame, int level) {}
    public record Zone(Impact impact, int radius, List<List<String>> frames, int damage) {
        public Zone {
            frames = frames.stream().map(List::copyOf).toList();
        }

        public int x() { return impact.x(); }
        public int y() { return impact.y(); }
    }

    private final BlockingQueue<Impact> impacts = new LinkedBlockingQueue<>();
    private final BlockingQueue<Zone> zones = new LinkedBlockingQueue<>();
    private final Thread worker;

    public ExplosionWorker() {
        worker = new Thread(this::run, "Взрывы");
        worker.setDaemon(true);
        worker.start();
    }

    public void submit(int x, int y) {
        submit(new Impact(x, y, x, y, false, 0, 0, Game.MIN_LEVEL));
    }

    public void submit(Impact impact) {
        impacts.add(impact);
    }

    public Zone pollZone() {
        return zones.poll();
    }

    private void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Impact impact = impacts.take();
                zones.add(prepare(impact));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static Zone prepare(Impact impact) {
        int radius = (Weapons.EXPLOSION_MAX_DAMAGE - 1) / Weapons.EXPLOSION_DAMAGE_STEP;
        List<List<String>> frames = new ArrayList<>();
        for (int phase = 0; phase < 4; phase++) {
            List<String> sprite = new ArrayList<>();
            for (int dy = -radius; dy <= radius; dy++) {
                StringBuilder row = new StringBuilder();
                for (int dx = -radius; dx <= radius; dx++) {
                    int ring = Math.max(Math.abs(dx), Math.abs(dy));
                    char cell = ring == radius ? '.' : ' ';
                    if (phase == 3) {
                        if (ring == 0) cell = '.';
                    } else if (ring <= phase) {
                        cell = ring == 0 ? (phase == 2 ? '@' : '*') : ring == phase ? '+' : '*';
                    }
                    row.append(cell);
                }
                sprite.add(row.toString());
            }
            frames.add(sprite);
        }
        int distance = Math.max(Math.abs(impact.hullX() - impact.x()),
                Math.abs(impact.hullY() - impact.y()));
        int damage = impact.damagesPlayer()
                ? Math.max(0, Weapons.EXPLOSION_MAX_DAMAGE - distance * Weapons.EXPLOSION_DAMAGE_STEP)
                : 0;
        return new Zone(impact, radius, frames, damage);
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
