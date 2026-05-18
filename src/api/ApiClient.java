package api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import config.ConfigurationManager;
import config.SystemConfig;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Unified API client for both Vision and Language Model operations.
 * Supports both local services and external APIs (Google Gemini).
 */
public class ApiClient {
    private static final String VISION_API_URL = "http://localhost:5002/describe";

    private static final Gson gson = new Gson();
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // === Vision API Methods ===

    /**
     * Analyzes an image using either local Python service or external Vision API
     */
    public static String analyzeImage(BufferedImage image, String prompt) throws IOException, InterruptedException {
        if (ConfigurationManager.useApiVision() && ConfigurationManager.isVisionApiAvailable()) {
            System.out.println("Using Vision API: " + ConfigurationManager.useApiVision());
            return callExternalVisionApi(prompt, image);
        } else {
            System.out.println("Using Vision API: " + ConfigurationManager.useApiVision());
            return callLocalVisionService(prompt, image);
        }
    }

    /**
     * Analyzes an image using multimodal approach - combines vision and text generation in one request
     */
    public static String analyzeImageMultimodal(BufferedImage image, String prompt) throws IOException, InterruptedException {
        if (ConfigurationManager.useApiMultimodal() && ConfigurationManager.isMultimodalApiConfigAvailable()) {
            System.out.println("Using Multimodal API: " + ConfigurationManager.useApiMultimodal());
            return callExternalMultimodalApi(prompt, image);
        } else {
            System.out.println("Using Multimodal API: " + ConfigurationManager.useApiMultimodal());
            LocalLanguageModelClient localClient = createLocalLanguageModelClient();
            if (localClient.supportsImages()) {
                System.out.println("Using local multimodal backend: " + localClient.getBackendName());
                return localClient.generateWithImage(prompt, encodeImageToBase64Png(image), "image/png");
            }

            // Fallback to local processing - analyze image then generate response
            String imageDescription = callLocalVisionService(ConfigurationManager.getVisionPrompt(), image);
            if (imageDescription != null && !imageDescription.isBlank()) {
                return localClient.generate(String.format(prompt + " Based on this activity: %s", imageDescription));
            }
            return null;
        }
    }

    /**
     * Calls local Python vision service
     */
    private static String callLocalVisionService(String prompt, BufferedImage image) throws IOException, InterruptedException {
        String base64Image = encodeImageToBase64(image);
        Map<String, String> payload = Map.of("prompt", prompt, "image", base64Image);
        String jsonPayload = gson.toJson(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(VISION_API_URL))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        System.out.println("Sending request to Python vision service...");
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

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
     * Calls external vision API (Google Gemini Vision)
     */
    private static String callExternalVisionApi(String prompt, BufferedImage image) throws IOException, InterruptedException {
        SystemConfig.ApiConfig visionConfig = ConfigurationManager.getVisionApiConfig();

        if (visionConfig == null) {
            System.err.println("Vision API configuration not available");
            return null;
        }

        System.out.println("Using Vision API: " + visionConfig.getModelName());
        String base64Image = encodeImageToBase64(image);

        // Build the request payload for Google Gemini Vision API
        Map<String, Object> payload = getStringObjectMap(prompt, base64Image);

        String jsonPayload = gson.toJson(payload);
        String fullUrl = visionConfig.getUrl() + "?key=" + visionConfig.getKey();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        System.out.println("Sending vision request to: " + visionConfig.getUrl());
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return parseGeminiVisionResponse(response.body());
        } else {
            System.err.printf("Vision API error - Status: %d, Response: %s%n", response.statusCode(), response.body());
            return null;
        }
    }

    private static Map<String, Object> getStringObjectMap(String prompt, String base64Image) {
        Map<String, Object> textPart = Map.of("text", prompt);
        Map<String, Object> imagePart = Map.of(
            "inline_data", Map.of(
                "mime_type", "image/jpeg",
                "data", base64Image
            )
        );

        Map<String, Object> content = Map.of(
            "parts", List.of(textPart, imagePart)
        );

        return Map.of(
            "contents", List.of(content),
            "generationConfig", Map.of(
                "temperature", 0.4,
                "maxOutputTokens", 200
            )
        );
    }

    // === Language Model API Methods ===

    /**
     * Generates a response using either local Ollama or external Language Model API
     */
    public static String generateResponse(String prompt) throws IOException, InterruptedException {
        if (ConfigurationManager.useApiAnalysis() && ConfigurationManager.isAnalysisApiAvailable()) {
            return callExternalLanguageApi(prompt);
        } else {
            return callLocalLanguageModel(prompt);
        }
    }

    /**
     * Calls external language model API (Google Gemini)
     */
    private static String callExternalLanguageApi(String prompt) throws IOException, InterruptedException {
        SystemConfig.ApiConfig analysisConfig = ConfigurationManager.getAnalysisApiConfig();

        if (analysisConfig == null) {
            System.err.println("Analysis API configuration not available");
            return null;
        }

        System.out.println("Using Analysis API: " + ConfigurationManager.useApiAnalysis());

        // Build the request payload for Google Gemini API
        Map<String, Object> content = Map.of(
            "parts", List.of(Map.of("text", prompt))
        );

        Map<String, Object> payload = Map.of(
            "contents", List.of(content),
            "generationConfig", Map.of(
                "temperature", 0.7,
                "maxOutputTokens", 150
            )
        );

        String jsonPayload = gson.toJson(payload);
        String fullUrl = analysisConfig.getUrl() + "?key=" + analysisConfig.getKey();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        System.out.println("Sending analysis request to: " + analysisConfig.getUrl());
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return parseGeminiTextResponse(response.body());
        } else {
            System.err.printf("Analysis API error - Status: %d, Response: %s%n", response.statusCode(), response.body());
            return null;
        }
    }

    /**
     * Calls external multimodal API (Google Gemini) for combined vision and text processing
     */
    private static String callExternalMultimodalApi(String prompt, BufferedImage image) throws IOException, InterruptedException {
        SystemConfig.ApiConfig multimodalConfig = ConfigurationManager.getMultimodalApiConfig();

        if (multimodalConfig == null) {
            System.err.println("Multimodal API configuration not available");
            return null;
        }

        System.out.println("Using Multimodal API model: " + multimodalConfig.getModelName());
        String base64Image = encodeImageToBase64(image);

        // Build the request payload for Google Gemini Multimodal API
        Map<String, Object> payload = getObjectMap(prompt, base64Image);

        String jsonPayload = gson.toJson(payload);
        String fullUrl = multimodalConfig.getUrl() + "?key=" + multimodalConfig.getKey();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        System.out.println("Sending multimodal request to: " + multimodalConfig.getUrl());
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return parseGeminiTextResponse(response.body());
        } else {
            System.err.printf("Multimodal API error - Status: %d, Response: %s%n", response.statusCode(), response.body());
            return null;
        }
    }

    private static Map<String, Object> getObjectMap(String prompt, String base64Image) {
        Map<String, Object> textPart = Map.of("text", prompt);
        Map<String, Object> imagePart = Map.of(
            "inline_data", Map.of(
                "mime_type", "image/jpeg",
                "data", base64Image
            )
        );

        Map<String, Object> content = Map.of(
            "parts", List.of(textPart, imagePart)
        );

        return Map.of(
            "contents", List.of(content),
            "generationConfig", Map.of(
                "temperature", 0.7,
                "maxOutputTokens", 150
            )
        );
    }

    /**
     * Calls the configured local language model backend with a specific prompt.
     */
    private static String callLocalLanguageModel(String prompt) throws IOException, InterruptedException {
        LocalLanguageModelClient client = createLocalLanguageModelClient();
        System.out.println("Using local language model backend: " + client.getBackendName());
        return client.generate(prompt);
    }

    private static LocalLanguageModelClient createLocalLanguageModelClient() {
        String provider = ConfigurationManager.getLocalLlmProvider();

        if ("lm_studio".equalsIgnoreCase(provider) || "lm-studio".equalsIgnoreCase(provider) || "lmstudio".equalsIgnoreCase(provider)) {
            return new LmStudioLanguageModelClient(gson, httpClient);
        }

        if (provider != null && !"ollama".equalsIgnoreCase(provider) && !provider.isBlank()) {
            System.err.println("Unknown local LLM provider '" + provider + "'. Falling back to Ollama.");
        }

        return new OllamaLanguageModelClient(gson, httpClient);
    }

    // === Utility Methods ===

    /**
     * Encodes a BufferedImage to Base64 string
     */
    private static String encodeImageToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedImage jpegImage = image;
        if (image.getColorModel().hasAlpha()) {
            jpegImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D graphics = jpegImage.createGraphics();
            graphics.setColor(java.awt.Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.drawImage(image, 0, 0, null);
            graphics.dispose();
        }

        if (!ImageIO.write(jpegImage, "jpeg", baos)) {
            throw new IOException("No JPEG writer available for screenshot encoding");
        }
        byte[] imageBytes = baos.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    private static String encodeImageToBase64Png(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", baos)) {
            throw new IOException("No PNG writer available for screenshot encoding");
        }
        byte[] imageBytes = baos.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    /**
     * Parses Google Gemini Vision API response
     */
    private static String parseGeminiVisionResponse(String responseBody) {
        try {
            JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
            if (jsonObject.has("candidates") && !jsonObject.getAsJsonArray("candidates").isEmpty()) {
                JsonObject candidate = jsonObject.getAsJsonArray("candidates").get(0).getAsJsonObject();
                if (candidate.has("content")) {
                    JsonObject content = candidate.getAsJsonObject("content");
                    if (content.has("parts") && !content.getAsJsonArray("parts").isEmpty()) {
                        JsonObject part = content.getAsJsonArray("parts").get(0).getAsJsonObject();
                        if (part.has("text")) {
                            String description = part.get("text").getAsString().trim();
                            System.out.println("Vision API response received successfully");
                            return description.replaceAll("(?i)screenshot", "activity");
                        }
                    }
                }
            }
            System.err.println("Vision API returned unexpected response format: " + responseBody);
            return null;
        } catch (Exception e) {
            System.err.println("Error parsing Vision API response: " + e.getMessage());
            return null;
        }
    }

    /**
     * Parses Google Gemini Text API response
     */
    private static String parseGeminiTextResponse(String responseBody) {
        try {
            JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
            if (jsonObject.has("candidates") && !jsonObject.getAsJsonArray("candidates").isEmpty()) {
                JsonObject candidate = jsonObject.getAsJsonArray("candidates").get(0).getAsJsonObject();
                if (candidate.has("content")) {
                    JsonObject content = candidate.getAsJsonObject("content");
                    if (content.has("parts") && !content.getAsJsonArray("parts").isEmpty()) {
                        JsonObject part = content.getAsJsonArray("parts").get(0).getAsJsonObject();
                        if (part.has("text")) {
                            String text = part.get("text").getAsString().trim();
                            System.out.println("Analysis API response received successfully");
                            return text;
                        }
                    }
                }
            }
            System.err.println("Analysis API returned unexpected response format: " + responseBody);
            return null;
        } catch (Exception e) {
            System.err.println("Error parsing Analysis API response: " + e.getMessage());
            return null;
        }
    }
}
