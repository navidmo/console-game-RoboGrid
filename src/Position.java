/**
 * Represents a single cell on the game grid by its row and column indices.
 * <p>
 * This is a small <em>value object</em>: it holds data (row, column) and has no behavior
 * beyond simple queries and coordinate math. The class is <em>immutable</em>—once created,
 * a {@code Position} never changes. When the robot "moves", we do not modify an existing
 * {@code Position}; we create a new one. Immutability helps avoid bugs where one part of
 * the program changes a position and other parts see unexpected updates.
 * </p>
 * <p>
 * Row and column are zero-based: the top-left cell is (0, 0), and row increases downward
 * while column increases to the right.
 * </p>
 */
public class Position {
    private final int row;
    private final int column;

    /**
     * Creates a position for the given row and column.
     *
     * @param row    zero-based row index (0 = top)
     * @param column zero-based column index (0 = left)
     */
    public Position(int row, int column) {
        this.row = row;
        this.column = column;
    }

    /**
     * Returns the zero-based row index of this position.
     *
     * @return the row index
     */
    public int getRow() {
        return row;
    }

    /**
     * Returns the zero-based column index of this position.
     *
     * @return the column index
     */
    public int getColumn() {
        return column;
    }

    /**
     * Creates a new position by adding the given row and column deltas to this position.
     * Used when the robot moves one step in a direction (e.g. North = -1 row, 0 column).
     *
     * @param rowChange    how many rows to add (negative = up, positive = down)
     * @param columnChange how many columns to add (negative = left, positive = right)
     * @return a new {@code Position} at (this.row + rowChange, this.column + columnChange)
     */
    public Position translate(int rowChange, int columnChange) {
        return new Position(row + rowChange, column + columnChange);
    }

    /**
     * Checks whether this position and the other refer to the same cell (same row and column).
     * Handles {@code null} safely: returns {@code false} if {@code other} is null.
     *
     * @param other another position to compare (may be null)
     * @return true if both positions have the same row and column
     */
    public boolean sameCell(Position other) {
        return other != null && row == other.row && column == other.column;
    }

    @Override
    public String toString() {
        return "(" + row + ", " + column + ")";
    }
}
