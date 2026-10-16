import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class ExplosionIntegrationTest {
    public static void main(String[] args) throws InterruptedException {
        try (ExplosionWorker worker = new ExplosionWorker()) {
            Game game = new Game(1L, false, worker);
            EnemyMine mine = new EnemyMine(game.subBoxRight(), game.subRow());
            game.addEnemy(mine);
            game.update();
            check(!mine.isAlive(), "Contact must detonate the mine");
            awaitExplosion(game);
            check(game.getHull() == 25, "Mine must deal 75 damage once");
            check(countExplosions(game) == 1, "Mine must produce one explosion");
            game.update();
            game.update();
            check(render(game).contains(".+*+."), "Renderer must draw the worker's damage zone");
            for (int i = 0; i < Weapons.EXPLOSION_VISUAL_FRAMES; i++) game.update();
            check(game.getHull() == 25, "Animation must not deal repeated damage");
            check(countExplosions(game) == 0, "Explosion must expire");
        }
        try (ExplosionWorker worker = new ExplosionWorker()) {
            Game game = new Game(1L, false, worker);
            game.addProjectile(new Torpedo(game.subBoxRight() + 1, game.subRow()));
            game.update();
            awaitExplosion(game);
            check(game.getHull() == 90, "Torpedo must keep its direct damage without extra blast damage");
            check(countExplosions(game) == 1, "Torpedo impact must produce one explosion");
        }
        try (ExplosionWorker worker = new ExplosionWorker()) {
            Game game = new Game(1L, false, worker);
            EnemyMine target = new EnemyMine(game.subBoxRight() + 9, game.subRow());
            game.addEnemy(target);
            game.addProjectile(new Torpedo(game.subBoxRight() + 7, game.subRow(), true));
            game.update();
            awaitExplosion(game);
            check(!target.isAlive() && game.getKills() == 1, "Player torpedo must destroy its target");
            check(game.getHull() == 100, "Player torpedo blast must not hurt the player");
            check(countExplosions(game) == 1, "Enemy hit must not duplicate the torpedo explosion");
        }
        try (ExplosionWorker worker = new ExplosionWorker()) {
            Game game = new Game(1L, false, worker);
            DepthCharge charge = new DepthCharge(game.subBoxRight(), game.subRow());
            game.addProjectile(charge);
            game.update();
            awaitExplosion(game);
            check(!charge.isAlive() && game.getHull() == 25, "Depth charge must explode against the hull");
        }
        try (ExplosionWorker worker = new ExplosionWorker()) {
            Game game = new Game(1L, false, worker);
            DepthCharge charge = new DepthCharge(100, game.getFloor().heightAt(100));
            game.addProjectile(charge);
            game.update();
            awaitExplosion(game);
            check(!charge.isAlive() && game.getHull() == 100, "Seabed impact must explode away from the hull");
        }
        try (ExplosionWorker worker = new ExplosionWorker()) {
            Game game = new Game(1L, false, worker);
            game.addExplosion(new Explosion(game.subBoxRight() + 2, game.subRow()));
            // Move the hull away before delivery; damage uses the position at detonation.
            for (int i = 0; i < 5; i++) game.diveUp();
            awaitExplosion(game);
            check(game.getHull() == 75, "Damage at the outer ring must use the captured hull position");
        }
        System.out.println("Explosion integration checks passed");
    }

    private static void awaitExplosion(Game game) throws InterruptedException {
        long deadline = System.nanoTime() + 1_000_000_000L;
        while (countExplosions(game) == 0) {
            if (System.nanoTime() >= deadline) throw new AssertionError("Explosion delivery timed out");
            Thread.sleep(1);
            game.update();
        }
    }

    private static int countExplosions(Game game) {
        int count = 0;
        for (Explosion ignored : game.getExplosions()) count++;
        return count;
    }

    private static String render(Game game) {
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(output)) {
            System.setOut(capture);
            try (Renderer renderer = new Renderer(game)) {
                renderer.draw();
            }
        } finally {
            System.setOut(original);
        }
        return output.toString();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
