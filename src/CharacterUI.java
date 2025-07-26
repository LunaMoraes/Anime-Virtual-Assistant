import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URL;

/**
 * Creates the always-on-top, transparent window for the assistant's image.
 */
public class CharacterUI extends JWindow {

    public CharacterUI() throws IOException {
        Image image = ImageIO.read(new URL(AppState.CHARACTER_IMAGE_URL));
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
}
