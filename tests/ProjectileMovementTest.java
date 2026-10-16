public class ProjectileMovementTest {

    public static void main(String[] args) {
        Game game = new Game(1L, false);
        DepthCharge charge = new DepthCharge(100, 2);
        Torpedo torpedo = new Torpedo(100, 2, true);
        Torpedo enemyTorpedo = new Torpedo(100, 2);
        int enemyStart = enemyTorpedo.getX();
        int scroll = 0;

        for (int tick = 1; tick <= 40; tick++) {
            game.update();
            scroll += game.getScrollStep();
            int chargeY = charge.getY();
            int torpedoX = torpedo.getX();
            charge.update(game);
            torpedo.update(game);
            enemyTorpedo.update(game);

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
            check(torpedo.getX() > torpedoX, "Player torpedo must keep moving forward during scrolling");
            int playerWaterTravel = torpedo.getX() - 100 + scroll;
            int enemyWaterTravel = enemyTorpedo.getX() - enemyStart + scroll;
            check(playerWaterTravel == -enemyWaterTravel,
                    "Both torpedoes must have equal speed relative to water");
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

        Game combat = new Game(1L, false);
        Enemy narrowTarget = new Enemy(110, 5) {
            @Override public void update(Game ignored) {}
            @Override public String sprite() { return "X"; }
        };
        combat.addEnemy(narrowTarget);
        combat.addProjectile(new Torpedo(100, 5, true));
        combat.update();
        combat.update();
        check(!narrowTarget.isAlive() && combat.getKills() == 1,
                "Fast torpedo must hit a narrow target along its swept path");
        System.out.println("Projectile movement checks passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
