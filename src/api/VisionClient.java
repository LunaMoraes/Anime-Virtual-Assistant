package api;

import java.awt.image.BufferedImage;
import java.io.IOException;

public interface VisionClient {
    String analyze(BufferedImage image, String prompt) throws IOException, InterruptedException;
}
