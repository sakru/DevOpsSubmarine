/**
 * Projectile: torpedo, depth charge.

 */
public abstract class Projectile {

    protected int x;
    protected int y;
    protected final int vx;
    protected final int vy;
    private boolean alive = true;

    protected Projectile(int x, int y, int vx, int vy) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
    }

    public void update(Game game) {
        x += vx;
        y += vy;
        if (x < 0 || x >= Game.FIELD_W || y < 1 || y > Game.FIELD_H) {
            kill();
        }
    }

    public int prevX() {
        return x - vx;
    }

    public int prevY() {
        return y - vy;
    }

    public abstract String sprite();

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
