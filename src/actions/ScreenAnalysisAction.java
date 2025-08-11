package actions;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import api.TtsApiClient;
import config.ConfigurationManager;
import personality.PersonalityManager;
import core.AppState;

/**
 * Action that captures screenshots and processes them with AI analysis.
 * This combines the screenshot capture and AI processing into a single cohesive action.
 */
public class ScreenAnalysisAction implements Action {

    private final List<BufferedImage> screenshotBuffer = new ArrayList<>();
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    private volatile String currentUnifiedPrompt = null;
    private volatile ActionContext currentGlobalContext = null;

    @Override
    public String getActionId() {
        return "screen_analysis";
    }

    @Override
    public String getDescription() {
        return "Captures screen and analyzes it with AI to generate appropriate responses";
    }

    @Override
    public boolean canExecute(ActionContext context) {
    return AppState.isRunning && !isProcessing.get();
    }

    @Override
    public ActionResult execute(ActionContext context) {
        if (!canExecute(context)) {
            return ActionResult.skipped("Already processing or not running");
        }

        if (!isProcessing.compareAndSet(false, true)) {
            return ActionResult.skipped("Processing already in progress");
        }

        try {
            // Get screenshot from context (provided by ThinkingEngine)
            BufferedImage screenshot = context.get("screenshot", BufferedImage.class);
            if (screenshot == null) {
                return ActionResult.failure("No screenshot provided in context");
            }

            // Store in buffer
            synchronized (screenshotBuffer) {
                screenshotBuffer.add(screenshot);
                if (screenshotBuffer.size() > 4) {
                    screenshotBuffer.remove(0);
                }
            }

            // Process the screenshot
            List<BufferedImage> images;
            synchronized (screenshotBuffer) {
                if (screenshotBuffer.isEmpty()) {
                    return ActionResult.skipped("No screenshots to process");
                }
                images = new ArrayList<>();
                images.add(screenshotBuffer.get(screenshotBuffer.size() - 1));
                screenshotBuffer.clear();
            }

            // Mark global processing state so the thinking loop can pause while we work
            AppState.isActionProcessing = true;

            // Collect any additional task content contributed by other actions for this run
            currentUnifiedPrompt = getOtherTaskContent(context);
            // Capture global context for bracket routing
            currentGlobalContext = context.contains("global_context") ? context.get("global_context", ActionContext.class) : null;

            // Process synchronously since ThinkingEngine already controls the flow
            try {
                processAndRespond(images.get(0));
                return ActionResult.success("Screen analysis completed");
            } catch (Exception e) {
                System.err.println("Error during AI processing: " + e.getMessage());
                e.printStackTrace();
                return ActionResult.failure("Error during screen analysis: " + e.getMessage());
            } finally {
                isProcessing.set(false);
                AppState.isActionProcessing = false;
            }

        } catch (Exception e) {
            isProcessing.set(false);
            return ActionResult.failure("Error during screen analysis: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void processAndRespond(BufferedImage image) throws Exception {
        String selectedTtsVoice = AppState.selectedTtsCharacterVoice;
        String selectedLanguage = AppState.selectedLanguage;
        // Build the unified prompt (memory, levels, personality, etc.)
        String unified = currentUnifiedPrompt != null ? currentUnifiedPrompt : "";
        String personalityPrompt = AppState.useMultimodal()
            ? PersonalityManager.getCurrentMultimodalPrompt()
            : PersonalityManager.getCurrentPersonalityPrompt();
        // Add the essential speak task appends (speak prompts, memory, last 5 comments, etc.)
        String fullPersonalityPrompt = getSpeakTaskAppends(personalityPrompt);
        
        String prompt;
        if (unified.isBlank()) {
            // No tasks, just personality prompt
            prompt = fullPersonalityPrompt;
        } else {
            // Tasks present, need to add tasks instruction at the beginning
            String tasksInstruction = config.ConfigurationManager.getTasksInstruction();
            StringBuilder promptBuilder = new StringBuilder();
            if (tasksInstruction != null && !tasksInstruction.isBlank()) {
                promptBuilder.append(tasksInstruction).append("\n\n");
            }
            promptBuilder.append(unified).append("\n\n").append(fullPersonalityPrompt);
            prompt = promptBuilder.toString();
        }

        // Get expected bracket prefixes from global context if available
        List<String> expectedBracketPrefixes = null;
        ActionContext global = currentGlobalContext;
        if (global != null && global.contains("expected_bracket_prefixes")) {
            expectedBracketPrefixes = (List<String>) global.get("expected_bracket_prefixes", List.class);
        }
        ActionManager am = (global != null && global.contains("action_manager")) ? global.get("action_manager", ActionManager.class) : null;
        java.util.Collection<Action> actions = (am != null) ? am.getRegisteredActions() : java.util.List.of();

        // Use ThinkingEngine helper for model execution and bracket routing
        String rawModelOutput = null;
        try {
            rawModelOutput = ThinkingEngine.runImageAwarePromptFlow(image, prompt, expectedBracketPrefixes, actions, global);
        } catch (Exception e) {
            System.err.println("Error during image-aware prompt flow: " + e.getMessage());
        }

        String finalResponseToSpeak = parseFinalResponse(rawModelOutput);
        if (finalResponseToSpeak != null && !finalResponseToSpeak.isBlank()) {
            System.out.println("Spoken (after stripping brackets): " + finalResponseToSpeak);
            System.out.println("Speaking: " + finalResponseToSpeak);

            if (AppState.useTTS()) {
                // Speak the response - TtsApiClient will handle UI updates automatically
                TtsApiClient.speak(finalResponseToSpeak, selectedTtsVoice, 1.0, selectedLanguage);
            } else {
                // TTS disabled: just show the speech bubble temporarily without audio
                final api.TtsApiClient.UICallback cb = TtsApiClient.getUICallback();
                if (cb != null) {
                    cb.showSpeechBubble(finalResponseToSpeak);
                    cb.showStaticImage();
                    // Hide bubble after a short delay so UI doesn't stick
                    new Thread(() -> {
                        try { Thread.sleep(Math.min(5000, 500 + finalResponseToSpeak.length() * 40)); } catch (InterruptedException ignored) {}
                        try { cb.hideSpeechBubble(); } catch (Throwable ignored) {}
                    }, "bubble-timer").start();
                }
            }

            // Save to memory
            PersonalityManager.saveResponseToMemory(finalResponseToSpeak);
        }
    }
    private String parseFinalResponse(String rawResponse) {
        if (rawResponse == null) {
            return null;
        }
        int thinkTagEnd = rawResponse.lastIndexOf("</think>");
        String afterThink = (thinkTagEnd != -1)
        ? rawResponse.substring(thinkTagEnd + "</think>".length())
        : rawResponse;

    // Only speak from explicit [speak:(...)] sections (case-sensitive)
    String spokenFromBrackets = collectSpeakSections(afterThink);
    return (spokenFromBrackets != null && !spokenFromBrackets.isBlank()) ? spokenFromBrackets.trim() : null;
    }

    private static String getSpeakTaskAppends(String personalityPrompt) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(personalityPrompt);
        
        promptBuilder.append(ConfigurationManager.getSpeakTaskPrompt());

        // Add recent context: last five comments and memories
        java.util.List<String> lastFive = personality.PersonalityManager.getLastResponses();
        if (lastFive != null && !lastFive.isEmpty()) {
            promptBuilder.append("\nYour 5 past comments are:\n");
            for (String r : lastFive) {
                if (r != null && !r.isBlank()) {
                    promptBuilder.append("- ").append(r.replace("\"", "'"))
                                 .append("\n");
                }
            }
        }
        promptBuilder.append("\". Your new comment MUST be different, do not make it repetitive.");

        String stm = config.MemoryStore.getShortTerm();
        String ltm = config.MemoryStore.getLongTerm();
        if (stm != null && !stm.isBlank()) {
            promptBuilder.append("This is your Short term memory to add context: ").append(stm.replace("\"", "'"))
                         .append("\n");
        }
        if (ltm != null && !ltm.isBlank()) {
            promptBuilder.append("This is your Long term memory to add context: ").append(ltm.replace("\"", "'"))
                         .append("\n");
        }

        return promptBuilder.toString();
    }

    // Retrieve additional task content contributed by other actions; SA does not assemble or duplicate prompts
    private String getOtherTaskContent(ActionContext context) {
        if (context == null) return "";
        if (!context.contains("other_task_content")) return "";
        StringBuilder sb = context.get("other_task_content", StringBuilder.class);
        return sb != null ? sb.toString() : "";
    }

    // removed removeBracketSections: speech is driven only by [speak:(...)]

    // Extract ONLY [speak:(...)] sections for TTS
    private String collectSpeakSections(String s) {
        if (s == null || s.isEmpty()) return "";
        StringBuilder preferred = new StringBuilder();
        int idx = 0;
        int preferredCount = 0;
        while ((idx = s.indexOf('[', idx)) != -1) {
            int end = s.indexOf(']', idx + 1);
            if (end == -1) break;
            String inside = s.substring(idx + 1, end).trim();
            if (inside.startsWith("speak:")) {
                // Extract speak payload inside parentheses if present: speak:(content)
                int lp = inside.indexOf('('), rp = inside.lastIndexOf(')');
                String payload = (lp != -1 && rp > lp)
                        ? inside.substring(lp + 1, rp)
                        : inside.substring("speak:".length()).trim();
                if (preferred.length() > 0) preferred.append(' ');
                preferred.append(payload);
                preferredCount++;
            }
            idx = end + 1;
        }
        if (preferredCount > 0) {
            System.out.println("Collected " + preferredCount + " [speak:(...)] section(s).");
            return preferred.toString();
        }
        return "";
    }
}
