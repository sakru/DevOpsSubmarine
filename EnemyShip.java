/**
 * Enemy Ship
 */
public class EnemyShip extends Enemy {

    public EnemyShip(int x, int y) {
        super(x, y);
    }

    @Override
    public void update(Game game) {
        // TODO: move left, drop depth charge
    }

    @Override
    public String sprite() {
        return "__/\\__";
    }
}
