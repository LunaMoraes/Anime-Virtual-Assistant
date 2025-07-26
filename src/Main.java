import javax.swing.*;
import java.io.IOException;
import java.util.List;

/**
 * The main entry point for the AI Assistant application.
 * This class initializes and coordinates all the other components.
 */
public class Main {
    // A static reference to the core logic so the UI can access it.
    public static AssistantCore assistantCore;

    public static void main(String[] args) {
        // 1. Fetch available voices from the TTS API before starting the UI.
        System.out.println("Fetching available TTS characters...");
        List<String> voices = TtsApiClient.getAvailableCharacters();

        // If the TTS API isn't running, show an error and exit.
        if (voices == null || voices.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Could not connect to the TTS API Server.\nPlease ensure api.py is running.", "Connection Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 2. Set the initial voice in the shared application state.
        AppState.selectedTtsCharacterVoice = voices.get(0);
        System.out.println("TTS voices found. Default voice: " + AppState.selectedTtsCharacterVoice);

        // 3. Instantiate the core logic but do not start it automatically.
        // The Start/Stop button in the SettingsWindow will control it.
        assistantCore = new AssistantCore();

        // 4. Create and show the UI components on the Swing Event Dispatch Thread.
        SwingUtilities.invokeLater(() -> {
            new SettingsWindow(voices.toArray(new String[0]));
            try {
                new CharacterUI().setVisible(true);
            } catch (IOException e) {
                System.err.println("Failed to load character image. Please check the URL.");
                e.printStackTrace();
            }
        });
    }
}
