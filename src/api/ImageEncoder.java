package api;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public final class ImageEncoder {
    private ImageEncoder() {}

    public static String toBase64Jpeg(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedImage jpegImage = image;
        if (image.getColorModel().hasAlpha()) {
            jpegImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = jpegImage.createGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.drawImage(image, 0, 0, null);
            graphics.dispose();
        }

        if (!ImageIO.write(jpegImage, "jpeg", baos)) {
            throw new IOException("No JPEG writer available for screenshot encoding");
        }
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    public static String toBase64Png(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", baos)) {
            throw new IOException("No PNG writer available for screenshot encoding");
        }
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }
}
