import java.util.Arrays;

public final class Sonar {
    private final int[][] echoes = new int[Game.FIELD_H + 1][Game.FIELD_W];
    private int originX, originY, pingFrame, nextPing;
    private double radius = -1;
    private boolean enabled;

    public void update(int frame, int scroll, int x, int y, boolean active) {
        enabled = active;
        if (scroll > 0) {
            for (int[] row : echoes) {
                System.arraycopy(row, scroll, row, 0, row.length - scroll);
                Arrays.fill(row, row.length - scroll, row.length, 0);
            }
            originX -= scroll;
        }
        if (!active) {
            radius = -1;
            nextPing = frame;
            return;
        }
        if (frame >= nextPing) {
            originX = x;
            originY = y;
            pingFrame = frame;
            nextPing = frame + Weapons.SONAR_INTERVAL;
        }
        radius = (frame - pingFrame) * Weapons.SONAR_WAVE_SPEED;
        for (int row = 1; row <= Game.FIELD_H; row++) {
            double dy = row - originY;
            if (Math.abs(dy) > radius) continue;
            int outer = (int) (2 * Math.sqrt(radius * radius - dy * dy));
            double previous = radius - Weapons.SONAR_WAVE_SPEED;
            int inner = previous >= Math.abs(dy) ? (int) (2 * Math.sqrt(previous * previous - dy * dy)) : -1;
            if (inner < 0) {
                reveal(row, originX - outer, originX + outer, frame);
            } else {
                reveal(row, originX - outer, originX - inner - 1, frame);
                reveal(row, originX + inner + 1, originX + outer, frame);
            }
        }
    }

    private void reveal(int row, int left, int right, int frame) {
        left = Math.max(0, left);
        right = Math.min(Game.FIELD_W - 1, right);
        if (left <= right) Arrays.fill(echoes[row], left, right + 1, frame + Weapons.SONAR_ECHO_TICKS);
    }

    public boolean isWaveAt(int x, int y) {
        double dx = (x - originX) / 2.0;
        double dy = y - originY;
        double squared = dx * dx + dy * dy;
        double previous = radius - Weapons.SONAR_WAVE_SPEED;
        return enabled && radius >= 0 && squared <= radius * radius
                && (previous < 0 || squared > previous * previous);
    }

    public boolean isVisible(int x, int y, int frame) {
        return enabled && x >= 0 && x < Game.FIELD_W && y >= 1 && y <= Game.FIELD_H && echoes[y][x] > 0;
    }

    public boolean detects(int x, int y, int width, int height, int frame) {
        for (int row = Math.max(1, y); row < Math.min(Game.FIELD_H + 1, y + height); row++) {
            for (int col = Math.max(0, x); col < Math.min(Game.FIELD_W, x + width); col++) {
                if (enabled && echoes[row][col] > frame) return true;
            }
        }
        return false;
    }

    public int echoColor(int x, int y, int frame) {
        return echoes[y][x] - frame > Weapons.SONAR_ECHO_TICKS / 3 ? 32 : 90;
    }
}
