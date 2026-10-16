/*
 * Builds the seabed by choosing a ground height for each map column.
 * When the world scrolls, it shifts the old terrain left and grows new columns on the right.
 */
import java.util.Random;

public class SeaFloor {

    private static final int VARIATION = 4;
    private static final int DEEPEST_ROW = Game.FIELD_H;
    private static final int SHALLOWEST_ROW = Game.MIN_FLOOR_ROW;

    private final int[] heights = new int[Game.FIELD_W];
    private int minRow;
    private int maxRow;

    public SeaFloor(int level) {
        this(level, new Random());
    }

    public SeaFloor(int level, Random rng) {
        generate(level, rng);
    }

    public void generate(int level, Random rng) {
        int safeLevel = Math.max(Game.MIN_LEVEL, Math.min(Game.MAX_LEVEL, level));
        maxRow = Math.max(SHALLOWEST_ROW, DEEPEST_ROW - (safeLevel - Game.MIN_LEVEL));
        minRow = Math.max(SHALLOWEST_ROW, maxRow - VARIATION);

        int row = maxRow;
        for (int x = 0; x < heights.length; x++) {
            row = nextHeight(row, rng);
            heights[x] = row;
        }
    }

    public void scrollLeft(int columns, Random rng) {
        if (columns <= 0) {
            return;
        }
        columns = Math.min(columns, heights.length);
        System.arraycopy(heights, columns, heights, 0, heights.length - columns);
        for (int x = heights.length - columns; x < heights.length; x++) {
            int previous = x == 0 ? maxRow : heights[x - 1];
            heights[x] = nextHeight(previous, rng);
        }
    }

    private int nextHeight(int previous, Random rng) {
        return Math.max(minRow, Math.min(maxRow, previous + rng.nextInt(3) - 1));
    }

    public int heightAt(int x) {
        return heights[x];
    }
}
