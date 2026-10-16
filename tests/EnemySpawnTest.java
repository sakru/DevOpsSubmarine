import java.util.HashSet;
import java.util.Set;

public class EnemySpawnTest {
    public static void main(String[] args) {
        int[] totals = new int[Game.Difficulty.values().length];
        for (Game.Difficulty difficulty : Game.Difficulty.values()) {
            boolean reachedLimit = false;
            Set<Enemy> seen = new HashSet<>();
            for (int level = Game.MIN_LEVEL; level <= Game.MAX_LEVEL; level++) {
                for (int seed = 0; seed < 5; seed++) {
                    Game game = new Game(seed, false, null, difficulty) {
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
                        if (submarines > game.getDifficulty().maxSubmarines) {
                            throw new AssertionError("Too many enemy submarines at level " + level);
                        }
                        reachedLimit |= submarines == game.getDifficulty().maxSubmarines;
                    }
                }
            }
            if (!reachedLimit || seen.size() <= difficulty.maxSubmarines) {
                throw new AssertionError("Test must exercise the limit and subsequent spawning");
            }
            totals[difficulty.ordinal()] = seen.size();
        }
        if (!(totals[0] < totals[1] && totals[1] < totals[2])) {
            throw new AssertionError("Harder modes must produce more enemy submarines");
        }
        System.out.println("Enemy spawn checks passed across all modes and 12 levels");
    }
}
