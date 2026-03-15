/**
 * The four compass directions the robot can face.
 * <p>
 * We use an {@code enum} because the robot can only face one of these four values—no other
 * directions exist in the game. Each constant (NORTH, EAST, SOUTH, WEST) stores the
 * <em>delta</em> that gets applied to the robot's position when it moves forward: for
 * example, NORTH means "row decreases by 1, column unchanged" so the robot moves up the grid.
 * </p>
 * <p>
 * Grid layout: row 0 is the top, row increases downward; column 0 is the left, column
 * increases to the right. So NORTH = (-1, 0), EAST = (0, 1), SOUTH = (1, 0), WEST = (0, -1).
 * </p>
 */
public enum Direction {
    NORTH(-1, 0),
    EAST(0, 1),
    SOUTH(1, 0),
    WEST(0, -1);

    private final int rowChange;
    private final int columnChange;

    Direction(int rowChange, int columnChange) {
        this.rowChange = rowChange;
        this.columnChange = columnChange;
    }

    /**
     * Computes the position one step ahead in this direction from the given position.
     * Does not change any state; used so the controller can check for rocks and edges
     * before actually moving the robot.
     *
     * @param position current position
     * @return the position that would be reached by moving one step in this direction
     */
    public Position move(Position position) {
        return position.translate(rowChange, columnChange);
    }

    /**
     * Returns the direction obtained by turning left (counter-clockwise) from this direction.
     * For example, NORTH turns to WEST, WEST to SOUTH, and so on.
     *
     * @return the new direction after a left turn
     */
    public Direction turnLeft() {
        return switch (this) {
            case NORTH -> WEST;
            case WEST -> SOUTH;
            case SOUTH -> EAST;
            case EAST -> NORTH;
        };
    }

    /**
     * Returns the direction obtained by turning right (clockwise) from this direction.
     * For example, NORTH turns to EAST, EAST to SOUTH, and so on.
     *
     * @return the new direction after a right turn
     */
    public Direction turnRight() {
        return switch (this) {
            case NORTH -> EAST;
            case EAST -> SOUTH;
            case SOUTH -> WEST;
            case WEST -> NORTH;
        };
    }

    /**
     * Human-readable label for this direction, used in the UI (e.g. "North" instead of "NORTH").
     *
     * @return a short label such as "North", "East", "South", or "West"
     */
    public String getLabel() {
        return switch (this) {
            case NORTH -> "North";
            case EAST -> "East";
            case SOUTH -> "South";
            case WEST -> "West";
        };
    }
}
