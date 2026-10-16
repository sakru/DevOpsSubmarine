/**
 * Game: game state and rules.
 */
import java.util.ArrayList;
import java.util.Random;

public class Game {

    public static final int FIELD_W = 160;
    public static final int FIELD_H = 36;
    public static final int MAX_DEPTH = 15;
    public static final int SUB_COL = 10;
    public static final int TOP_ROW = 3;
    public static final int BOTTOM_ROW = FIELD_H - 3;
    /** Every generated level leaves room for the entire hull at this depth. */
    public static final int SAFE_DEPTH = 5;
    public static final int TICKS_PER_SECOND = 25;
    public static final int SECONDS_PER_LEVEL = 15;
    public static final int MAX_LEVEL = 12;
    public static final int MIN_LEVEL = 1;
    public static final int START_DEPTH = 5;
    public static final int SHIP_ROW = 1;
    public static final int MAX_ENEMY_SUBS = 2;

    public enum StopReason { HULL_BROKEN, QUIT, EOF, INTERRUPTED }

    private static final double ROWS_PER_DEPTH = (FIELD_H - 2.0 * TOP_ROW) / MAX_DEPTH;
    /** First ground row must be below the bottom of the hull at SAFE_DEPTH. */
    public static final int MIN_FLOOR_ROW = TOP_ROW
            + (int) Math.round(SAFE_DEPTH * ROWS_PER_DEPTH)
            + Submarine.OFFSET_Y + Submarine.HEIGHT;
    private static final int TICKS_PER_LEVEL = TICKS_PER_SECOND * SECONDS_PER_LEVEL;
    private static final double INITIAL_SPAWN_SECONDS = 2.4;
    private static final double MIN_SPAWN_SECONDS = 0.8;
    private static final double SPAWN_STEP_SECONDS = 0.2;
    private static final int SPAWN_ROW_TOP = 4;
    private static final int SPAWN_ROW_BOTTOM = FIELD_H - 7;
    private static final int MIN_SHIP_RESPAWN_SECONDS = 1;
    private static final int MAX_SHIP_RESPAWN_SECONDS = 10;

    private int targetDepth = START_DEPTH;
    private double depth = START_DEPTH;
    private int hull = 100;
    private int level = MIN_LEVEL;
    private boolean running = true;
    private StopReason stopReason;

    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private final ArrayList<Projectile> projectiles = new ArrayList<>();
    private final ArrayList<Explosion> explosions = new ArrayList<>();

    private SeaFloor floor;

    private final Random rng;
    private final boolean autoLevel;
    private final ExplosionWorker explosionWorker;

    private int frame = 0;
    private int spawnEvery = 60;
    private int spawnCounter = 0;
    private EnemyShip activeShip;
    private int shipSpawnCooldown;
    private int scrollStep;
    private long distance;
    private int torpedoCooldown;
    private int kills;

    public Game() {
        this(new Random().nextLong());
    }

    public Game(ExplosionWorker explosionWorker) {
        this(new Random().nextLong(), true, explosionWorker);
    }

    public Game(long seed) {
        this(seed, true);
    }

    public Game(long seed, boolean autoLevel) {
        this(seed, autoLevel, null);
    }

    public Game(long seed, boolean autoLevel, ExplosionWorker explosionWorker) {
        this.rng = new Random(seed);
        this.autoLevel = autoLevel;
        this.explosionWorker = explosionWorker;
        applyLevel(MIN_LEVEL);
        clampToWater();
        shipSpawnCooldown = nextShipSpawnCooldown();
    }

    public SeaFloor getFloor() {
        return floor;
    }

    public Random getRng() {
        return rng;
    }

    public boolean isAutoLevel() {
        return autoLevel;
    }

    public void setLevel(int level) {
        int l = Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level));
        if (l == this.level) {
            return;
        }
        this.level = l;
        applyLevel(l);
        clampToWater();
        discardBuriedObjects();
    }

    private void applyLevel(int l) {
        floor = new SeaFloor(l, rng);
        double spawnSeconds = Math.max(MIN_SPAWN_SECONDS,
                INITIAL_SPAWN_SECONDS - (l - MIN_LEVEL) * SPAWN_STEP_SECONDS);
        spawnEvery = (int) Math.round(TICKS_PER_SECOND * spawnSeconds);
        spawnCounter = 0;
    }

    private void autoLevelUp() {
        if (autoLevel && level < MAX_LEVEL && frame % TICKS_PER_LEVEL == 0) {
            setLevel(level + 1);
        }
    }

    /** Keeps the sub in the water: within field, above the floor, never negative. */
    private void clampToWater() {
        int maxD = maxDepth();
        targetDepth = Math.max(0, Math.min(maxD, targetDepth));
        depth = Math.max(0, Math.min(maxD, depth));
    }

    /** A new seabed removes buried objects without causing combat damage. */
    private void discardBuriedObjects() {
        for (Enemy enemy : enemies) {
            if (!isAboveFloor(enemy.getX(), enemy.getX() + enemy.width() - 1,
                    enemy.getY(), enemy.height())) {
                enemy.kill();
            }
        }
        for (Projectile projectile : projectiles) {
            if (!isAboveFloor(projectile.getX(), projectile.getX() + projectile.width() - 1,
                    projectile.getY())) {
                projectile.kill();
            }
        }
        explosions.removeIf(ex -> !isAboveFloor(ex.getX(), ex.getX(), ex.getY()));
        removeDead();
    }

    /** Partly visible sprites are checked across all their visible columns. */
    private boolean isAboveFloor(int left, int right, int row) {
        return isAboveFloor(left, right, row, 1);
    }

    private boolean isAboveFloor(int left, int right, int row, int height) {
        int visibleLeft = Math.max(0, left);
        int visibleRight = Math.min(FIELD_W - 1, right);
        int bottom = row + height - 1;
        if (row < 1 || bottom > FIELD_H || visibleLeft > visibleRight) {
            return false;
        }
        return bottom < highestFloorRow(visibleLeft, visibleRight);
    }

    private int highestFloorRow(int left, int right) {
        int topFloor = FIELD_H;
        for (int col = left; col <= right; col++) {
            topFloor = Math.min(topFloor, floor.heightAt(col));
        }
        return topFloor;
    }

    public void update() {
        if (!running) {
            return;
        }
        frame++;
        advanceForward();
        if (torpedoCooldown > 0) {
            torpedoCooldown--;
        }
        moveDepth();
        spawnEnemies();
        for (Enemy e : enemies) {
            if (e.isAlive()) {
                e.update(this);
                if (!isAboveFloor(e.getX(), e.getX() + e.width() - 1, e.getY(), e.height())) {
                    e.kill();
                }
                if (e.isAlive() && e instanceof WeaponCarrier carrier) {
                    carrier.tryAttack(this);
                }
            }
        }
        for (Projectile p : projectiles) {
            if (p.isAlive()) {
                p.update(this);
            }
        }
        for (Explosion ex : explosions) {
            ex.tick();
        }
        checkCollisions();
        collectExplosionZones();
        removeDead();
        if (hull <= 0) {
            hull = 0;
            stop(StopReason.HULL_BROKEN);
        } else {
            autoLevelUp();
        }
    }

    public void diveUp() {
        targetDepth = Math.max(0, targetDepth - 1);
    }

    public void fireTorpedo() {
        if (running && hull > 0 && torpedoCooldown == 0) {
            addProjectile(new Torpedo(subBoxRight() + 1, subRow(), true));
            torpedoCooldown = Weapons.PLAYER_TORPEDO_COOLDOWN;
        }
    }

    private void advanceForward() {
        scrollStep = frame % Weapons.FORWARD_MOVE_INTERVAL == 0
                ? Weapons.PLAYER_FORWARD_SPEED : 0;
        distance += scrollStep;
        floor.scrollLeft(scrollStep, rng);
        for (Explosion explosion : explosions) {
            explosion.scrollLeft(scrollStep);
        }
    }

    public int getScrollStep() {
        return scrollStep;
    }

    public long getDistance() {
        return distance;
    }

    public int getKills() {
        return kills;
    }

    public void diveDown() {
        targetDepth = Math.min(maxDepth(), targetDepth + 1);
    }

    private void moveDepth() {
        clampToWater();
        double d = targetDepth - depth;
        if (d == 0) {
            return;
        }
        double step = Math.min(Weapons.SUB_SPEED, Math.abs(d));
        depth += Math.signum(d) * step;
    }

    /** Rows from the anchor to the bottom of the hull box. */
    private static int bottomOffset() {
        return Submarine.HEIGHT - 1 + Submarine.OFFSET_Y;
    }

    private int maxDepthAt(int leftCol, int rightCol) {
        int topFloor = highestFloorRow(leftCol, rightCol);
        int maxAnchorRow = Math.min(BOTTOM_ROW, topFloor - 1 - bottomOffset());
        return Math.max(0, rowToDepthFloor(maxAnchorRow));
    }


    private int maxDepth() {
        return maxDepthAt(subBoxLeft(), subBoxRight());
    }

    private void spawnEnemies() {
        spawnShip();
        spawnCounter++;
        if (spawnCounter < spawnEvery) {
            return;
        }
        spawnCounter = 0;
        int type = rng.nextInt(6);
        if (type == 0 || type >= 4) {
            return;
        }
        if (type == 1 && enemies.stream()
                .filter(e -> e.isAlive() && e instanceof EnemySub).count() >= MAX_ENEMY_SUBS) {
            return;
        }
        int height = type == 1 ? EnemySub.HEIGHT : 1;
        int bottom = Math.min(SPAWN_ROW_BOTTOM, highestFloorRow(0, FIELD_W - 1) - height);
        int y = SPAWN_ROW_TOP + rng.nextInt(bottom - SPAWN_ROW_TOP + 1);
        if (type == 1) {
            addEnemy(new EnemySub(FIELD_W - 1, y, rng));
        } else {
            addEnemy(new EnemyMine(FIELD_W - 1, y));
        }
    }

    private void spawnShip() {
        if (activeShip == null && --shipSpawnCooldown <= 0) {
            addEnemy(new EnemyShip(FIELD_W - 1, SHIP_ROW, rng));
        }
    }

    private int nextShipSpawnCooldown() {
        int seconds = MIN_SHIP_RESPAWN_SECONDS
                + rng.nextInt(MAX_SHIP_RESPAWN_SECONDS - MIN_SHIP_RESPAWN_SECONDS + 1);
        return seconds * TICKS_PER_SECOND;
    }

    private void checkCollisions() {
        checkProjectileCollisions();
        checkEnemyCollisions();
    }

    private void checkProjectileCollisions() {
        for (Projectile p : projectiles) {
            if (p.isAlive()) {
                checkProjectilePath(p);
            }
        }
    }

    private void checkProjectilePath(Projectile projectile) {
        int dx = projectile.getX() - projectile.prevX();
        int dy = projectile.getY() - projectile.prevY();
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        int width = projectile.width();
        for (int step = 0; step <= steps; step++) {
            double fraction = steps == 0 ? 0 : (double) step / steps;
            int x = projectile.prevX() + (int) Math.round(dx * fraction);
            int y = projectile.prevY() + (int) Math.round(dy * fraction);
            if (x + width <= 0 || x >= FIELD_W || y < 1 || y > FIELD_H) {
                projectile.kill();
                return;
            }
            for (int part = 0; part < width; part++) {
                int hitX = x + (projectile.isPlayerOwned() ? width - part - 1 : part);
                if (hitX >= 0 && hitX < FIELD_W && hitProjectileAt(projectile, hitX, y)) {
                    return;
                }
            }
        }
    }

    private boolean hitProjectileAt(Projectile projectile, int x, int y) {
        if (y >= floor.heightAt(x)) {
            projectile.hit(this, x, y, false);
            return true;
        }
        if (projectile.isPlayerOwned()) {
            return hitEnemyAt(projectile, x, y);
        }
        if (pointInBox(x, y, subBoxLeft(), subBoxTop(subRow()),
                subBoxRight(), subBoxBottom(subRow()))) {
            projectile.hit(this, x, y, true);
            return true;
        }
        return false;
    }

    private boolean hitEnemyAt(Projectile projectile, int x, int y) {
        for (Enemy enemy : enemies) {
            if (enemy.isAlive() && pointInBox(x, y, enemy.getX(), enemy.getY(),
                    enemy.getX() + enemy.width() - 1, enemy.getY() + enemy.height() - 1)) {
                enemy.kill();
                projectile.hit(this, x, y, false);
                kills++;
                return true;
            }
        }
        return false;
    }

    private void checkEnemyCollisions() {
        for (Enemy e : enemies) {
            if (!e.isAlive()) {
                continue;
            }
            int el = e.getX();
            int er = e.getX() + e.width() - 1;
            if (boxesOverlap(subBoxLeft(), subBoxTop(subRow()),
                    subBoxRight(), subBoxBottom(subRow()), el, e.getY(), er,
                    e.getY() + e.height() - 1)) {
                e.onContact(this);
            }
        }
    }

    public void damageHull(int damage) {
        if (damage < 0) {
            throw new IllegalArgumentException("Damage cannot be negative");
        }
        hull = Math.max(0, hull - damage);
    }

    private void removeDead() {
        enemies.removeIf(e -> !e.isAlive());
        projectiles.removeIf(p -> !p.isAlive());
        explosions.removeIf(ex -> ex.isDone() || ex.getX() + ex.getRadius() < 0
                || ex.getX() - ex.getRadius() >= FIELD_W);
        if (activeShip != null && !activeShip.isAlive()) {
            activeShip = null;
            shipSpawnCooldown = nextShipSpawnCooldown();
        }
    }

    public void addEnemy(Enemy enemy) {
        if (enemy instanceof EnemyShip ship) {
            if (activeShip != null && activeShip.isAlive()) {
                throw new IllegalArgumentException("Only one surface ship can be active");
            }
            activeShip = ship;
        }
        enemies.add(enemy);
    }

    public void addProjectile(Projectile projectile) {
        projectiles.add(projectile);
    }

    public void addExplosion(Explosion explosion) {
        // Capture the hull at impact so worker timing cannot change the damage.
        int hullX = Math.max(subBoxLeft(), Math.min(subBoxRight(), explosion.getX()));
        int hullY = Math.max(subBoxTop(subRow()), Math.min(subBoxBottom(subRow()), explosion.getY()));
        ExplosionWorker.Impact impact = new ExplosionWorker.Impact(explosion.getX(), explosion.getY(),
                hullX, hullY, explosion.damagesPlayer(), distance, frame, level);
        if (explosionWorker == null) {
            acceptExplosionZone(ExplosionWorker.prepare(impact));
        } else {
            explosionWorker.submit(impact);
        }
    }

    private void collectExplosionZones() {
        if (explosionWorker == null) return;
        ExplosionWorker.Zone zone;
        while ((zone = explosionWorker.pollZone()) != null) {
            acceptExplosionZone(zone);
        }
    }

    private void acceptExplosionZone(ExplosionWorker.Zone zone) {
        damageHull(zone.damage());
        Explosion explosion = new Explosion(zone, (int) (distance - zone.impact().distance()),
                frame - zone.impact().frame());
        if (!explosion.isDone() && zone.impact().level() == level) {
            explosions.add(explosion);
        }
    }

    public Iterable<Enemy> getEnemies() {
        return enemies;
    }

    public Iterable<Projectile> getProjectiles() {
        return projectiles;
    }

    public Iterable<Explosion> getExplosions() {
        return explosions;
    }

    // --- geometry -----------------------------------------------------------

    public int subBoxLeft() {
        return SUB_COL + Submarine.OFFSET_X;
    }

    public int subBoxRight() {
        return subBoxLeft() + Submarine.WIDTH - 1;
    }

    public int subBoxTop(int anchorRow) {
        return anchorRow + Submarine.OFFSET_Y;
    }

    public int subBoxBottom(int anchorRow) {
        return subBoxTop(anchorRow) + Submarine.HEIGHT - 1;
    }

    public boolean isAlive() {
        return hull > 0;
    }

    public int getDepth() {
        return (int) Math.round(depth);
    }

    public double getExactDepth() {
        return depth;
    }

    public int getHull() {
        return hull;
    }

    public int getLevel() {
        return level;
    }

    /** Whole seconds of survival. */
    public int getSurvivalSeconds() {
        return frame / TICKS_PER_SECOND;
    }

    public int getFrame() {
        return frame;
    }

    public int getScore() {
        return getSurvivalSeconds();
    }

    public int getSpawnEvery() {
        return spawnEvery;
    }

    public boolean isRunning() {
        return running;
    }

    public StopReason getStopReason() {
        return stopReason;
    }

    public String getStopMessage() {
        if (stopReason == null) {
            return null;
        }
        return switch (stopReason) {
            case HULL_BROKEN -> "Hull broken: destroyed at depth " + getDepth();
            case QUIT -> "Quit: player pressed 0";
            case EOF -> "Input stream ended (EOF)";
            case INTERRUPTED -> "Interrupted";
        };
    }

    public void stop() {
        stop(StopReason.QUIT);
    }

    public void stop(StopReason reason) {
        running = false;
        if (stopReason == null) {
            stopReason = reason;
        }
    }

    public int subRow() {
        return depthToRow(depth);
    }


    public int depthToRow(double d) {
        int r = TOP_ROW + (int) Math.round(d * ROWS_PER_DEPTH);
        return Math.max(TOP_ROW, Math.min(BOTTOM_ROW, r));
    }

    public int rowToDepth(int row) {
        return (int) Math.round((row - TOP_ROW) / ROWS_PER_DEPTH);
    }

    public int rowToDepthFloor(int row) {
        int d = (int) Math.floor((row - TOP_ROW) / ROWS_PER_DEPTH + 1e-9);
        return Math.max(0, d);
    }

    public int getMaxSafeDepth() {
        return maxDepth();
    }

    // --- collision helpers ----------------------------------------------------

    private static boolean boxesOverlap(int x1, int y1, int x2, int y2,
                                        int x3, int y3, int x4, int y4) {
        return x1 <= x4 && x3 <= x2 && y1 <= y4 && y3 <= y2;
    }

    private static boolean pointInBox(int px, int py,
                                      int left, int top, int right, int bottom) {
        return px >= left && px <= right && py >= top && py <= bottom;
    }

}
