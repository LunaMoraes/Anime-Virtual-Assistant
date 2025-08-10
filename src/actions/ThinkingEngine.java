package actions;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import core.AppState;

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
        // If an action is still processing or TTS is speaking, skip this cycle entirely
        if (AppState.isActionProcessing || AppState.isSpeaking) {
            return;
        }
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
        int divisor = AppState.getChatFrequencyDivisor();

        // Determine if this tick should trigger a chat based on frequency
        boolean shouldChatThisTick = divisor <= 1 || (AppState.tickCounter % divisor == 0);

        // Initialize a shared list to track reasons that block the default chat this tick
        // Other actions may append reasons like "levels", "cooldown", etc.
        @SuppressWarnings("unchecked")
        List<String> defaultChatBlockedBy = (List<String>) context.get("DefaultChatBlockedBy", List.class);
        if (defaultChatBlockedBy == null) {
            defaultChatBlockedBy = new ArrayList<>();
            context.put("DefaultChatBlockedBy", defaultChatBlockedBy);
        }

        // Inform memory_task whether screen_analysis will run this tick, so it can decide to unify or run standalone
        boolean willRunScreenAnalysis = defaultChatBlockedBy.isEmpty() && shouldChatThisTick;
        context.put("will_run_screen_analysis", willRunScreenAnalysis);
        
        // Increment global tick counter at each analysis cycle
        AppState.tickCounter++;
        System.err.println("Current Tick: " + AppState.tickCounter);
        // Capture screenshot first - this will be used by multiple actions
        BufferedImage screenshot = captureScreenshot();
        if (screenshot != null) {
            context.put("screenshot", screenshot);
        }

        // Brain actions, maintenance actions should come first.

        // levels_task contributes level system task.
        if (actionManager.hasAction("levels_task")) {
            ActionResult r = actionManager.executeAction("levels_task", context);
            if (r.isFailure()) {
                System.err.println("levels_task failed: " + r.getMessage());
            }
        }

        // memory_task contributes memory system task and may run standalone every 5 ticks
        if (actionManager.hasAction("memory_task") && (AppState.tickCounter % 5 == 0)) {
            ActionResult r = actionManager.executeAction("memory_task", context);
            if (r.isFailure()) {
                System.err.println("memory_task failed: " + r.getMessage());
            }
        }


        // Now execute the screen analysis action which will assemble the full LLM prompt
        if (defaultChatBlockedBy.isEmpty() && shouldChatThisTick) {
            // Keep a safety check to avoid calling a missing action
            if (actionManager.hasAction("screen_analysis")) {
                ActionResult result = actionManager.executeAction("screen_analysis", context);
                if (result.isFailure()) {
                    System.err.println("Screen analysis failed: " + result.getMessage());
                } else if (result.isSkipped()) {
                    // This is normal and expected when the system is busy
                } else {
                    System.out.println("Screen analysis executed successfully");
                }
            } else {
                System.out.println("No suitable actions available at this time");
            }
        } else {
            // Only log when blocked by reasons (not when suppressed by frequency gate)
            if (shouldChatThisTick && !defaultChatBlockedBy.isEmpty()) {
                System.out.println("Default chat not running, blocked by: " + String.join(", ", defaultChatBlockedBy));
            }
        }

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
}
