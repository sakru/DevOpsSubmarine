import java.io.ByteArrayInputStream;

public class HorizontalMovementTest {
    public static void main(String[] args) throws InterruptedException {
        Game game = new Game(1L, false) {
            @Override public void damageHull(int damage) {}
        };
        Controller controller = new Controller(game, new ByteArrayInputStream("aaaa".getBytes()));
        try {
            controller.awaitDrained(1000);
            controller.poll();
            check(game.getForwardSpeed() == 0, "A must brake to zero without reversing");
        } finally {
            controller.shutdown();
        }
        int originalLeft = game.subBoxLeft();
        for (int tick = 0; tick < 120; tick++) game.update();
        check(game.subBoxLeft() == 1 && game.subBoxLeft() < originalLeft,
                "Braking must move the submarine towards the rear edge");
        long rearDistance = game.getDistance();
        EnemyMine mine = new EnemyMine(Game.FIELD_W - 3, 5);
        game.addEnemy(mine);
        int mineX = mine.getX();
        for (int tick = 0; tick < 30; tick++) game.update();
        check(game.getDistance() - rearDistance == 20 && mineX - mine.getX() == 20,
                "World and enemies must keep scrolling when the hull reaches the rear edge");
        check(game.subBoxLeft() == 1, "Braking must not move the hull outside the field");
        for (int i = 0; i < 20; i++) game.accelerate();
        check(game.getForwardSpeed() == Weapons.MAX_PLAYER_FORWARD_SPEED, "Throttle must have an upper limit");
        for (int tick = 0; tick < 600; tick++) game.update();
        check(game.subBoxRight() == Game.FIELD_W - 1, "Acceleration must advance the hull but keep it in the field");
        check(game.getDistance() > rearDistance, "World scrolling must continue at the front edge too");
        System.out.println("Horizontal movement checks passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
