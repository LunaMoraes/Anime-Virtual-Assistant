package api;

import java.io.IOException;

/**
 * Generates text with a locally hosted language model backend.
 */
interface LocalLanguageModelClient {
    String getBackendName();

    String generate(String prompt) throws IOException, InterruptedException;

    default boolean supportsImages() {
        return false;
    }

    default String generateWithImage(String prompt, String base64Image, String mimeType) throws IOException, InterruptedException {
        throw new UnsupportedOperationException(getBackendName() + " does not support image prompts");
    }
}
