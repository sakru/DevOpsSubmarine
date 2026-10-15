/**
 * Game: game state and rules.

 */
import java.util.ArrayList;
import java.util.Random;

public class Game {

    public static final int FIELD_W = 80;
    public static final int FIELD_H = 24;
    public static final int MAX_DEPTH = 10;
    public static final int SUB_COL = 10;
    public static final int TOP_ROW = 3;
    public static final int BOTTOM_ROW = FIELD_H - 3;
    public static final int SAFE_DEPTH = 5;
    public static final int SECONDS_PER_LEVEL = 15;
    public static final int MAX_LEVEL = 12;
    public static final int MIN_LEVEL = 1;
    public static final int START_DEPTH = 5;
    public static final int SHIP_ROW_OFFSET = 2;

    public enum StopReason { HULL_BROKEN, QUIT, EOF, INTERRUPTED }

    private static final double ROWS_PER_DEPTH = (FIELD_H - 2.0 * TOP_ROW) / MAX_DEPTH;
    private static final int SPAWN_ROW_TOP = 4;
    private static final int SPAWN_ROW_BOTTOM = 17;

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

    private int frame = 0;
    private int spawnEvery = 60;
    private int spawnCounter = 0;

    public Game() {
        this(1L);
    }

    public Game(long seed) {
        this(seed, true);
    }

    public Game(long seed, boolean autoLevel) {
        this.rng = new Random(seed);
        this.autoLevel = autoLevel;
        this.floor = new SeaFloor(MIN_LEVEL, rng);
        applyLevel(MIN_LEVEL);
        clampToWater();
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
    }

    private void applyLevel(int l) {
        floor = new SeaFloor(l, rng);
        spawnEvery = Math.max(20, 60 - (l - MIN_LEVEL) * 5);
        spawnCounter = 0;
    }

    private void autoLevelUp() {
        if (autoLevel && level < MAX_LEVEL && (frame / 60) % SECONDS_PER_LEVEL == 0) {
            setLevel(level + 1);
        }
    }

    /** Keeps the sub in the water: within field, above the floor, never negative. */
    private void clampToWater() {
        double maxD = maxDepthAt(subBoxLeft(), subBoxRight());
        if (maxD < 0) {
            maxD = 0;
        }
        targetDepth = (int) Math.max(0, Math.min(maxD, targetDepth));
        depth = Math.max(0, Math.min(maxD, depth));
    }

    public void update() {
        if (!running) {
            return;
        }
        frame++;
        moveDepth();
        spawnEnemies();
        for (Enemy e : enemies) {
            e.update(this);
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
        removeDead();
        autoLevelUp();
        if (hull <= 0) {
            hull = 0;
            stop(StopReason.HULL_BROKEN);
        }
    }

    public void diveUp() {
        targetDepth = Math.max(0, targetDepth - 1);
    }

    public void diveDown() {
        targetDepth = (int) Math.min(maxDepth(), targetDepth + 1);
    }

    private void moveDepth() {
        double maxD = Math.max(0, maxDepthAt(subBoxLeft(), subBoxRight()));
        targetDepth = (int) Math.min(targetDepth, maxD);
        double d = targetDepth - depth;
        if (d == 0) {
            return;
        }
        double step = Math.min(Weapons.SUB_SPEED, Math.abs(d));
        depth += Math.signum(d) * step;
        depth = Math.min(depth, maxD);
    }

    /** Rows from the anchor to the bottom of the hull box. */
    private static int bottomOffset() {
        return Weapons.SUB_H - 1 + Weapons.SUB_OFFSET_Y; // 5 - 1 + (-2) = 2
    }

    private double maxDepthAt(int leftCol, int rightCol) {
        int topFloor = Integer.MAX_VALUE;
        for (int c = leftCol; c <= rightCol; c++) {
            topFloor = Math.min(topFloor, floor.heightAt(c));
        }
        int maxAnchorRow = Math.min(BOTTOM_ROW, topFloor - 1 - bottomOffset());
        return Math.max(0, rowToDepthFloor(maxAnchorRow));
    }


    private double maxDepth() {
        return maxDepthAt(subBoxLeft(), subBoxRight());
    }

    private void spawnEnemies() {
        spawnCounter++;
        if (spawnCounter < spawnEvery) {
            return;
        }
        spawnCounter = 0;
        boolean ship = rng.nextBoolean();
        if (ship) {
            int row = floor.heightAt(FIELD_W - 1) + SHIP_ROW_OFFSET;
            row = Math.min(row, FIELD_H);
            addEnemy(new EnemyShip(FIELD_W - 1, row, rng));
        } else {
            int y = SPAWN_ROW_TOP + rng.nextInt(SPAWN_ROW_BOTTOM - SPAWN_ROW_TOP + 1);
            addEnemy(new EnemySub(FIELD_W - 1, y, rng));
        }
    }

    private void checkCollisions() {
        int subRow = subRow();
        int left = subBoxLeft();
        int right = subBoxRight();
        int top = subBoxTop(subRow);
        int bottom = subBoxBottom(subRow);

        for (Projectile p : projectiles) {
            if (!p.isAlive() || !(p instanceof Torpedo)) {
                continue;
            }
            int px = p.prevX();
            int py = p.prevY();
            int x = p.getX();
            int y = p.getY();
            if (segmentHitsBox(px, py, x, y, left, top, right, bottom)) {
                p.kill();
                hull -= Weapons.TORPEDO_DAMAGE;
            }
        }

        for (Projectile p : projectiles) {
            if (!p.isAlive() || !(p instanceof DepthCharge)) {
                continue;
            }
            int x = p.getX();
            int y = p.getY();
            if (pointInBox(x, y, left, top, right, bottom)) {
                p.kill();
                addExplosion(new Explosion(x, y));
            } else if (y >= floor.heightAt(x)) {
                p.kill();
                int ex = Math.max(1, Math.min(FIELD_H - 1, y));
                addExplosion(new Explosion(x, ex));
            }
        }

        for (Explosion ex : explosions) {
            if (!ex.hasDamaged() && ex.overlapsBox(left, top, right, bottom)) {
                ex.markDamaged();
                hull -= ex.damageAt(SUB_COL, subRow);
            }
        }

        for (Enemy e : enemies) {
            if (!e.isAlive()) {
                continue;
            }
            int el = e.getX();
            int er = e.getX() + e.sprite().length() - 1;
            int et = e.getY();
            int eb = e.getY();
            if (boxesOverlap(left, top, right, bottom, el, et, er, eb)) {
                e.kill();
                hull -= Weapons.RAM_DAMAGE;
            }
        }
    }

    private void removeDead() {
        enemies.removeIf(e -> !e.isAlive());
        projectiles.removeIf(p -> !p.isAlive());
        explosions.removeIf(Explosion::isDone);
    }

    public void addEnemy(Enemy enemy) {
        enemies.add(enemy);
    }

    public void addProjectile(Projectile projectile) {
        projectiles.add(projectile);
    }

    public void addExplosion(Explosion explosion) {
        explosions.add(explosion);
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
        return SUB_COL + Weapons.SUB_OFFSET_X;
    }

    public int subBoxRight() {
        return SUB_COL + Weapons.SUB_OFFSET_X + Weapons.SUB_W - 1;
    }

    public int subBoxTop(int anchorRow) {
        return anchorRow + Weapons.SUB_OFFSET_Y;
    }

    public int subBoxBottom(int anchorRow) {
        return anchorRow + Weapons.SUB_OFFSET_Y + Weapons.SUB_H - 1;
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
        return frame / 60;
    }

    public int getFrame() {
        return frame;
    }

    public int getScore() {
        return frame / 60;
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
        return (int) Math.floor(maxDepthAt(subBoxLeft(), subBoxRight()) + 1e-9);
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

    private static boolean segmentHitsBox(int x1, int y1, int x2, int y2,
                                          int left, int top, int right, int bottom) {
        int minX = Math.min(x1, x2) - 1;
        int maxX = Math.max(x1, x2) + 1;
        int minY = Math.min(y1, y2) - 1;
        int maxY = Math.max(y1, y2) + 1;
        return boxesOverlap(minX, minY, maxX, maxY, left, top, right, bottom);
    }
}
