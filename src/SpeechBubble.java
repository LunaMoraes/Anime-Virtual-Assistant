import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * A speech bubble that appears above the character during TTS playback.
 */
public class SpeechBubble extends JWindow {
    private String text;
    private final int maxWidth = 300;
    private final int padding = 15;
    private final int arcSize = 20;

    public SpeechBubble(String text) {
        this.text = text;
        setupBubble();
    }

    private void setupBubble() {
        setBackground(new Color(0, 0, 0, 0)); // Transparent background
        setAlwaysOnTop(true);

        // Create custom panel for the speech bubble
        JPanel bubblePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Draw bubble background
                g2d.setColor(new Color(255, 255, 255, 240)); // Semi-transparent white
                RoundRectangle2D bubble = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight() - 10, arcSize, arcSize);
                g2d.fill(bubble);

                // Draw bubble border
                g2d.setColor(new Color(200, 200, 200, 200));
                g2d.setStroke(new BasicStroke(2));
                g2d.draw(bubble);

                // Draw speech bubble tail (pointing down to character)
                int[] tailX = {getWidth() / 2 - 10, getWidth() / 2, getWidth() / 2 + 10};
                int[] tailY = {getHeight() - 10, getHeight(), getHeight() - 10};
                g2d.setColor(new Color(255, 255, 255, 240));
                g2d.fillPolygon(tailX, tailY, 3);
                g2d.setColor(new Color(200, 200, 200, 200));
                g2d.drawPolygon(tailX, tailY, 3);

                g2d.dispose();
            }
        };

        bubblePanel.setOpaque(false);
        bubblePanel.setLayout(new BorderLayout());

        // Create text label with word wrapping
        JLabel textLabel = new JLabel("<html><div style='text-align: center; width: " + (maxWidth - 2 * padding) + "px;'>" + text + "</div></html>");
        textLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        textLabel.setForeground(Color.BLACK);
        textLabel.setHorizontalAlignment(SwingConstants.CENTER);
        textLabel.setBorder(BorderFactory.createEmptyBorder(padding, padding, padding + 10, padding));

        bubblePanel.add(textLabel, BorderLayout.CENTER);
        add(bubblePanel);
        pack();
    }

    public void showAbove(Component component) {
        Point componentLocation = component.getLocationOnScreen();
        int x = componentLocation.x + (component.getWidth() - getWidth()) / 2;
        int y = componentLocation.y - getHeight() - 10; // 10px gap above component

        // Make sure bubble stays on screen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if (x < 0) x = 0;
        if (x + getWidth() > screenSize.width) x = screenSize.width - getWidth();
        if (y < 0) y = componentLocation.y + component.getHeight() + 10; // Show below if no room above

        setLocation(x, y);
        setVisible(true);
    }

    public void hideBubble() {
        setVisible(false);
        dispose();
    }
}
