public class SubmarineDamageTest {
    public static void main(String[] args) {
        Submarine sub = new Submarine();
        Submarine other = new Submarine();
        check(sub.renderLine(2).contains("SONAR") && sub.renderLine(3).contains("ENGINE")
                && sub.renderLine(3).contains("TORPEDO"), "All three modules must be visible");
        sub.damageAt(11, 3, 10);
        check(sub.getHealth(Submarine.Module.ENGINE) == 90, "Hit must damage its module");
        check(sub.getHealth(Submarine.Module.SONAR) == 100
                && sub.getHealth(Submarine.Module.TORPEDO) == 100, "Other modules must remain intact");
        check(sub.renderLine(3).charAt(11) == ':', "Light impact must leave a persistent mark");
        sub.damageAt(11, 3, 60);
        check(sub.renderLine(3).charAt(11) == 'X', "Repeated damage must worsen the impact area");
        check(other.renderLine(3).equals(Submarine.spriteLine(3)), "Damage must belong to one submarine");
        check(sub.contains(22, 0) && !sub.contains(0, 0), "Collision shape must exclude empty space beside the mast");

        Game game = new Game(1L, false);
        game.damageHullAt(10, game.subBoxLeft() + 11, game.subBoxTop(game.subRow()) + 3);
        check(game.getHull() == 90 && game.getSubmarine().getHealth(Submarine.Module.ENGINE) == 90,
                "Actual hull damage must update the module and its appearance");
        Torpedo passing = new Torpedo(game.subBoxRight() + 1, game.subBoxTop(game.subRow()));
        game.addProjectile(passing);
        game.update();
        check(passing.isAlive() && game.getHull() == 90, "Torpedo in empty sprite padding must not hit the hull");

        game.fireTorpedo();
        for (Projectile projectile : game.getProjectiles()) {
            if (projectile.isPlayerOwned()) {
                check(projectile.getY() == game.subBoxTop(game.subRow()) + Submarine.TORPEDO_ROW,
                        "Player torpedo must launch from the bow tubes");
            }
        }
        System.out.println("Submarine damage checks passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
