package gui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicRadioButtonUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public final class UIUtils {
    private UIUtils() {}

    private static final String ORIG_FG_KEY = "uiutils.orig.fg";
    private static final String ORIG_BG_KEY = "uiutils.orig.bg";
    private static final String ORIG_OPAQUE_KEY = "uiutils.orig.opaque";
    private static final String HOVER_INSTALLED_KEY = "uiutils.hover.installed";
    private static final Color HOVER_FG = new Color(200, 100, 200);
    private static final Border HOVER_BORDER = BorderFactory.createLineBorder(new Color(200, 100, 200, 120), 1, true);
    private static final Border EMPTY_BORDER = BorderFactory.createEmptyBorder(1,1,1,1);
    private static final Color HOVER_BG_FILL = new Color(40, 40, 40, 255); // SOLID background to prevent bleed

    public static void styleCheckBox(JCheckBox box) {
        if (box == null) return;
        // Force white text and SOLID background always - no transparency
        box.setForeground(Color.WHITE);
        box.setOpaque(true);
        box.setBackground(new Color(40, 40, 40, 255)); // SOLID background
        box.setContentAreaFilled(true);
        box.setFocusPainted(false);
        box.setBorderPainted(true);
        box.setBorder(EMPTY_BORDER);
        box.setRolloverEnabled(false); // Disable default rollover to prevent conflicts
        installHover(box);
    }

    public static void styleRadio(JRadioButton btn) {
        if (btn == null) return;
        // Force white text and SOLID background always - no transparency
        btn.setForeground(Color.WHITE);
        btn.setOpaque(true);
        btn.setBackground(new Color(40, 40, 40, 255)); // SOLID background
        btn.setContentAreaFilled(true);
        btn.setFocusPainted(false);
        btn.setBorderPainted(true);
        btn.setBorder(EMPTY_BORDER);
        btn.setRolloverEnabled(false); // Disable default rollover to prevent conflicts
        
        // Custom UI for pink selection dot instead of blue
        btn.setUI(new BasicRadioButtonUI() {
            @Override
            protected void paintFocus(Graphics g, Rectangle textRect, Dimension size) {
                // No focus painting to avoid blue outline
            }
            
            @Override
            public Icon getDefaultIcon() {
                return new Icon() {
                    @Override
                    public void paintIcon(Component c, Graphics g, int x, int y) {
                        JRadioButton rb = (JRadioButton) c;
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        
                        // Draw outer circle (border) - properly aligned
                        g2.setColor(new Color(150, 100, 200, 255));
                        g2.drawOval(x, y, 12, 12);
                        
                        // Fill inner dot if selected - properly centered
                        if (rb.isSelected()) {
                            g2.setColor(new Color(150, 100, 200, 255)); // Pink dot
                            g2.fillOval(x + 3, y + 3, 6, 6);
                        }
                        
                        g2.dispose();
                    }
                    
                    @Override
                    public int getIconWidth() { return 13; }
                    
                    @Override
                    public int getIconHeight() { return 13; }
                };
            }
        });
        
        installHover(btn);
    }

    private static void installHover(AbstractButton btn) {
        Object installed = btn.getClientProperty(HOVER_INSTALLED_KEY);
        if (Boolean.TRUE.equals(installed)) return;
        btn.putClientProperty(HOVER_INSTALLED_KEY, Boolean.TRUE);
        
        // Store original state
        btn.putClientProperty(ORIG_FG_KEY, btn.getForeground());
        btn.putClientProperty(ORIG_BG_KEY, btn.getBackground());
        btn.putClientProperty(ORIG_OPAQUE_KEY, btn.isOpaque());
        
        btn.addMouseListener(new MouseAdapter() {
            @Override 
            public void mouseEntered(MouseEvent e) {
                // ALWAYS keep solid background - never allow transparency
                btn.setOpaque(true);
                btn.setBackground(HOVER_BG_FILL);
                btn.setForeground(HOVER_FG);
                btn.setBorder(HOVER_BORDER);
                btn.repaint();
            }
            
            @Override 
            public void mouseExited(MouseEvent e) {
                // Restore original state but FORCE solid background
                Color origFg = (Color) btn.getClientProperty(ORIG_FG_KEY);
                Color origBg = (Color) btn.getClientProperty(ORIG_BG_KEY);
                
                if (origFg != null) btn.setForeground(origFg);
                else btn.setForeground(Color.WHITE);
                
                if (origBg != null) btn.setBackground(origBg);
                else btn.setBackground(new Color(40, 40, 40, 255)); // Fallback solid
                
                // NEVER allow transparency
                btn.setOpaque(true);
                btn.setBorder(EMPTY_BORDER);
                btn.repaint();
            }
        });
    }

    public static <T> void styleCombo(JComboBox<T> combo) {
        if (combo == null) return;
        
        // FORCE solid backgrounds everywhere
        combo.setOpaque(true);
        combo.setBackground(new Color(40, 40, 40, 255)); // SOLID
        combo.setForeground(Color.WHITE);
        combo.setBorder(BorderFactory.createLineBorder(new Color(150, 100, 200, 255), 1, true));

        // Custom UI to prevent system L&F from overriding colors
        combo.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton btn = super.createArrowButton();
                btn.setOpaque(true);
                btn.setBackground(new Color(150, 100, 200, 255)); // SOLID
                btn.setBorder(null);
                return btn;
            }
        });

        ListCellRenderer<? super T> base = combo.getRenderer();
        combo.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            Component c = base.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (c instanceof JComponent jc) {
                jc.setOpaque(true); // SOLID backgrounds only
                // ALWAYS use the same colors - NO selection highlighting
                jc.setBackground(new Color(40, 40, 40, 255)); // SOLID - same for selected/unselected
                jc.setForeground(Color.WHITE);
                jc.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
            }
            return c;
        });
    }
}
