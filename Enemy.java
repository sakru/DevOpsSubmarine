/**
 * Enemy: skeleton för hostile units.
 */
public abstract class Enemy {

    protected int x;
    protected int y;
    private boolean alive = true;

    protected Enemy(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public abstract void update(Game game);

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
