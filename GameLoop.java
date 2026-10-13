/**
 * GameLoop: main game loop and console renderer.
 */
import java.util.Arrays;

public class GameLoop {

    private static final String[] SUBMARINE = {
        "                 ____                   ",
        "              ___/  \\___                ",
        "    ________/            \\__________    ",
        "/      .   .   DEVOPS MK1   .   .      \\",
        "<______________________________________>"
    };

    private static final int FIELD_W = 80;
    private static final int FIELD_H = 24;
    private static final int SUB_COL = 10;

    private final Controller controller;

    private boolean running = true;
    private int depth = 5;
    private int level = 1;

    public GameLoop() {
        controller = new Controller(this);
    }

    public void start() {
        while (running) {
            controller.poll();
            updateGameLogic();
            clearScreen();
            drawUI();
            pauseForFrame();
        }
    }

    private void updateGameLogic() {
    }

    private void clearScreen() {
        System.out.print("\033[H\033[2J");
    }

    private void drawUI() {
        int anchorRow = 3 + depth * (FIELD_H - 6) / 10;
        int floorStart = FIELD_H - level + 1;

        System.out.println("Hull: 100%");
        System.out.println(border());
        for (int row = 1; row <= FIELD_H; row++) {
            char[] line = new char[FIELD_W];
            Arrays.fill(line, ' ');
            if (row >= floorStart) {
                Arrays.fill(line, '#');
            }
            drawSubOnLine(line, row, anchorRow);
            System.out.println("|" + new String(line) + "|");
        }
        System.out.println(border());
        System.out.println("Depth: " + depth + "/10  Level: " + level);
    }

    private void drawSubOnLine(char[] line, int row, int anchorRow) {
        for (int i = 0; i < SUBMARINE.length; i++) {
            if (row != anchorRow - 2 + i) continue;
            String artLine = SUBMARINE[i];
            for (int c = 0; c < artLine.length(); c++) {
                int col = SUB_COL - 1 + c;
                if (col >= 0 && col < line.length) {
                    line[col] = artLine.charAt(c);
                }
            }
        }
    }

    private String border() {
        StringBuilder sb = new StringBuilder("+");
        for (int i = 0; i < FIELD_W; i++) sb.append('-');
        return sb.append('+').toString();
    }

    private void pauseForFrame() {
        try {
            Thread.sleep(40);
        } catch (InterruptedException e) {
            running = false;
        }
    }

    public void setDepth(int depth) {
        this.depth = Math.max(0, Math.min(10, depth));
    }

    public int getDepth() {
        return depth;
    }

    public void stop() {
        running = false;
    }
}