import java.util.List;

/** Explosion position, appearance and lifetime. */
public class Explosion {

    private int x;
    private final int y;
    private final boolean damagesPlayer;
    private int framesLeft = Weapons.EXPLOSION_VISUAL_FRAMES;
    private int radius;
    private List<String> sprite = List.of("*");

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
        sprite = zone.sprite();
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

    public List<String> spriteLines() {
        return sprite;
    }
}
