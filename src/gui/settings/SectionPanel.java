package gui.settings;

import javax.swing.*;
import java.awt.*;

/**
 * Reusable titled section container with consistent styling.
 */
public class SectionPanel extends JPanel {
    public SectionPanel(String title, JComponent content) {
        this(title, content, false);
    }

    public SectionPanel(String title, JComponent content, boolean centerTitle) {
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
        if (centerTitle) {
            // Use a 3-column grid title row so the center aligns with the middle column below
            JPanel titleWrap = new JPanel(new GridLayout(1, 3, 16, 0));
            titleWrap.setOpaque(false);
            // left filler
            titleWrap.add(new JLabel());
            // centered title in the middle cell
            titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
            titleWrap.add(titleLabel);
            // right filler
            titleWrap.add(new JLabel());
            add(titleWrap, BorderLayout.NORTH);
        } else {
            add(titleLabel, BorderLayout.NORTH);
        }
    JPanel body = new JPanel();
    body.setOpaque(false);
    body.setLayout(new BorderLayout());
    body.add(Box.createVerticalStrut(10), BorderLayout.NORTH);
    body.add(content, BorderLayout.CENTER);
    add(body, BorderLayout.CENTER);
    }
}
