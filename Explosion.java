/*
 * Keeps track of one explosion on screen: its position, animation and remaining lifetime.
 * ExplosionWorker supplies the blast calculations and animation frames.
 */
import java.util.List;

public class Explosion {

    private int x;
    private final int y;
    private final boolean damagesPlayer;
    private int framesLeft = Weapons.EXPLOSION_VISUAL_FRAMES;
    private int radius;
    private List<List<String>> frames = List.of(List.of("*"));

    public Explosion(int x, int y) {
        this(x, y, true);
    }

    public Explosion(int x, int y, boolean damagesPlayer) {
        this.x = x;
        this.y = y;
        this.damagesPlayer = damagesPlayer;
    }

    public Explosion(ExplosionWorker.Zone zone, int scroll, int elapsedFrames) {
        this(zone.x() - scroll, zone.y(), false);
        radius = zone.radius();
        frames = zone.frames();
        framesLeft -= elapsedFrames;
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

    public boolean damagesPlayer() {
        return damagesPlayer;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getRadius() {
        return radius;
    }

    public int getHorizontalRadius() {
        return radius * Weapons.EXPLOSION_COLUMN_SCALE;
    }

    public List<String> spriteLines() {
        if (isDone()) return List.of();
        int age = Weapons.EXPLOSION_VISUAL_FRAMES - framesLeft;
        int phase = Math.min(frames.size() - 1, age * frames.size() / Weapons.EXPLOSION_VISUAL_FRAMES);
        return frames.get(phase);
    }
}
