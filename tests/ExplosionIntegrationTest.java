import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class ExplosionIntegrationTest {
    public static void main(String[] args) throws InterruptedException {
        try (ExplosionWorker worker = new ExplosionWorker()) {
            Game game = new Game(1L, false, worker);
            EnemyMine mine = new EnemyMine(game.subBoxRight(), hitRow(game));
            game.addEnemy(mine);
            game.update();
            check(!mine.isAlive(), "Contact must detonate the mine");
            awaitExplosion(game);
            check(game.getHull() == 25, "Mine must deal 75 damage once");
            check(countExplosions(game) == 1, "Mine must produce one explosion");
            Explosion blast = game.getExplosions().iterator().next();
            String frame = render(game);
            String rowPrefix = "\033[" + (blast.getY() + 2) + ";1H|";
            int start = frame.indexOf(rowPrefix) + rowPrefix.length();
            String centre = blast.spriteLines().get(blast.getRadius());
            for (int x = 0; x < centre.length(); x++) {
                if (centre.charAt(x) != ' ') {
                    check(frame.charAt(start + blast.getX() - blast.getHorizontalRadius() + x) == centre.charAt(x),
                            "Renderer must draw the worker's current animation phase");
                }
            }
            for (int i = 0; i < Weapons.EXPLOSION_VISUAL_FRAMES; i++) game.update();
            check(game.getHull() == 25, "Animation must not deal repeated damage");
            check(countExplosions(game) == 0, "Explosion must expire");
        }
        try (ExplosionWorker worker = new ExplosionWorker()) {
            Game game = new Game(1L, false, worker);
            game.addProjectile(new Torpedo(game.subBoxRight() + 1, hitRow(game)));
            game.update();
            awaitExplosion(game);
            check(game.getHull() == 90, "Torpedo must keep its direct damage without extra blast damage");
            check(countExplosions(game) == 1, "Torpedo impact must produce one explosion");
        }
        try (ExplosionWorker worker = new ExplosionWorker()) {
            Game game = new Game(1L, false, worker);
            EnemyMine target = new EnemyMine(game.subBoxRight() + 9, hitRow(game));
            game.addEnemy(target);
            game.addProjectile(new Torpedo(game.subBoxRight() + 7, hitRow(game), true));
            game.update();
            awaitExplosion(game);
            check(!target.isAlive() && game.getKills() == 1, "Player torpedo must destroy its target");
            check(game.getHull() == 100, "Player torpedo blast must not hurt the player");
            check(countExplosions(game) == 1, "Enemy hit must not duplicate the torpedo explosion");
        }
        try (ExplosionWorker worker = new ExplosionWorker()) {
            Game game = new Game(1L, false, worker);
            DepthCharge charge = new DepthCharge(game.subBoxRight(), hitRow(game));
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
            game.addExplosion(new Explosion(game.subBoxRight() + 4, hitRow(game)));
            // Move the hull away before delivery; damage uses the position at detonation.
            for (int i = 0; i < 5; i++) game.diveUp();
            awaitExplosion(game);
            check(game.getHull() == 75, "Damage at the outer ring must use the captured hull position");
        }
        System.out.println("Explosion integration checks passed");
    }

    private static int hitRow(Game game) {
        return game.subBoxTop(game.subRow()) + Submarine.TORPEDO_ROW;
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
            try (Renderer renderer = new Renderer(game, Game.FIELD_W + 2, Game.FIELD_H + 4)) {
                renderer.draw();
            }
        } finally {
            System.setOut(original);
        }
        return output.toString().replaceAll("\033\\[[0-9;]*m", "");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
