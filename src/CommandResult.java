/**
 * Holds the outcome of executing one game command (move or turn).
 * <p>
 * Instead of returning only a boolean, the controller returns a {@code CommandResult} so the UI
 * can show a clear message to the player and know whether the robot has reached the goal. This
 * keeps the UI and the game logic in sync: the UI displays {@link #getMessage()}, and can show
 * "Mission complete" when {@link #hasReachedGoal()} is true.
 * </p>
 */
public class CommandResult {
    private final boolean successful;
    private final boolean reachedGoal;
    private final String message;

    /**
     * Creates a result for one command execution.
     *
     * @param successful   true if the command was applied (e.g. move or turn happened)
     * @param reachedGoal  true if the robot is now on the goal cell
     * @param message      text to show the player (e.g. "Robo moved forward to (2, 3).")
     */
    public CommandResult(boolean successful, boolean reachedGoal, String message) {
        this.successful = successful;
        this.reachedGoal = reachedGoal;
        this.message = message;
    }

    /**
     * Returns whether the command was applied successfully (move or turn was performed).
     *
     * @return true if the command succeeded
     */
    public boolean isSuccessful() {
        return successful;
    }

    /**
     * Returns whether the robot has reached the charging station (goal) after this command.
     *
     * @return true if the robot is on the goal cell
     */
    public boolean hasReachedGoal() {
        return reachedGoal;
    }

    /**
     * Returns the message to display to the player describing what happened.
     *
     * @return a short, human-readable message
     */
    public String getMessage() {
        return message;
    }
}
