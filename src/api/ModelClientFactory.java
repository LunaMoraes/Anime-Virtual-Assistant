package api;

import com.google.gson.Gson;
import api.gemini.GeminiApiClient;
import api.gemini.GeminiLanguageModelClient;
import api.gemini.GeminiMultimodalClient;
import api.gemini.GeminiVisionClient;
import api.local.LocalLanguageModelClientFactory;

import java.net.http.HttpClient;
import java.time.Duration;

public final class ModelClientFactory {
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final GeminiApiClient GEMINI_API_CLIENT = new GeminiApiClient(GSON, HTTP_CLIENT);
    private static final LocalLanguageModelClientFactory LOCAL_LANGUAGE_MODEL_CLIENT_FACTORY =
            new LocalLanguageModelClientFactory(GSON, HTTP_CLIENT);

    private ModelClientFactory() {}

    public static VisionClient createVisionClient() {
        return new GeminiVisionClient(GEMINI_API_CLIENT);
    }

    public static LanguageModelClient createLanguageModelClient() {
        return new ConfiguredLanguageModelClient(
            new GeminiLanguageModelClient(GEMINI_API_CLIENT),
            LOCAL_LANGUAGE_MODEL_CLIENT_FACTORY
        );
    }

    public static LanguageModelClient createApiLanguageModelClient() {
        return new GeminiLanguageModelClient(GEMINI_API_CLIENT);
    }

    public static MultimodalClient createMultimodalClient() {
        return new ConfiguredMultimodalClient(
            new GeminiMultimodalClient(GEMINI_API_CLIENT),
            LOCAL_LANGUAGE_MODEL_CLIENT_FACTORY
        );
    }
}
