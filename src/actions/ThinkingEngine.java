package actions;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The thinking engine that analyzes the current situation and decides which actions to execute.
 * This is the core decision-making component of the virtual assistant.
 */
public class ThinkingEngine {

    private final ActionManager actionManager;
    private final AtomicBoolean isThinking = new AtomicBoolean(false);

    public ThinkingEngine(ActionManager actionManager) {
        this.actionManager = actionManager;
    }

    /**
     * Main thinking method that analyzes the situation and executes appropriate actions.
     * This is called periodically by the scheduler.
     */
    public void think() {
        if (!isThinking.compareAndSet(false, true)) {
            return; // Already thinking, skip this cycle
        }

        try {
            analyzeSituationAndAct();
        } catch (Exception e) {
            System.err.println("Error during thinking process: " + e.getMessage());
            e.printStackTrace();
        } finally {
            isThinking.set(false);
        }
    }

    /**
     * Analyzes the current situation and decides which actions to execute.
     * For now, this simply executes the screen analysis action, but in the future
     * this could become much more sophisticated with different decision trees.
     */
    private void analyzeSituationAndAct() {
        ActionContext context = new ActionContext();

        // Capture screenshot first - this will be used by multiple actions
        BufferedImage screenshot = captureScreenshot();
        if (screenshot != null) {
            context.put("screenshot", screenshot);
        }

        // For now, we just execute the screen analysis action
        // In the future, this could analyze various conditions:
        // - Time of day
        // - User activity patterns
        // - System state
        // - External triggers
        // - User preferences

        List<String> availableActions = actionManager.getAvailableActions(context);

        if (availableActions.contains("screen_analysis")) {
            ActionResult result = actionManager.executeAction("screen_analysis", context);

            if (result.isFailure()) {
                System.err.println("Screen analysis failed: " + result.getMessage());
            } else if (result.isSkipped()) {
                // This is normal and expected when the system is busy
                // System.out.println("Screen analysis skipped: " + result.getMessage());
            } else {
                System.out.println("Screen analysis executed successfully");
            }
        } else {
            System.out.println("No suitable actions available at this time");
        }

        // Future expansion could include:
        // - executeNoteTakingActions(context);
        // - executeSchedulingActions(context);
        // - executeMaintenanceActions(context);
        // - executeUserInteractionActions(context);
    }

    /**
     * Captures a screenshot of the entire screen.
     * This method is centralized here so all actions can use the same screenshot capture logic.
     */
    private BufferedImage captureScreenshot() {
        try {
            Robot robot = new Robot();
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            return robot.createScreenCapture(screenRect);
        } catch (AWTException e) {
            System.err.println("Failed to capture screenshot: " + e.getMessage());
            return null;
        }
    }

    /**
     * Checks if the thinking engine is currently processing.
     */
    public boolean isThinking() {
        return isThinking.get();
    }

    // Future methods for different types of decision-making:

    /**
     * Future method: Analyzes if note-taking actions should be executed.
     */
    private void executeNoteTakingActions(ActionContext context) {
        // TODO: Implement when note-taking functionality is added
        // Could analyze if user is working on something that should be documented
        // Could trigger based on certain screen patterns or time intervals
    }

    /**
     * Future method: Analyzes if scheduling-related actions should be executed.
     */
    private void executeSchedulingActions(ActionContext context) {
        // TODO: Implement when scheduling functionality is added
        // Could check calendar, reminders, time-based triggers
    }

    /**
     * Future method: Executes system maintenance and optimization actions.
     */
    private void executeMaintenanceActions(ActionContext context) {
        // TODO: Implement system maintenance actions
        // Could clean up old data, optimize performance, check system health
    }
}
