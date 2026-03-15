import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import java.io.IOException;

/**
 * The user-interface layer for the Robot Rescue Mission game.
 * <p>
 * This class is responsible for: starting the terminal (via Lanterna), reading keyboard input,
 * drawing the grid and sidebar, and delegating game logic to {@link RobotController}. It does
 * <strong>not</strong> decide whether a move is legal—that stays inside the controller. The UI
 * only maps keys to {@link Command}s and displays the result messages. This separation makes
 * it easier to test game rules without a screen and to change the UI (e.g. different keys or
 * graphics) without touching the rules.
 * </p>
 */
public class RobotGame {
    private static final int GRID_START_COLUMN = 4;
    private static final int GRID_START_ROW = 8;
    private static final int SIDEBAR_WIDTH = 44;
    private static final String HELP_LINE_TOP = "Use W or Up key to move the robot.";
    private static final String HELP_LINE_MIDDLE = "Use A or Left key to turn left, D or Right key to turn right.";
    private static final String HELP_LINE_BOTTOM = "Press R for a new random map, or Q to quit.";

    /**
     * Entry point: creates the terminal, starts the screen, and runs the game loop until the user quits.
     *
     * @param args command-line arguments (unused)
     * @throws IOException if terminal or screen setup fails
     */
    public static void main(String[] args) throws IOException {
        DefaultTerminalFactory factory = new DefaultTerminalFactory();
        factory.setInitialTerminalSize(new TerminalSize(110, 30));

        try (Screen screen = new TerminalScreen(factory.createTerminal())) {
            screen.startScreen();
            screen.setCursorPosition(null);
            runGameLoop(screen);
        }
    }

    /**
     * Main game loop: repeatedly draws the current state, reads a key, and updates the game.
     * Handles resize events at the start of each iteration so the layout stays correct.
     */
    static void runGameLoop(Screen screen) throws IOException {
        runGameLoop(screen, new RandomLevelFactory());
    }

    /**
     * Package-private overload used by the end-to-end tests. By accepting a factory object,
     * the tests can provide a predictable map while still exercising the real Lanterna loop.
     */
    static void runGameLoop(Screen screen, RandomLevelFactory levelFactory) throws IOException {
        TerminalSize terminalSize = screen.getTerminalSize();
        GameSetup setup = levelFactory.createSetup(terminalSize);
        RobotController controller = new RobotController(setup.getGrid(), setup.getRobot());
        String message = setup.getBriefing();
        boolean running = true;

        while (running) {
            // Lanterna keeps track of resize events.
            // We ask for the new size at the start of each loop so drawing stays safe.
            terminalSize = syncScreenSize(screen);
            draw(screen, controller, message, terminalSize);
            KeyStroke keyStroke = screen.readInput();

            if (keyStroke == null) {
                continue;
            }

            if (keyStroke.getKeyType() == KeyType.EOF || keyStroke.getKeyType() == KeyType.ESCAPE) {
                break;
            }

            if (keyStroke.getKeyType() == KeyType.CHARACTER) {
                char key = Character.toLowerCase(keyStroke.getCharacter());

                if (key == 'q') {
                    break;
                }

                // Restart creates a completely new board, robot, and briefing.
                if (key == 'r') {
                    setup = levelFactory.createSetup(terminalSize);
                    controller = new RobotController(setup.getGrid(), setup.getRobot());
                    message = setup.getBriefing();
                    continue;
                }
            }

            CommandResult result = toCommandResult(controller, keyStroke);
            if (result != null) {
                message = result.getMessage();

                if (result.hasReachedGoal()) {
                    message = message + " Mission complete. Press R to play again or Q to quit.";
                }
            }
        }
    }

    /**
     * Converts a key press into a command, runs it through the controller, and returns the result.
     * If the key is not a game key, returns a "that key does not control Robo" message. If the
     * robot has already reached the goal, further moves are ignored with a message to press R or Q.
     *
     * @param controller the game controller
     * @param keyStroke  the key that was pressed
     * @return the result of the command, or null if no command was executed (e.g. unknown key)
     */
    private static CommandResult toCommandResult(RobotController controller, KeyStroke keyStroke) {
        Command command = mapKeyToCommand(keyStroke);

        if (command == null) {
            return new CommandResult(false, controller.hasReachedGoal(), "That key does not control Robo. Try W, A, D, R, or Q.");
        }

        if (controller.hasReachedGoal()) {
            return new CommandResult(false, true, "You already reached the goal. Press R for a fresh map.");
        }

        return controller.execute(command);
    }

    /**
     * Maps a Lanterna key stroke to a game command (move forward, turn left, turn right).
     * Arrow keys and character keys (W, A, D, etc.) are supported; other keys return null.
     *
     * @param keyStroke the key that was pressed
     * @return the corresponding Command, or null if the key is not a game command
     */
    private static Command mapKeyToCommand(KeyStroke keyStroke) {
        return switch (keyStroke.getKeyType()) {
            case ARROW_UP -> Command.MOVE_FORWARD;
            case ARROW_LEFT -> Command.TURN_LEFT;
            case ARROW_RIGHT -> Command.TURN_RIGHT;
            case CHARACTER -> mapCharacterKey(keyStroke.getCharacter());
            default -> null;
        };
    }

    /**
     * Maps a character key (e.g. 'w', 'a', 'd') to a command. Used when key type is CHARACTER.
     *
     * @param key the character (may be null)
     * @return the command for that key, or null if not a movement key
     */
    private static Command mapCharacterKey(Character key) {
        if (key == null) {
            return null;
        }

        return switch (Character.toLowerCase(key)) {
            case 'w', 'f' -> Command.MOVE_FORWARD;
            case 'a', 'l' -> Command.TURN_LEFT;
            case 'd' -> Command.TURN_RIGHT;
            default -> null;
        };
    }

    /**
     * Paints one full frame: title, instructions, grid, sidebar, and status message. If the
     * terminal is too small to show the grid, draws a "terminal too small" message instead.
     *
     * @param screen      Lanterna screen to draw on
     * @param controller  provides grid and robot state
     * @param message     current status message to show in the sidebar
     * @param terminalSize current terminal dimensions (for safe drawing and layout)
     */
    private static void draw(Screen screen, RobotController controller, String message, TerminalSize terminalSize) throws IOException {
        screen.clear();
        TextGraphics graphics = screen.newTextGraphics();
        Grid grid = controller.getGrid();
        Robot robot = controller.getRobot();

        graphics.setForegroundColor(TextColor.ANSI.CYAN);
        graphics.enableModifiers(SGR.BOLD);
        safePutString(graphics, terminalSize, 2, 1, "Robot Rescue Mission");
        graphics.disableModifiers(SGR.BOLD);

        graphics.setForegroundColor(TextColor.ANSI.WHITE);
        safePutString(graphics, terminalSize, 2, 3, "Guide Robo across the map and find the charging station.");
        safePutString(graphics, terminalSize, 2, 4, HELP_LINE_TOP);
        safePutString(graphics, terminalSize, 2, 5, HELP_LINE_MIDDLE);
        safePutString(graphics, terminalSize, 2, 6, HELP_LINE_BOTTOM);

        // If the terminal is too small, we skip full drawing and show a message.
        if (!canShowGrid(terminalSize, grid)) {
            drawTooSmallMessage(graphics, terminalSize, message);
            screen.refresh();
            return;
        }

        // Layout decides whether the sidebar appears beside or below the grid.
        Layout layout = chooseLayout(terminalSize, grid);
        drawGrid(graphics, terminalSize, grid, robot, GRID_START_COLUMN, GRID_START_ROW);
        drawSidebar(graphics, terminalSize, controller, message, layout.sidebarColumn(), layout.sidebarRow(), layout.sidebarWidth());

        screen.refresh();
    }

    /**
     * Draws the grid with row/column headers and one symbol per cell (robot, goal, rock, or empty).
     *
     * @param graphics     Lanterna text graphics
     * @param terminalSize used for safe clipping
     * @param grid         the game board
     * @param robot        current robot position and direction
     * @param startColumn  column where the grid starts on screen
     * @param startRow     row where the grid starts on screen
     */
    private static void drawGrid(TextGraphics graphics, TerminalSize terminalSize, Grid grid, Robot robot, int startColumn, int startRow) {
        graphics.setForegroundColor(TextColor.ANSI.WHITE);
        safePutString(graphics, terminalSize, startColumn, startRow - 2, "Grid");

        for (int column = 0; column < grid.getColumns(); column++) {
            safePutString(graphics, terminalSize, startColumn + 4 + (column * 4), startRow - 1, " " + column + " ");
        }

        for (int row = 0; row < grid.getRows(); row++) {
            graphics.setForegroundColor(TextColor.ANSI.WHITE);
            safePutString(graphics, terminalSize, startColumn, startRow + row, row + " ");

            for (int column = 0; column < grid.getColumns(); column++) {
                Position cell = new Position(row, column);
                graphics.setForegroundColor(colorForCell(grid, robot, cell));
                safePutString(graphics, terminalSize, startColumn + 4 + (column * 4), startRow + row, "[" + symbolForCell(grid, robot, cell) + "]");
            }
        }
    }

    /**
     * Draws the adventure panel: robot name, position, direction, goal, move count, legend, and message.
     *
     * @param graphics     Lanterna text graphics
     * @param terminalSize used for safe clipping
     * @param controller   for robot and grid info and goal-reached state
     * @param message      status message (wrapped if long)
     * @param startColumn  left edge of the sidebar
     * @param startRow     top row of the sidebar
     * @param sidebarWidth max width for wrapping the message
     */
    private static void drawSidebar(TextGraphics graphics, TerminalSize terminalSize, RobotController controller, String message, int startColumn, int startRow, int sidebarWidth) {
        Robot robot = controller.getRobot();
        Grid grid = controller.getGrid();

        graphics.setForegroundColor(TextColor.ANSI.YELLOW);
        graphics.enableModifiers(SGR.BOLD);
        safePutString(graphics, terminalSize, startColumn, startRow - 2, "Adventure Panel");
        graphics.disableModifiers(SGR.BOLD);

        graphics.setForegroundColor(TextColor.ANSI.WHITE);
        safePutString(graphics, terminalSize, startColumn, startRow, "Robot friend: " + robot.getName());
        safePutString(graphics, terminalSize, startColumn, startRow + 1, "Current spot: " + robot.getPosition());
        safePutString(graphics, terminalSize, startColumn, startRow + 2, "Facing: " + robot.getDirection().getLabel());
        safePutString(graphics, terminalSize, startColumn, startRow + 3, "Goal: " + grid.getGoal());
        safePutString(graphics, terminalSize, startColumn, startRow + 4, "Moves made: " + robot.getMovesTaken());
        safePutString(graphics, terminalSize, startColumn, startRow + 6, "How To Read The Map");

        graphics.setForegroundColor(TextColor.ANSI.CYAN);
        safePutString(graphics, terminalSize, startColumn, startRow + 7, "[^ > v <] Robo and direction");
        graphics.setForegroundColor(TextColor.ANSI.GREEN);
        safePutString(graphics, terminalSize, startColumn, startRow + 8, "[G] Charging station");
        graphics.setForegroundColor(TextColor.ANSI.RED);
        safePutString(graphics, terminalSize, startColumn, startRow + 9, "[#] Rock");
        graphics.setForegroundColor(TextColor.ANSI.WHITE);
        safePutString(graphics, terminalSize, startColumn, startRow + 10, "[.] Open path");
        safePutString(graphics, terminalSize, startColumn, startRow + 12, "Quick Tips");
        safePutString(graphics, terminalSize, startColumn, startRow + 13, "1. Turn first, then move.");
        safePutString(graphics, terminalSize, startColumn, startRow + 14, "2. Avoid rocks and edges.");
        safePutString(graphics, terminalSize, startColumn, startRow + 15, "3. Press R for a fresh puzzle.");

        graphics.setForegroundColor(controller.hasReachedGoal() ? TextColor.ANSI.GREEN : TextColor.ANSI.WHITE);
        drawWrappedText(graphics, terminalSize, startColumn, startRow + 17, sidebarWidth, message, 3);
    }

    /**
     * Chooses the color for a cell: cyan for robot, green for goal, red for rock, white for empty.
     */
    private static TextColor colorForCell(Grid grid, Robot robot, Position cell) {
        if (robot.getPosition().sameCell(cell)) {
            return TextColor.ANSI.CYAN;
        }

        if (grid.isGoal(cell)) {
            return TextColor.ANSI.GREEN;
        }

        if (grid.hasRock(cell)) {
            return TextColor.ANSI.RED;
        }

        return TextColor.ANSI.WHITE;
    }

    /**
     * Chooses the character for a cell: ^ > v < for robot (by direction), G for goal, # for rock, . for empty.
     */
    private static char symbolForCell(Grid grid, Robot robot, Position cell) {
        if (robot.getPosition().sameCell(cell)) {
            return switch (robot.getDirection()) {
                case NORTH -> '^';
                case EAST -> '>';
                case SOUTH -> 'v';
                case WEST -> '<';
            };
        }

        if (grid.isGoal(cell)) {
            return 'G';
        }

        if (grid.hasRock(cell)) {
            return '#';
        }

        return '.';
    }

    /** Trims text to fit within the given width, appending "..." if shortened. */
    private static String trimToWidth(String text, int width) {
        if (text.length() <= width) {
            return text;
        }

        return text.substring(0, width - 3) + "...";
    }

    /**
     * Draws text across multiple lines, breaking at spaces when possible so the sidebar stays readable.
     *
     * @param maxLines maximum number of lines to draw (extra text is dropped)
     */
    private static void drawWrappedText(TextGraphics graphics, TerminalSize terminalSize, int startColumn, int startRow, int width, String text, int maxLines) {
        String remaining = text;

        for (int line = 0; line < maxLines && !remaining.isEmpty(); line++) {
            String currentLine = bestLine(remaining, width);
            safePutString(graphics, terminalSize, startColumn, startRow + line, currentLine);
            remaining = remaining.substring(currentLine.length()).trim();
        }
    }

    /** Returns the first line of text that fits in width, breaking at the last space if possible. */
    private static String bestLine(String text, int width) {
        if (text.length() <= width) {
            return text;
        }

        int breakPoint = text.lastIndexOf(' ', width);
        if (breakPoint <= 0) {
            return text.substring(0, width);
        }

        return text.substring(0, breakPoint);
    }

    /** Applies any pending terminal resize to the screen and returns the current size. */
    private static TerminalSize syncScreenSize(Screen screen) throws IOException {
        TerminalSize updatedSize = screen.doResizeIfNecessary();
        if (updatedSize != null) {
            return updatedSize;
        }
        return screen.getTerminalSize();
    }

    /** Returns true if the terminal is large enough to draw the full grid and a bit of margin. */
    private static boolean canShowGrid(TerminalSize terminalSize, Grid grid) {
        int requiredColumns = GRID_START_COLUMN + 4 + (grid.getColumns() * 4);
        int requiredRows = GRID_START_ROW + grid.getRows();
        return terminalSize.getColumns() >= requiredColumns && terminalSize.getRows() >= requiredRows + 1;
    }

    /**
     * Chooses layout: sidebar beside the grid if the window is wide enough, otherwise below the grid.
     */
    private static Layout chooseLayout(TerminalSize terminalSize, Grid grid) {
        int gridWidth = 4 + (grid.getColumns() * 4);
        int sideBySideStart = GRID_START_COLUMN + gridWidth + 6;
        int stackedStart = GRID_START_ROW + grid.getRows() + 3;

        boolean canShowSideBySide = terminalSize.getColumns() >= sideBySideStart + SIDEBAR_WIDTH
            && terminalSize.getRows() >= GRID_START_ROW + 20;

        if (canShowSideBySide) {
            return new Layout(sideBySideStart, GRID_START_ROW, SIDEBAR_WIDTH);
        }

        int availableWidth = Math.max(24, terminalSize.getColumns() - GRID_START_COLUMN - 2);
        return new Layout(GRID_START_COLUMN, stackedStart, availableWidth);
    }

    /**
     * Draws a message asking the user to enlarge the terminal or press R for a smaller map.
     * Used when the terminal is too small to show the full grid.
     */
    private static void drawTooSmallMessage(TextGraphics graphics, TerminalSize terminalSize, String message) {
        graphics.setForegroundColor(TextColor.ANSI.YELLOW);
        safePutString(graphics, terminalSize, 2, 9, "Your terminal is too small for this map right now.");
        graphics.setForegroundColor(TextColor.ANSI.WHITE);
        safePutString(graphics, terminalSize, 2, 11, "Please enlarge the terminal window or press R for a smaller random map.");
        safePutString(graphics, terminalSize, 2, 13, trimToWidth(message, Math.max(10, terminalSize.getColumns() - 4)));
    }

    /**
     * Draws a string only within the visible terminal area. Clips or skips drawing if the position
     * is off-screen or the text would extend past the right edge, so we never draw outside the terminal.
     */
    private static void safePutString(TextGraphics graphics, TerminalSize terminalSize, int column, int row, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        if (row < 0 || row >= terminalSize.getRows() || column >= terminalSize.getColumns()) {
            return;
        }

        int safeColumn = Math.max(0, column);
        int startOffset = Math.max(0, -column);
        int availableWidth = terminalSize.getColumns() - safeColumn;

        if (availableWidth <= 0 || startOffset >= text.length()) {
            return;
        }

        String visibleText = text.substring(startOffset);
        if (visibleText.length() > availableWidth) {
            visibleText = visibleText.substring(0, availableWidth);
        }

        if (!visibleText.isEmpty()) {
            graphics.putString(safeColumn, row, visibleText);
        }
    }

    /**
     * Holds layout values for the sidebar: where it starts (column, row) and its width.
     * Used by {@link #chooseLayout} and {@link #drawSidebar}.
     */
    private record Layout(int sidebarColumn, int sidebarRow, int sidebarWidth) {
    }
}
