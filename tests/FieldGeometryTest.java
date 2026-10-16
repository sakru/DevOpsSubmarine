public class FieldGeometryTest {
    public static void main(String[] args) {
        for (int level = Game.MIN_LEVEL; level <= Game.MAX_LEVEL; level++) {
            Game game = new Game(1L, false) {
                @Override public void damageHull(int damage) {}
            };
            game.setLevel(level);
            check(game.getMaxSafeDepth() >= Game.SAFE_DEPTH, "Every level must retain a safe starting depth");
            for (int tick = 0; tick < 240; tick++) {
                game.diveDown();
                game.update();
                checkHull(game);
            }
            for (int tick = 0; tick < 240; tick++) {
                game.diveUp();
                game.update();
                checkHull(game);
            }
            check(game.getDepth() == 0, "Submarine must be able to return to the surface");
        }
        System.out.println("Field geometry checks passed across all 12 levels");
    }

    private static void checkHull(Game game) {
        check(game.getExactDepth() >= 0 && game.getExactDepth() <= Game.MAX_DEPTH, "Depth bounds");
        check(game.subBoxTop(game.subRow()) >= 1, "Hull must stay inside the top of the field");
        for (int x = game.subBoxLeft(); x <= game.subBoxRight(); x++) {
            check(game.subBoxBottom(game.subRow()) < game.getFloor().heightAt(x),
                    "Entire hull must remain above the scrolling seabed");
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
