import java.util.List;

/**
 * The game rule engine: decides whether moves are legal and executes commands.
 * <p>
 * The controller sits between the UI ({@link RobotGame}) and the domain objects ({@link Grid},
 * {@link Robot}). The UI turns keyboard input into {@link Command}s and asks the controller to
 * {@link #execute(Command) execute} them. The controller answers: "Is this move legal?" and
 * "What should happen next?" It checks the grid (bounds and rocks) before telling the robot to
 * move, so all game rules live here instead of in the UI or the robot.
 * </p>
 */
public class RobotController {
    private final Grid grid;
    private final Robot robot;

    /**
     * Creates a controller for the given grid and robot. Validates that the robot starts inside
     * the grid and not on a rock; throws if the setup is invalid so bugs are caught early.
     *
     * @param grid  the game board
     * @param robot the robot (must be placed inside the grid and not on a rock)
     * @throws IllegalArgumentException if the robot is outside the grid or on a rock
     */
    public RobotController(Grid grid, Robot robot) {
        this.grid = grid;
        this.robot = robot;

        if (!grid.isInside(robot.getPosition())) {
            throw new IllegalArgumentException("Robot must start inside the grid.");
        }

        if (grid.hasRock(robot.getPosition())) {
            throw new IllegalArgumentException("Robot cannot start on a rock.");
        }
    }

    /**
     * Runs a list of commands one after another and prints each step to the console.
     * Useful for demos or automated tests without the graphical UI.
     *
     * @param commands the sequence of commands to execute
     */
    public void runScript(List<Command> commands) {
        for (int step = 0; step < commands.size(); step++) {
            Command command = commands.get(step);
            CommandResult result = execute(command);

            System.out.println("Step " + (step + 1) + ": " + command.getLabel());
            System.out.println(result.getMessage());
            System.out.println(grid.render(robot));
            System.out.println(robot.getStatusLine());
            System.out.println();

            if (result.hasReachedGoal()) {
                System.out.println("Mission complete: the robot reached the charging station.");
                break;
            }
        }
    }

    /**
     * Executes one command (move forward, turn left, or turn right) and returns the result.
     * For moves, the controller checks the next cell first; if it is off the grid or has a rock,
     * the move is rejected and a message is returned without changing the robot.
     *
     * @param command the command to execute
     * @return a result describing whether the command succeeded and what message to show the player
     */
    public CommandResult execute(Command command) {
        return switch (command) {
            case MOVE_FORWARD -> moveForward();
            case TURN_LEFT -> turnLeft();
            case TURN_RIGHT -> turnRight();
        };
    }

    /**
     * Returns whether the robot is currently on the goal (charging station) cell.
     *
     * @return true if the robot has reached the goal
     */
    public boolean hasReachedGoal() {
        return grid.isGoal(robot.getPosition());
    }

    /**
     * Returns the robot managed by this controller.
     *
     * @return the robot
     */
    public Robot getRobot() {
        return robot;
    }

    /**
     * Returns the grid managed by this controller.
     *
     * @return the grid
     */
    public Grid getGrid() {
        return grid;
    }

    private CommandResult turnLeft() {
        robot.turnLeft();
        return buildResult(true, robot.getName() + " turned left and is now facing " + robot.getDirection().getLabel() + ".");
    }

    private CommandResult turnRight() {
        robot.turnRight();
        return buildResult(true, robot.getName() + " turned right and is now facing " + robot.getDirection().getLabel() + ".");
    }

    /**
     * Tries to move the robot forward one step. Checks that the next cell is inside the grid and
     * not a rock; only then updates the robot's position and returns success.
     */
    private CommandResult moveForward() {
        Position nextPosition = robot.peekForwardPosition();

        if (!grid.isInside(nextPosition)) {
            return buildResult(false, "The edge of the map is there, so " + robot.getName() + " stayed put.");
        }

        if (grid.hasRock(nextPosition)) {
            return buildResult(false, robot.getName() + " spotted a rock and stayed safe.");
        }

        robot.moveForward();
        return buildResult(true, robot.getName() + " moved forward to " + robot.getPosition() + ".");
    }

    private CommandResult buildResult(boolean successful, String message) {
        return new CommandResult(successful, grid.isGoal(robot.getPosition()), message);
    }
}
