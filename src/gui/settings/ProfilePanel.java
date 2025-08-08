package gui.settings;

import levels.LevelManager;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * Displays user profile: attributes XP and available skills.
 */
public class ProfilePanel extends JPanel {
    public ProfilePanel() {
        super();
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        add(createAttributesSection());
        add(Box.createVerticalStrut(20));
        add(createSkillsSection());
        add(Box.createVerticalGlue());
    }

    private JComponent createAttributesSection() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        Map<String, Integer> xpMap = LevelManager.getAttributeXp();
        int row = 0;
        for (Map.Entry<String, Integer> e : xpMap.entrySet()) {
            JLabel name = styledLabel(e.getKey() + ":");
            JLabel value = styledLabel(String.valueOf(e.getValue()));
            gbc.gridx = 0; gbc.gridy = row; panel.add(name, gbc);
            gbc.gridx = 1; panel.add(value, gbc);
            row++;
        }

        if (xpMap.isEmpty()) {
            JLabel empty = styledLabel("No attributes found.");
            panel.add(empty);
        }

        return panel;
    }

    private JComponent createSkillsSection() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        java.util.Map<String, levels.SkillInfo> skills = LevelManager.getAvailableSkills();
        if (skills == null || skills.isEmpty()) {
            JLabel empty = styledLabel("No skills available yet.");
            panel.add(empty);
        } else {
            for (var entry : skills.entrySet()) {
                String name = entry.getKey();
                levels.SkillInfo info = entry.getValue();
                String line = String.format("• %s  —  attribute: %s, xp: %d", name, info.getAttribute(), info.getExperience());
                panel.add(styledLabel(line));
            }
        }

        return panel;
    }

    private JLabel styledLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("Arial", Font.PLAIN, 12));
        return l;
    }
}
