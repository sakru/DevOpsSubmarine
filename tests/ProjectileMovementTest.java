public class ProjectileMovementTest {

    public static void main(String[] args) {
        Game game = new Game(1L, false);
        DepthCharge charge = new DepthCharge(100, 2);
        Torpedo torpedo = new Torpedo(100, 2, true);
        int scroll = 0;

        for (int tick = 1; tick <= 40; tick++) {
            game.update();
            scroll += game.getScrollStep();
            int chargeY = charge.getY();
            int torpedoX = torpedo.getX();
            charge.update(game);
            torpedo.update(game);

            check(charge.getX() == 100, "Depth charge must stay in its column");
            check(charge.prevX() == 100 && charge.prevY() == chargeY,
                    "Depth charge must retain its previous position for collisions");
            int expectedY = 2 + (int) Math.floor((tick + 1e-9)
                    / Weapons.DEPTH_CHARGE_FALL_INTERVAL) * Weapons.DEPTH_CHARGE_FALL_SPEED;
            check(charge.getY() == expectedY, "Depth charge fall interval");
            check(torpedo.getX() == 100 + tick * Weapons.TORPEDO_SPEED - scroll,
                    "Torpedo must move and scroll with the world");
            check(torpedo.prevX() == torpedoX && torpedo.prevY() == 2,
                    "Torpedo must retain its previous position for collisions");
        }
        check(scroll > 0, "Test must include world scrolling");

        Game simulation = new Game(1L, false);
        DepthCharge droppedCharge = new DepthCharge(100, 2);
        simulation.addProjectile(droppedCharge);
        for (int tick = 0; tick < 120; tick++) {
            simulation.update();
            check(droppedCharge.isAlive(), "Test charge must still be falling");
            check(droppedCharge.getX() == 100,
                    "Full game loop must keep a falling charge in its screen column");
        }
        check(droppedCharge.getY() > 2, "Test charge must fall during simulation");
        System.out.println("Projectile movement checks passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
