package api.local;

import com.google.gson.Gson;
import config.ConfigurationManager;

import java.net.http.HttpClient;

public final class LocalLanguageModelClientFactory {
    private final Gson gson;
    private final HttpClient httpClient;

    public LocalLanguageModelClientFactory(Gson gson, HttpClient httpClient) {
        this.gson = gson;
        this.httpClient = httpClient;
    }

    public LocalLanguageModelClient create() {
        String provider = ConfigurationManager.getLocalLlmProvider();

        if ("lm_studio".equalsIgnoreCase(provider) || "lm-studio".equalsIgnoreCase(provider) || "lmstudio".equalsIgnoreCase(provider)) {
            return new LmStudioLanguageModelClient(gson, httpClient);
        }

        if (provider != null && !"ollama".equalsIgnoreCase(provider) && !provider.isBlank()) {
            System.err.println("Unknown local LLM provider '" + provider + "'. Falling back to Ollama.");
        }

        return new OllamaLanguageModelClient(gson, httpClient);
    }
}
