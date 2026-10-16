import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public abstract class Enemy {

    protected int x;
    protected int y;
    private boolean alive = true;

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
        int width = 0;
        for (String line : spriteLines()) {
            width = Math.max(width, line.length());
        }
        return width;
    }

    public int height() {
        return spriteLines().length;
    }

    public String[] spriteLines() {
        return sprite().split("\n", -1);
    }

    public void onContact(Game game) {
        kill();
        game.damageHullAt(Weapons.RAM_DAMAGE,
                Math.max(game.subBoxLeft(), Math.min(game.subBoxRight(), x)),
                Math.max(game.subBoxTop(game.subRow()), Math.min(game.subBoxBottom(game.subRow()), y)));
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

    private static final String SPRITE = "__/\\__";
    public static final int WIDTH = SPRITE.length();
    private final Set<Integer> dropColumns = new HashSet<>();
    private int moveTicks;

    public EnemyShip(int x, int y) {
        this(x, y, new Random());
    }

    public EnemyShip(int x, int y, Random rng) {
        super(x, y);
        int count = Weapons.DEPTH_CHARGE_MIN_PER_PASS
                + rng.nextInt(Weapons.DEPTH_CHARGE_MAX_PER_PASS
                - Weapons.DEPTH_CHARGE_MIN_PER_PASS + 1);
        while (dropColumns.size() < count) {
            dropColumns.add(rng.nextInt(Game.FIELD_W - WIDTH / 2 + 1));
        }
    }

    @Override
    public void update(Game game) {
        if (++moveTicks >= Weapons.SHIP_MOVE_INTERVAL) {
            moveLeft(Weapons.ENEMY_SPEED);
            moveTicks = 0;
        }
    }

    @Override
    public void tryAttack(Game game) {
        if (!isAlive()) {
            return;
        }
        int dropX = x + width() / 2;
        if (dropColumns.remove(dropX)) {
            game.addProjectile(new DepthCharge(dropX, y + 1));
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

    public EnemySub(int x, int y) {
        super(x, y);
        cooldown = Weapons.TORPEDO_COOLDOWN;
    }

    @Override
    public void update(Game game) {
        if (game.getFrame() % Weapons.FORWARD_MOVE_INTERVAL == 0) {
            moveLeft(game.getScrollStep() - Weapons.ENEMY_SUB_FORWARD_SPEED);
            if (x >= Game.FIELD_W) kill();
        }
    }

    @Override
    public void tryAttack(Game game) {
        if (!isAlive()) {
            return;
        }
        if (--cooldown <= 0) {
            game.addProjectile(new Torpedo(x - 1, y + 3));
            cooldown = Weapons.TORPEDO_COOLDOWN;
        }
    }

    @Override
    public String sprite() {
        return SPRITE;
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
