package gui.settings;

import levels.LevelManager;
import levels.SkillInfo;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * Displays user profile: attributes XP and available skills.
 * Note: No inner scroll pane; outer tab's scroll handles mouse wheel.
 */
public class ProfilePanel extends JPanel {

    public ProfilePanel() {
        super();
        setOpaque(false);
        setLayout(new BorderLayout(0, 10));

        rebuild();

        // Live updates when levels change
        LevelManager.addLevelsListener(this::rebuild);
    }

    private void rebuild() {
        removeAll();

        Map<String, Integer> attrLevels = LevelManager.getAttributeXp();
        Map<String, SkillInfo> skills = LevelManager.getAvailableSkills();

        if (attrLevels == null || attrLevels.isEmpty()) {
            add(styledLabel("No attributes found."), BorderLayout.NORTH);
        } else {
            // Three-column grid for attributes
            JPanel grid = new JPanel(new GridLayout(0, 3, 16, 12));
            grid.setOpaque(false);

            for (Map.Entry<String, Integer> e : attrLevels.entrySet()) {
                String attr = e.getKey();
                Integer lvl = e.getValue();

                JPanel card = new JPanel(new BorderLayout());
                card.setOpaque(false);
                JLabel attrLabel = styledLabel(attr + ": " + (lvl != null ? lvl : 0));
                attrLabel.setFont(new Font("Arial", Font.BOLD, 12));
                card.add(attrLabel, BorderLayout.NORTH);

                JPanel nested = new JPanel();
                nested.setOpaque(false);
                nested.setLayout(new BoxLayout(nested, BoxLayout.Y_AXIS));
                int count = 0;
                if (skills != null) {
                    for (var s : skills.entrySet()) {
                        String name = s.getKey();
                        SkillInfo info = s.getValue();
                        if (info != null && info.getAttribute() != null && info.getAttribute().equalsIgnoreCase(attr)) {
                            JLabel item = styledLabel("    \u2022 " + name + ": " + info.getExperience() + "xp");
                            nested.add(item);
                            count++;
                        }
                    }
                }
                if (count == 0) nested.add(styledLabel("    (no skills yet)"));

                card.add(nested, BorderLayout.CENTER);
                card.setBorder(BorderFactory.createEmptyBorder(4, 4, 8, 4));
                grid.add(card);
            }

            add(grid, BorderLayout.CENTER);
        }

        revalidate();
        repaint();
    }

    private JLabel styledLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("Arial", Font.PLAIN, 12));
        return l;
    }
}
