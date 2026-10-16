/**
 * Projectile: torpedo, depth charge.

 */
public abstract class Projectile {

    protected int x;
    protected int y;
    protected final int vx;
    protected final int vy;
    private int previousX;
    private int previousY;
    private final double moveEveryTicks;
    private double ticksSinceMove;
    private boolean alive = true;

    protected Projectile(int x, int y, int vx, int vy) {
        this(x, y, vx, vy, 1);
    }

    protected Projectile(int x, int y, int vx, int vy, double moveEveryTicks) {
        if (!Double.isFinite(moveEveryTicks) || moveEveryTicks < 1) {
            throw new IllegalArgumentException("Movement interval must be finite and at least one tick");
        }
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        previousX = x;
        previousY = y;
        this.moveEveryTicks = moveEveryTicks;
    }

    public void update(Game game) {
        previousX = x;
        previousY = y;
        if (scrollsWithWorld()) {
            x -= game.getScrollStep();
        }
        if (++ticksSinceMove + 1e-9 < moveEveryTicks) {
            return;
        }
        ticksSinceMove -= moveEveryTicks;
        x += vx;
        y += vy;
    }

    protected boolean scrollsWithWorld() {
        return true;
    }

    public int prevX() {
        return previousX;
    }

    public int prevY() {
        return previousY;
    }

    public abstract void hit(Game game, int hitX, int hitY, boolean hitHull);

    public boolean isPlayerOwned() {
        return false;
    }

    public abstract String sprite();

    public int width() {
        return sprite().length();
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isAlive() {
        return alive;
    }

    public void kill() {
        alive = false;
    }
}
