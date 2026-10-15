/**
 * EnemyShip: enemy surface ship, rides at the water surface and
 * drops depth charges while moving left. Leaves the field on the left.
 */
import java.util.Random;
import java.util.HashSet;
import java.util.Set;

public class EnemyShip extends Enemy implements WeaponCarrier {

    private final Set<Integer> dropColumns = new HashSet<>();
    private int moveTicks;

    public EnemyShip(int x, int y) {
        this(x, y, new Random());
    }

    public EnemyShip(int x, int y, Random rng) {
        super(x, y);
        int count = Weapons.DEPTH_CHARGE_MIN_PER_PASS
                + rng.nextInt(Weapons.DEPTH_CHARGE_MAX_PER_PASS
                - Weapons.DEPTH_CHARGE_MIN_PER_PASS + 1);
        while (dropColumns.size() < count) {
            dropColumns.add(rng.nextInt(Game.FIELD_W));
        }
    }

    @Override
    public void update(Game game) {
        if (++moveTicks >= Weapons.SHIP_MOVE_INTERVAL) {
            moveLeft(Weapons.ENEMY_SPEED);
            moveTicks = 0;
        }
    }

    @Override
    public void tryAttack(Game game) {
        if (!isAlive()) {
            return;
        }
        int dropX = x + width() / 2;
        if (dropColumns.remove(dropX)) {
            game.addProjectile(new DepthCharge(dropX, y + 1));
        }
    }

    @Override
    public String sprite() {
        return "__/\\__";
    }
}
