import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.List;

/**
 * Creates the main settings window for the application.
 * This window is a standard JFrame and can be minimized.
 */
public class SettingsWindow extends JFrame {

    public SettingsWindow(String[] voices) {
        setTitle("AI Assistant Settings");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Closing this window will exit the app
        setSize(500, 350);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- Voice Controls ---
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Assistant Voice:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        JComboBox<String> voiceSelector = new JComboBox<>(voices);
        voiceSelector.setSelectedItem(AppState.selectedTtsCharacterVoice);
        panel.add(voiceSelector, gbc);

        voiceSelector.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                AppState.selectedTtsCharacterVoice = (String) e.getItem();
                System.out.println("Voice changed to: " + AppState.selectedTtsCharacterVoice);
            }
        });

        // --- Personality Controls ---
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        panel.add(new JLabel("Personality:"), gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        JPanel personalityPanel = createPersonalityPanel();
        panel.add(personalityPanel, gbc);

        // --- Language Controls ---
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        panel.add(new JLabel("Language:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 3;
        String[] languages = {"English", "Japanese", "Chinese"};
        JComboBox<String> languageSelector = new JComboBox<>(languages);
        languageSelector.setSelectedItem(AppState.selectedLanguage);
        panel.add(languageSelector, gbc);

        languageSelector.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                AppState.selectedLanguage = (String) e.getItem();
                System.out.println("Language changed to: " + AppState.selectedLanguage);
            }
        });

        // --- Start/Stop Button ---
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton startStopButton = new JButton("Start Assistant");
        startStopButton.addActionListener(e -> {
            if (AppState.isRunning) {
                Main.assistantCore.stopProcessing();
                startStopButton.setText("Start Assistant");
            } else {
                Main.assistantCore.startProcessing();
                startStopButton.setText("Stop Assistant");
            }
        });
        panel.add(startStopButton, gbc);

        add(panel);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel createPersonalityPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ButtonGroup personalityGroup = new ButtonGroup();

        List<Personality> personalities = AppState.getAvailablePersonalities();

        if (personalities.isEmpty()) {
            JLabel noPersonalitiesLabel = new JLabel("No personalities found. Check data folder.");
            panel.add(noPersonalitiesLabel);
            return panel;
        }

        for (Personality personality : personalities) {
            JRadioButton radioButton = new JRadioButton(personality.getName());
            personalityGroup.add(radioButton);

            // Set selected if this is the current personality
            if (AppState.selectedPersonality != null &&
                    personality.getName().equals(AppState.selectedPersonality.getName())) {
                radioButton.setSelected(true);
            }

            radioButton.addActionListener(e -> {
                if (radioButton.isSelected()) {
                    AppState.setSelectedPersonality(personality);
                }
            });

            panel.add(radioButton);
        }

        return panel;
    }
}
