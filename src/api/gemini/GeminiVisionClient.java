package api.gemini;

import api.ImageEncoder;
import api.VisionClient;
import config.ConfigurationManager;

import java.awt.image.BufferedImage;
import java.io.IOException;

public final class GeminiVisionClient implements VisionClient {
    private final GeminiApiClient geminiApiClient;

    public GeminiVisionClient(GeminiApiClient geminiApiClient) {
        this.geminiApiClient = geminiApiClient;
    }

    @Override
    public String analyze(BufferedImage image, String prompt) throws IOException, InterruptedException {
        System.out.println("Using Vision API: " + ConfigurationManager.useApiVision());
        return geminiApiClient.generateWithImage(
            ConfigurationManager.getVisionApiConfig(),
            prompt,
            ImageEncoder.toBase64Jpeg(image),
            0.4,
            200,
            "Vision"
        );
    }
}
