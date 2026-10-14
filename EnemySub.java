/**
 * EnemySub
 * */
import java.util.Random;

public class EnemySub extends Enemy {

    private int cooldown;

    public EnemySub(int x, int y) {
        super(x, y);
        cooldown = 10 + new Random().nextInt(30);
    }

    @Override
    public void update(Game game) {
        x -= Weapons.ENEMY_SPEED;
        if (x < 0) {
            kill();
            return;
        }
        if (--cooldown <= 0) {
            game.addProjectile(new Torpedo(x, y));
            cooldown = Weapons.TORPEDO_COOLDOWN;
        }
    }

    @Override
    public String sprite() {
        return "<=>";
    }
}
