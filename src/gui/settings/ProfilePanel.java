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
    // Keep transparent to preserve themed background
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
            // Create a main panel to hold the columns, using a GridLayout.
            // This ensures columns are of equal width.
            JPanel columnsContainer = new JPanel(new GridLayout(1, 3, 16, 0));
            columnsContainer.setOpaque(false);

            // Create three panels to act as our columns.
            // Each column will stack components vertically.
            JPanel[] columns = new JPanel[3];
            for (int i = 0; i < 3; i++) {
                columns[i] = new JPanel();
                columns[i].setOpaque(false);
                columns[i].setLayout(new BoxLayout(columns[i], BoxLayout.Y_AXIS));
                columnsContainer.add(columns[i]);
            }

            int currentColumn = 0;
            for (Map.Entry<String, Integer> e : attrLevels.entrySet()) {
                String attr = e.getKey();
                Integer lvl = e.getValue();

                // Create the card for the attribute
                JPanel card = new JPanel(new BorderLayout());
                card.setOpaque(false);
                // Align the card to the left within its column
                card.setAlignmentX(Component.LEFT_ALIGNMENT);

                int shown = (lvl != null ? lvl : 0);
                JPanel header = new JPanel();
                header.setOpaque(false);
                header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
                header.setAlignmentX(Component.LEFT_ALIGNMENT);
                JLabel attrLabel = styledLabel(attr + ": " + shown + "/99");
                attrLabel.setFont(new Font("Arial", Font.BOLD, 12));
                attrLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                header.add(attrLabel);

                if (shown > 0) {
                    int xpLeft = LevelManager.getXpToNextLevelForAttribute(attr);
                    double ratio = LevelManager.getLevelProgressRatioForAttribute(attr);
                    LevelBar bar = new LevelBar(ratio);
                    bar.setAlignmentX(Component.LEFT_ALIGNMENT);
                    bar.setToolTipText("XP to next level: " + xpLeft);
                    header.add(Box.createRigidArea(new Dimension(0, 3)));
                    header.add(bar);
                }

                card.add(header, BorderLayout.NORTH);

                // Create the nested panel for skills under this attribute
                JPanel nested = new JPanel();
                nested.setOpaque(false);
                nested.setLayout(new BoxLayout(nested, BoxLayout.Y_AXIS));
                int count = 0;
                if (skills != null) {
                    for (var s : skills.entrySet()) {
                        String name = s.getKey();
                        SkillInfo info = s.getValue();
                        if (info != null && info.getAttribute() != null && info.getAttribute().equalsIgnoreCase(attr)) {
                            // Use HTML to allow text wrapping for long skill names.
                            // This is the key fix to prevent the panel from becoming too wide.
                            String skillText = "<html><body style='width: 110px; margin-left: 10px;'>&bull; " + name + ": " + info.getExperience() + "xp</body></html>";
                            JLabel item = styledLabel(skillText);
                            nested.add(item);
                            count++;
                        }
                    }
                }
                if (count == 0) {
                    nested.add(styledLabel("    (no skills yet)"));
                }

                card.add(nested, BorderLayout.CENTER);
                card.setBorder(BorderFactory.createEmptyBorder(4, 4, 8, 4));

                // Add the card to the current column
                columns[currentColumn].add(card);

                // Add some vertical space between cards in the same column
                columns[currentColumn].add(Box.createRigidArea(new Dimension(0, 10)));

                // Move to the next column for the next attribute
                currentColumn = (currentColumn + 1) % 3;
            }

            // To ensure all columns are pushed to the top, add vertical glue to each.
            for (int i = 0; i < 3; i++) {
                columns[i].add(Box.createVerticalGlue());
            }

            add(columnsContainer, BorderLayout.CENTER);
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

    // Slim fixed-size progress bar with pink progress fill.
    private static class LevelBar extends JComponent {
        private double ratio; // 0..1
        private static final int W = 120;
        private static final int H = 6;
        private static final Color BG = new Color(80, 70, 90, 160);
        private static final Color FG = new Color(180, 100, 200, 230);
        private static final Color BORDER = new Color(180, 100, 200, 200);

        LevelBar(double ratio) { setRatio(ratio); setOpaque(false); }
        void setRatio(double r) { this.ratio = Math.max(0.0, Math.min(1.0, r)); repaint(); }
        @Override public Dimension getPreferredSize() { return new Dimension(W, H); }
        @Override public Dimension getMinimumSize() { return getPreferredSize(); }
        @Override public Dimension getMaximumSize() { return getPreferredSize(); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            // background
            g2.setColor(BG);
            g2.fillRoundRect(0, 0, w, h, h, h);
            // progress
            int pw = (int) Math.round(w * ratio);
            if (pw > 0) {
                g2.setColor(FG);
                g2.fillRoundRect(0, 0, pw, h, h, h);
            }
            // border
            g2.setColor(BORDER);
            g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);
            g2.dispose();
        }
    }
}
