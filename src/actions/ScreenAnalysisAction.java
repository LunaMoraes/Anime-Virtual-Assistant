package actions;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import api.ApiClient;
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
    private static final ExecutorService PROCESSOR = Executors.newSingleThreadExecutor(r -> new Thread(r, "screen-analysis-processor"));
    private volatile String currentUnifiedPrompt = null;

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

            // Process in background executor to avoid blocking and prevent thread leaks
            PROCESSOR.submit(() -> {
                try {
                    processAndRespond(images.get(0));
                } catch (Exception e) {
                    System.err.println("Error during AI processing: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    isProcessing.set(false);
            AppState.isActionProcessing = false;
                }
            });

            return ActionResult.success("Screen analysis initiated");

        } catch (Exception e) {
            isProcessing.set(false);
            return ActionResult.failure("Error during screen analysis: " + e.getMessage());
        }
    }

    private void processAndRespond(BufferedImage image) throws Exception {
    String finalResponseToSpeak;

    // Access AppState directly now that it's in a proper package
    boolean useMultimodal = AppState.useMultimodal();
    String selectedTtsVoice = AppState.selectedTtsCharacterVoice;
    String selectedLanguage = AppState.selectedLanguage;

        if (useMultimodal) {
            System.out.println("Using multimodal mode - single request");
            String rawResponse = processMultimodal(image);
            System.out.println("RAW model output: " + rawResponse);
            finalResponseToSpeak = parseFinalResponse(rawResponse);
        } else {
            System.out.println("Using traditional mode - separate vision and analysis requests");
            System.out.println("Analyzing screenshot with vision service...");
            String imageDescription = analyzeImage(image);

            if (imageDescription == null || imageDescription.isBlank()) {
                System.err.println("Vision service did not return a description.");
                return;
            } else {
                System.out.println("Vision service description: " + imageDescription);
                System.out.println("Generating final response with language model...");
                String rawResponse = generateResponse(imageDescription);
                System.out.println("RAW model output: " + rawResponse);
                finalResponseToSpeak = parseFinalResponse(rawResponse);
            }
        }

        // Always attempt to dispatch bracketed task sections, even if there's nothing to speak
        try {
            handleTaskSections(rawForTasksCache);
        } catch (Exception taskEx) {
            System.err.println("Task dispatch error: " + taskEx.getMessage());
        }

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

    private String analyzeImage(BufferedImage image) throws Exception {
        String prompt = ConfigurationManager.getVisionPrompt();
        return ApiClient.analyzeImage(image, prompt);
    }

    private String generateResponse(String context) throws Exception {
        // For traditional path, still include any contributed task content so LLM can parcel outputs.
        String personalityPrompt = PersonalityManager.getCurrentPersonalityPrompt();
        String base;
        if (personalityPrompt == null) {
            String fallbackPrompt = ConfigurationManager.getFallbackPrompt();
            base = String.format(fallbackPrompt, context.replace("\"", "'"));
        } else {
            base = getString(context, personalityPrompt);
        }

        String unified = currentUnifiedPrompt != null ? currentUnifiedPrompt : "";
        String finalPrompt = unified.isBlank() ? base : unified + "\n\n" + base;
        System.out.println("Final prompt sent to LLM: " + finalPrompt);
        return ApiClient.generateResponse(finalPrompt);
    }

    private static String getString(String context, String personalityPrompt) {
        String lastResponse = PersonalityManager.getLastResponse();

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(String.format(personalityPrompt, context.replace("\"", "'")));
        
        // Ensure the model knows how to format the spoken output
        promptBuilder.append(" For this task, output your spoken sentence wrapped exactly as [speak:(content)].");

        if (lastResponse != null && !lastResponse.isEmpty()) {
            promptBuilder.append("Do not use special characters, formatting or emojis in your response.");
            promptBuilder.append(" Your previous comment was: \"");
            promptBuilder.append(lastResponse.replace("\"", "'"));
            promptBuilder.append("\". Your new comment MUST be different, do not make it repetitive.");
        }

        return promptBuilder.toString();
    }

    private String parseFinalResponse(String rawResponse) {
        if (rawResponse == null) {
            return null;
        }
        // cache raw for later task extraction
        this.rawForTasksCache = rawResponse;
        int thinkTagEnd = rawResponse.lastIndexOf("</think>");
        String afterThink = (thinkTagEnd != -1)
        ? rawResponse.substring(thinkTagEnd + "</think>".length())
        : rawResponse;

    // Prefer any non-level bracketed sections as speech blocks
    String spokenFromBrackets = collectSpeakSections(afterThink);
    if (spokenFromBrackets != null && !spokenFromBrackets.isBlank()) {
        System.out.println("Collected spoken text from brackets.");
        return spokenFromBrackets.trim();
    }

    // Fallback: speak outside-of-bracket text
    String fallback = removeBracketSections(afterThink).trim();
    return fallback.isEmpty() ? null : fallback;
    }

    // --- Simple bracketed sections parsing and routing ---
    private String rawForTasksCache;

    private void handleTaskSections(String raw) {
        if (raw == null) return;
        // find all occurrences of [ ... ] and inspect content
        int idx = 0;
        boolean anyFound = false;
        boolean levelsFound = false;
        while ((idx = raw.indexOf('[', idx)) != -1) {
            int end = raw.indexOf(']', idx + 1);
            if (end == -1) break;
            String inside = raw.substring(idx + 1, end).trim();
            System.out.println("Bracketed section found: [" + inside + "]");
            anyFound = true;
            if (inside.toLowerCase().startsWith("levels:")) {
                levelsFound = true;
            }
            dispatchTask(inside);
            idx = end + 1;
        }
        if (!anyFound) {
            System.out.println("No bracketed sections found in model output.");
        } else if (!levelsFound) {
            System.out.println("No [levels:...] command found; no level changes will be applied this cycle.");
        }
    }

    private void dispatchTask(String content) {
        try {
            // very small parser expecting patterns like:
            // levels:add_exp_on_skill(skill_name)
            // levels:add_skill(skill_name, attribute)
            String lower = content.toLowerCase();
            if (!lower.startsWith("levels:")) return;
            String cmd = content.substring("levels:".length()).trim();
            if (cmd.startsWith("add_exp_on_skill")) {
                int lp = cmd.indexOf('('), rp = cmd.lastIndexOf(')');
                if (lp != -1 && rp > lp) {
                    String arg = cmd.substring(lp + 1, rp).trim();
                    String skill = stripQuotes(arg);
                    System.out.println("Dispatch: levels.addExpOnSkill(" + skill + ")");
                    levels.LevelManager.addExpOnSkill(skill, 1);
                }
            } else if (cmd.startsWith("add_skill")) {
                int lp = cmd.indexOf('('), rp = cmd.lastIndexOf(')');
                if (lp != -1 && rp > lp) {
                    String args = cmd.substring(lp + 1, rp);
                    String[] parts = args.split(",");
                    String skill = parts.length > 0 ? stripQuotes(parts[0].trim()) : null;
                    String attr = parts.length > 1 ? stripQuotes(parts[1].trim()) : null;
                    System.out.println("Dispatch: levels.addSkill(" + skill + ", " + attr + ")");
                    levels.LevelManager.addSkill(skill, attr);
                }
            }
        } catch (Exception ignored) {}
    }

    private String stripQuotes(String s) {
        if (s == null) return null;
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private String processMultimodal(BufferedImage image) throws Exception {
        String personalityPrompt = PersonalityManager.getCurrentMultimodalPrompt();

        System.out.println("DEBUG: Multimodal prompt from personality: " + personalityPrompt);

        if (personalityPrompt == null || personalityPrompt.trim().isEmpty()) {
            System.err.println("No multimodal personality prompt found, falling back to traditional mode.");
            String imageDescription = analyzeImage(image);
            return generateResponse(imageDescription);
        }

        String unified = currentUnifiedPrompt != null ? currentUnifiedPrompt : "";
        String finalPrompt = unified.isBlank() ? getFinalPrompt(personalityPrompt) : unified + "\n\n" + getFinalPrompt(personalityPrompt);
        System.out.println("Final multimodal prompt sent: " + finalPrompt);

        return ApiClient.analyzeImageMultimodal(image, finalPrompt);
    }

    private static String getFinalPrompt(String personalityPrompt) {
        String lastResponse = PersonalityManager.getLastResponse();

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(personalityPrompt);
        
        // Ensure the model knows how to format the spoken output
        promptBuilder.append(" For this task, output your spoken sentence wrapped exactly as [speak:(content)].");

        if (lastResponse != null && !lastResponse.isEmpty()) {
            promptBuilder.append(" Do not use special characters, formatting or emojis in your response.");
            promptBuilder.append(" Your previous comment was: \"");
            promptBuilder.append(lastResponse.replace("\"", "'"));
            promptBuilder.append("\". Your new comment MUST be different.");
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

    // Remove any bracketed task sections like [levels:add_skill(...)] from the text to be spoken
    private String removeBracketSections(String s) {
        if (s == null || s.isEmpty()) return s;
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            int open = s.indexOf('[', i);
            if (open == -1) {
                out.append(s, i, s.length());
                break;
            }
            int close = s.indexOf(']', open + 1);
            if (close == -1) {
                out.append(s.substring(i));
                break;
            }
            // append text before bracket, skip bracket content
            out.append(s, i, open);
            i = close + 1;
        }
        return out.toString();
    }

    // Prefer [speak:(...)] sections for TTS; if none, fall back to any non-level brackets
    private String collectSpeakSections(String s) {
        if (s == null || s.isEmpty()) return "";
        StringBuilder preferred = new StringBuilder();
        StringBuilder fallback = new StringBuilder();
        int idx = 0;
        int preferredCount = 0;
        int fallbackCount = 0;
        while ((idx = s.indexOf('[', idx)) != -1) {
            int end = s.indexOf(']', idx + 1);
            if (end == -1) break;
            String inside = s.substring(idx + 1, end).trim();
            String lower = inside.toLowerCase();
            if (lower.startsWith("levels:")) {
                // Not speech
            } else if (lower.startsWith("speak:")) {
                // Extract speak payload inside parentheses if present: speak:(content)
                int lp = inside.indexOf('('), rp = inside.lastIndexOf(')');
                String payload = (lp != -1 && rp > lp)
                        ? inside.substring(lp + 1, rp)
                        : inside.substring("speak:".length()).trim();
                if (preferred.length() > 0) preferred.append(' ');
                preferred.append(payload);
                preferredCount++;
            } else {
                if (fallback.length() > 0) fallback.append(' ');
                fallback.append(inside);
                fallbackCount++;
            }
            idx = end + 1;
        }
        if (preferredCount > 0) {
            System.out.println("Collected " + preferredCount + " [speak:(...)] section(s).");
            return preferred.toString();
        }
        if (fallbackCount > 0) {
            System.out.println("Collected " + fallbackCount + " speak bracket section(s) (fallback).");
        }
        return fallback.toString();
    }
}
