package gui.settings;

import core.AppState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;

public class LanguagePanel extends JPanel {
    public LanguagePanel() {
        super(new FlowLayout(FlowLayout.LEFT));
        setOpaque(false);

        String[] languages = {"English", "Japanese", "Chinese"};
        JComboBox<String> languageSelector = new JComboBox<>(languages);
        languageSelector.setSelectedItem(AppState.selectedLanguage);
        languageSelector.setPreferredSize(new Dimension(200, 30));
        add(languageSelector);

        languageSelector.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                AppState.selectedLanguage = (String) e.getItem();
                System.out.println("Language changed to: " + AppState.selectedLanguage);
                AppState.saveCurrentSettings();
            }
        });
    }
}
