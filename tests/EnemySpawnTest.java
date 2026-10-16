import java.util.HashSet;
import java.util.Set;

public class EnemySpawnTest {
    public static void main(String[] args) {
        boolean reachedLimit = false;
        Set<Enemy> seen = new HashSet<>();
        for (int level = Game.MIN_LEVEL; level <= Game.MAX_LEVEL; level++) {
            for (int seed = 0; seed < 5; seed++) {
                Game game = new Game(seed, false) {
                    @Override
                    public void damageHull(int damage) {
                        // Keep the simulation running long enough for enemies to leave and respawn.
                    }
                };
                game.setLevel(level);
                for (int tick = 0; tick < 1500; tick++) {
                    game.update();
                    int submarines = 0;
                    for (Enemy enemy : game.getEnemies()) {
                        if (enemy instanceof EnemySub) {
                            submarines++;
                            seen.add(enemy);
                        }
                    }
                    if (submarines > Game.MAX_ENEMY_SUBS) {
                        throw new AssertionError("Too many enemy submarines at level " + level);
                    }
                    reachedLimit |= submarines == Game.MAX_ENEMY_SUBS;
                }
            }
        }
        if (!reachedLimit || seen.size() <= Game.MAX_ENEMY_SUBS) {
            throw new AssertionError("Test must exercise the limit and subsequent spawning");
        }
        System.out.println("Enemy spawn checks passed across all 12 levels");
    }
}
