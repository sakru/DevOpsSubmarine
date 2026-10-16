public class ExplosionWorkerTest {
    public static void main(String[] args) throws InterruptedException {
        try (ExplosionWorker worker = new ExplosionWorker()) {
            worker.submit(30, 10);
            worker.submit(70, 15);
            check(awaitZone(worker), 30, 10);
            check(awaitZone(worker), 70, 15);
            if (worker.pollZone() != null) throw new AssertionError("Duplicate zone");
        }
        boolean alive = Thread.getAllStackTraces().keySet().stream()
                .anyMatch(thread -> thread.getName().equals("explosions") && thread.isAlive());
        if (alive) throw new AssertionError("Worker must stop on close");
        System.out.println("Explosion worker checks passed");
    }

    private static ExplosionWorker.Zone awaitZone(ExplosionWorker worker)
            throws InterruptedException {
        long deadline = System.nanoTime() + 1_000_000_000L;
        ExplosionWorker.Zone zone;
        while ((zone = worker.pollZone()) == null) {
            if (System.nanoTime() >= deadline) throw new AssertionError("Zone delivery timed out");
            Thread.sleep(1);
        }
        return zone;
    }

    private static void check(ExplosionWorker.Zone zone, int x, int y) {
        if (zone.x() != x || zone.y() != y || zone.radius() != 2) {
            throw new AssertionError("Unexpected explosion zone: " + zone);
        }
        if (!zone.frames().get(1).get(2).equals("..++*++..")) {
            throw new AssertionError("Explosion must show its centre and damage rings");
        }
        Explosion scrolled = new Explosion(zone, 4, 2);
        if (scrolled.getX() != x - 4 || scrolled.getY() != y || scrolled.isDone()) {
            throw new AssertionError("Delayed zone must account for world movement");
        }
        Explosion expired = new Explosion(zone, 0, Weapons.EXPLOSION_VISUAL_FRAMES);
        if (!expired.isDone()) throw new AssertionError("Old zones must not restart their animation");
    }
}
