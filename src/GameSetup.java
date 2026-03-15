/**
 * Holds everything needed to start one new game: the grid, the robot, and the briefing message.
 * <p>
 * The {@link RandomLevelFactory} creates a complete starting state and returns it as a single
 * {@code GameSetup} instead of three separate values. The UI or main loop can then get the grid,
 * robot, and briefing from one object, which keeps the code cleaner and makes it easy to add more
 * setup data later (e.g. level name or difficulty).
 * </p>
 */
public class GameSetup {
    private final Grid grid;
    private final Robot robot;
    private final String briefing;

    /**
     * Creates a game setup with the given grid, robot, and briefing text.
     *
     * @param grid     the game board (size, rocks, goal)
     * @param robot    the robot with its starting position and direction
     * @param briefing short message shown to the player (e.g. "Reach the green goal at (5, 8).")
     */
    public GameSetup(Grid grid, Robot robot, String briefing) {
        this.grid = grid;
        this.robot = robot;
        this.briefing = briefing;
    }

    /**
     * Returns the grid for this game.
     *
     * @return the game board
     */
    public Grid getGrid() {
        return grid;
    }

    /**
     * Returns the robot for this game.
     *
     * @return the robot (with start position and direction)
     */
    public Robot getRobot() {
        return robot;
    }

    /**
     * Returns the briefing message to show the player at the start of the level.
     *
     * @return the briefing text
     */
    public String getBriefing() {
        return briefing;
    }
}
