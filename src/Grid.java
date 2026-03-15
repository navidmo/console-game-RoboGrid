/**
 * Represents the game board: size, obstacle (rock) positions, and goal cell.
 * <p>
 * The grid is immutable in shape and goal—rows, columns, and goal are set at construction.
 * Rocks are added after construction via {@link #addRock(int, int)}. The controller and UI
 * use this class to check whether a position is inside the grid, has a rock, or is the goal,
 * so all board rules live in one place.
 * </p>
 */
public class Grid {
    private final int rows;
    private final int columns;
    private final boolean[][] rocks;
    private final Position goal;

    /**
     * Creates a grid with the given dimensions and goal position. The goal must be inside the grid.
     * Rocks are not placed here; they are added later with {@link #addRock(int, int)}.
     *
     * @param rows    number of rows (must be positive)
     * @param columns number of columns (must be positive)
     * @param goal    the charging-station cell the robot must reach
     * @throws IllegalArgumentException if rows or columns are non-positive, or goal is outside the grid
     */
    public Grid(int rows, int columns, Position goal) {
        if (rows <= 0 || columns <= 0) {
            throw new IllegalArgumentException("Grid size must be positive.");
        }

        this.rows = rows;
        this.columns = columns;
        this.goal = goal;
        this.rocks = new boolean[rows][columns];

        if (!isInside(goal)) {
            throw new IllegalArgumentException("Goal must be inside the grid.");
        }
    }

    /**
     * Marks a cell as blocked by a rock. The cell must be inside the grid and must not be the goal.
     *
     * @param row    row index of the rock
     * @param column column index of the rock
     * @throws IllegalArgumentException if the position is outside the grid or is the goal
     */
    public void addRock(int row, int column) {
        Position rockPosition = new Position(row, column);

        if (!isInside(rockPosition)) {
            throw new IllegalArgumentException("Rock must be inside the grid.");
        }

        if (goal.sameCell(rockPosition)) {
            throw new IllegalArgumentException("Rock cannot be placed on the goal.");
        }

        rocks[row][column] = true;
    }

    /**
     * Returns the number of rows in the grid.
     *
     * @return row count
     */
    public int getRows() {
        return rows;
    }

    /**
     * Returns the number of columns in the grid.
     *
     * @return column count
     */
    public int getColumns() {
        return columns;
    }

    /**
     * Checks whether the given position is within the grid bounds (0 to rows-1, 0 to columns-1).
     * Used to prevent the robot from moving off the board.
     *
     * @param position the position to check
     * @return true if the position is inside the grid
     */
    public boolean isInside(Position position) {
        return position.getRow() >= 0
            && position.getRow() < rows
            && position.getColumn() >= 0
            && position.getColumn() < columns;
    }

    /**
     * Returns whether the given cell contains a rock (blocked). Returns false if the position is outside the grid.
     *
     * @param position the cell to check
     * @return true if that cell has a rock
     */
    public boolean hasRock(Position position) {
        return isInside(position) && rocks[position.getRow()][position.getColumn()];
    }

    /**
     * Returns whether the given position is the goal (charging station) cell.
     *
     * @param position the position to check
     * @return true if this is the goal cell
     */
    public boolean isGoal(Position position) {
        return goal.sameCell(position);
    }

    /**
     * Returns the goal position (charging station).
     *
     * @return the goal cell
     */
    public Position getGoal() {
        return goal;
    }

    /**
     * Builds a text representation of the grid with row/column headers and one character per cell
     * (robot, goal, rock, or empty). Used for the plain-console version or debugging.
     *
     * @param robot the robot to draw on the grid (its position and direction affect the output)
     * @return a multi-line string showing the board
     */
    public String render(Robot robot) {
        StringBuilder builder = new StringBuilder();

        builder.append("  ");
        for (int column = 0; column < columns; column++) {
            builder.append(column).append(' ');
        }
        builder.append(System.lineSeparator());

        for (int row = 0; row < rows; row++) {
            builder.append(row).append(' ');

            for (int column = 0; column < columns; column++) {
                Position cell = new Position(row, column);
                builder.append(symbolAt(cell, robot)).append(' ');
            }

            builder.append(System.lineSeparator());
        }

        return builder.toString();
    }

    /**
     * Chooses the character to display for one cell (robot, goal, rock, or empty). Robot takes precedence so it shows on top.
     */
    private char symbolAt(Position cell, Robot robot) {
        if (robot.getPosition().sameCell(cell)) {
            return 'R';
        }

        if (isGoal(cell)) {
            return 'G';
        }

        if (hasRock(cell)) {
            return '#';
        }

        return '.';
    }
}
