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
    private boolean damaged = false;

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

    public boolean hasDamaged() {
        return damaged;
    }

    public void markDamaged() {
        damaged = true;
    }

    /** True when the blast circle touches the given box. */
    public boolean overlapsBox(int left, int top, int right, int bottom) {
        int radius = blastRadius();
        return Math.abs(x - (left + right) / 2.0) <= radius + (right - left) / 2.0
                && Math.abs(y - (top + bottom) / 2.0) <= radius + (bottom - top) / 2.0;
    }

    public int blastRadius() {
        return (Weapons.EXPLOSION_MAX_DAMAGE - 1) / Weapons.EXPLOSION_DAMAGE_STEP;
    }

    public String sprite() {
        return "*";
    }
}
