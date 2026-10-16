/**
 * Torpedo
 */
public class Torpedo extends Projectile {

    private static final String PLAYER_SPRITE = "-[==]>";
    private static final String ENEMY_SPRITE = "<[==]-";
    private final boolean playerOwned;

    public Torpedo(int x, int y) {
        this(x, y, false);
    }

    public Torpedo(int x, int y, boolean playerOwned) {
        super(playerOwned ? x : x - ENEMY_SPRITE.length() + 1, y,
                playerOwned ? Weapons.TORPEDO_SPEED : -Weapons.TORPEDO_SPEED, 0);
        this.playerOwned = playerOwned;
    }

    @Override
    public boolean isPlayerOwned() {
        return playerOwned;
    }

    @Override
    public void hit(Game game, int hitX, int hitY, boolean hitHull) {
        kill();
        if (hitHull && !playerOwned) {
            game.damageHullAt(Weapons.TORPEDO_DAMAGE, hitX, hitY, Integer.signum(vx));
        }
        game.addExplosion(new Explosion(hitX, hitY, false));
    }

    @Override
    public String sprite() {
        return playerOwned ? PLAYER_SPRITE : ENEMY_SPRITE;
    }
}
