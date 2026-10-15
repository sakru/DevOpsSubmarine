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

    protected void moveLeft(int speed) {
        x -= speed;
        if (x + width() <= 0) {
            kill();
        }
    }

    public int width() {
        int width = 0;
        for (String line : spriteLines()) {
            width = Math.max(width, line.length());
        }
        return width;
    }

    public int height() {
        return spriteLines().length;
    }

    public String[] spriteLines() {
        return sprite().split("\n", -1);
    }

    public void onContact(Game game) {
        kill();
        game.damageHull(Weapons.RAM_DAMAGE);
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
