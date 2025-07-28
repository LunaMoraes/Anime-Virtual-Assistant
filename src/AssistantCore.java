import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AssistantCore {

    private ScheduledExecutorService scheduler;
    private final List<BufferedImage> screenshotBuffer = new ArrayList<>();
    private final Gson gson = new Gson();
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
                    screenshotBuffer.remove(0);
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
            images.add(screenshotBuffer.get(screenshotBuffer.size() - 1));
            screenshotBuffer.clear();
        }

        new Thread(() -> {
            try {
                System.out.println("Analyzing screenshot with vision service...");
                String imageDescription = getImageDescription(images.get(0));
                if (imageDescription == null || imageDescription.isBlank()) {
                    System.err.println("Vision service did not return a description.");
                } else {
                    System.out.println("Vision service description: " + imageDescription);
                    System.out.println("Generating final response with language model...");
                    String rawResponse = getFinalResponse(imageDescription);
                    String finalResponseToSpeak = parseFinalResponse(rawResponse);

                    if (finalResponseToSpeak != null && !finalResponseToSpeak.isBlank()) {
                        System.out.println("Final response: " + rawResponse);
                        System.out.println("Speaking: " + finalResponseToSpeak);
                        TtsApiClient.speak(finalResponseToSpeak, AppState.selectedTtsCharacterVoice, 0.7, AppState.selectedLanguage);

                        // --- MEMORY UPDATE ---
                        // Save the newly generated response to the personality's memory.
                        if (AppState.selectedPersonality != null) {
                            AppState.selectedPersonality.setLastResponse(finalResponseToSpeak);
                            System.out.println("Saved to memory: \"" + finalResponseToSpeak + "\"");
                        }
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

    private String getImageDescription(BufferedImage image) throws IOException, InterruptedException {
        String prompt = "Describe the user's activity in this image. Focus on the content and what they are doing. Do NOT use the words 'screenshot', 'screen', or 'image'.";
        String base64Image = encodeImageToBase64(image);
        Map<String, String> payload = Map.of("prompt", prompt, "image", base64Image);
        String jsonPayload = gson.toJson(payload);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AppState.VISION_API_URL))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();
        System.out.println("Sending request to Python vision service...");
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            JsonObject jsonObject = JsonParser.parseString(response.body()).getAsJsonObject();
            String description = jsonObject.get("description").getAsString();
            return description.replaceAll("(?i)screenshot", "activity");
        } else {
            System.err.printf("Error from vision service: %d - %s%n", response.statusCode(), response.body());
            return null;
        }
    }

    /**
     * UPDATED: This method now appends the last response to the prompt to ensure variety.
     */
    private String getFinalResponse(String context) throws IOException, InterruptedException {
        Personality currentPersonality = AppState.selectedPersonality;
        if (currentPersonality == null) {
            System.err.println("No personality selected, using fallback prompt.");
            String fallbackPrompt = "Based on this screen description: \"%s\" Give a SHORT comment (maximum 15 words).";
            return callLanguageModel(String.format(fallbackPrompt, context.replace("\"", "'")));
        }

        String personalityPrompt = currentPersonality.getPrompt();
        String lastResponse = currentPersonality.getLastResponse();

        // Build the final prompt with the personality and memory context
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(String.format(personalityPrompt, context.replace("\"", "'")));

        // Add the memory instruction if a previous response exists
        if (lastResponse != null && !lastResponse.isEmpty()) {
            promptBuilder.append(" Your previous comment was: \"");
            promptBuilder.append(lastResponse.replace("\"", "'"));
            promptBuilder.append("\". Your new comment MUST be different.");
        }

        String finalPrompt = promptBuilder.toString();
        System.out.println("Final prompt sent to LLM: " + finalPrompt);

        return callLanguageModel(finalPrompt);
    }

    /**
     * Calls either the local Ollama model or the external API based on configuration
     */
    private String callLanguageModel(String prompt) throws IOException, InterruptedException {
        if (AppState.useApiModel && AppState.isApiConfigAvailable()) {
            return callExternalApi(prompt);
        } else {
            return callOllama(AppState.getCurrentModelName(), prompt, null);
        }
    }

    /**
     * Calls external API (Google Gemini) for language model inference
     */
    private String callExternalApi(String prompt) throws IOException, InterruptedException {
        String apiUrl = AppState.getApiUrl();
        String apiKey = AppState.getApiKey();

        if (apiUrl == null || apiKey == null) {
            System.err.println("API configuration not available");
            return null;
        }

        // Build the request payload for Google Gemini API
        Map<String, Object> content = Map.of(
            "parts", List.of(Map.of("text", prompt))
        );
        Map<String, Object> payload = Map.of(
            "contents", List.of(content),
            "generationConfig", Map.of(
                "temperature", 0.7,
                "maxOutputTokens", 100
            )
        );

        String jsonPayload = gson.toJson(payload);
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

        String fullUrl = apiUrl + "?key=" + apiKey;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(45))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        System.out.println("Sending request to external API: " + AppState.getCurrentModelName());
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonObject jsonObject = JsonParser.parseString(response.body()).getAsJsonObject();
            if (jsonObject.has("candidates") && jsonObject.getAsJsonArray("candidates").size() > 0) {
                JsonObject candidate = jsonObject.getAsJsonArray("candidates").get(0).getAsJsonObject();
                if (candidate.has("content")) {
                    JsonObject content2 = candidate.getAsJsonObject("content");
                    if (content2.has("parts") && content2.getAsJsonArray("parts").size() > 0) {
                        JsonObject part = content2.getAsJsonArray("parts").get(0).getAsJsonObject();
                        if (part.has("text")) {
                            return part.get("text").getAsString().trim();
                        }
                    }
                }
            }
            System.err.println("Unexpected API response format: " + response.body());
            return null;
        } else {
            System.err.printf("Error from external API: %d - %s%n", response.statusCode(), response.body());
            return null;
        }
    }

    private String callOllama(String model, String prompt, List<BufferedImage> images) throws IOException, InterruptedException {
        Map<String, Object> payload = Map.of("model", model, "prompt", prompt, "stream", false);
        String jsonPayload = gson.toJson(payload);
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AppState.OLLAMA_API_URL))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(45))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();
        System.out.printf("Sending request to Ollama for model: %s...%n", model);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            JsonObject jsonObject = JsonParser.parseString(response.body()).getAsJsonObject();
            return jsonObject.get("response").getAsString().trim();
        } else {
            System.err.printf("Error from Ollama model %s: %d - %s%n", model, response.statusCode(), response.body());
            return null;
        }
    }

    private String encodeImageToBase64(BufferedImage image) throws IOException {
        int maxWidth = 800;
        int maxHeight = 600;
        double scale = Math.min(Math.min((double) maxWidth / image.getWidth(), (double) maxHeight / image.getHeight()), 1.0);
        int newWidth = (int) (image.getWidth() * scale);
        int newHeight = (int) (image.getHeight() * scale);
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(image, 0, 0, newWidth, newHeight, null);
        g2d.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(resizedImage, "jpg", baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);
    }
}
