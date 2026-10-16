import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class SubmarineDamageTest {
    public static void main(String[] args) {
        Submarine sub = new Submarine();
        Submarine other = new Submarine();
        check(sub.renderLine(2).contains("SONAR") && sub.renderLine(3).contains("ENGINE")
                && sub.renderLine(3).contains("TORPEDO"), "All three modules must be visible");
        sub.damageAt(11, 3, 10);
        check(sub.getHealth(Submarine.Module.ENGINE) == 80, "Hit must damage its module");
        check(sub.getHealth(Submarine.Module.SONAR) == 100
                && sub.getHealth(Submarine.Module.TORPEDO) == 100, "Other modules must remain intact");
        check(sub.damageColor(11, 3) == 31, "Damaged module must be red");
        sub.damageAt(2, 4, 10);
        check(sub.damageColor(2, 4) == 33, "Damaged hull outside a module must be yellow");
        check(sub.renderLine(3).charAt(11) == ':', "Light impact must leave a persistent mark");
        sub.damageAt(11, 3, 60);
        check(sub.renderLine(3).charAt(11) == 'X', "Repeated damage must worsen the impact area");
        check(other.renderLine(3).equals(Submarine.spriteLine(3)), "Damage must belong to one submarine");
        check(sub.contains(22, 0) && !sub.contains(0, 0), "Collision shape must exclude empty space beside the mast");

        Game game = new Game(1L, false);
        game.damageHullAt(10, game.subBoxLeft() + 11, game.subBoxTop(game.subRow()) + 3);
        check(game.getHull() == 90 && game.getSubmarine().getHealth(Submarine.Module.ENGINE) == 80,
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
        Game engine = new Game(1L, false);
        engine.diveDown();
        engine.damageHullAt(50, engine.subBoxLeft() + 11, engine.subBoxTop(engine.subRow()) + 3);
        double depth = engine.getExactDepth();
        engine.update();
        engine.accelerate();
        engine.diveUp();
        engine.update();
        check(engine.getHull() == 50 && engine.isRunning()
                && engine.getForwardSpeed() == 0 && engine.getExactDepth() == depth,
                "Disabled engine must block horizontal and vertical controls");

        Game tubes = new Game(1L, false);
        tubes.damageHullAt(49, tubes.subBoxLeft() + 32, tubes.subBoxTop(tubes.subRow()) + 3);
        check(tubes.getSubmarine().isActive(Submarine.Module.TORPEDO), "Module must remain active above zero health");
        tubes.damageHullAt(1, tubes.subBoxLeft() + 32, tubes.subBoxTop(tubes.subRow()) + 3);
        tubes.fireTorpedo();
        check(!tubes.getSubmarine().isActive(Submarine.Module.TORPEDO)
                && !tubes.getProjectiles().iterator().hasNext(), "Disabled torpedo room must prevent firing");

        Game colors = new Game(1L, false);
        colors.getSubmarine().damageAt(2, 4, 10);
        colors.getSubmarine().damageAt(11, 3, 10);
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(output)) {
            System.setOut(capture);
            try (Renderer renderer = new Renderer(colors, 80, 24)) { renderer.draw(); }
        } finally {
            System.setOut(original);
        }
        check(output.toString().contains("\033[31m") && output.toString().contains("\033[33m")
                && output.toString().contains("\033[0m"), "Renderer must output red, yellow and color resets");
        System.out.println("Submarine damage checks passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
