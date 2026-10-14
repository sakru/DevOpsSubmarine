/**
 * EnemyShip: enemy surface ship, rides just below the seabed line and
 * drops depth charges while moving left. Leaves the field on the left.
 */
import java.util.Random;

public class EnemyShip extends Enemy {

    private int cooldown;

    public EnemyShip(int x, int y) {
        this(x, y, new Random());
    }

    public EnemyShip(int x, int y, Random rng) {
        super(x, y);
        cooldown = Weapons.DEPTH_CHARGE_FIRST_DROP + rng.nextInt(10);
    }

    @Override
    public void update(Game game) {
        x -= Weapons.ENEMY_SPEED;
        if (x + sprite().length() <= 0) {
            kill();
            return;
        }
        if (--cooldown <= 0) {
            game.addProjectile(new DepthCharge(x + sprite().length() / 2, y - 1));
            cooldown = Weapons.DEPTH_CHARGE_COOLDOWN;
        }
    }

    @Override
    public String sprite() {
        return "__/\\__";
    }
}
