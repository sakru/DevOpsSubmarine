/**
 * Weapons.
 *
 */
public final class Weapons {

    public static final int TORPEDO_SPEED = 3;
    public static final int DEPTH_CHARGE_FALL_SPEED = 1;
    public static final double DEPTH_CHARGE_FALL_INTERVAL = 16.0 / 1.2;
    public static final int TORPEDO_DAMAGE = 50;
    public static final int TORPEDO_PENETRATION = 8;
    public static final int ENEMY_SPEED = 1;
    public static final int FORWARD_MOVE_INTERVAL = 3;
    public static final int PLAYER_FORWARD_SPEED = 2;
    public static final int MAX_PLAYER_FORWARD_SPEED = 4;
    // Screen movement per forward tick: enemy submarines pass more slowly than the seabed.
    public static final int ENEMY_SUB_FORWARD_SPEED = PLAYER_FORWARD_SPEED / 2;
    public static final int SHIP_MOVE_INTERVAL = 3;
    public static final int TORPEDO_COOLDOWN = 3 * Game.TICKS_PER_SECOND;
    public static final int SONAR_INTERVAL = 4 * Game.TICKS_PER_SECOND;
    public static final int SONAR_ECHO_TICKS = 2 * Game.TICKS_PER_SECOND;
    public static final int SONAR_WAVE_SPEED = 2;
    public static final int ENEMY_DIVE_INTERVAL = 6;
    public static final int DEPTH_CHARGE_MIN_PER_PASS = 3;
    public static final int DEPTH_CHARGE_MAX_PER_PASS = 7;
    public static final double SUB_SPEED = 0.25;
    public static final int EXPLOSION_MAX_DAMAGE = 50;
    public static final int EXPLOSION_DAMAGE_STEP = 25;
    // Terminal cells are roughly twice as tall as they are wide.
    public static final int EXPLOSION_COLUMN_SCALE = 2;
    public static final int EXPLOSION_VISUAL_FRAMES = 8;
    public static final int RAM_DAMAGE = 25;
    public static final int GROUND_DAMAGE = 5;

    private Weapons() {
    }
}
