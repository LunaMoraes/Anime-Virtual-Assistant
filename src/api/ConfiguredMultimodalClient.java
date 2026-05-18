package api;

import api.local.LocalLanguageModelClient;
import api.local.LocalLanguageModelClientFactory;
import config.ConfigurationManager;

import java.awt.image.BufferedImage;
import java.io.IOException;

final class ConfiguredMultimodalClient implements MultimodalClient {
    private final MultimodalClient apiMultimodalClient;
    private final VisionClient apiVisionClient;
    private final LanguageModelClient apiLanguageModelClient;
    private final LocalLanguageModelClientFactory localLanguageModelClientFactory;

    ConfiguredMultimodalClient(
            MultimodalClient apiMultimodalClient,
            VisionClient apiVisionClient,
            LanguageModelClient apiLanguageModelClient,
            LocalLanguageModelClientFactory localLanguageModelClientFactory
    ) {
        this.apiMultimodalClient = apiMultimodalClient;
        this.apiVisionClient = apiVisionClient;
        this.apiLanguageModelClient = apiLanguageModelClient;
        this.localLanguageModelClientFactory = localLanguageModelClientFactory;
    }

    @Override
    public String analyze(BufferedImage image, String prompt) throws IOException, InterruptedException {
        if (ConfigurationManager.useApiMultimodal() && ConfigurationManager.isMultimodalApiConfigAvailable()) {
            System.out.println("Using Multimodal API: " + ConfigurationManager.useApiMultimodal());
            String multimodalResponse = apiMultimodalClient.analyze(image, prompt);
            if (multimodalResponse != null && !multimodalResponse.isBlank()) {
                return multimodalResponse;
            }

            System.err.println("Multimodal API returned no content; falling back to API vision -> analysis.");
            String imageDescription = apiVisionClient.analyze(image, ConfigurationManager.getVisionPrompt());
            if (imageDescription != null && !imageDescription.isBlank()) {
                String finalPrompt = prompt + "\n\nBased on this activity: " + imageDescription;
                return apiLanguageModelClient.generate(finalPrompt);
            }

            System.err.println("API multimodal fallback failed because vision returned no description.");
            return null;
        }

        System.out.println("Using Multimodal API: " + ConfigurationManager.useApiMultimodal());
        LocalLanguageModelClient localClient = localLanguageModelClientFactory.create();
        if (!localClient.supportsImages()) {
            System.err.println("Selected local backend '" + localClient.getBackendName() + "' does not support direct image prompts.");
            return null;
        }

        System.out.println("Using local multimodal backend: " + localClient.getBackendName());
        return localClient.generateWithImage(prompt, ImageEncoder.toBase64Png(image), "image/png");
    }
}
