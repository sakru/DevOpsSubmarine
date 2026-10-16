/**
 * Renderer: draws the game state to the console.
 */
import java.util.List;

public class Renderer implements AutoCloseable {

    private final Game game;
    private final Camera camera;
    private TerminalSize terminalSize;
    private int[][] colors;
    private boolean closed;

    public Renderer(Game game) {
        this(game, new TerminalSize());
    }

    private Renderer(Game game, TerminalSize terminalSize) {
        this(game, terminalSize.current().columns(), terminalSize.current().rows());
        this.terminalSize = terminalSize;
    }

    public Renderer(Game game, int columns, int rows) {
        this.game = game;
        this.camera = new Camera(columns - 2, rows - 4);
        Runtime.getRuntime().addShutdownHook(new Thread(this::close, "renderer-restore"));
        System.out.print("\033[?1049h\033[?25l\033[2J\033[H");
        System.out.flush();
    }

    public synchronized void draw() {
        if (closed) {
            return;
        }
        if (!canDrawGame()) {
            String hint = "Enlarge terminal to " + (Submarine.WIDTH + 2) + "x" + (Submarine.HEIGHT + 4)
                    + " to continue (0: quit)";
            StringBuilder frame = new StringBuilder("\033[H");
            appendText(frame, hint);
            System.out.print(frame);
            System.out.flush();
            return;
        }
        camera.follow(game);
        char[][] field = composeField();
        StringBuilder frame = new StringBuilder((Game.FIELD_W + 20) * (Game.FIELD_H + 4));

        moveToLine(frame, 1);
        Submarine sub = game.getSubmarine();
        int enemies = 0;
        for (Enemy enemy : game.getEnemies()) if (enemy.isAlive()) enemies++;
        appendText(frame, "Hull:" + game.getHull() + "% ENG:" + sub.getHealth(Submarine.Module.ENGINE)
                + " SON:" + sub.getHealth(Submarine.Module.SONAR)
                + " TOR:" + sub.getHealth(Submarine.Module.TORPEDO)
                + " V:" + game.getForwardSpeed() + " K:" + game.getKills() + " En:"
                + (sub.isActive(Submarine.Module.SONAR) ? enemies : "--") + " " + game.getDifficulty());
        moveToLine(frame, 2);
        frame.append(border()).append("\033[K");
        for (int row = 0; row < field.length; row++) {
            moveToLine(frame, row + 3);
            frame.append('|');
            int color = 0;
            for (int col = 0; col < field[row].length; col++) {
                if (colors[row][col] != color) {
                    color = colors[row][col];
                    frame.append("\033[").append(color).append('m');
                }
                frame.append(field[row][col]);
            }
            if (color != 0) frame.append("\033[0m");
            frame.append("|\033[K");
        }
        moveToLine(frame, camera.height() + 3);
        frame.append(border()).append("\033[K");
        moveToLine(frame, camera.height() + 4);
        appendText(frame, "Depth:" + game.getDepth() + "/" + Game.MAX_DEPTH
                + " Lv:" + game.getLevel() + " W/S:depth A/D:speed F/Space:fire 0:quit");

        System.out.print(frame);
        System.out.flush();
    }

    public synchronized boolean canDrawGame() {
        if (terminalSize != null) {
            TerminalSize.Size size = terminalSize.current();
            int width = Math.max(1, Math.min(Game.FIELD_W, size.columns() - 2));
            int height = Math.max(1, Math.min(Game.FIELD_H, size.rows() - 4));
            if (camera.width() != width || camera.height() != height) {
                camera.resize(width, height);
                System.out.print("\033[2J");
            }
        }
        return !closed && camera.canFitSubmarine();
    }

    private void moveToLine(StringBuilder frame, int row) {
        frame.append("\033[").append(row).append(";1H");
    }

    private void appendText(StringBuilder frame, String text) {
        frame.append(text, 0, Math.min(text.length(), camera.width() + 2)).append("\033[K");
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        System.out.print("\033[?25h\033[?1049l");
        System.out.flush();
    }

    private char[][] composeField() {
        char[][] field = new char[camera.height()][camera.width()];
        colors = new int[camera.height()][camera.width()];
        for (int row = 0; row < field.length; row++) {
            for (int col = 0; col < field[row].length; col++) {
                field[row][col] = camera.top() + row >= game.getFloor().heightAt(camera.left() + col) ? '#' : ' ';
            }
        }
        for (int row = 0; row < Submarine.HEIGHT; row++) {
            drawLine(field, game.subBoxLeft(), game.subBoxTop(game.subRow()) + row,
                    game.getSubmarine().renderLine(row), false);
            int y = game.subBoxTop(game.subRow()) + row - camera.top();
            if (y < 0 || y >= field.length) continue;
            for (int col = 0; col < Submarine.WIDTH; col++) {
                int x = game.subBoxLeft() + col - camera.left();
                if (x >= 0 && x < camera.width()) colors[y][x] = game.getSubmarine().damageColor(col, row);
            }
        }
        for (Enemy enemy : game.getEnemies()) {
            String[] sprite = enemy.spriteLines();
            for (int row = 0; row < sprite.length; row++) {
                drawLine(field, enemy.getX(), enemy.getY() + row, sprite[row], false);
            }
        }
        for (Projectile projectile : game.getProjectiles()) {
            drawLine(field, projectile.getX(), projectile.getY(), projectile.sprite(), false);
        }
        if (game.getSubmarine().isActive(Submarine.Module.SONAR)) drawEnemyIndicators(field);
        for (Explosion explosion : game.getExplosions()) {
            List<String> sprite = explosion.spriteLines();
            for (int row = 0; row < sprite.size(); row++) {
                drawLine(field, explosion.getX() - explosion.getHorizontalRadius(),
                        explosion.getY() - explosion.getRadius() + row, sprite.get(row), true);
            }
        }
        return field;
    }

    private void drawEnemyIndicators(char[][] field) {
        int right = camera.left() + camera.width() - 1;
        int bottom = camera.top() + camera.height() - 1;
        for (Enemy enemy : game.getEnemies()) {
            int enemyRight = enemy.getX() + enemy.width() - 1;
            int enemyBottom = enemy.getY() + enemy.height() - 1;
            if (enemy.getX() <= right && enemyRight >= camera.left()
                    && enemy.getY() <= bottom && enemyBottom >= camera.top()) continue;
            int x = Math.max(camera.left(), Math.min(right, enemy.getX() + enemy.width() / 2));
            int y = Math.max(camera.top(), Math.min(bottom, enemy.getY() + enemy.height() / 2));
            field[y - camera.top()][x - camera.left()] = enemy.getX() > right ? '>'
                    : enemyRight < camera.left() ? '<' : enemy.getY() > bottom ? 'v' : '^';
            colors[y - camera.top()][x - camera.left()] = 0;
        }
    }

    private void drawLine(char[][] field, int left, int y, String sprite, boolean transparent) {
        int row = y - camera.top();
        if (row < 0 || row >= field.length) return;
        for (int i = 0; i < sprite.length(); i++) {
            int col = left + i - camera.left();
            char cell = sprite.charAt(i);
            if (col >= 0 && col < camera.width() && (!transparent || cell != ' ')) {
                field[row][col] = cell;
                colors[row][col] = 0;
            }
        }
    }

    private String border() {
        return "+" + "-".repeat(camera.width()) + "+";
    }
}
