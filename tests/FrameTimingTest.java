public class FrameTimingTest {
    public static void main(String[] args) {
        long start = 100_000_000L;
        check(Main.remainingFrameNanos(start, start) == 40_000_000L, "Idle frame"); // 40ms+-
        check(Main.remainingFrameNanos(start, start + 12_000_000L) == 28_000_000L,
                "Processing time must be deducted from sleep");
        check(Main.remainingFrameNanos(start, start + 40_000_000L) == 0, "Full frame");
        check(Main.remainingFrameNanos(start, start + 65_000_000L) == 0,
                "Slow frames must not add sleep");
        System.out.println("Frame timing checks passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
