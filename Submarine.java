/*
 * Describes the inside of a vessel: rooms, local damage, crew and repairs.
 * Player and enemy vessels share these rules. Crew, below, moves sailors through the interior
 * and keeps track of their injuries and repair work.
 */
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Random;

public final class Submarine {

    public record Point(int col, int row) {}

    private static final String[] SPRITE = {
        "                      |",
        "                 .----+----.",
        "       __________|  SONAR  |________",
        "   ___/ [ENGINE]--+----|----+--[TORPEDO] \\___",
        "<=[##]--[|||]====+==[NAV]==+==[==> [==> =====>",
        "  \\     +       +---o--+--+        +      /",
        "   \\___________________________________/"
    };
    public static final int WIDTH = Arrays.stream(SPRITE).mapToInt(String::length).max().orElseThrow();
    public static final int HEIGHT = SPRITE.length;
    public static final int OFFSET_X = -1;
    public static final int OFFSET_Y = -2;
    public static final int TORPEDO_ROW = 4;
    private static final int MODULE_HEALTH = 50;

    static {
        for (int row = 0; row < HEIGHT; row++) SPRITE[row] += " ".repeat(WIDTH - SPRITE[row].length());
    }

    public enum Module {
        ENGINE(new Room(8, 3, 9, 3)), SONAR(new Room(17, 1, 11, 2)), TORPEDO(new Room(29, 3, 11, 3));
        final Room room;
        Module(Room room) { this.room = room; }
    }

    public record Room(int x, int y, int width, int height) {
        boolean contains(int col, int row) {
            return col >= x && col < x + width && row >= y && row < y + height;
        }
    }

    private final String[] sprite;
    private final int width, height;
    private final Room[] rooms;
    private final int[][] damage;
    private final Room navigation;
    private int hullDamage;
    private boolean navigationHit;
    private final EnumMap<Module, Integer> health = new EnumMap<>(Module.class);
    private final Crew crew;
    private Module repairing;
    private int repairTicks;
    public static final int REPAIR_TICKS = 5 * Game.TICKS_PER_SECOND;

    public Submarine() {
        this(String.join("\n", SPRITE), Arrays.stream(Module.values())
                .map(m -> m.room).toArray(Room[]::new));
    }

    public Submarine(String shape, Room... rooms) {
        sprite = shape.split("\n", -1);
        width = Arrays.stream(sprite).mapToInt(String::length).max().orElseThrow();
        height = sprite.length;
        for (int row = 0; row < height; row++) sprite[row] += " ".repeat(width - sprite[row].length());
        this.rooms = rooms.clone();
        Room nav = null;
        for (int row = 0; row < height; row++) {
            int col = sprite[row].indexOf("NAV");
            if (col >= 0) nav = new Room(col - 1, row, 5, 1);
        }
        navigation = nav;
        damage = new int[height][width];
        for (Module module : Module.values()) health.put(module, MODULE_HEALTH);
        crew = new Crew(this);
    }

    public int width() { return width; }
    public int height() { return height; }
    public Crew getCrew() { return crew; }
    public int getHull() { return navigationHit ? 0 : 100 - hullDamage; }
    public boolean isNavigationHit() { return navigationHit; }
    public Module getRepairing() { return repairing; }
    public int getRepairSeconds() { return (repairTicks + Game.TICKS_PER_SECOND - 1) / Game.TICKS_PER_SECOND; }

    public String renderLine(int row) {
        char[] line = sprite[row].toCharArray();
        for (int col = 0; col < width; col++) {
            int severity = damage[row][col];
            if (severity > 0) line[col] = severity >= 60 ? 'X' : severity >= 25 ? 'x' : ':';
            if (severity < 100 && crew.colorAt(col, row) != 0) line[col] = '.';
        }
        return new String(line);
    }

    public int getHealth(Module module) {
        return health.get(module) * 100 / MODULE_HEALTH;
    }

    public boolean isActive(Module module) {
        return getHealth(module) > 0;
    }

    private boolean isSafe(int col, int row) {
        if (damage[row][col] > 0) return false;
        for (Module module : Module.values()) {
            if (rooms[module.ordinal()].contains(col, row) && getHealth(module) < 100) return false;
        }
        return true;
    }

    public int damageColor(int col, int row) {
        if (col < 0 || col >= width || row < 0 || row >= height) return 0;
        if (damage[row][col] >= 100) return 31;
        int sailor = crew.colorAt(col, row);
        if (sailor != 0) return sailor;
        if (navigationHit && navigation.contains(col, row)) return 31;
        for (Module module : Module.values()) {
            if (rooms[module.ordinal()].contains(col, row) && getHealth(module) < 100
                    && sprite[row].charAt(col) != ' ') return 31;
        }
        return damage[row][col] > 0 ? 33 : 0;
    }

    public boolean contains(int col, int row) {
        if (col < 0 || col >= width || row < 0 || row >= height) return false;
        String line = sprite[row];
        int first = width - line.stripLeading().length();
        int last = line.stripTrailing().length() - 1;
        return col >= first && col <= last;
    }

    public Point closestPoint(int col, int row) {
        Point closest = new Point(0, height / 2);
        double best = Double.POSITIVE_INFINITY;
        for (int y = 0; y < height; y++) {
            int first = width - sprite[y].stripLeading().length();
            int last = sprite[y].stripTrailing().length() - 1;
            int x = Math.max(first, Math.min(last, col));
            double distance = Math.hypot((x - col) / 2.0, y - row);
            if (distance < best) {
                best = distance;
                closest = new Point(x, y);
            }
        }
        return closest;
    }

    public void startRepair() {
        if (repairing != null || crew.getAvailable() == 0) return;
        for (Module module : Module.values()) {
            if (getHealth(module) < 100) {
                repairing = module;
                repairTicks = REPAIR_TICKS;
                return;
            }
        }
    }

    public void update(int frame) {
        if (crew.getAvailable() == 0) {
            repairing = null;
            repairTicks = 0;
        }
        crew.update(frame, repairing);
        if (repairing != null && --repairTicks == 0) {
            health.put(repairing, MODULE_HEALTH);
            repairing = null;
        }
    }

    public void damageAt(int col, int row, int amount) {
        damageAt(col, row, amount, 0);
    }

    public void damageAt(int col, int row, int amount, int direction) {
        if (amount <= 0) return;
        col = Math.max(0, Math.min(width - 1, col));
        row = Math.max(0, Math.min(height - 1, row));
        int crewHitX = col;
        for (int step = 0; step <= (direction == 0 ? 0 : Weapons.TORPEDO_PENETRATION); step++) {
            if (navigation != null && navigation.contains(col + step * direction, row)) {
                navigationHit = true;
                crewHitX = col + step * direction;
                break;
            }
            Module hit = null;
            for (Module module : Module.values()) {
                if (rooms[module.ordinal()].contains(col + step * direction, row)) { hit = module; break; }
            }
            if (hit != null) {
                health.put(hit, Math.max(0, health.get(hit) - amount));
                crewHitX = col + step * direction;
                break;
            }
        }
        crew.hit(crewHitX, row, amount);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double distance = Math.hypot((x - col) / 2.0, y - row);
                if (distance <= 1.5 && contains(x, y)) {
                    damage[y][x] = Math.min(100, damage[y][x] + Math.max(1, (int) (amount / (1 + distance))));
                    hullDamage = Math.max(hullDamage, damage[y][x]);
                }
            }
        }
    }

    public static final class Crew {
        public static final int TOTAL = 100;
        private final Submarine ship;
        private final int[] positions = new int[TOTAL];
        private final int[] health = new int[TOTAL];
        private final boolean[] deck;
        private final int[] distance, queue, marks;
        private final int[] steps;
        private final Random random = new Random(0);
        private int living = TOTAL;

        private Crew(Submarine ship) {
            this.ship = ship;
            int cells = ship.width * ship.height;
            deck = new boolean[cells];
            distance = new int[cells];
            queue = new int[cells];
            marks = new int[cells];
            steps = new int[]{-1, 1, -ship.width, ship.width};
            int count = 0;
            for (int row = 1; row < ship.height - 1; row++) {
                for (int col = 1; col < ship.width - 1; col++) {
                    char c = ship.sprite[row].charAt(col);
                    int cell = row * ship.width + col;
                    if (ship.contains(col - 1, row) && ship.contains(col + 1, row)
                            && !Character.isLetter(c) && "_/\\".indexOf(c) < 0) {
                        deck[cell] = true;
                        if (" -=".indexOf(c) >= 0) queue[count++] = cell;
                    }
                }
            }
            if (count == 0) throw new IllegalArgumentException("Vessel must have interior space for its crew");
            for (int i = 0; i < TOTAL; i++) positions[i] = queue[i % count];
            Arrays.fill(health, 100);
            paint();
        }

        public int getLiving() { return living; }
        public Point getPosition(int sailor) { return new Point(positions[sailor] % ship.width, positions[sailor] / ship.width); }
        public boolean isAlive(int sailor) { return health[sailor] > 0; }
        public int getHealth(int sailor) { return health[sailor]; }
        public int getAvailable() { return (int) Arrays.stream(health).filter(hp -> hp == 100).count(); }
        public int colorAt(int col, int row) { return marks[row * ship.width + col]; }

        private void hit(int col, int row, int amount) {
            for (int i = 0; i < TOTAL; i++) {
                double distance = Math.hypot((positions[i] % ship.width - col) / 2.0,
                        positions[i] / ship.width - row);
                if (health[i] > 0 && distance <= 1.5) {
                    health[i] = Math.max(0, health[i] - Math.max(1, (int) (amount / (1 + distance))));
                    if (health[i] == 0) living--;
                }
            }
            paint();
        }

        private void update(int frame, Module repair) {
            if (frame % 3 != 0 || getAvailable() == 0) return;
            Arrays.fill(distance, -1);
            int head = 0, tail = 0;
            for (int cell = 0; cell < deck.length; cell++) {
                if (deck[cell] && (repair == null ? ship.isSafe(cell % ship.width, cell / ship.width)
                        : ship.rooms[repair.ordinal()].contains(cell % ship.width, cell / ship.width))) {
                    distance[cell] = 0;
                    queue[tail++] = cell;
                }
            }
            while (head < tail) {
                int cell = queue[head++];
                for (int step : steps) {
                    int next = cell + step;
                    if (next >= 0 && next < deck.length && deck[next] && distance[next] < 0) {
                        distance[next] = distance[cell] + 1;
                        queue[tail++] = next;
                    }
                }
            }
            for (int i = 0; i < TOTAL; i++) {
                if (health[i] < 100) continue;
                int cell = positions[i];
                int start = random.nextInt(steps.length);
                for (int j = 0; j < steps.length; j++) {
                    int next = cell + steps[(start + j) % steps.length];
                    if (next >= 0 && next < deck.length && deck[next] && distance[next] >= 0
                            && (distance[cell] > 0 ? distance[next] < distance[cell] : distance[next] == 0)) {
                        positions[i] = next;
                        break;
                    }
                }
            }
            paint();
        }

        private void paint() {
            Arrays.fill(marks, 0);
            for (int i = 0; i < TOTAL; i++) {
                int cell = positions[i];
                char c = ship.sprite[cell / ship.width].charAt(cell % ship.width);
                if ((health[i] < 100 || c == ' ') && marks[cell] != 31) marks[cell] = health[i] == 100 ? 37 : 31;
            }
        }
    }
}
