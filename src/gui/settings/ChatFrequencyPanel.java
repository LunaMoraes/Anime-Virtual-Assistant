package gui.settings;

import core.AppState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;

/**
 * Allows choosing chat frequency: frequent (2), medium (3), scarse (5)
 */
public class ChatFrequencyPanel extends JPanel {
    private final JComboBox<String> combo;

    public ChatFrequencyPanel() {
        super(new FlowLayout(FlowLayout.LEFT));
        setOpaque(false);

        combo = new JComboBox<>(new String[]{"frequent", "medium", "scarse"});
        combo.setSelectedItem(AppState.getChatFrequency());
        combo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String sel = (String) e.getItem();
                AppState.setChatFrequency(sel);
                System.out.println("Chat frequency set to: " + sel + " (divisor=" + AppState.getChatFrequencyDivisor() + ")");
            }
        });

        add(combo);
    }
}
