/**
 * Explosion area damage
 *
 * Damage at distance d: EXPLOSION_MAX_DAMAGE - d x EXPLOSION_DAMAGE_STEP
 */
public class Explosion {

    private int x;
    private final int y;
    private final boolean damagesPlayer;
    private int framesLeft = Weapons.EXPLOSION_VISUAL_FRAMES;
    private boolean damaged = false;

    public Explosion(int x, int y) {
        this(x, y, true);
    }

    public Explosion(int x, int y, boolean damagesPlayer) {
        this.x = x;
        this.y = y;
        this.damagesPlayer = damagesPlayer;
    }

    public void tick() {
        framesLeft--;
    }

    public void scrollLeft(int columns) {
        x -= columns;
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

    public int damageOnceAtBox(int left, int top, int right, int bottom) {
        if (damaged || isDone()) {
            return 0;
        }
        damaged = true;
        if (!damagesPlayer) {
            return 0;
        }
        int closestX = Math.max(left, Math.min(right, x));
        int closestY = Math.max(top, Math.min(bottom, y));
        return damageAt(closestX, closestY);
    }

    public String sprite() {
        return "*";
    }
}
