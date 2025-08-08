import javax.swing.*;
import java.util.List;
import api.TtsApiClient;

/**
 * The main entry point for the AI Assistant application.
 * UPDATED: Now uses the new modular architecture with specialized managers.
 */
public class Main {
    public static AssistantCore assistantCore;
    public static CharacterUI characterUI;

    public static void main(String[] args) {
        System.out.println("Initializing AI Assistant...");

        // Initialize the application state and all managers
        AppState.initialize();

        System.out.println("Fetching available TTS characters...");
        List<String> voices = TtsApiClient.getAvailableCharacters();

        if (voices == null || voices.isEmpty()) {
            System.out.println("TTS API not available at startup - user will be prompted when starting assistant");
            AppState.isTtsApiAvailable = false;
            // Create empty voices array for SettingsWindow
            voices = new java.util.ArrayList<>();

            // Show error dialog to user immediately
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null,
                    "Could not connect to the TTS API Server.\nPlease ensure start_api_coqui.py is running.",
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
            });
        } else {
            AppState.isTtsApiAvailable = true;
            // Set default voice if none was loaded from settings
            if (AppState.selectedTtsCharacterVoice == null && !voices.isEmpty()) {
                AppState.selectedTtsCharacterVoice = voices.get(0);
                AppState.saveCurrentSettings();
            }
            System.out.println("TTS voices found. Selected voice: " + AppState.selectedTtsCharacterVoice);
        }

        assistantCore = new AssistantCore();

        List<String> finalVoices = voices;
        SwingUtilities.invokeLater(() -> {
            // Create CharacterUI first so it can be updated by SettingsWindow
            characterUI = new CharacterUI();
            characterUI.setVisible(true);

            // Set up TTS UI callback so TtsApiClient can control the UI properly
            TtsApiClient.setUICallback(new TtsApiClient.UICallback() {
                @Override
                public void showSpeakingImage() {
                    characterUI.showSpeakingImage();
                }

                @Override
                public void showSpeechBubble(String text) {
                    characterUI.showSpeechBubble(text);
                }

                @Override
                public void showStaticImage() {
                    characterUI.showStaticImage();
                }

                @Override
                public void hideSpeechBubble() {
                    characterUI.hideSpeechBubble();
                }
            });

            // Now create SettingsWindow
            new SettingsWindow(finalVoices.toArray(new String[0]));
        });
    }
}
