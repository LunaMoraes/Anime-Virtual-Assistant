package gui.settings;

import core.AppState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;

/**
 * Simple panel exposing a global Enable TTS toggle.
 */
public class TtsTogglePanel extends JPanel {
    private final JCheckBox enableTts;

    public TtsTogglePanel() {
        super(new FlowLayout(FlowLayout.LEFT));
        setOpaque(false);

        enableTts = new JCheckBox("Enable TTS");
        enableTts.setOpaque(false);
        enableTts.setSelected(AppState.useTTS());
        enableTts.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
                boolean enabled = enableTts.isSelected();
                AppState.setUseTTS(enabled);
                // Try to update any voice selector visible in the window
                SwingUtilities.invokeLater(() -> {
                    Window w = SwingUtilities.getWindowAncestor(TtsTogglePanel.this);
                    if (w instanceof JFrame frame) {
                        Component combo = findComboBox(frame.getContentPane());
                        if (combo instanceof JComboBox) {
                            ((JComboBox<?>) combo).setEnabled(enabled);
                        }
                    }
                });
            }
        });

        add(enableTts);
    }

    private Component findComboBox(Container container) {
        for (Component c : container.getComponents()) {
            if (c instanceof JComboBox) return c;
            if (c instanceof Container) {
                Component found = findComboBox((Container) c);
                if (found != null) return found;
            }
        }
        return null;
    }
}
