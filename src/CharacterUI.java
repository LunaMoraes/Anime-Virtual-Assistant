import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import personality.Personality;

/**
 * Creates the always-on-top, transparent window for the assistant's image.
 * UPDATED: Now manages both static and speaking images for the current personality.
 */
public class CharacterUI extends JWindow {
    private SpeechBubble currentBubble;
    private final JLabel imageLabel;
    private ImageIcon staticIcon;
    private ImageIcon speakingIcon;

    public CharacterUI() {
        imageLabel = new JLabel();
        setBackground(new Color(0, 0, 0, 0));
        add(imageLabel);
        setAlwaysOnTop(true);

        // Initial image update
        updatePersonalityImages();

        // Position the window at the bottom right of the screen
        // MOVED pack() to after the image is set to ensure correct sizing.
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(screenSize.width - getWidth(), screenSize.height - getHeight());
    }

    /**
     * Loads the images for the currently selected personality in AppState.
     */
    public void updatePersonalityImages() {
        Personality current = AppState.getSelectedPersonality();
        if (current == null) {
            System.err.println("Cannot update images, no personality selected.");
            return;
        }

        staticIcon = loadImageIcon(current.getStaticImagePath());
        speakingIcon = loadImageIcon(current.getSpeakingImagePath());

        // Set the initial image to static
        showStaticImage();
    }

    private ImageIcon loadImageIcon(String path) {
        try {
            Image image = ImageIO.read(new File(path));
            image = image.getScaledInstance(200, 200, Image.SCALE_SMOOTH);
            return new ImageIcon(image);
        } catch (IOException e) {
            System.err.println("Failed to load image: " + path + ". Using fallback.");
            // Try to load the fallback image
            try {
                Image fallbackImage = ImageIO.read(new File(AppState.FALLBACK_IMAGE_URL));
                fallbackImage = fallbackImage.getScaledInstance(200, 200, Image.SCALE_SMOOTH);
                return new ImageIcon(fallbackImage);
            } catch (IOException ex) {
                System.err.println("Failed to load fallback image as well.");
                return new ImageIcon(); // Return an empty icon
            }
        }
    }

    /**
     * Displays the 'speaking' image. Should be called from the Event Dispatch Thread.
     */
    public void showSpeakingImage() {
        SwingUtilities.invokeLater(() -> {
            imageLabel.setIcon(speakingIcon);
            packAndReposition(); // Use helper to resize and position
        });
    }

    /**
     * Displays the 'static' image. Should be called from the Event Dispatch Thread.
     */
    public void showStaticImage() {
        SwingUtilities.invokeLater(() -> {
            imageLabel.setIcon(staticIcon);
            packAndReposition(); // Use helper to resize and position
        });
    }

    /**
     * Helper method to pack the window and reposition it at the bottom right.
     * This ensures the window is correctly sized after an image is set.
     */
    private void packAndReposition() {
        pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(screenSize.width - getWidth(), screenSize.height - getHeight());
    }


    public void showSpeechBubble(String text) {
        hideSpeechBubble();
        currentBubble = new SpeechBubble(text);
        currentBubble.showAbove(this);
    }

    public void hideSpeechBubble() {
        if (currentBubble != null) {
            currentBubble.hideBubble();
            currentBubble = null;
        }
    }
}
