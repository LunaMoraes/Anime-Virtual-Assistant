import javax.swing.*;
import java.io.IOException;
import java.util.List;

/**
 * The main entry point for the AI Assistant application.
 * UPDATED: Initializes CharacterUI without IOExceptions in the constructor.
 */
public class Main {
    public static AssistantCore assistantCore;
    public static CharacterUI characterUI;

    public static void main(String[] args) {
        System.out.println("Loading personalities from data folder...");
        AppState.loadPersonalities();

        System.out.println("Fetching available TTS characters...");
        List<String> voices = TtsApiClient.getAvailableCharacters();

        if (voices == null || voices.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Could not connect to the TTS API Server.\nPlease ensure api.py is running.", "Connection Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        AppState.selectedTtsCharacterVoice = voices.get(0);
        System.out.println("TTS voices found. Default voice: " + AppState.selectedTtsCharacterVoice);

        assistantCore = new AssistantCore();

        SwingUtilities.invokeLater(() -> {
            // Create CharacterUI first so it can be updated by SettingsWindow
            characterUI = new CharacterUI();
            characterUI.setVisible(true);

            // Now create SettingsWindow, which will trigger the initial image load via AppState
            new SettingsWindow(voices.toArray(new String[0]));
        });
    }
}
