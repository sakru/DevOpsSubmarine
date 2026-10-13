/**
 * SeaFloor
 */
import java.util.Random;

public class SeaFloor {

    private static final int VARIATION = 4;

    private final int[] heights = new int[Game.FIELD_W];

    public SeaFloor(int level) {
        generate(level);
    }

    public void generate(int level) {
        Random rnd = new Random();
        int base = Math.max(1, Game.FIELD_H - level + 1);
        int min = Math.max(1, base - VARIATION);
        int row = base;
        for (int x = 0; x < heights.length; x++) {
            row += rnd.nextInt(3) - 1;
            if (row < min) row = min;
            if (row > base) row = base;
            heights[x] = row;
        }
    }

    public int heightAt(int x) {
        return heights[x];
    }
}
