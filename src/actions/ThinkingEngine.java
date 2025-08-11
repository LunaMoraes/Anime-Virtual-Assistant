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
        // Make global context available to actions for cross-tick data sharing
        ActionContext global = actionManager.getGlobalContext();
        context.put("global_context", global);
        context.put("action_manager", actionManager);
        // Also expose in the global context so actions that only retain the global reference can resolve it
        try { global.put("action_manager", actionManager); } catch (Throwable ignored) {}

        // 1) Flush any queued raw model outputs from previous cycles and route bracket commands
        try {
            @SuppressWarnings("unchecked")
            java.util.List<String> queued = global.get("raw_model_output_queue", java.util.List.class);
            if (queued != null && !queued.isEmpty()) {
                // Copy then clear
                java.util.List<String> copy = new java.util.ArrayList<>(queued);
                queued.clear();
                // Collect expected bracket prefixes for this tick (if any)
                @SuppressWarnings("unchecked")
                java.util.List<String> expectedPrefixes = (java.util.List<String>) global.get("expected_bracket_prefixes", java.util.List.class);
                for (String raw : copy) {
                    routeBracketSections(raw, actionManager.getRegisteredActions(), global, expectedPrefixes);
                }
            }
        } catch (Exception e) {
            System.err.println("Error routing queued bracket sections: " + e.getMessage());
        }
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

        // Prepare expected bracket prefixes for this tick
        java.util.List<String> expectedBracketPrefixes = new java.util.ArrayList<>();
        // levels_task contributes level system task.
        if (actionManager.hasAction("levels_task")) {
            ActionResult r = actionManager.executeAction("levels_task", context);
            if (r.isFailure()) {
                System.err.println("levels_task failed: " + r.getMessage());
            } else {
                // Add levels: prefix for this tick
                expectedBracketPrefixes.addAll(((BracketAwareAction)actionManager.getRegisteredActions().stream().filter(a -> a.getActionId().equals("levels_task")).findFirst().orElse(null)).getBracketPrefixes());
            }
        }

        // memory_task contributes memory system task and may run standalone every 5 ticks
        if (actionManager.hasAction("memory_task") && (AppState.tickCounter % 5 == 0)) {
            ActionResult r = actionManager.executeAction("memory_task", context);
            if (r.isFailure()) {
                System.err.println("memory_task failed: " + r.getMessage());
            } else {
                expectedBracketPrefixes.addAll(((BracketAwareAction)actionManager.getRegisteredActions().stream().filter(a -> a.getActionId().equals("memory_task")).findFirst().orElse(null)).getBracketPrefixes());
            }
        }
        // Store expectedBracketPrefixes in global for this tick (for queued routing)
        global.put("expected_bracket_prefixes", expectedBracketPrefixes);

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
            // Build tasks-only content if any tasks contributed this tick
            boolean hasTaskContent = context.contains("other_task_content")
                    && context.get("other_task_content", StringBuilder.class) != null
                    && context.get("other_task_content", StringBuilder.class).length() > 0;

            // Log only when blocked by reasons
            if (shouldChatThisTick && !defaultChatBlockedBy.isEmpty()) {
                System.out.println("Default chat not running, blocked by: " + String.join(", ", defaultChatBlockedBy));
            }

            // If speak is throttled OR blocked, but we have task content, run a tasks-only request now
            if ((!shouldChatThisTick || !defaultChatBlockedBy.isEmpty()) && hasTaskContent) {
                try {
                    String tasksOnlyPrompt = buildTasksOnlyPrompt(context);
                    if (tasksOnlyPrompt != null && !tasksOnlyPrompt.isBlank()) {
                        System.out.println("Running tasks-only request...");
                        BufferedImage shot = context.contains("screenshot") ? context.get("screenshot", BufferedImage.class) : null;
                        String rawTasksResponse = null;
                        try {
                            rawTasksResponse = runImageAwarePromptFlow(shot, tasksOnlyPrompt, expectedBracketPrefixes, actionManager.getRegisteredActions(), global);
                        } catch (Exception ex) {
                            System.err.println("Error during tasks-only processing: " + ex.getMessage());
                        }

                        if (rawTasksResponse != null && !rawTasksResponse.isBlank()) {
                            System.out.println("Tasks-only RAW model output: " + rawTasksResponse);
                        } else {
                            System.out.println("Tasks-only request returned no content.");
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error in tasks-only request: " + e.getMessage());
                }
            }
        }

    }

    private String buildTasksOnlyPrompt(ActionContext context) {
        // Build a prompt from system tasks + contributed task content only (no personality/speak/vision/multimodal)
        if (!context.contains("other_task_content")) return null;
        StringBuilder other = context.get("other_task_content", StringBuilder.class);
        if (other == null || other.length() == 0) return null;

        String tasksInstruction = config.ConfigurationManager.getTasksInstruction();
        StringBuilder finalPrompt = new StringBuilder();
        if (tasksInstruction != null && !tasksInstruction.isBlank()) {
            finalPrompt.append(tasksInstruction).append("\n\n");
        }
        finalPrompt.append(other);
        return finalPrompt.toString();
    }

    /**
     * Shared helper to run an image-aware prompt flow (multimodal, vision, or text-only),
     * then route bracket sections using expected prefixes. Returns the raw model output.
     * @param shot Screenshot (nullable)
     * @param prompt The prompt to send
     * @param expectedBracketPrefixes List of bracket prefixes to check for this tick (nullable)
     * @param actions Registered actions for bracket routing
     * @param context The global context for bracket routing
     */
    public static String runImageAwarePromptFlow(BufferedImage shot, String prompt, List<String> expectedBracketPrefixes, java.util.Collection<Action> actions, ActionContext context) throws Exception {
        if (prompt == null || prompt.isBlank()) return null;
        String rawModelOutput;
        if (shot != null) {
            if (core.AppState.useMultimodal()) {
                System.out.println("Image-aware (multimodal) with screenshot");
                System.out.println("Prompt (multimodal):\n" + prompt);
                rawModelOutput = api.ApiClient.analyzeImageMultimodal(shot, prompt);
            } else {
                System.out.println("Image-aware (traditional) vision -> analysis");
                String vPrompt = config.ConfigurationManager.getVisionPrompt();
                String desc = api.ApiClient.analyzeImage(shot, vPrompt);
                if (desc != null && !desc.isBlank()) {
                    String finalPrompt = prompt + "\n\nBased on this activity: " + desc;
                    System.out.println("Final prompt (traditional):\n" + finalPrompt);
                    rawModelOutput = api.ApiClient.generateResponse(finalPrompt);
                } else {
                    System.err.println("Image-aware: vision returned no description; falling back to text-only prompt.");
                    System.out.println("Prompt (text-only fallback):\n" + prompt);
                    rawModelOutput = api.ApiClient.generateResponse(prompt);
                }
            }
        } else {
            // No screenshot available; fallback to text-only prompt
            System.out.println("Image-aware (no screenshot) using text-only prompt");
            System.out.println("Prompt (text-only):\n" + prompt);
            rawModelOutput = api.ApiClient.generateResponse(prompt);
        }

        System.out.println("Raw model output (after routing):\n" + rawModelOutput);
        // Route bracket sections using expected prefixes
        routeBracketSections(rawModelOutput, actions, context, expectedBracketPrefixes);
        return rawModelOutput;
    }

    /**
     * Routes bracketed sections in the model output to BracketAwareActions, and checks for expected prefixes.
     * @param raw The raw model output
     * @param actions Registered actions
     * @param context Global context
     * @param expectedPrefixes List of bracket prefixes to check for this tick (nullable)
     */
    public static void routeBracketSections(String raw, java.util.Collection<Action> actions, ActionContext context, List<String> expectedPrefixes) {
        if (raw == null || raw.isBlank() || actions == null || actions.isEmpty()) return;
        int idx = 0;
        boolean anyFound = false;
        java.util.Set<String> foundPrefixes = new java.util.HashSet<>();
        while ((idx = raw.indexOf('[', idx)) != -1) {
            int end = raw.indexOf(']', idx + 1);
            if (end == -1) break;
            String inside = raw.substring(idx + 1, end).trim();
            anyFound = true;
            if (!inside.startsWith("speak:")) {
                System.out.println("Bracketed section found: [" + inside + "]");
            }
            // Track found prefixes
            if (expectedPrefixes != null) {
                for (String prefix : expectedPrefixes) {
                    if (inside.startsWith(prefix)) {
                        foundPrefixes.add(prefix);
                    }
                }
            }
            for (Action a : actions) {
                if (a instanceof BracketAwareAction baa) {
                    for (String p : baa.getBracketPrefixes()) {
                        if (inside.startsWith(p)) {
                            try { baa.handleBracket(inside, context); } catch (Throwable ignored) {}
                            break;
                        }
                    }
                }
            }
            idx = end + 1;
        }
        if (!anyFound) {
            System.out.println("No bracketed sections found in model output.");
        } else if (expectedPrefixes != null && !expectedPrefixes.isEmpty()) {
            for (String prefix : expectedPrefixes) {
                if (!foundPrefixes.contains(prefix)) {
                    System.out.println("No [" + prefix + "...] command found; no effect for this prefix this cycle.");
                }
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
