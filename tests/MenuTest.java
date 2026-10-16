import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class MenuTest {
    public static void main(String[] args) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream text = new PrintStream(output)) {
            for (Game.Difficulty difficulty : Game.Difficulty.values()) {
                InputStream input = bytes("1" + (difficulty.ordinal() + 1) + "f");
                check(new Menu(input, text).chooseDifficulty() == difficulty, "Difficulty selection");
                check(input.read() == 'f', "Menu must leave game input unread");
            }
            check(new Menu(bytes("\nletters91\n?903"), text).chooseDifficulty() == null,
                    "Invalid input and EOF must be handled safely");
            check(new Menu(bytes("1013"), text).chooseDifficulty() == Game.Difficulty.HARD,
                    "Back must return to the main menu");
            check(new Menu(bytes("0"), text).chooseDifficulty() == null, "Exit from main menu");
            try {
                new Menu(new InputStream() {
                    @Override public int read() throws IOException { throw new IOException("Input failed"); }
                }, text).chooseDifficulty();
                throw new AssertionError("Read errors must be reported");
            } catch (IOException e) {
                check(e.getMessage().equals("Input failed"), "Input error details");
            }

            Game game = new Game(7L, false, null, Game.Difficulty.HARD);
            game.addEnemy(new EnemyMine(110, 5));
            game.addProjectile(new Torpedo(100, 5, true));
            while (game.getFrame() < Game.TICKS_PER_SECOND) game.update();
            game.damageHull(100);
            game.update();
            check(game.getStopReason() == Game.StopReason.HULL_BROKEN, "Test session must end in death");
            output.reset();
            new Menu(bytes(""), text).showStats(game);
            String stats = output.toString();
            check(stats.contains("HARD") && stats.contains("Hull broken:") && stats.contains("Time: 1 s")
                    && stats.contains("Enemies destroyed: 1") && stats.contains("Hull: 0%")
                    && stats.contains("Distance: " + game.getDistance()), "Death statistics must reflect the session");
        }
        System.out.println("Menu and session statistics checks passed");
    }

    private static InputStream bytes(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.US_ASCII));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
