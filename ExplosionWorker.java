/*
 * Prepares blast damage and animation frames in the background.
 * It reads impact snapshots from a queue and sends the results back for the game loop to apply.
 */
import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public final class ExplosionWorker implements AutoCloseable {

    public record Impact(int x, int y, int hullX, int hullY, boolean damagesPlayer,
                         long distance, int frame, int level, int localX, int localY) {
        public Impact(int x, int y, int hullX, int hullY, boolean damagesPlayer,
                      long distance, int frame, int level) {
            this(x, y, hullX, hullY, damagesPlayer, distance, frame, level, 0, 0);
        }
    }
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
        worker = new Thread(this::run, "explosions");
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
                int horizontalRadius = radius * Weapons.EXPLOSION_COLUMN_SCALE;
                for (int dx = -horizontalRadius; dx <= horizontalRadius; dx++) {
                    double distance = blastDistance(dx, dy);
                    if (distance > radius) {
                        row.append(' ');
                        continue;
                    }
                    boolean centre = dx == 0 && dy == 0;
                    char cell = distance >= radius - 0.5 ? '.' : ' ';
                    if (phase == 3) {
                        if (centre) cell = '.';
                    } else if (distance <= phase) {
                        cell = centre ? (phase == 2 ? '@' : '*') : distance >= phase - 0.5 ? '+' : '*';
                    }
                    row.append(cell);
                }
                sprite.add(row.toString());
            }
            frames.add(sprite);
        }
        int damage = impact.damagesPlayer() ? damageAt(impact.hullX() - impact.x(), impact.hullY() - impact.y()) : 0;
        return new Zone(impact, radius, frames, damage);
    }

    static int damageAt(int dx, int dy) {
        double distance = blastDistance(dx, dy);
        int radius = (Weapons.EXPLOSION_MAX_DAMAGE - 1) / Weapons.EXPLOSION_DAMAGE_STEP;
        return distance <= radius ? Math.max(0, Weapons.EXPLOSION_MAX_DAMAGE
                - (int) Math.ceil(distance * Weapons.EXPLOSION_DAMAGE_STEP)) : 0;
    }

    private static double blastDistance(int dx, int dy) {
        return Math.hypot(dx / (double) Weapons.EXPLOSION_COLUMN_SCALE, dy);
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
