package api.local;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

class LmStudioLanguageModelClient implements LocalLanguageModelClient {
    static final String DEFAULT_URL = resolveSetting("lmstudio.chat.url", "LM_STUDIO_CHAT_URL", "http://127.0.0.1:1234/v1/chat/completions");
    static final String DEFAULT_MODEL = resolveSetting("lmstudio.model", "LM_STUDIO_MODEL", "google/gemma-4-e4b");
    private static final String COMMAND_SYSTEM_PROMPT =
            "You are a deterministic command emitter for an application. "
                    + "Never explain. Never include analysis. Never include markdown. "
                    + "Return only the requested square bracket sections. Start your first token with [.";
    private static final double TEMPERATURE = 0.0;

    private final Gson gson;
    private final HttpClient httpClient;

    LmStudioLanguageModelClient(Gson gson, HttpClient httpClient) {
        this.gson = gson;
        this.httpClient = httpClient;
    }

    @Override
    public String getBackendName() {
        return "LM Studio";
    }

    @Override
    public String generate(String prompt) throws IOException, InterruptedException {
        Map<String, Object> payload = Map.of(
            "model", DEFAULT_MODEL,
            "messages", List.of(
                Map.of("role", "system", "content", COMMAND_SYSTEM_PROMPT),
                Map.of("role", "user", "content", prompt)
            ),
            "temperature", TEMPERATURE,
            "reasoning", "off",
            "stream", false
        );

        return sendChatCompletion(payload);
    }

    @Override
    public boolean supportsImages() {
        return true;
    }

    @Override
    public String generateWithImage(String prompt, String base64Image, String mimeType) throws IOException, InterruptedException {
        Map<String, Object> textPart = Map.of(
            "type", "text",
            "text", prompt
        );
        Map<String, Object> imagePart = Map.of(
            "type", "image_url",
            "image_url", Map.of("url", "data:" + mimeType + ";base64," + base64Image)
        );
        Map<String, Object> message = Map.of(
            "role", "user",
            "content", List.of(textPart, imagePart)
        );
        Map<String, Object> payload = Map.of(
            "model", DEFAULT_MODEL,
            "messages", List.of(
                Map.of("role", "system", "content", COMMAND_SYSTEM_PROMPT),
                message
            ),
            "temperature", TEMPERATURE,
            "reasoning", "off",
            "stream", false
        );

        return sendChatCompletion(payload);
    }

    private String sendChatCompletion(Map<String, Object> payload) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .uri(URI.create(DEFAULT_URL))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(180))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                .build();

        System.out.println("Sending request to LM Studio: " + DEFAULT_MODEL + " at " + DEFAULT_URL);
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseChatCompletion(response.body());
            }

            System.err.printf("LM Studio error - Status: %d, Response: %s%n", response.statusCode(), response.body());
            return null;
        } catch (ConnectException e) {
            System.err.println("LM Studio unavailable at " + DEFAULT_URL + ". Start the LM Studio server and load model '" + DEFAULT_MODEL + "'.");
            return null;
        } catch (HttpTimeoutException e) {
            System.err.println("LM Studio request timed out after 180 seconds at " + DEFAULT_URL + ". The loaded model may be busy or the prompt/image may be too large.");
            return null;
        }
    }

    private String parseChatCompletion(String responseBody) {
        try {
            JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
            if (jsonObject.has("choices") && !jsonObject.getAsJsonArray("choices").isEmpty()) {
                JsonObject choice = jsonObject.getAsJsonArray("choices").get(0).getAsJsonObject();
                if (choice.has("message")) {
                    JsonObject message = choice.getAsJsonObject("message");
                    if (message.has("content") && !message.get("content").isJsonNull()) {
                        String content = message.get("content").getAsString();
                        if (!content.isBlank()) {
                            return content;
                        }

                        String finishReason = choice.has("finish_reason") && !choice.get("finish_reason").isJsonNull()
                            ? choice.get("finish_reason").getAsString()
                            : "unknown";
                        int reasoningLength = message.has("reasoning_content") && !message.get("reasoning_content").isJsonNull()
                            ? message.get("reasoning_content").getAsString().length()
                            : 0;
                        System.err.printf("LM Studio returned empty content (finish_reason=%s, reasoning_content_chars=%d).%n", finishReason, reasoningLength);
                        return null;
                    }
                }
            }
            System.err.println("LM Studio returned unexpected response format: " + responseBody);
            return null;
        } catch (Exception e) {
            System.err.println("Error parsing LM Studio response: " + e.getMessage());
            return null;
        }
    }

    private static String resolveSetting(String systemProperty, String environmentVariable, String fallback) {
        String propertyValue = System.getProperty(systemProperty);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }

        String environmentValue = System.getenv(environmentVariable);
        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue;
        }

        return fallback;
    }
}
