/**
 * Depth charge
 */
public class DepthCharge extends Projectile {

    public DepthCharge(int x, int y) {
        super(x, y, 0, Weapons.DEPTH_CHARGE_FALL_SPEED);
    }

    @Override
    public String sprite() {
        return "o";
    }
}
