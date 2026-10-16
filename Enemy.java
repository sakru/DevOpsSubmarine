import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public abstract class Enemy {

    protected int x;
    protected int y;
    private boolean alive = true;
    private int hull = 100;
    private int nextContact;
    private int nextGroundHit;
    protected Submarine vessel;

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
        if (vessel != null) return vessel.width();
        int width = 0;
        for (String line : spriteLines()) {
            width = Math.max(width, line.length());
        }
        return width;
    }

    public int height() {
        if (vessel != null) return vessel.height();
        return spriteLines().length;
    }

    public String[] spriteLines() {
        if (vessel != null) {
            String[] lines = new String[vessel.height()];
            for (int row = 0; row < lines.length; row++) lines[row] = vessel.renderLine(row);
            return lines;
        }
        return sprite().split("\n", -1);
    }

    public int getHull() { return hull; }
    public Submarine getVessel() { return vessel; }

    public boolean contains(int col, int row) {
        return vessel == null ? col >= x && col < x + width() && row >= y && row < y + height()
                : vessel.contains(col - x, row - y);
    }

    public void damageAt(int amount, int hitX, int hitY, int direction) {
        if (amount < 0) throw new IllegalArgumentException("Damage cannot be negative");
        if (vessel != null) {
            Submarine.Point point = vessel.closestPoint(hitX - x, hitY - y);
            vessel.damageAt(point.col(), point.row(), amount, direction);
            hull = vessel.getHull();
        } else {
            hull = Math.max(0, hull - amount);
        }
        if (hull == 0) kill();
    }

    public void onContact(Game game) {
        if (game.getFrame() < nextContact) return;
        nextContact = game.getFrame() + Game.TICKS_PER_SECOND;
        damageAt(Weapons.RAM_DAMAGE, game.subBoxRight(), game.subRow(), 0);
        game.damageHullAt(Weapons.RAM_DAMAGE,
                Math.max(game.subBoxLeft(), Math.min(game.subBoxRight(), x)),
                Math.max(game.subBoxTop(game.subRow()), Math.min(game.subBoxBottom(game.subRow()), y)));
    }

    public void hitSeabed(Game game, int top) {
        if (vessel == null) { kill(); return; }
        if (game.getFrame() >= nextGroundHit) {
            damageAt(Weapons.GROUND_DAMAGE, x + width() / 2, y + height() - 1, 0);
            nextGroundHit = game.getFrame() + Game.TICKS_PER_SECOND;
        }
        y = Math.max(1, top);
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

class EnemyShip extends Enemy implements WeaponCarrier {

    private static final String SPRITE = "     __|__       \n ___/     \\_____ \n \\_____________/\n  \\===========/ \n   \\_________/  ";
    public static final int WIDTH = SPRITE.lines().mapToInt(String::length).max().orElseThrow();
    private final Set<Integer> dropColumns = new HashSet<>();
    private int moveTicks;

    public EnemyShip(int x, int y) {
        this(x, y, new Random());
    }

    public EnemyShip(int x, int y, Random rng) {
        super(x, y);
        vessel = new Submarine(SPRITE, new Submarine.Room(10, 1, 5, 1),
                new Submarine.Room(6, 1, 4, 1), new Submarine.Room(2, 1, 4, 1));
        int count = Weapons.DEPTH_CHARGE_MIN_PER_PASS
                + rng.nextInt(Weapons.DEPTH_CHARGE_MAX_PER_PASS
                - Weapons.DEPTH_CHARGE_MIN_PER_PASS + 1);
        while (dropColumns.size() < count) {
            dropColumns.add(rng.nextInt(Game.FIELD_W - WIDTH / 2 + 1));
        }
    }

    @Override
    public void update(Game game) {
        vessel.update(game.getFrame());
        if (++moveTicks >= Weapons.SHIP_MOVE_INTERVAL) {
            moveLeft(vessel.isActive(Submarine.Module.ENGINE) ? Weapons.ENEMY_SPEED : game.getScrollStep());
            moveTicks = 0;
        }
    }

    @Override
    public void tryAttack(Game game) {
        if (!isAlive() || !vessel.isActive(Submarine.Module.TORPEDO)) {
            return;
        }
        int dropX = x + width() / 2;
        if (dropColumns.remove(dropX)) {
            game.addProjectile(new DepthCharge(dropX, y + height()));
        }
    }

    @Override
    public String sprite() {
        return SPRITE;
    }
}

class EnemySub extends Enemy implements WeaponCarrier {

    private static final String SPRITE = "              |                 \n"
            + "        .-----+----.            \n"
            + "   _____|__[o]_____|________    \n"
            + " / [==<]===| NAV |===[||]    \\  \n"
            + "<__________|_____|___________>";
    public static final int HEIGHT = SPRITE.split("\n").length;
    public static final int WIDTH = SPRITE.lines().mapToInt(String::length).max().orElseThrow();
    private int cooldown;
    private final Sonar sonar = new Sonar();
    private int targetRow = -1;
    private boolean targetDetected;

    public EnemySub(int x, int y) {
        super(x, y);
        vessel = new Submarine(SPRITE, new Submarine.Room(20, 3, 6, 1),
                new Submarine.Room(8, 1, 11, 2), new Submarine.Room(3, 3, 7, 1));
        cooldown = Weapons.TORPEDO_COOLDOWN;
    }

    @Override
    public void update(Game game) {
        vessel.update(game.getFrame());
        if (game.getFrame() % Weapons.FORWARD_MOVE_INTERVAL == 0) {
            moveLeft(game.getScrollStep() - (vessel.isActive(Submarine.Module.ENGINE) ? Weapons.ENEMY_SUB_FORWARD_SPEED : 0));
            if (x >= Game.FIELD_W) kill();
        }
        sonar.update(game.getFrame(), game.getScrollStep(), x + WIDTH / 2, y + 1,
                isAlive() && vessel.isActive(Submarine.Module.SONAR));
        targetDetected = sonar.detects(game.subBoxLeft(), game.subBoxTop(game.subRow()),
                Submarine.WIDTH, Submarine.HEIGHT, game.getFrame());
        if (targetDetected) targetRow = game.subBoxTop(game.subRow()) + Submarine.TORPEDO_ROW;
        if (vessel.isActive(Submarine.Module.ENGINE) && targetRow >= 0 && game.getFrame() % Weapons.ENEMY_DIVE_INTERVAL == 0) {
            int nextY = y + Integer.signum(targetRow - (y + 3));
            if (game.isAboveFloor(x, x + WIDTH - 1, nextY, HEIGHT)) y = nextY;
        }
    }

    @Override
    public void tryAttack(Game game) {
        if (!isAlive() || !vessel.isActive(Submarine.Module.TORPEDO)) {
            return;
        }
        if (cooldown > 0) cooldown--;
        if (cooldown == 0 && targetDetected && y + 3 == targetRow && x > game.subBoxRight()) {
            game.addProjectile(new Torpedo(x - 1, y + 3));
            cooldown = Weapons.TORPEDO_COOLDOWN;
        }
    }

    @Override
    public String sprite() {
        return SPRITE;
    }

    public Sonar getSonar() {
        return sonar;
    }
}

class EnemyMine extends Enemy {

    public EnemyMine(int x, int y) {
        super(x, y);
    }

    @Override
    public void update(Game game) {
        moveLeft(game.getScrollStep());
    }

    @Override
    public String sprite() {
        return "(+)";
    }

    @Override
    public void damageAt(int amount, int hitX, int hitY, int direction) {
        if (amount > 0) super.damageAt(100, hitX, hitY, direction);
    }

    @Override
    public void onContact(Game game) {
        kill();
        game.addExplosion(new Explosion(x, y));
    }
}

class DepthCharge extends Projectile {

    public DepthCharge(int x, int y) {
        super(x, y, 0, Weapons.DEPTH_CHARGE_FALL_SPEED, Weapons.DEPTH_CHARGE_FALL_INTERVAL);
    }

    @Override
    public void hit(Game game, int hitX, int hitY, boolean hitHull) {
        kill();
        game.addExplosion(new Explosion(hitX, hitY));
    }

    @Override
    public String sprite() {
        return "o";
    }

    @Override
    protected boolean scrollsWithWorld() {
        return false;
    }
}
