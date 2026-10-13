/**
 * Explosion area damage
 *
 * Damage at distance d: EXPLOSION_MAX_DAMAGE - d x EXPLOSION_DAMAGE_STEP
 */
public class Explosion {

    private static final int DURATION_FRAMES = 2;

    private final int x;
    private final int y;
    private int framesLeft = DURATION_FRAMES;

    public Explosion(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void tick() {
        framesLeft--;
    }

    public boolean isDone() {
        return framesLeft <= 0;
    }

    public int damageAt(int cx, int cy) {
        int dist = Math.max(Math.abs(cx - x), Math.abs(cy - y));
        return Math.max(0, Weapons.EXPLOSION_MAX_DAMAGE - dist * Weapons.EXPLOSION_DAMAGE_STEP);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public String sprite() {
        return "*";
    }
}
