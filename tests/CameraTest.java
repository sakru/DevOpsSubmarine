import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CameraTest {
    public static void main(String[] args) {
        Game game = new Game(1L, false) {
            @Override public void damageHull(int damage) {}
        };
        Camera camera = new Camera(78, 20);
        for (int tick = 0; tick < 480; tick++) {
            if (tick < 240) {
                game.accelerate();
                game.diveDown();
            } else {
                game.brake();
                game.diveUp();
            }
            game.update();
            camera.follow(game);
            check(camera.left() >= 0 && camera.left() + camera.width() <= Game.FIELD_W, "Horizontal camera bounds");
            check(camera.top() >= 1 && camera.top() + camera.height() - 1 <= Game.FIELD_H, "Vertical camera bounds");
            check(game.subBoxLeft() >= camera.left()
                    && game.subBoxRight() < camera.left() + camera.width(), "Camera must retain the complete hull width");
            check(game.subBoxTop(game.subRow()) >= camera.top()
                    && game.subBoxBottom(game.subRow()) < camera.top() + camera.height(), "Camera must retain the complete hull height");
        }

        camera.resize(200, Submarine.HEIGHT - 1);
        check(!camera.canFitSubmarine(), "Too-short viewport must not silently clip the sonar");
        camera.resize(200, 41);
        camera.follow(game);
        check(camera.canFitSubmarine() && game.subBoxTop(game.subRow()) >= camera.top(),
                "Growing the terminal must restore the complete submarine");
        TerminalSize.Size fallback = new TerminalSize.Size(80, 24);
        check(new TerminalSize("/dev/no-such-game-terminal", fallback).current().equals(fallback),
                "Missing terminal must preserve the fallback dimensions");

        Game contacts = new Game(1L, false);
        contacts.addEnemy(new EnemyMine(190, contacts.subRow()));
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(output)) {
            System.setOut(capture);
            try (Renderer renderer = new Renderer(contacts, 80, 24)) {
                renderer.draw();
            }
        } finally {
            System.setOut(original);
        }
        Matcher lines = Pattern.compile("\\x1b\\[(\\d+);1H([^\\x1b]*)").matcher(output.toString());
        boolean indicator = false;
        int count = 0;
        while (lines.find()) {
            int row = Integer.parseInt(lines.group(1));
            String text = lines.group(2);
            check(row <= 24 && text.length() <= 80, "Frame must fit an 80 by 24 terminal");
            if (row >= 3 && row <= 22 && text.charAt(78) == '>') indicator = true;
            count++;
        }
        check(count == 24 && indicator, "Off-screen enemy must have a visible edge indicator");
        check(output.toString().contains("SONAR") && output.toString().contains("ENGINE")
                && output.toString().contains("TORPEDO"), "Small terminal must preserve module detail");

        contacts.getSubmarine().damageAt(22, 2, 100);
        output.reset();
        try (PrintStream capture = new PrintStream(output)) {
            System.setOut(capture);
            try (Renderer renderer = new Renderer(contacts, 80, 24)) { renderer.draw(); }
        } finally {
            System.setOut(original);
        }
        String withoutColor = output.toString().replaceAll("\033\\[[0-9;]*m", "");
        check(withoutColor.contains("En:--"), "Disabled sonar must hide the contact count");
        lines = Pattern.compile("\\x1b\\[(\\d+);1H([^\\x1b]*)").matcher(withoutColor);
        while (lines.find()) {
            int row = Integer.parseInt(lines.group(1));
            if (row >= 3 && row <= 22) check(lines.group(2).charAt(78) != '>', "Disabled sonar must hide edge contacts");
        }

        output.reset();
        try (PrintStream capture = new PrintStream(output)) {
            System.setOut(capture);
            try (Renderer renderer = new Renderer(contacts, 202, Submarine.HEIGHT + 3)) {
                check(!renderer.canDrawGame(), "Too-small terminal must pause game updates");
                renderer.draw();
            }
        } finally {
            System.setOut(original);
        }
        check(output.toString().contains("Enlarge terminal to 48x11"),
                "Too-small terminal must explain how to resume");
        System.out.println("Camera checks passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
