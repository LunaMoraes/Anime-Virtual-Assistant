package api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

class OllamaLanguageModelClient implements LocalLanguageModelClient {
    static final String DEFAULT_URL = resolveSetting("ollama.api.url", "OLLAMA_API_URL", "http://localhost:11434/api/generate");
    static final String DEFAULT_MODEL = resolveSetting("ollama.model", "OLLAMA_MODEL", "qwen3:4b");

    private final Gson gson;
    private final HttpClient httpClient;

    OllamaLanguageModelClient(Gson gson, HttpClient httpClient) {
        this.gson = gson;
        this.httpClient = httpClient;
    }

    @Override
    public String getBackendName() {
        return "Ollama";
    }

    @Override
    public String generate(String prompt) throws IOException, InterruptedException {
        Map<String, Object> payload = Map.of(
            "model", DEFAULT_MODEL,
            "prompt", prompt,
            "stream", false,
            "options", Map.of("temperature", 0.7)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(DEFAULT_URL))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                .build();

        System.out.println("Sending request to Ollama: " + DEFAULT_MODEL + " at " + DEFAULT_URL);
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject jsonObject = JsonParser.parseString(response.body()).getAsJsonObject();
                return jsonObject.get("response").getAsString();
            }

            System.err.printf("Ollama error - Status: %d, Response: %s%n", response.statusCode(), response.body());
            return null;
        } catch (ConnectException e) {
            System.err.println("Ollama unavailable at " + DEFAULT_URL + ". Start Ollama and ensure model '" + DEFAULT_MODEL + "' is pulled.");
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
