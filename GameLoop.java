public class GameLoop {
    private boolean running = true;
    private int depth = 5;   // submarine depth 0-10
    private int level = 1;    // sea floor level 1-max (floor occupies bottom 'level' rows)
    
    public void start() {
        while (running) {
            handleInput();
            updateGameLogic();
            clearScreen();
            drawUI();
            pauseForFrame();
        }
    }
    
    private void handleInput() {
        // TODO: read key (W/S for depth, arrows for direction)
        // Update submarine depth (0-10). Clamp to [0,10]
    }
    
    private void updateGameLogic() {
        // TODO: update position of submarine, sea floor level, enemy positions
        // Empty for now — implemented as needed
    }
    
    private void clearScreen() {
        // Use ANSI escape to clear console (works in most modern consoles)
        System.out.print("\u000c");
    }
    
    private void drawUI() {
        // Draw simple ASCII scene:
        // - Field height = 20 rows, width = 10 chars per row
        // - Water above floor = '~'
        // - Floor = '#' (bottom 'level' rows)
        // - Submarine = '🛥️' at row = 20 - depth (depth 0 → row 20 (surface), depth 10 → row 10 (bottom))
        // - HUD: "Hull: 100%" at top
        // - Status: "Depth: d/10  Level: l" at bottom
        
        int fieldHeight = 20;
        int fieldWidth = 10;
        int floorStartRow = fieldHeight - level + 1; // 1-based row where floor starts
        int subRow = fieldHeight - depth;             // 1-based row for submarine (depth 0 → row 20, depth 10 → row 10)
        
        // HUD line
        System.out.println("Hull: 100%");
        
        // Field rows 1 to fieldHeight
        for (int row = 1; row <= fieldHeight; row++) {
            if (row < floorStartRow) {
                // Water above floor
                System.out.print("          "); // 10 spaces as placeholder
                System.out.println();
            } else if (row >= floorStartRow) {
                // Floor or submarine row
                if (row == subRow) {
                    // Submarine row: place '🛥️' centered in 10 chars
                    System.out.print("       🛥️   "); // 10 chars: 4 spaces, '🛥️', 5 spaces
                } else {
                    // Floor row: all '#'
                    System.out.print("###########"); // 10 '#'
                }
                System.out.println();
            }
        }
        
        // Status bar (bottom)
        System.out.println("Depth: " + depth + "/10  Level: " + level);
    }
    
    private void pauseForFrame() {
        // Wait ~40–50 ms for ~20–25 FPS
        // Use Thread.sleep(40) — fixed frame rate
        try {
            Thread.sleep(40);
        } catch (InterruptedException e) {
            running = false;
        }
    }
    
    // Method to stop cleanly (e.g., on Ctrl+C)
    public void stop() {
        running = false;
    }
}