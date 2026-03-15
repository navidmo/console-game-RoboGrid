/**
 * Represents the robot's state: where it is, which way it faces, and how many moves it has made.
 * <p>
 * The robot is a <em>domain object</em> that knows only about itself. It does <strong>not</strong>
 * know about the grid, rocks, keyboard input, or drawing. That separation keeps the code easier to
 * test and reason about: the {@link RobotController} checks the grid and then tells the robot to
 * move or turn; the robot just updates its own position and direction.
 * </p>
 */
public class Robot {
    private final String name;
    private Position position;
    private Direction direction;
    private int movesTaken;

    /**
     * Creates a robot with the given name, starting position, and initial facing direction.
     *
     * @param name            display name (e.g. "Robo")
     * @param startPosition   cell where the robot begins
     * @param startDirection  direction the robot faces at the start
     */
    public Robot(String name, Position startPosition, Direction startDirection) {
        this.name = name;
        this.position = startPosition;
        this.direction = startDirection;
    }

    /**
     * Returns the robot's display name.
     *
     * @return the name (e.g. "Robo")
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the robot's current cell on the grid.
     *
     * @return current position (row, column)
     */
    public Position getPosition() {
        return position;
    }

    /**
     * Returns the direction the robot is currently facing.
     *
     * @return NORTH, EAST, SOUTH, or WEST
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * Returns how many successful forward moves the robot has made (turns are not counted).
     *
     * @return the number of forward moves
     */
    public int getMovesTaken() {
        return movesTaken;
    }

    /**
     * Computes the position one step ahead in the current facing direction, without moving.
     * The controller uses this to check whether that cell is inside the grid and not a rock
     * before calling {@link #moveForward()}.
     *
     * @return the position that would be reached by moving forward one step
     */
    public Position peekForwardPosition() {
        return direction.move(position);
    }

    /**
     * Moves the robot one step forward in its current direction and increments the move counter.
     * Should only be called after the controller has verified the move is legal (inside grid, no rock).
     */
    public void moveForward() {
        position = peekForwardPosition();
        movesTaken++;
    }

    /**
     * Turns the robot left (counter-clockwise) without changing position.
     */
    public void turnLeft() {
        direction = direction.turnLeft();
    }

    /**
     * Turns the robot right (clockwise) without changing position.
     */
    public void turnRight() {
        direction = direction.turnRight();
    }

    /**
     * Builds a one-line status string for debugging or plain-console output (e.g. "Robo is at (2, 3), facing North, successful moves: 5").
     *
     * @return a human-readable status line
     */
    public String getStatusLine() {
        return name + " is at " + position
            + ", facing " + direction.getLabel()
            + ", successful moves: " + movesTaken;
    }
}
