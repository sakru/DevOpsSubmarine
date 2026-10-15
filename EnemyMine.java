public class EnemyMine extends Enemy {

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
