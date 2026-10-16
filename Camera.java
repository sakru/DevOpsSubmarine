/*
 * Chooses which part of the map fits in the terminal.
 * It follows the player, keeps the whole submarine in view and stops at the map edges.
 */
public final class Camera {
    private int width;
    private int height;
    private int left;
    private int top = 1;

    public Camera(int width, int height) {
        resize(width, height);
    }

    public void resize(int width, int height) {
        this.width = Math.max(1, Math.min(Game.FIELD_W, width));
        this.height = Math.max(1, Math.min(Game.FIELD_H, height));
    }

    public boolean canFitSubmarine() {
        return width >= Submarine.WIDTH && height >= Submarine.HEIGHT;
    }

    public void follow(Game game) {
        int marginX = Math.min(8, Math.max(0, (width - Submarine.WIDTH) / 2));
        int marginY = Math.min(4, Math.max(0, (height - Submarine.HEIGHT) / 2));
        if (game.subBoxLeft() < left + marginX) left = game.subBoxLeft() - marginX;
        if (game.subBoxRight() >= left + width - marginX) {
            left = game.subBoxRight() - width + marginX + 1;
        }
        if (game.subBoxTop(game.subRow()) < top + marginY) {
            top = game.subBoxTop(game.subRow()) - marginY;
        }
        if (game.subBoxBottom(game.subRow()) >= top + height - marginY) {
            top = game.subBoxBottom(game.subRow()) - height + marginY + 1;
        }
        left = Math.max(0, Math.min(Game.FIELD_W - width, left));
        top = Math.max(1, Math.min(Game.FIELD_H - height + 1, top));
    }

    public int left() { return left; }
    public int top() { return top; }
    public int width() { return width; }
    public int height() { return height; }
}
