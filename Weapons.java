/**
 * Weapons.
 *
 */
public final class Weapons {

    public static final int TORPEDO_SPEED = 1;
    public static final int DEPTH_CHARGE_FALL_SPEED = 1;
    public static final double DEPTH_CHARGE_FALL_INTERVAL = 16.0 / 1.2;
    public static final int TORPEDO_DAMAGE = 10;
    public static final int ENEMY_SPEED = 1;
    public static final int FORWARD_MOVE_INTERVAL = 3;
    public static final int PLAYER_FORWARD_SPEED = 2;
    // Screen movement per forward tick: enemy submarines pass more slowly than the seabed.
    public static final int ENEMY_SUB_FORWARD_SPEED = PLAYER_FORWARD_SPEED / 2;
    public static final int SHIP_MOVE_INTERVAL = 3;
    public static final int TORPEDO_COOLDOWN = 30;
    public static final int PLAYER_TORPEDO_COOLDOWN = 3 * Game.TICKS_PER_SECOND;
    public static final int DEPTH_CHARGE_MIN_PER_PASS = 3;
    public static final int DEPTH_CHARGE_MAX_PER_PASS = 7;
    public static final double SUB_SPEED = 0.25;
    public static final int EXPLOSION_MAX_DAMAGE = 75;
    public static final int EXPLOSION_DAMAGE_STEP = 25;
    public static final int EXPLOSION_VISUAL_FRAMES = 3;
    public static final int RAM_DAMAGE = 25;

    private Weapons() {
    }
}
