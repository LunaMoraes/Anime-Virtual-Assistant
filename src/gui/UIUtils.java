package gui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicRadioButtonUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.BasicStroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

public final class UIUtils {
    private UIUtils() {}

    private static final String ORIG_FG_KEY = "uiutils.orig.fg";
    private static final String ORIG_BG_KEY = "uiutils.orig.bg";
    private static final String ORIG_OPAQUE_KEY = "uiutils.orig.opaque";
    private static final String HOVER_INSTALLED_KEY = "uiutils.hover.installed";
    private static final String LIST_HOVER_INDEX_KEY = "uiutils.combo.hover.index";
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

                        int w = getIconWidth();
                        int h = getIconHeight();

                        // Compute centered geometry to avoid any top-left bias on HiDPI
                        double cx = x + w / 2.0;
                        double cy = y + h / 2.0;
                        double outerDiameter = Math.min(w, h) - 4.0; // padding for crisp stroke
                        double ox = Math.round(cx - outerDiameter / 2.0);
                        double oy = Math.round(cy - outerDiameter / 2.0);

                        g2.setColor(new Color(150, 100, 200, 255));
                        g2.setStroke(new BasicStroke(1.25f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g2.draw(new Ellipse2D.Double(ox, oy, outerDiameter, outerDiameter));

                        if (rb.isSelected()) {
                            double innerDiameter = Math.max(4.0, Math.floor(outerDiameter * 0.5));
                            double ix = Math.round(cx - innerDiameter / 2.0);
                            double iy = Math.round(cy - innerDiameter / 2.0);
                            g2.setColor(new Color(150, 100, 200, 255));
                            g2.fill(new Ellipse2D.Double(ix, iy, innerDiameter, innerDiameter));
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

        // Custom UI to prevent system L&F from overriding colors or painting blue when focused/selected
        combo.setUI(new BasicComboBoxUI() {
            // Subtle hover background for popup items (solid, no transparency)
            private final Color LIST_BG = new Color(40, 40, 40, 255);
            private final Color LIST_FG = Color.WHITE;

            @Override
            protected JButton createArrowButton() {
                JButton btn = super.createArrowButton();
                btn.setOpaque(true);
                btn.setBackground(new Color(150, 100, 200, 255)); // SOLID
                btn.setBorder(null);
                return btn;
            }

            @Override
            public void paint(Graphics g, JComponent c) {
                // Always paint a solid background first, so the L&F cannot bleed blue
                g.setColor(new Color(40, 40, 40, 255));
                g.fillRect(0, 0, c.getWidth(), c.getHeight());
                super.paint(g, c);
            }

            @Override
            public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
                // Ignore focus highlight and force consistent colors
                Color bg = new Color(40, 40, 40, 255);
                g.setColor(bg);
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

                ListCellRenderer<Object> r = (ListCellRenderer<Object>) comboBox.getRenderer();
                Object value = comboBox.getSelectedItem();
                Component c = r.getListCellRendererComponent(listBox, value, -1, false, false);
                if (c instanceof JComponent jc) {
                    jc.setOpaque(false); // background already painted above
                    jc.setForeground(Color.WHITE);
                }
                currentValuePane.paintComponent(g, c, comboBox, bounds.x, bounds.y, bounds.width, bounds.height, true);
            }

            @Override
            protected ComboPopup createPopup() {
                BasicComboPopup popup = new BasicComboPopup(comboBox) {
                    @Override
                    protected JList<Object> createList() {
                        JList<Object> list = super.createList();
                        list.setOpaque(true);
                        list.setBackground(LIST_BG);
                        list.setForeground(LIST_FG);
                        list.setSelectionBackground(LIST_BG); // keep same as default (no blue)
                        list.setSelectionForeground(LIST_FG);
                        // Track hovered index without querying mouse in renderer (avoids recursion)
                        list.addMouseMotionListener(new MouseMotionAdapter() {
                            @Override
                            public void mouseMoved(MouseEvent e) {
                                int idx = list.locationToIndex(e.getPoint());
                                Integer prev = (Integer) list.getClientProperty(LIST_HOVER_INDEX_KEY);
                                if (prev == null || prev != idx) {
                                    list.putClientProperty(LIST_HOVER_INDEX_KEY, idx);
                                    list.repaint();
                                }
                            }
                        });
                        list.addMouseListener(new MouseAdapter() {
                            @Override
                            public void mouseExited(MouseEvent e) {
                                Integer prev = (Integer) list.getClientProperty(LIST_HOVER_INDEX_KEY);
                                if (prev == null || prev != -1) {
                                    list.putClientProperty(LIST_HOVER_INDEX_KEY, -1);
                                    list.repaint();
                                }
                            }
                        });
                        return list;
                    }

                    @Override
                    protected JScrollPane createScroller() {
                        JScrollPane sp = super.createScroller();
                        sp.getViewport().setOpaque(true);
                        sp.getViewport().setBackground(LIST_BG);
                        sp.setOpaque(true);
                        sp.setBackground(LIST_BG);
                        return sp;
                    }
                };
                return popup;
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
            // Ensure the popup list itself doesn't paint blue selection
            list.setSelectionBackground(new Color(40, 40, 40, 255));
            list.setSelectionForeground(Color.WHITE);
            // Hover effect for options inside the popup list, based on tracked index
            Integer hoverIdx = (Integer) list.getClientProperty(LIST_HOVER_INDEX_KEY);
            boolean isHovered = hoverIdx != null && index >= 0 && hoverIdx == index;
            if (isHovered && c instanceof JComponent jc2) {
                jc2.setBackground(new Color(60, 50, 70, 255));
                jc2.setForeground(new Color(200, 100, 200));
                jc2.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(150, 100, 200, 180), 1, true),
                        BorderFactory.createEmptyBorder(1, 3, 1, 3)));
            }
            return c;
        });
    }
}
