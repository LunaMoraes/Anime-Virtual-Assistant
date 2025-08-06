import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import api.ApiClient;
import api.TtsApiClient;
import personality.PersonalityManager;
import config.ConfigurationManager;

/**
 * Simplified AssistantCore - now only orchestrates the AI processing pipeline.
 * Heavy lifting is delegated to specialized managers and API clients.
 */
public class AssistantCore {

    private ScheduledExecutorService scheduler;
    private final List<BufferedImage> screenshotBuffer = new ArrayList<>();
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

    public void startProcessing() {
        if (AppState.isRunning) return;
        scheduler = Executors.newScheduledThreadPool(2);
        scheduler.scheduleAtFixedRate(this::captureScreenshot, 0, 500, TimeUnit.MILLISECONDS);
        scheduler.scheduleAtFixedRate(this::processAndRespond, 3, 5, TimeUnit.SECONDS);
        AppState.isRunning = true;
        System.out.println("AI Assistant background tasks started.");
    }

    public void stopProcessing() {
        if (!AppState.isRunning) return;
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        AppState.isRunning = false;
        System.out.println("AI Assistant background tasks stopped.");
    }

    private void captureScreenshot() {
        try {
            Robot robot = new Robot();
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage screenshot = robot.createScreenCapture(screenRect);
            synchronized (screenshotBuffer) {
                screenshotBuffer.add(screenshot);
                if (screenshotBuffer.size() > 4) {
                    screenshotBuffer.removeFirst();
                }
            }
        } catch (AWTException e) {
            e.printStackTrace();
            stopProcessing();
        }
    }

    private void processAndRespond() {
        if (!isProcessing.compareAndSet(false, true)) {
            return;
        }

        List<BufferedImage> images;
        synchronized (screenshotBuffer) {
            if (screenshotBuffer.isEmpty()) {
                isProcessing.set(false);
                return;
            }
            images = new ArrayList<>();
            images.add(screenshotBuffer.getLast());
            screenshotBuffer.clear();
        }

        new Thread(() -> {
            try {
                System.out.println("Analyzing screenshot with vision service...");
                String imageDescription = analyzeImage(images.getFirst());

                if (imageDescription == null || imageDescription.isBlank()) {
                    System.err.println("Vision service did not return a description.");
                } else {
                    System.out.println("Vision service description: " + imageDescription);
                    System.out.println("Generating final response with language model...");
                    String rawResponse = generateResponse(imageDescription);
                    String finalResponseToSpeak = parseFinalResponse(rawResponse);

                    if (finalResponseToSpeak != null && !finalResponseToSpeak.isBlank()) {
                        System.out.println("Final response: " + rawResponse);
                        System.out.println("Speaking: " + finalResponseToSpeak);

                        // Speak the response - TtsApiClient will handle UI updates automatically
                        TtsApiClient.speak(finalResponseToSpeak, AppState.selectedTtsCharacterVoice, 1.0, AppState.selectedLanguage);

                        // Save to memory
                        PersonalityManager.saveResponseToMemory(finalResponseToSpeak);
                    }
                }
            } catch (Exception e) {
                System.err.println("An error occurred during AI processing: " + e.getMessage());
                e.printStackTrace();
            } finally {
                isProcessing.set(false);
            }
        }).start();
    }

    /**
     * Analyzes the given image using the appropriate vision service.
     */
    private String analyzeImage(BufferedImage image) throws Exception {
        String prompt = ConfigurationManager.getVisionPrompt();
        return ApiClient.analyzeImage(image, prompt);
    }

    /**
     * Generates a response based on the image description and current personality.
     */
    private String generateResponse(String context) throws Exception {
        String personalityPrompt = PersonalityManager.getCurrentPersonalityPrompt();

        if (personalityPrompt == null) {
            System.err.println("No personality selected, using fallback prompt.");
            String fallbackPrompt = ConfigurationManager.getFallbackPrompt();
            return ApiClient.generateResponse(String.format(fallbackPrompt, context.replace("\"", "'")));
        }

        String lastResponse = PersonalityManager.getLastResponse();

        // Build the final prompt with the personality and memory context
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(String.format(personalityPrompt, context.replace("\"", "'")));

        // Add the memory instruction if a previous response exists
        if (lastResponse != null && !lastResponse.isEmpty()) {
            promptBuilder.append("Do not use special characters, formatting or emojis in your response.");
            promptBuilder.append(" Your previous comment was: \"");
            promptBuilder.append(lastResponse.replace("\"", "'"));
            promptBuilder.append("\". Your new comment MUST be different.");
        }

        String finalPrompt = promptBuilder.toString();
        System.out.println("Final prompt sent to LLM: " + finalPrompt);

        return ApiClient.generateResponse(finalPrompt);
    }

    /**
     * Parses the final response to remove thinking tags if present.
     */
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
}
