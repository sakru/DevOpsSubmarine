/**
 * Depth charge
 */
public class DepthCharge extends Projectile {

    public DepthCharge(int x, int y) {
        super(x, y, 0, Weapons.DEPTH_CHARGE_FALL_SPEED, Weapons.DEPTH_CHARGE_FALL_INTERVAL);
    }

    @Override
    public void hit(Game game, int hitX, int hitY, boolean hitHull) {
        kill();
        game.addExplosion(new Explosion(hitX, hitY));
    }

    @Override
    public String sprite() {
        return "o";
    }

    @Override
    protected boolean scrollsWithWorld() {
        return false;
    }
}
