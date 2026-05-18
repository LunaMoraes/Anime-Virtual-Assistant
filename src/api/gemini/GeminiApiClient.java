package api.gemini;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import config.SystemConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public final class GeminiApiClient {
    private final Gson gson;
    private final HttpClient httpClient;

    public GeminiApiClient(Gson gson, HttpClient httpClient) {
        this.gson = gson;
        this.httpClient = httpClient;
    }

    String generateText(SystemConfig.ApiConfig config, String prompt, double temperature, int maxTokens, String logLabel)
            throws IOException, InterruptedException {
        if (config == null) {
            System.err.println(logLabel + " API configuration not available");
            return null;
        }

        Map<String, Object> content = Map.of(
            "parts", List.of(Map.of("text", prompt))
        );
        Map<String, Object> payload = Map.of(
            "contents", List.of(content),
            "generationConfig", Map.of(
                "temperature", temperature,
                "maxOutputTokens", maxTokens
            )
        );

        return send(config, payload, logLabel, false);
    }

    String generateWithImage(SystemConfig.ApiConfig config, String prompt, String base64Image, double temperature, int maxTokens, String logLabel)
            throws IOException, InterruptedException {
        if (config == null) {
            System.err.println(logLabel + " API configuration not available");
            return null;
        }

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
        Map<String, Object> payload = Map.of(
            "contents", List.of(content),
            "generationConfig", Map.of(
                "temperature", temperature,
                "maxOutputTokens", maxTokens
            )
        );

        return send(config, payload, logLabel, "Vision".equals(logLabel));
    }

    private String send(SystemConfig.ApiConfig config, Map<String, Object> payload, String logLabel, boolean sanitizeScreenshot)
            throws IOException, InterruptedException {
        String fullUrl = config.getUrl() + "?key=" + config.getKey();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                .build();

        System.out.println("Sending " + logLabel.toLowerCase() + " request to: " + config.getUrl());
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            String text = parseTextResponse(response.body(), logLabel);
            return sanitizeScreenshot && text != null ? text.replaceAll("(?i)screenshot", "activity") : text;
        }

        System.err.printf("%s API error - Status: %d, Response: %s%n", logLabel, response.statusCode(), response.body());
        return null;
    }

    private String parseTextResponse(String responseBody, String logLabel) {
        try {
            JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
            if (jsonObject.has("candidates") && !jsonObject.getAsJsonArray("candidates").isEmpty()) {
                JsonObject candidate = jsonObject.getAsJsonArray("candidates").get(0).getAsJsonObject();
                if (candidate.has("content")) {
                    JsonObject content = candidate.getAsJsonObject("content");
                    if (content.has("parts") && !content.getAsJsonArray("parts").isEmpty()) {
                        JsonObject part = content.getAsJsonArray("parts").get(0).getAsJsonObject();
                        if (part.has("text")) {
                            System.out.println(logLabel + " API response received successfully");
                            return part.get("text").getAsString().trim();
                        }
                    }
                }
            }
            System.err.println(logLabel + " API returned unexpected response format: " + responseBody);
            return null;
        } catch (Exception e) {
            System.err.println("Error parsing " + logLabel + " API response: " + e.getMessage());
            return null;
        }
    }
}
