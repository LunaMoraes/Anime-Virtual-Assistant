package gui.settings;

import levels.LevelManager;
import levels.SkillInfo;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * Displays user profile: attributes XP and available skills.
 */
public class ProfilePanel extends JPanel {
    private final JPanel content;

    public ProfilePanel() {
        super();
        setOpaque(false);
        setLayout(new BorderLayout());

        content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
    JScrollPane scroll = new JScrollPane(
        content,
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scroll.setBorder(null);
    scroll.setOpaque(false);
    scroll.getViewport().setOpaque(false);
    scroll.getVerticalScrollBar().setUnitIncrement(20);
        add(scroll, BorderLayout.CENTER);

        rebuild();

        // Live updates when levels change
        LevelManager.addLevelsListener(this::rebuild);
    }

    private void rebuild() {
        content.removeAll();

        // Title
        JLabel title = styledLabel("Profile");
        title.setFont(new Font("Arial", Font.BOLD, 14));
        content.add(title);
        content.add(Box.createVerticalStrut(8));

        Map<String, Integer> attrLevels = LevelManager.getAttributeXp();
        Map<String, SkillInfo> skills = LevelManager.getAvailableSkills();

        if (attrLevels == null || attrLevels.isEmpty()) {
            content.add(styledLabel("No attributes found."));
        } else {
            for (Map.Entry<String, Integer> e : attrLevels.entrySet()) {
                String attr = e.getKey();
                Integer lvl = e.getValue();

                // Attribute header
                JPanel row = new JPanel(new BorderLayout());
                row.setOpaque(false);
                JLabel attrLabel = styledLabel(attr + ": " + (lvl != null ? lvl : 0));
                attrLabel.setFont(new Font("Arial", Font.BOLD, 12));
                row.add(attrLabel, BorderLayout.NORTH);

                // Nested skills for this attribute
                JPanel nested = new JPanel();
                nested.setOpaque(false);
                nested.setLayout(new BoxLayout(nested, BoxLayout.Y_AXIS));
                int count = 0;
                if (skills != null) {
                    for (var s : skills.entrySet()) {
                        String name = s.getKey();
                        SkillInfo info = s.getValue();
                        if (info != null && info.getAttribute() != null && info.getAttribute().equalsIgnoreCase(attr)) {
                            JLabel item = styledLabel("    • " + name + ": " + info.getExperience() + "xp");
                            nested.add(item);
                            count++;
                        }
                    }
                }
                if (count == 0) nested.add(styledLabel("    (no skills yet)"));

                row.add(nested, BorderLayout.CENTER);
                row.setBorder(BorderFactory.createEmptyBorder(4, 4, 8, 4));
                content.add(row);
            }
        }

    content.add(Box.createVerticalGlue());
    // Let layout recompute preferred sizes for proper scroll behavior
    content.setPreferredSize(null);
    content.revalidate();
    content.repaint();
    }

    private JLabel styledLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("Arial", Font.PLAIN, 12));
        return l;
    }
}
