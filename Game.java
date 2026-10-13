
public class Game {

    public static final int FIELD_W = 80;
    public static final int FIELD_H = 24;
    public static final int MAX_DEPTH = 10;

    private int depth = 5;
    private int hull = 100;
    private int level = 1;
    private boolean running = true;

    public void update() {
        // TODO: enemy spawning, movement, collisions
    }

    public void setDepth(int depth) {
        this.depth = Math.max(0, Math.min(MAX_DEPTH, depth));
    }

    public int getDepth() {
        return depth;
    }

    public int getHull() {
        return hull;
    }

    public int getLevel() {
        return level;
    }

    public boolean isRunning() {
        return running;
    }

    public void stop() {
        running = false;
    }
}
