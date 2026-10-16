/**
 * Renderer: draws the game state to the console.
 */
import java.util.Arrays;

public class Renderer implements AutoCloseable {

    private final Game game;
    private boolean closed;

    public Renderer(Game game) {
        this.game = game;
        Runtime.getRuntime().addShutdownHook(new Thread(this::close, "renderer-restore"));
        System.out.print("\033[?1049h\033[?25l\033[2J\033[H");
        System.out.flush();
    }

    public synchronized void draw() {
        if (closed) {
            return;
        }
        int anchorRow = game.subRow();
        SeaFloor floor = game.getFloor();
        StringBuilder frame = new StringBuilder((Game.FIELD_W + 20) * (Game.FIELD_H + 4));

        moveToLine(frame, 1);
        frame.append("Hull: ").append(game.getHull())
             .append("%  Distance: ").append(game.getDistance())
             .append("  Kills: ").append(game.getKills()).append("\033[K");
        moveToLine(frame, 2);
        frame.append(border()).append("\033[K");
        for (int row = 1; row <= Game.FIELD_H; row++) {
            char[] line = new char[Game.FIELD_W];
            Arrays.fill(line, ' ');
            for (int col = 0; col < Game.FIELD_W; col++) {
                if (row >= floor.heightAt(col)) {
                    line[col] = '#';
                }
            }
            drawSubOnLine(line, row, anchorRow);
            drawEnemiesOnLine(line, row);
            drawProjectilesOnLine(line, row);
            drawExplosionsOnLine(line, row);
            moveToLine(frame, row + 2);
            frame.append('|').append(line).append("|\033[K");
        }
        moveToLine(frame, Game.FIELD_H + 3);
        frame.append(border()).append("\033[K");
        moveToLine(frame, Game.FIELD_H + 4);
        frame.append("Depth: ").append(game.getDepth())
             .append("/").append(Game.MAX_DEPTH)
             .append("  Level: ").append(game.getLevel())
             .append("  W/S or arrows: depth  Space/F: torpedo  0: quit\033[K");

        System.out.print(frame);
        System.out.flush();
    }

    private void moveToLine(StringBuilder frame, int row) {
        frame.append("\033[").append(row).append(";1H");
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

    private void drawSubOnLine(char[] line, int row, int anchorRow) {
        int spriteRow = row - game.subBoxTop(anchorRow);
        if (spriteRow >= 0 && spriteRow < Submarine.HEIGHT) {
            drawSprite(line, game.subBoxLeft(), Submarine.spriteLine(spriteRow));
        }
    }

    private void drawEnemiesOnLine(char[] line, int row) {
        for (Enemy e : game.getEnemies()) {
            String[] sprite = e.spriteLines();
            int spriteRow = row - e.getY();
            if (spriteRow >= 0 && spriteRow < sprite.length) {
                drawSprite(line, e.getX(), sprite[spriteRow]);
            }
        }
    }

    private void drawProjectilesOnLine(char[] line, int row) {
        for (Projectile p : game.getProjectiles()) {
            if (p.getY() != row) continue;
            drawSprite(line, p.getX(), p.sprite());
        }
    }

    private void drawExplosionsOnLine(char[] line, int row) {
        for (Explosion explosion : game.getExplosions()) {
            int spriteRow = row - (explosion.getY() - explosion.getRadius());
            if (!explosion.isDone() && spriteRow >= 0 && spriteRow < explosion.spriteLines().size()) {
                drawSprite(line, explosion.getX() - explosion.getRadius(),
                        explosion.spriteLines().get(spriteRow), true);
            }
        }
    }

    private void drawSprite(char[] line, int left, String sprite) {
        drawSprite(line, left, sprite, false);
    }

    private void drawSprite(char[] line, int left, String sprite, boolean transparent) {
        for (int i = 0; i < sprite.length(); i++) {
            int col = left + i;
            if (col >= 0 && col < line.length && (!transparent || sprite.charAt(i) != ' ')) {
                line[col] = sprite.charAt(i);
            }
        }
    }

    private String border() {
        StringBuilder sb = new StringBuilder("+");
        for (int i = 0; i < Game.FIELD_W; i++) sb.append('-');
        return sb.append('+').toString();
    }
}
