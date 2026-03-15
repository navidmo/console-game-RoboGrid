/**
 * The three actions the player can ask the robot to perform.
 * <p>
 * The UI (keyboard input) is converted into one of these commands. The {@link RobotController}
 * then executes the command and applies game rules (e.g. cannot move onto a rock or off the grid).
 * Using an enum keeps all valid commands in one place and avoids magic strings.
 * </p>
 */
public enum Command {
    MOVE_FORWARD("Move forward"),
    TURN_LEFT("Turn left"),
    TURN_RIGHT("Turn right");

    private final String label;

    Command(String label) {
        this.label = label;
    }

    /**
     * Returns a short, user-friendly description of this command (e.g. for console or help text).
     *
     * @return the label for this command
     */
    public String getLabel() {
        return label;
    }
}
