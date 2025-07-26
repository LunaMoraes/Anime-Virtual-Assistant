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

/**
 * The "brain" of the application. This version has been reverted to use Java's
 * built-in HttpClient and fixes the timeout issue by creating a new client
 * for each request, preventing connection pool errors.
 */
public class AssistantCore {

    private ScheduledExecutorService scheduler;
    private final List<BufferedImage> screenshotBuffer = new ArrayList<>();
    private final Gson gson = new Gson();
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

    public void startProcessing() {
        if (AppState.isRunning) return;

        // Test Ollama connectivity first
        testOllamaConnection();

        scheduler = Executors.newScheduledThreadPool(2);
        scheduler.scheduleAtFixedRate(this::captureScreenshot, 0, 500, TimeUnit.MILLISECONDS);
        scheduler.scheduleAtFixedRate(this::processAndRespond, 3, 5, TimeUnit.SECONDS);
        AppState.isRunning = true;
        System.out.println("AI Assistant background tasks started.");
    }

    private void testOllamaConnection() {
        System.out.println("Testing Ollama connection with simple text request...");
        try {
            // Create the exact same payload that worked in curl
            Map<String, Object> payload = Map.of(
                "model", "qwen2.5vl:3b",
                "prompt", "Say hello in one word",
                "stream", false
            );

            String jsonPayload = gson.toJson(payload);
            System.out.println("Test payload: " + jsonPayload);

            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:11434/api/generate"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

            System.out.println("Sending test request...");
            long startTime = System.currentTimeMillis();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            long responseTime = System.currentTimeMillis() - startTime;

            System.out.printf("Test response received in %dms, status: %d%n", responseTime, response.statusCode());
            System.out.println("Response body: " + response.body());

            if (response.statusCode() == 200) {
                JsonObject jsonObject = JsonParser.parseString(response.body()).getAsJsonObject();
                if (jsonObject.has("response")) {
                    String result = jsonObject.get("response").getAsString();
                    System.out.println("✓ Ollama connection test PASSED: " + result);
                } else {
                    System.err.println("✗ Response missing 'response' field");
                }
            } else {
                System.err.println("✗ HTTP error: " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("✗ Ollama connection test FAILED: " + e.getMessage());
            e.printStackTrace();
        }
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
            return; // A task is already running
        }

        List<BufferedImage> images;
        synchronized (screenshotBuffer) {
            if (screenshotBuffer.size() < 1) { // Changed from 2 to 1
                isProcessing.set(false);
                return;
            }
            // Send only the most recent image instead of 2 images
            images = new ArrayList<>();
            images.add(screenshotBuffer.get(screenshotBuffer.size() - 1));
            screenshotBuffer.clear();
        }

        new Thread(() -> {
            try {
                System.out.println("Analyzing screenshot with vision model...");
                String imageDescription = getImageDescription(images);
                if (imageDescription == null || imageDescription.isBlank()) {
                    System.err.println("Vision model did not return a description.");
                } else {
                    System.out.println("Vision model description: " + imageDescription);

                    System.out.println("Generating final response with language model...");
                    String rawResponse = getFinalResponse(imageDescription);

                    String finalResponseToSpeak = parseFinalResponse(rawResponse);

                    if (finalResponseToSpeak != null && !finalResponseToSpeak.isBlank()) {
                        System.out.println("Final response: " + rawResponse);
                        System.out.println("Speaking: " + finalResponseToSpeak);
                        TtsApiClient.speak(finalResponseToSpeak, AppState.selectedTtsCharacterVoice, 1.0, AppState.selectedLanguage);
                    }
                }
            } catch (Exception e) {
                System.err.println("An error occurred during AI processing: " + e.getMessage());
                e.printStackTrace();
            } finally {
                isProcessing.set(false); // Release the lock
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

    private String getImageDescription(List<BufferedImage> images) throws IOException, InterruptedException {
        String prompt = "You are a factual screen analyzer. Describe the content of these screenshots in a detailed, neutral way. Focus on what the user is doing.";
        return callOllama(AppState.VISION_MODEL, prompt, images);
    }

    private String getFinalResponse(String context) throws IOException, InterruptedException {
        String prompt = String.format(
                "You are a tsundere AI assistant. A different AI analyzed the user's screen and gave this description: \"%s\". " +
                        "Based on that description, make a brief, sassy, in-character comment about what the user is doing. " +
                        "Start your response with a tsundere phrase like 'Hmph,' or 'Geez,'. " +
                        "Do NOT repeat the description. Respond with ONLY your tsundere comment. " +
                        "It's not like you care, baka!",
                context.replace("\"", "'")
        );
        return callOllama(AppState.LANGUAGE_MODEL, prompt, null);
    }

    // CORRECTED: This method now creates a new HttpClient for each request to prevent connection issues.
    private String callOllama(String model, String prompt, List<BufferedImage> images) throws IOException, InterruptedException {
        Map<String, Object> payload;

        if (images != null && !images.isEmpty()) {
            List<String> base64Images = new ArrayList<>();
            for (BufferedImage img : images) {
                base64Images.add(encodeImageToBase64(img));
            }
            payload = Map.of("model", model, "prompt", prompt, "stream", false, "images", base64Images);
        } else {
            payload = Map.of("model", model, "prompt", prompt, "stream", false);
        }

        String jsonPayload = gson.toJson(payload);
        System.out.println("JSON Payload size: " + jsonPayload.length() + " characters");

        // Realistic timeouts - DeepSeek-R1 needs more time for reasoning
        int timeoutSeconds = (images != null && !images.isEmpty()) ? 30 : 45; // 30s for vision, 45s for reasoning models like DeepSeek-R1

        // Create a new client for each request with proper timeouts
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AppState.OLLAMA_API_URL))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        System.out.printf("Sending request to Ollama for model: %s (timeout: %ds)...%n", model, timeoutSeconds);
        System.out.println("Request URL: " + AppState.OLLAMA_API_URL);
        System.out.println("Has images: " + (images != null && !images.isEmpty()));
        long startTime = System.currentTimeMillis();

        try {
            System.out.println("Attempting to connect to Ollama...");
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            long responseTime = System.currentTimeMillis() - startTime;
            System.out.printf("Received response from Ollama for model: %s in %dms (Status: %d).%n",
                model, responseTime, response.statusCode());

            if (response.statusCode() == 200) {
                String responseBody = response.body();
                System.out.println("Response body length: " + responseBody.length() + " characters");

                JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
                if (jsonObject.has("response")) {
                    String result = jsonObject.get("response").getAsString().trim();
                    System.out.println("Extracted response length: " + result.length() + " characters");
                    return result;
                } else {
                    System.err.println("Response does not contain 'response' field");
                    System.err.println("Available fields: " + jsonObject.keySet());
                    return null;
                }
            } else {
                System.err.printf("Error from Ollama model %s: %d - %s%n", model, response.statusCode(), response.body());
                return null;
            }
        } catch (IOException | InterruptedException e) {
            long responseTime = System.currentTimeMillis() - startTime;
            System.err.printf("Request to Ollama model %s failed after %dms: %s%n", model, responseTime, e.getMessage());
            System.err.println("Exception type: " + e.getClass().getSimpleName());
            throw e;
        }
    }

    private String encodeImageToBase64(BufferedImage image) throws IOException {
        // Reduce image size even more to avoid timeouts
        int maxWidth = 400; // Reduced from 800
        int maxHeight = 300; // Reduced from 600

        // Calculate scaling to fit within max dimensions
        double scaleX = (double) maxWidth / image.getWidth();
        double scaleY = (double) maxHeight / image.getHeight();
        double scale = Math.min(Math.min(scaleX, scaleY), 1.0); // Don't upscale

        int newWidth = (int) (image.getWidth() * scale);
        int newHeight = (int) (image.getHeight() * scale);

        // Create resized image
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(image, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        System.out.printf("Resized image from %dx%d to %dx%d (scale: %.2f)%n",
                image.getWidth(), image.getHeight(), newWidth, newHeight, scale);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // Use JPEG with lower quality to reduce size further
        ImageIO.write(resizedImage, "jpg", baos);
        byte[] imageBytes = baos.toByteArray();
        String base64 = Base64.getEncoder().encodeToString(imageBytes);

        System.out.printf("Image bytes: %d, Base64 size: %d characters%n", imageBytes.length, base64.length());

        // Validate the Base64 encoding
        try {
            Base64.getDecoder().decode(base64);
            System.out.println("✓ Base64 encoding validation passed");
        } catch (IllegalArgumentException e) {
            System.err.println("✗ Base64 encoding validation failed: " + e.getMessage());
            throw new IOException("Invalid Base64 encoding", e);
        }

        return base64;
    }
}
