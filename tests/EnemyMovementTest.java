public class EnemyMovementTest {

    public static void main(String[] args) {
        Game game = new Game(1L, false);
        EnemySub submarine = new EnemySub(150, 5, game.getRng());
        EnemyMine mine = new EnemyMine(150, 5);
        game.addEnemy(submarine);
        game.addEnemy(mine);

        for (int tick = 0; tick < Game.TICKS_PER_SECOND * 3; tick++) {
            int previousX = submarine.getX();
            game.update();
            if (game.getScrollStep() == 0) {
                check(submarine.getX() == previousX,
                        "Submarine movement must follow forward ticks");
            }
        }
        int submarineTravel = 150 - submarine.getX();
        int mineTravel = 150 - mine.getX();
        check(submarine.isAlive() && mine.isAlive(), "Test enemies must remain in play");
        check(submarineTravel == 25, "Submarine must travel 25 columns in three seconds");
        check(mineTravel == game.getDistance(), "Mine must follow world scrolling");
        check(submarineTravel * 2 == mineTravel,
                "Submarine must pass at half the seabed scroll speed");
        System.out.println("Enemy movement checks passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
