package api.gemini;

import api.ImageEncoder;
import api.MultimodalClient;
import config.ConfigurationManager;

import java.awt.image.BufferedImage;
import java.io.IOException;

public final class GeminiMultimodalClient implements MultimodalClient {
    private final GeminiApiClient geminiApiClient;

    public GeminiMultimodalClient(GeminiApiClient geminiApiClient) {
        this.geminiApiClient = geminiApiClient;
    }

    @Override
    public String analyze(BufferedImage image, String prompt) throws IOException, InterruptedException {
        if (ConfigurationManager.getMultimodalApiConfig() != null) {
            System.out.println("Using Multimodal API model: " + ConfigurationManager.getMultimodalApiConfig().getModelName());
        }
        return geminiApiClient.generateWithImage(
            ConfigurationManager.getMultimodalApiConfig(),
            prompt,
            ImageEncoder.toBase64Jpeg(image),
            0.7,
            150,
            "Multimodal"
        );
    }
}
