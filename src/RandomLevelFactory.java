import com.googlecode.lanterna.TerminalSize;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Creates a new random puzzle (grid, robot, briefing) each time the game starts or the player presses R.
 * <p>
 * Putting level generation in its own class keeps {@link RobotGame} focused on input and drawing.
 * The factory picks grid size (optionally based on terminal size), chooses start and goal positions,
 * builds a guaranteed safe path between them, then places rocks on the remaining cells so every
 * level is solvable but still challenging.
 * </p>
 */
public class RandomLevelFactory {
    private static final int FALLBACK_ROWS = 4;
    private static final int FALLBACK_COLUMNS = 4;
    private static final int MIN_ROWS = 7;
    private static final int MAX_ROWS = 9;
    private static final int MIN_COLUMNS = 10;
    private static final int MAX_COLUMNS = 12;

    private final Random random = new Random();

    /**
     * Creates a new game setup without using terminal size (e.g. for tests or non-GUI use).
     * Grid dimensions are chosen at random within the factory's min/max bounds.
     *
     * @return a new {@link GameSetup} with grid, robot, and briefing
     */
    public GameSetup createSetup() {
        return createSetup(null);
    }

    /**
     * Builds the complete starting state for one game. The order of steps matters: we choose grid
     * size (possibly smaller if the terminal is small), then pick start and goal, then build a
     * safe path between them, then add rocks everywhere else so the level is always solvable.
     *
     * @param terminalSize if non-null, used to shrink the grid when the terminal is too small
     * @return a new {@link GameSetup} with grid, robot, and briefing
     */
    public GameSetup createSetup(TerminalSize terminalSize) {
        int rows = chooseRows(terminalSize);
        int columns = chooseColumns(terminalSize);

        Position start = randomPosition(rows, columns);
        Position goal = randomGoal(rows, columns, start);
        Direction direction = randomDirection();

        Grid grid = new Grid(rows, columns, goal);
        boolean[][] safePath = buildSafePath(rows, columns, start, goal);
        int rockTarget = Math.max(8, (rows * columns) / 5);
        placeRocks(grid, safePath, start, rockTarget);

        Robot robot = new Robot("Robo", start, direction);
        String briefing = "New map ready. Reach the green goal at " + goal
            + ". Start exploring and watch out for rocks.";

        return new GameSetup(grid, robot, briefing);
    }

    /**
     * Picks a random row count. If terminal size is provided, limits rows so the grid fits on screen.
     */
    private int chooseRows(TerminalSize terminalSize) {
        if (terminalSize == null) {
            return randomBetween(MIN_ROWS, MAX_ROWS);
        }

        int maxRowsThatFit = Math.max(FALLBACK_ROWS, Math.min(MAX_ROWS, terminalSize.getRows() - 12));
        int minRows = Math.min(MIN_ROWS, maxRowsThatFit);
        return randomBetween(minRows, maxRowsThatFit);
    }

    /**
     * Picks a random column count. If terminal size is provided, limits columns so the grid fits.
     */
    private int chooseColumns(TerminalSize terminalSize) {
        if (terminalSize == null) {
            return randomBetween(MIN_COLUMNS, MAX_COLUMNS);
        }

        int maxColumnsThatFit = Math.max(FALLBACK_COLUMNS, Math.min(MAX_COLUMNS, (terminalSize.getColumns() - 8) / 4));
        int minColumns = Math.min(MIN_COLUMNS, maxColumnsThatFit);
        return randomBetween(minColumns, maxColumnsThatFit);
    }

    /**
     * Chooses a goal position that is preferably far from the start (Manhattan distance).
     * We scan all cells: those at or beyond the preferred distance go into preferredGoals;
     * we also keep fallbackGoals as the set of cells that are farthest from start, in case
     * the grid is too small. This way we always pick a goal without infinite loops.
     */
    private Position randomGoal(int rows, int columns, Position start) {
        int preferredDistance = Math.max(5, (rows + columns) / 2);
        int bestDistanceSeen = -1;
        List<Position> preferredGoals = new ArrayList<>();
        List<Position> fallbackGoals = new ArrayList<>();

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                Position candidate = new Position(row, column);

                if (candidate.sameCell(start)) {
                    continue;
                }

                int distance = manhattanDistance(start, candidate);

                if (distance >= preferredDistance) {
                    preferredGoals.add(candidate);
                }

                if (distance > bestDistanceSeen) {
                    bestDistanceSeen = distance;
                    fallbackGoals.clear();
                }

                if (distance == bestDistanceSeen) {
                    fallbackGoals.add(candidate);
                }
            }
        }

        if (!preferredGoals.isEmpty()) {
            return preferredGoals.get(random.nextInt(preferredGoals.size()));
        }

        return fallbackGoals.get(random.nextInt(fallbackGoals.size()));
    }

    /** Picks a random valid cell (row in [0, rows), column in [0, columns)). */
    private Position randomPosition(int rows, int columns) {
        return new Position(random.nextInt(rows), random.nextInt(columns));
    }

    /** Returns a random direction (NORTH, EAST, SOUTH, or WEST). */
    private Direction randomDirection() {
        Direction[] directions = Direction.values();
        return directions[random.nextInt(directions.length)];
    }

    /**
     * Builds a safe path from start to goal so the level is always solvable. The path is
     * "L"-shaped: start → middle → goal. Whether we move in rows first or columns first,
     * and where the middle is, are randomized so maps vary.
     */
    private boolean[][] buildSafePath(int rows, int columns, Position start, Position goal) {
        boolean[][] safePath = new boolean[rows][columns];
        reserveCell(safePath, start);
        reserveCell(safePath, goal);

        Position current = start;
        boolean moveRowsFirst = random.nextBoolean();

        if (moveRowsFirst) {
            int middleRow = randomBetween(Math.min(start.getRow(), goal.getRow()), Math.max(start.getRow(), goal.getRow()));
            current = walkTowardRow(safePath, current, middleRow);
            current = walkTowardColumn(safePath, current, goal.getColumn());
            walkTowardRow(safePath, current, goal.getRow());
        } else {
            int middleColumn = randomBetween(Math.min(start.getColumn(), goal.getColumn()), Math.max(start.getColumn(), goal.getColumn()));
            current = walkTowardColumn(safePath, current, middleColumn);
            current = walkTowardRow(safePath, current, goal.getRow());
            walkTowardColumn(safePath, current, goal.getColumn());
        }

        return safePath;
    }

    /** Walks from current toward targetRow one step at a time, marking each cell as safe. */
    private Position walkTowardRow(boolean[][] safePath, Position current, int targetRow) {
        int step = Integer.compare(targetRow, current.getRow());

        while (current.getRow() != targetRow) {
            current = current.translate(step, 0);
            reserveCell(safePath, current);
        }

        return current;
    }

    /** Walks from current toward targetColumn one step at a time, marking each cell as safe. */
    private Position walkTowardColumn(boolean[][] safePath, Position current, int targetColumn) {
        int step = Integer.compare(targetColumn, current.getColumn());

        while (current.getColumn() != targetColumn) {
            current = current.translate(0, step);
            reserveCell(safePath, current);
        }

        return current;
    }

    /** Marks a cell as part of the safe path (no rock will be placed there). */
    private void reserveCell(boolean[][] safePath, Position position) {
        safePath[position.getRow()][position.getColumn()] = true;
    }

    /**
     * Places rocks randomly up to rockTarget count, never on start, goal, or the safe path.
     * Stops after maxAttempts to avoid infinite loops if the grid is nearly full.
     */
    private void placeRocks(Grid grid, boolean[][] safePath, Position start, int rockTarget) {
        int placed = 0;
        int attempts = 0;
        int maxAttempts = grid.getRows() * grid.getColumns() * 8;

        while (placed < rockTarget && attempts < maxAttempts) {
            Position rock = randomPosition(grid.getRows(), grid.getColumns());
            attempts++;

            if (rock.sameCell(start) || grid.isGoal(rock) || safePath[rock.getRow()][rock.getColumn()] || grid.hasRock(rock)) {
                continue;
            }

            grid.addRock(rock.getRow(), rock.getColumn());
            placed++;
        }
    }

    /** Manhattan distance between two positions (sum of absolute row and column differences). */
    private int manhattanDistance(Position first, Position second) {
        return Math.abs(first.getRow() - second.getRow()) + Math.abs(first.getColumn() - second.getColumn());
    }

    /** Returns a random integer in [minimum, maximum] (inclusive). */
    private int randomBetween(int minimum, int maximum) {
        return minimum + random.nextInt((maximum - minimum) + 1);
    }
}
