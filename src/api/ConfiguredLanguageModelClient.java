package api;

import api.local.LocalLanguageModelClient;
import api.local.LocalLanguageModelClientFactory;
import config.ConfigurationManager;

import java.io.IOException;

final class ConfiguredLanguageModelClient implements LanguageModelClient {
    private final LanguageModelClient apiLanguageModelClient;
    private final LocalLanguageModelClientFactory localLanguageModelClientFactory;

    ConfiguredLanguageModelClient(
            LanguageModelClient apiLanguageModelClient,
            LocalLanguageModelClientFactory localLanguageModelClientFactory
    ) {
        this.apiLanguageModelClient = apiLanguageModelClient;
        this.localLanguageModelClientFactory = localLanguageModelClientFactory;
    }

    @Override
    public String generate(String prompt) throws IOException, InterruptedException {
        if (ConfigurationManager.useApiAnalysis() && ConfigurationManager.isAnalysisApiAvailable()) {
            return apiLanguageModelClient.generate(prompt);
        }

        LocalLanguageModelClient localClient = localLanguageModelClientFactory.create();
        System.out.println("Using local language model backend: " + localClient.getBackendName());
        return localClient.generate(prompt);
    }
}
