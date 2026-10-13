/**
 * EnemySub
 */
public class EnemySub extends Enemy {

    public EnemySub(int x, int y) {
        super(x, y);
    }

    @Override
    public void update(Game game) {
        // TODO: move left, fire torpedo at range
    }

    @Override
    public String sprite() {
        return "<=>";
    }
}
