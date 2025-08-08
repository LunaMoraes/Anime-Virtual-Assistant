package gui.settings;

import javax.swing.*;
import java.awt.*;

/**
 * Reusable titled section container with consistent styling.
 */
public class SectionPanel extends JPanel {
    public SectionPanel(String title, JComponent content) {
        super(new BorderLayout());
        // Semi-transparent dark background for readability
        setBackground(new Color(40, 40, 40, 200));
        setOpaque(true);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(150, 100, 200, 180), 2),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setForeground(Color.WHITE);
        add(titleLabel, BorderLayout.NORTH);

        add(Box.createVerticalStrut(10), BorderLayout.CENTER);
    add(content, BorderLayout.SOUTH);
    }
}
