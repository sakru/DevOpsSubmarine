/**
 * Game: game state and rules. No rendering, no input.
 */
import java.util.ArrayList;

public class Game {

    public static final int FIELD_W = 80;
    public static final int FIELD_H = 24;
    public static final int MAX_DEPTH = 10;
    public static final int SUB_COL = 10;

    private int targetDepth = 5;
    private double depth = 5;
    private int hull = 100;
    private int level = 1;
    private boolean running = true;

    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private final ArrayList<Projectile> projectiles = new ArrayList<>();
    private final ArrayList<Explosion> explosions = new ArrayList<>();

    private SeaFloor floor = new SeaFloor(level);

    private int frame = 0;
    private int spawnEvery = 60;

    public SeaFloor getFloor() {
        return floor;
    }

    public void setLevel(int level) {
        this.level = level;
        floor = new SeaFloor(level);
        spawnEvery = Math.max(20, 60 - level * 5);
    }

    public void update() {
        moveDepth();
        spawnEnemies();
        for (Enemy e : enemies) {
            e.update(this);
        }
        for (Projectile p : projectiles) {
            p.update(this);
        }
        for (Explosion ex : explosions) {
            ex.tick();
        }
        checkCollisions();
        removeDead();
        if (hull <= 0) {
            hull = 0;
            stop();
        }
    }

    public void diveUp() {
        targetDepth = Math.max(0, targetDepth - 1);
    }

    public void diveDown() {
        targetDepth = (int) Math.min(maxDepth(), targetDepth + 1);
    }

    private void moveDepth() {
        targetDepth = (int) Math.min(targetDepth, maxDepth());
        double d = targetDepth - depth;
        if (d == 0) {
            return;
        }
        depth += Math.signum(d) * Math.min(Weapons.SUB_SPEED, Math.abs(d));
    }

    private double maxDepth() {
        return Math.floor(rowToDepth(floor.heightAt(SUB_COL) - 3) + 1e-9);
    }
    private void spawnEnemies() {
        frame++;
        if (frame % spawnEvery != 0) {
            return;
        }
        int y = 4 + (int) (Math.random() * (FIELD_H - 10));
        addEnemy(new EnemySub(FIELD_W - 1, y));
    }

    private void checkCollisions() {
        int subRow = subRow();
        for (Projectile p : projectiles) {
            if (p.isAlive() && p.getX() == SUB_COL && Math.abs(p.getY() - subRow) <= 2) {
                hull -= Weapons.TORPEDO_DAMAGE;
                p.kill();
            }
        }
        for (Explosion ex : explosions) {
            hull -= ex.damageAt(SUB_COL, subRow);
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

    public int subRow() {
        return 3 + (int) Math.round(depth * (FIELD_H - 6) / MAX_DEPTH);
    }

    public int rowToDepth(int row) {
        return (row - 3) * MAX_DEPTH / (FIELD_H - 6);
    }

    public int getDepth() {
        return (int) Math.round(depth);
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
