package api;

import java.io.IOException;

public interface LanguageModelClient {
    String generate(String prompt) throws IOException, InterruptedException;
}
