import java.util.ArrayList;
import java.util.List;

public class EnemyMovementTest {

    public static void main(String[] args) {
        Game game = new Game(1L, false);
        EnemySub submarine = new EnemySub(150, 5);
        EnemyMine mine = new EnemyMine(150, 5);
        game.addEnemy(submarine);
        game.addEnemy(mine);

        for (int tick = 0; tick < Game.TICKS_PER_SECOND * 3; tick++) {
            int previousX = submarine.getX();
            game.update();
            if (game.getScrollStep() == 0) {
                check(submarine.getX() == previousX,
                        "Submarine movement must follow forward ticks");
            }
        }
        int submarineTravel = 150 - submarine.getX();
        int mineTravel = 150 - mine.getX();
        check(submarine.isAlive() && mine.isAlive(), "Test enemies must remain in play");
        check(submarineTravel == 25, "Submarine must travel 25 columns in three seconds");
        check(mineTravel == game.getDistance(), "Mine must follow world scrolling");
        check(submarineTravel * 2 == mineTravel,
                "Submarine must pass at half the seabed scroll speed");
        check(submarine.width() > 21 && submarine.width() < Submarine.WIDTH
                && submarine.height() > 3 && submarine.height() < Submarine.HEIGHT,
                "Enemy submarine must be larger than before but smaller than the player");

        Game enemyGame = new Game(1L, false);
        EnemySub shooter = new EnemySub(150, 5);
        for (int tick = 1; tick <= Weapons.TORPEDO_COOLDOWN * 2; tick++) {
            shooter.tryAttack(enemyGame);
            int shots = 0;
            for (Projectile ignored : enemyGame.getProjectiles()) shots++;
            check(shots == tick / Weapons.TORPEDO_COOLDOWN, "Enemy must respect the shared reload interval");
        }
        List<Integer> playerShots = new ArrayList<>();
        Game playerGame = new Game(1L, false) {
            @Override public void addProjectile(Projectile projectile) {
                if (projectile.isPlayerOwned()) playerShots.add(getFrame());
            }
        };
        playerGame.fireTorpedo();
        for (int tick = 1; tick <= Weapons.TORPEDO_COOLDOWN * 2; tick++) {
            playerGame.update();
            playerGame.fireTorpedo();
        }
        check(playerShots.equals(List.of(0, Weapons.TORPEDO_COOLDOWN, Weapons.TORPEDO_COOLDOWN * 2)),
                "Player and enemy must have identical firing intervals");
        System.out.println("Enemy movement checks passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
