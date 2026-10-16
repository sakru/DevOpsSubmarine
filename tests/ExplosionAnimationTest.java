import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class ExplosionAnimationTest {
    public static void main(String[] args) {
        ExplosionWorker.Impact impact = new ExplosionWorker.Impact(60, 12, 60, 12,
                true, 0, 0, Game.MIN_LEVEL);
        ExplosionWorker.Zone zone = ExplosionWorker.prepare(impact);
        Explosion explosion = new Explosion(zone, 0, 0);
        String[] centres = {"..  *  ..", "..++*++..", "++**@**++", "..  .  .."};
        for (int tick = 0; tick < Weapons.EXPLOSION_VISUAL_FRAMES; tick++) {
            int phase = tick * 4 / Weapons.EXPLOSION_VISUAL_FRAMES;
            check(explosion.spriteLines().get(2).equals(centres[phase]), "Animation phase at tick " + tick);
            check(explosion.getRadius() == 2 && zone.damage() == 75,
                    "Animation must not alter damage or the blast radius");
            explosion.tick();
        }
        check(explosion.isDone() && explosion.spriteLines().isEmpty(), "Expired explosion must stop drawing");
        Explosion delayed = new Explosion(zone, 4, 4);
        check(delayed.spriteLines().get(2).equals(centres[2]), "Delayed delivery must skip old frames");
        check(delayed.getX() == 56, "Animation must retain scroll correction");

        Game game = new Game(1L, false);
        Enemy target = new Enemy(60, 12) {
            @Override public void update(Game ignored) {}
            @Override public String sprite() { return "ABCDE"; }
        };
        game.addEnemy(target);
        game.addExplosion(new Explosion(62, 12, false));
        String output = render(game);
        check(output.contains("..AB*DE.."), "Transparent explosion cells must preserve the target underneath");

        Game edge = new Game(1L, false);
        edge.addExplosion(new Explosion(0, 1, false));
        check(render(edge).contains("*  .."), "Animation must clip at the top and left field edges");

        for (var frame : zone.frames()) {
            check(frame.get(0).charAt(0) == ' ' && frame.get(4).charAt(8) == ' ',
                    "Corners outside the circle must remain transparent");
        }
        check(damageAt(4, 0) == 25 && damageAt(0, 2) == 25,
                "Horizontal and vertical edges must use the same physical radius");
        check(damageAt(4, 2) == 0 && damageAt(5, 0) == 0,
                "Positions outside the circle must not take damage");
        check(damageAt(2, 1) > 0 && damageAt(2, 1) < damageAt(2, 0),
                "Damage must decrease with Euclidean distance");
        System.out.println("Explosion animation checks passed");
    }

    private static int damageAt(int dx, int dy) {
        return ExplosionWorker.prepare(new ExplosionWorker.Impact(60, 12, 60 + dx, 12 + dy,
                true, 0, 0, Game.MIN_LEVEL)).damage();
    }

    private static String render(Game game) {
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(output)) {
            System.setOut(capture);
            try (Renderer renderer = new Renderer(game, Game.FIELD_W + 2, Game.FIELD_H + 4)) {
                renderer.draw();
            }
        } finally {
            System.setOut(original);
        }
        return output.toString();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
