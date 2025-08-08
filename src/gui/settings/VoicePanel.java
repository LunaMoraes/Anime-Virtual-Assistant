package gui.settings;

import core.AppState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;

public class VoicePanel extends JPanel {
    private final JComboBox<String> voiceSelector;

    public VoicePanel(String[] voices) {
        super(new FlowLayout(FlowLayout.LEFT));
        setOpaque(false);

        voiceSelector = new JComboBox<>(voices);
        voiceSelector.setSelectedItem(AppState.selectedTtsCharacterVoice);
        voiceSelector.setPreferredSize(new Dimension(200, 30));
        add(voiceSelector);

        voiceSelector.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                AppState.selectedTtsCharacterVoice = (String) e.getItem();
                System.out.println("Voice changed to: " + AppState.selectedTtsCharacterVoice);
                AppState.saveCurrentSettings();
            }
        });
    }

    public JComboBox<String> getVoiceSelector() {
        return voiceSelector;
    }
}
