/**
 * Game: game state and rules. No rendering, no input.
 */
import java.util.ArrayList;

public class Game {

    public static final int FIELD_W = 80;
    public static final int FIELD_H = 24;
    public static final int MAX_DEPTH = 10;
    public static final int SUB_COL = 10;

    private int depth = 5;
    private int hull = 100;
    private int level = 1;
    private boolean running = true;

    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private final ArrayList<Projectile> projectiles = new ArrayList<>();
    private final ArrayList<Explosion> explosions = new ArrayList<>();

    public void update() {
        for (Enemy e : enemies) {
            e.update(this);
        }
        for (Projectile p : projectiles) {
            p.update(this);
        }
        for (Explosion ex : explosions) {
            ex.tick();
        }
        removeDead();
        checkCollisions();
    }

    /** Skeleton: projectile vs sub, explosion damage to sub. */
    private void checkCollisions() {
        int subRow = 3 + depth * (FIELD_H - 6) / MAX_DEPTH;
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
