/**
 * Torpedo
 */
public class Torpedo extends Projectile {

    public Torpedo(int x, int y) {
        super(x, y, -Weapons.TORPEDO_SPEED, 0);
    }

    @Override
    public String sprite() {
        return "<";
    }
}
