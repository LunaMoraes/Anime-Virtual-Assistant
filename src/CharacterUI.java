import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * Creates the always-on-top, transparent window for the assistant's image.
 */
public class CharacterUI extends JWindow {
    private SpeechBubble currentBubble;

    public CharacterUI() throws IOException {
        // Load image from local file instead of URL
        Image image = ImageIO.read(new File(AppState.CHARACTER_IMAGE_URL));
        image = image.getScaledInstance(200, 200, Image.SCALE_SMOOTH);
        JLabel label = new JLabel(new ImageIcon(image));

        setBackground(new Color(0, 0, 0, 0)); // Make the window background transparent
        add(label);
        pack();

        // Position the window at the bottom right of the screen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(screenSize.width - getWidth(), screenSize.height - getHeight());
        setAlwaysOnTop(true);
    }

    public void showSpeechBubble(String text) {
        // Hide any existing bubble first
        hideSpeechBubble();

        // Create and show new bubble
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
