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
            // Capture global context for bracket routing enqueue
            currentGlobalContext = context.contains("global_context") ? context.get("global_context", ActionContext.class) : null;

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

        String rawForImmediateRouting = null;
        if (useMultimodal) {
            System.out.println("Using multimodal mode - single request");
            String rawResponse = processMultimodal(image);
            System.out.println("RAW model output: " + rawResponse);
            rawForImmediateRouting = rawResponse;
            // Parse speak after we've captured RAW for routing
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
                rawForImmediateRouting = rawResponse;
                // Parse speak after we've captured RAW for routing
                finalResponseToSpeak = parseFinalResponse(rawResponse);
            }
        }

    // Route brackets immediately this tick, before starting TTS
        try {
            ActionContext global = currentGlobalContext;
            if (global != null) {
                ActionManager am = global.contains("action_manager") ? global.get("action_manager", ActionManager.class) : null;
                if (am != null && rawForImmediateRouting != null && !rawForImmediateRouting.isBlank()) {
                    System.out.println("Dispatching bracket sections immediately (same tick) before TTS...");
                    ThinkingEngine.routeBracketSections(rawForImmediateRouting, am.getRegisteredActions(), global);
                }
            }
        } catch (Exception ignored) {}

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
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(String.format(personalityPrompt, context.replace("\"", "'")));
        
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
            promptBuilder.append("Short term memory to add context: ").append(stm.replace("\"", "'"))
                         .append("\n");
        }
        if (ltm != null && !ltm.isBlank()) {
            promptBuilder.append("Long term memory to add context: ").append(ltm.replace("\"", "'"))
                         .append("\n");
        }

        return promptBuilder.toString();
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

    // Removed cache: routing is done immediately via local raw content

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
