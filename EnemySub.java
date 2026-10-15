/**
 * EnemySub: enemy submarine, moves left and fires torpedoes.
 *
 * Spawned by the game with its shared RNG, so runs are reproducible
 * for a given seed.
 */
import java.util.Random;

public class EnemySub extends Enemy implements WeaponCarrier {

    private static final String SPRITE = "        ___          \n"
            + "  _____/___\\______   \n"
            + "<=|__o___o___o_____|>";
    public static final int HEIGHT = SPRITE.split("\n").length;
    private int cooldown;

    public EnemySub(int x, int y) {
        this(x, y, new Random());
    }

    public EnemySub(int x, int y, Random rng) {
        super(x, y);
        cooldown = 10 + rng.nextInt(30);
    }

    @Override
    public void update(Game game) {
        if (game.getScrollStep() > 0) {
            moveLeft(Weapons.PLAYER_FORWARD_SPEED + Weapons.ENEMY_SUB_FORWARD_SPEED);
        }
    }

    @Override
    public void tryAttack(Game game) {
        if (!isAlive()) {
            return;
        }
        if (--cooldown <= 0) {
            game.addProjectile(new Torpedo(x - 1, y + height() / 2));
            cooldown = Weapons.TORPEDO_COOLDOWN;
        }
    }

    @Override
    public String sprite() {
        return SPRITE;
    }
}
