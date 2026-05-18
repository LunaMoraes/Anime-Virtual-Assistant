package api;

import java.awt.image.BufferedImage;
import java.io.IOException;

public interface MultimodalClient {
    String analyze(BufferedImage image, String prompt) throws IOException, InterruptedException;
}
