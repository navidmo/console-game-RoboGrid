// Lanterna types: we use a virtual terminal (fake keyboard/screen) so tests don't need a real console.
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
// AtomicReference lets the game thread report an exception back to the test thread safely.
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * End-to-end tests for the Lanterna version of the game.
 * <p>
 * These tests do not call controller methods directly. Instead, they create a virtual terminal,
 * start the real Lanterna screen, feed in fake key presses, and let {@link RobotGame} run its
 * normal game loop. This gives students a strong example of testing the full flow from input
 * to rendering to state changes.
 * </p>
 */
public class RobotGameE2ETest {

    @Test
    void movementKeysControlTheRobotThroughTheRealGameLoop() throws Exception {
        // --- Arrange: create a known map so we can predict the robot's final state ---
        // 5x5 grid, no rocks. Robot starts at (1,1) facing East; goal at (4,4).
        // We use the same GameSetup object the game will use, so we can assert on it afterward.
        GameSetup setup = setupWithoutRocks(
            new Position(1, 1),
            Direction.EAST,
            new Position(4, 4),
            "Teaching test map"
        );

        // --- Act: run the real game loop with scripted keys ---
        // Keys: W (move), D (turn right), W (move again), Q (quit).
        // ScriptedLevelFactory makes the game use our "setup" instead of a random one.
        String terminalText = runGameWithInputs(
            new ScriptedLevelFactory(setup),
            key('w'),
            key('d'),
            key('w'),
            key('q')
        );

        // --- Assert: the robot moved and turned as if a real player had pressed those keys ---
        // Started (1,1) East; W -> (1,2); D -> now South; W -> (2,2). So final position (2,2), facing South.
        assertTrue(setup.getRobot().getPosition().sameCell(new Position(2, 2)));
        assertEquals(Direction.SOUTH, setup.getRobot().getDirection());
        assertEquals(2, setup.getRobot().getMovesTaken());
        // Sanity check: the UI actually drew the title (proves the draw path ran).
        assertTrue(terminalText.contains("Robot Rescue Mission"));
    }

    @Test
    void restartLoadsANewSetupAndContinuesPlaying() throws Exception {
        // --- Arrange: two different setups. First is used at game start; second after R. ---
        // First map: robot at (0,0) facing East. We will NOT send any move keys before R.
        GameSetup firstSetup = setupWithoutRocks(
            new Position(0, 0),
            Direction.EAST,
            new Position(4, 4),
            "First map"
        );

        // Second map: robot at (2,1) facing East. After R, the game uses this; one W should reach (2,2).
        GameSetup secondSetup = setupWithoutRocks(
            new Position(2, 1),
            Direction.EAST,
            new Position(4, 4),
            "Second map"
        );

        // Factory returns firstSetup on first createSetup(), then secondSetup on second (and every later) call.
        ScriptedLevelFactory factory = new ScriptedLevelFactory(firstSetup, secondSetup);

        // --- Act: R (restart), then W (move on new map), then Q (quit) ---
        runGameWithInputs(
            factory,
            key('r'),
            key('w'),
            key('q')
        );

        // --- Assert: createSetup was called twice (initial + one restart); first robot never moved; second moved once ---
        assertEquals(2, factory.getCreateCount());
        assertTrue(firstSetup.getRobot().getPosition().sameCell(new Position(0, 0)));
        assertTrue(secondSetup.getRobot().getPosition().sameCell(new Position(2, 2)));
        assertEquals(1, secondSetup.getRobot().getMovesTaken());
    }

    @Test
    void manyRapidRestartKeysDoNotCrashTheGameLoop() throws Exception {
        // --- Arrange: two setups; we will hammer R many times then Q ---
        // Goal: prove the game loop doesn't crash or hang when restarts happen in quick succession.
        GameSetup firstSetup = setupWithoutRocks(
            new Position(1, 1),
            Direction.NORTH,
            new Position(4, 4),
            "Restart map 1"
        );
        GameSetup secondSetup = setupWithoutRocks(
            new Position(2, 2),
            Direction.SOUTH,
            new Position(4, 4),
            "Restart map 2"
        );

        ScriptedLevelFactory factory = new ScriptedLevelFactory(firstSetup, secondSetup);
        KeyStroke[] inputs = new KeyStroke[41];
        // 40× R (each triggers createSetup; after the second call we keep returning secondSetup).
        for (int index = 0; index < 40; index++) {
            inputs[index] = key('r');
        }
        inputs[40] = key('q');

        runGameWithInputs(factory, inputs);

        // createSetup was called 41 times: once at start + 40 restarts. Final state is second setup, unmoved.
        assertEquals(41, factory.getCreateCount());
        assertTrue(secondSetup.getRobot().getPosition().sameCell(new Position(2, 2)));
    }

    /**
     * Runs a full Lanterna session on a virtual terminal and returns the final terminal text.
     * The helper also captures background-thread failures so the tests can report them clearly.
     */
    private static String runGameWithInputs(RandomLevelFactory levelFactory, KeyStroke... inputs) throws Exception {
        // Virtual terminal: no real console. We can push KeyStrokes into it and read back what was "drawn."
        DefaultVirtualTerminal terminal = new DefaultVirtualTerminal(new TerminalSize(120, 35));
        // If the game loop throws, we store it here so the test thread can fail with that exception.
        AtomicReference<Throwable> failure = new AtomicReference<>();

        try (Screen screen = new TerminalScreen(terminal)) {
            screen.startScreen();
            screen.setCursorPosition(null);

            // The real game loop calls screen.readInput(), which blocks until a key is available.
            // So we run the loop in a background thread; the test thread feeds keys then waits.
            Thread gameThread = new Thread(() -> {
                try {
                    RobotGame.runGameLoop(screen, levelFactory);
                } catch (Throwable throwable) {
                    failure.set(throwable);
                }
            }, "robot-game-e2e");

            gameThread.start();

            // Feed all scripted keys into the virtual terminal. The game loop will read them one by one.
            for (KeyStroke input : inputs) {
                terminal.addInput(input);
            }

            // Wait up to 2 seconds for the game loop to process the keys and exit (e.g. on Q).
            gameThread.join(2_000);

            if (gameThread.isAlive()) {
                // Timeout: send EOF so the loop can exit; avoid leaving a stuck thread.
                terminal.addInput(new KeyStroke(com.googlecode.lanterna.input.KeyType.EOF));
                gameThread.join(1_000);
                fail("The game loop did not finish after the scripted inputs.");
            }

            if (failure.get() != null) {
                fail("The game loop threw an exception: " + failure.get().getMessage(), failure.get());
            }

            assertFalse(gameThread.isAlive());
            return terminal.toString();
        }
    }

    /**
     * Builds a simple open map that is easy to reason about in tests.
     * The goal is present, but there are no rocks to interfere with scripted movement.
     */
    private static GameSetup setupWithoutRocks(Position start, Direction direction, Position goal, String briefing) {
        Grid grid = new Grid(5, 5, goal);
        Robot robot = new Robot("TestRobo", start, direction);
        return new GameSetup(grid, robot, briefing);
    }

    /** Builds a KeyStroke for a normal character key (e.g. 'w', 'r', 'q') so test code reads like the key script. */
    private static KeyStroke key(char character) {
        return new KeyStroke(character, false, false);
    }

    /**
     * Test double for {@link RandomLevelFactory}.
     * <p>
     * Instead of generating random boards, it returns the setups we prepared in the test. After
     * it reaches the last setup, it keeps returning that one so repeated restarts still work.
     * </p>
     */
    private static final class ScriptedLevelFactory extends RandomLevelFactory {
        private final GameSetup[] setups;
        private int createCount;  // How many times createSetup has been called (for assertions).
        private int nextIndex;   // Which setup to return next (0, then 1, then stick at last).

        private ScriptedLevelFactory(GameSetup... setups) {
            this.setups = setups;
        }

        @Override
        public GameSetup createSetup(TerminalSize terminalSize) {
            createCount++;

            // Return setups in order until we've used all but the last; then always return the last one.
            // That way "press R 40 times" still works: the game keeps getting the same final setup.
            if (nextIndex < setups.length - 1) {
                return setups[nextIndex++];
            }

            nextIndex++;
            return setups[setups.length - 1];
        }

        private int getCreateCount() {
            return createCount;
        }
    }
}
