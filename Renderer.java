/**
 * Renderers game to the console.
 *
 * Renders the field (water, sea floor, submarine art) and the HUD (hull, depth, level).
 * Reads state from Game only — never mutates it.
 */
import java.util.Arrays;

public class Renderer {

    private static final String[] SUBMARINE = {
        "                 ____                   ",
        "              ___/  \\___                ",
        "    ________/            \\__________    ",
        "/      .   .   DEVOPS MK1   .   .      \\",
        "<______________________________________>"
    };

    private final Game game;

    public Renderer(Game game) {
        this.game = game;
    }

    public void clearScreen() {
        System.out.print("\033[H\033[2J");
    }

    public void draw() {
        int anchorRow = 3 + game.getDepth() * (Game.FIELD_H - 6) / Game.MAX_DEPTH;
        int floorStart = Game.FIELD_H - game.getLevel() + 1;

        System.out.println("Hull: " + game.getHull() + "%");
        System.out.println(border());
        for (int row = 1; row <= Game.FIELD_H; row++) {
            char[] line = new char[Game.FIELD_W];
            Arrays.fill(line, ' ');
            if (row >= floorStart) {
                Arrays.fill(line, '#');
            }
            drawSubOnLine(line, row, anchorRow);
            System.out.println("|" + new String(line) + "|");
        }
        System.out.println(border());
        System.out.println("Depth: " + game.getDepth() + "/10  Level: " + game.getLevel());
    }

    private void drawSubOnLine(char[] line, int row, int anchorRow) {
        for (int i = 0; i < SUBMARINE.length; i++) {
            if (row != anchorRow - 2 + i) continue;
            String artLine = SUBMARINE[i];
            for (int c = 0; c < artLine.length(); c++) {
                int col = Game.SUB_COL - 1 + c;
                if (col >= 0 && col < line.length) {
                    line[col] = artLine.charAt(c);
                }
            }
        }
    }

    private String border() {
        StringBuilder sb = new StringBuilder("+");
        for (int i = 0; i < Game.FIELD_W; i++) sb.append('-');
        return sb.append('+').toString();
    }
}
