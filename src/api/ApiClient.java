package api;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * @deprecated Use {@link VisionClient}, {@link LanguageModelClient}, and {@link MultimodalClient}
 * from {@link ModelClientFactory}. This class remains only as a compatibility facade.
 */
@Deprecated
public class ApiClient {
    private ApiClient() {}

    public static String analyzeImage(BufferedImage image, String prompt) throws IOException, InterruptedException {
        return ModelClientFactory.createVisionClient().analyze(image, prompt);
    }

    public static String analyzeImageMultimodal(BufferedImage image, String prompt) throws IOException, InterruptedException {
        return ModelClientFactory.createMultimodalClient().analyze(image, prompt);
    }

    public static String generateResponse(String prompt) throws IOException, InterruptedException {
        return ModelClientFactory.createLanguageModelClient().generate(prompt);
    }
}
