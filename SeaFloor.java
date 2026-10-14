/**
 * SeaFloor
 */
import java.util.Random;

public class SeaFloor {

    private static final int VARIATION = 4;
    private static final int FLOOR_TOP_LIMIT = Game.BOTTOM_ROW - 1;
    private static final int FLOOR_BOTTOM_LIMIT = 1;

    private final int[] heights = new int[Game.FIELD_W];

    public SeaFloor(int level) {
        this(level, new Random());
    }

    public SeaFloor(int level, Random rng) {
        generate(level, rng);
    }

    public void generate(int level, Random rng) {
        int safeLevel = Math.max(Game.MIN_LEVEL, Math.min(Game.MAX_LEVEL, level));
        int base = FLOOR_TOP_LIMIT - (safeLevel - Game.MIN_LEVEL) * 2;
        base = Math.max(FLOOR_BOTTOM_LIMIT, base);

        int row = base;
        for (int x = 0; x < heights.length; x++) {
            row += rng.nextInt(3) - 1; // -1, 0, +1
            if (row < base - VARIATION) row = base - VARIATION;
            if (row > FLOOR_TOP_LIMIT) row = FLOOR_TOP_LIMIT;
            if (row < FLOOR_BOTTOM_LIMIT) row = FLOOR_BOTTOM_LIMIT;
            heights[x] = row;
        }
    }

    public int heightAt(int x) {
        return heights[x];
    }
}
