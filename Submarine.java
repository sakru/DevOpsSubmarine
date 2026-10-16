import java.util.Arrays;
import java.util.EnumMap;

public final class Submarine {

    public record Point(int col, int row) {}

    private static final String[] SPRITE = {
        "                      |",
        "                 .----+----.",
        "       __________|  SONAR  |________",
        "   ___/ [ENGINE]--+----|----+--[TORPEDO] \\___",
        "<=[##]--[|||]====+==[NAV]==+==[==> [==> =====>",
        "  \\     +-------+---o--+--+--------+      /",
        "   \\___________________________________/"
    };
    public static final int WIDTH = Arrays.stream(SPRITE).mapToInt(String::length).max().orElseThrow();
    public static final int HEIGHT = SPRITE.length;
    public static final int OFFSET_X = -1;
    public static final int OFFSET_Y = -2;
    public static final int TORPEDO_ROW = 4;

    static {
        for (int row = 0; row < HEIGHT; row++) SPRITE[row] += " ".repeat(WIDTH - SPRITE[row].length());
    }

    public enum Module {
        ENGINE(8, 3, 9, 3), SONAR(17, 1, 11, 2), TORPEDO(29, 3, 11, 3);

        final int x, y, width, height;

        Module(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        boolean contains(int col, int row) {
            return col >= x && col < x + width && row >= y && row < y + height;
        }
    }

    private final int[][] damage = new int[HEIGHT][WIDTH];
    private final EnumMap<Module, Integer> health = new EnumMap<>(Module.class);

    public Submarine() {
        for (Module module : Module.values()) health.put(module, 100);
    }

    public static String spriteLine(int row) {
        return SPRITE[row];
    }

    public String renderLine(int row) {
        char[] line = SPRITE[row].toCharArray();
        for (int col = 0; col < WIDTH; col++) {
            int severity = damage[row][col];
            if (severity > 0) line[col] = severity >= 60 ? 'X' : severity >= 25 ? 'x' : ':';
        }
        return new String(line);
    }

    public int getHealth(Module module) {
        return health.get(module);
    }

    public boolean isActive(Module module) {
        return getHealth(module) > 0;
    }

    public int damageColor(int col, int row) {
        if (col < 0 || col >= WIDTH || row < 0 || row >= HEIGHT || damage[row][col] == 0) return 0;
        for (Module module : Module.values()) {
            if (module.contains(col, row) && getHealth(module) < 100) return 31;
        }
        return 33;
    }

    public boolean contains(int col, int row) {
        if (col < 0 || col >= WIDTH || row < 0 || row >= HEIGHT) return false;
        String line = SPRITE[row];
        int first = 0;
        int last = WIDTH - 1;
        while (first < WIDTH && line.charAt(first) == ' ') first++;
        while (last >= 0 && line.charAt(last) == ' ') last--;
        return col >= first && col <= last;
    }

    public Point closestPoint(int col, int row) {
        Point closest = new Point(0, TORPEDO_ROW);
        double best = Double.POSITIVE_INFINITY;
        for (int y = 0; y < HEIGHT; y++) {
            int first = WIDTH - SPRITE[y].stripLeading().length();
            int last = SPRITE[y].stripTrailing().length() - 1;
            int x = Math.max(first, Math.min(last, col));
            double distance = Math.hypot((x - col) / 2.0, y - row);
            if (distance < best) {
                best = distance;
                closest = new Point(x, y);
            }
        }
        return closest;
    }

    public void damageAt(int col, int row, int amount) {
        if (amount <= 0) return;
        col = Math.max(0, Math.min(WIDTH - 1, col));
        row = Math.max(0, Math.min(HEIGHT - 1, row));
        for (Module module : Module.values()) {
            if (module.contains(col, row)) health.put(module, Math.max(0, health.get(module) - amount));
        }
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                double distance = Math.hypot((x - col) / 2.0, y - row);
                if (distance <= 1.5 && SPRITE[y].charAt(x) != ' ') {
                    damage[y][x] = Math.min(100, damage[y][x] + Math.max(1, (int) (amount / (1 + distance))));
                }
            }
        }
    }
}
