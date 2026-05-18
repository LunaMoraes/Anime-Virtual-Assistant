package api.gemini;

import api.LanguageModelClient;
import config.ConfigurationManager;

import java.io.IOException;

public final class GeminiLanguageModelClient implements LanguageModelClient {
    private final GeminiApiClient geminiApiClient;

    public GeminiLanguageModelClient(GeminiApiClient geminiApiClient) {
        this.geminiApiClient = geminiApiClient;
    }

    @Override
    public String generate(String prompt) throws IOException, InterruptedException {
        System.out.println("Using Analysis API: " + ConfigurationManager.useApiAnalysis());
        return geminiApiClient.generateText(
            ConfigurationManager.getAnalysisApiConfig(),
            prompt,
            0.7,
            150,
            "Analysis"
        );
    }
}
