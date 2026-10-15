import java.util.Arrays;

public final class Submarine {

    private static final String[] SPRITE = {
        "                 ____                   ",
        "              ___/  \\___                ",
        "    ________/            \\__________    ",
        "/      .   .   DEVOPS MK1   .   .      \\",
        "<______________________________________>"
    };

    public static final int WIDTH = Arrays.stream(SPRITE).mapToInt(String::length).max().orElseThrow();
    public static final int HEIGHT = SPRITE.length;
    public static final int OFFSET_X = -1;
    public static final int OFFSET_Y = -2;

    private Submarine() {
    }

    public static String spriteLine(int row) {
        return SPRITE[row];
    }
}
