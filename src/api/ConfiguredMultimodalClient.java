package api;

import api.local.LocalLanguageModelClient;
import api.local.LocalLanguageModelClientFactory;
import config.ConfigurationManager;

import java.awt.image.BufferedImage;
import java.io.IOException;

final class ConfiguredMultimodalClient implements MultimodalClient {
    private final MultimodalClient apiMultimodalClient;
    private final LocalLanguageModelClientFactory localLanguageModelClientFactory;

    ConfiguredMultimodalClient(
            MultimodalClient apiMultimodalClient,
            LocalLanguageModelClientFactory localLanguageModelClientFactory
    ) {
        this.apiMultimodalClient = apiMultimodalClient;
        this.localLanguageModelClientFactory = localLanguageModelClientFactory;
    }

    @Override
    public String analyze(BufferedImage image, String prompt) throws IOException, InterruptedException {
        if (ConfigurationManager.useApiMultimodal() && ConfigurationManager.isMultimodalApiConfigAvailable()) {
            System.out.println("Using Multimodal API: " + ConfigurationManager.useApiMultimodal());
            return apiMultimodalClient.analyze(image, prompt);
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
