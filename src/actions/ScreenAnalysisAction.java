package actions;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import api.ApiClient;
import api.TtsApiClient;
import config.ConfigurationManager;
import personality.PersonalityManager;

/**
 * Action that captures screenshots and processes them with AI analysis.
 * This combines the screenshot capture and AI processing into a single cohesive action.
 */
public class ScreenAnalysisAction implements Action {

    private final List<BufferedImage> screenshotBuffer = new ArrayList<>();
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

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
        // Access AppState through reflection to avoid import issues
        try {
            Class<?> appStateClass = Class.forName("AppState");
            java.lang.reflect.Field isRunningField = appStateClass.getField("isRunning");
            boolean isRunning = (Boolean) isRunningField.get(null);
            return isRunning && !isProcessing.get();
        } catch (Exception e) {
            System.err.println("Could not access AppState.isRunning: " + e.getMessage());
            return false;
        }
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

            // Process in background thread to avoid blocking
            new Thread(() -> {
                try {
                    processAndRespond(images.get(0));
                } catch (Exception e) {
                    System.err.println("Error during AI processing: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    isProcessing.set(false);
                }
            }).start();

            return ActionResult.success("Screen analysis initiated");

        } catch (Exception e) {
            isProcessing.set(false);
            return ActionResult.failure("Error during screen analysis: " + e.getMessage());
        }
    }

    private void processAndRespond(BufferedImage image) throws Exception {
        String finalResponseToSpeak;

        // Check if we're using multimodal mode using reflection
        boolean useMultimodal = false;
        String selectedTtsVoice = null;
        String selectedLanguage = "English";

        try {
            Class<?> appStateClass = Class.forName("AppState");
            java.lang.reflect.Method useMultimodalMethod = appStateClass.getMethod("useMultimodal");
            useMultimodal = (Boolean) useMultimodalMethod.invoke(null);

            java.lang.reflect.Field voiceField = appStateClass.getField("selectedTtsCharacterVoice");
            selectedTtsVoice = (String) voiceField.get(null);

            java.lang.reflect.Field languageField = appStateClass.getField("selectedLanguage");
            selectedLanguage = (String) languageField.get(null);
        } catch (Exception e) {
            System.err.println("Could not access AppState fields: " + e.getMessage());
        }

        if (useMultimodal) {
            System.out.println("Using multimodal mode - single request");
            String rawResponse = processMultimodal(image);
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
                finalResponseToSpeak = parseFinalResponse(rawResponse);
            }
        }

        if (finalResponseToSpeak != null && !finalResponseToSpeak.isBlank()) {
            System.out.println("Final response: " + finalResponseToSpeak);
            System.out.println("Speaking: " + finalResponseToSpeak);

            // Speak the response - TtsApiClient will handle UI updates automatically
            TtsApiClient.speak(finalResponseToSpeak, selectedTtsVoice, 1.0, selectedLanguage);

            // Save to memory
            PersonalityManager.saveResponseToMemory(finalResponseToSpeak);
        }
    }

    private String analyzeImage(BufferedImage image) throws Exception {
        String prompt = ConfigurationManager.getVisionPrompt();
        return ApiClient.analyzeImage(image, prompt);
    }

    private String generateResponse(String context) throws Exception {
        String personalityPrompt = PersonalityManager.getCurrentPersonalityPrompt();

        if (personalityPrompt == null) {
            System.err.println("No personality selected, using fallback prompt.");
            String fallbackPrompt = ConfigurationManager.getFallbackPrompt();
            return ApiClient.generateResponse(String.format(fallbackPrompt, context.replace("\"", "'")));
        }

        String finalPrompt = getString(context, personalityPrompt);
        System.out.println("Final prompt sent to LLM: " + finalPrompt);

        return ApiClient.generateResponse(finalPrompt);
    }

    private static String getString(String context, String personalityPrompt) {
        String lastResponse = PersonalityManager.getLastResponse();

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(String.format(personalityPrompt, context.replace("\"", "'")));

        if (lastResponse != null && !lastResponse.isEmpty()) {
            promptBuilder.append("Do not use special characters, formatting or emojis in your response.");
            promptBuilder.append(" Your previous comment was: \"");
            promptBuilder.append(lastResponse.replace("\"", "'"));
            promptBuilder.append("\". Your new comment MUST be different.");
        }

        return promptBuilder.toString();
    }

    private String parseFinalResponse(String rawResponse) {
        if (rawResponse == null) {
            return null;
        }
        int thinkTagEnd = rawResponse.lastIndexOf("</think>");
        if (thinkTagEnd != -1) {
            return rawResponse.substring(thinkTagEnd + "</think>".length()).trim();
        }
        return rawResponse.trim();
    }

    private String processMultimodal(BufferedImage image) throws Exception {
        String personalityPrompt = PersonalityManager.getCurrentMultimodalPrompt();

        System.out.println("DEBUG: Multimodal prompt from personality: " + personalityPrompt);

        if (personalityPrompt == null || personalityPrompt.trim().isEmpty()) {
            System.err.println("No multimodal personality prompt found, falling back to traditional mode.");
            String imageDescription = analyzeImage(image);
            return generateResponse(imageDescription);
        }

        String finalPrompt = getFinalPrompt(personalityPrompt);
        System.out.println("Final multimodal prompt sent: " + finalPrompt);

        return ApiClient.analyzeImageMultimodal(image, finalPrompt);
    }

    private static String getFinalPrompt(String personalityPrompt) {
        String lastResponse = PersonalityManager.getLastResponse();

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(personalityPrompt);

        if (lastResponse != null && !lastResponse.isEmpty()) {
            promptBuilder.append(" Do not use special characters, formatting or emojis in your response.");
            promptBuilder.append(" Your previous comment was: \"");
            promptBuilder.append(lastResponse.replace("\"", "'"));
            promptBuilder.append("\". Your new comment MUST be different.");
        }

        return promptBuilder.toString();
    }
}
