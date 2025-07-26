import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;

/**
 * Creates the main settings window for the application.
 * This window is a standard JFrame and can be minimized.
 */
public class SettingsWindow extends JFrame {

    public SettingsWindow(String[] voices) {
        setTitle("AI Assistant Settings");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Closing this window will exit the app
        setSize(450, 250);

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

        // --- Language Controls ---
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Language:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
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
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        JButton startStopButton = new JButton("Start Assistant");
        startStopButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        startStopButton.setBackground(new Color(60, 179, 113));
        startStopButton.setForeground(Color.WHITE);
        panel.add(startStopButton, gbc);

        startStopButton.addActionListener(e -> {
            if (AppState.isRunning) {
                Main.assistantCore.stopProcessing();
                startStopButton.setText("Start Assistant");
                startStopButton.setBackground(new Color(60, 179, 113));
            } else {
                Main.assistantCore.startProcessing();
                startStopButton.setText("Stop Assistant");
                startStopButton.setBackground(new Color(220, 20, 60));
            }
        });

        add(panel);
        setLocationRelativeTo(null); // Center the window on screen
        setVisible(true);
    }
}
