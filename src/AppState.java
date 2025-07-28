import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds all the shared configuration and state for the application.
 * This version uses a separate Python service for vision and Ollama for language.
 */
public class AppState {

    // --- Service URLs ---
    public static final String OLLAMA_API_URL = "http://localhost:11434/api/generate";
    public static final String VISION_API_URL = "http://localhost:5002/describe"; // URL for the new Python vision server
    public static final String TTS_API_URL = "http://localhost:5001";

    // --- Model Configuration ---
    // The vision model is now handled by the Python service, so we only define the language model here.
    public static final String LANGUAGE_MODEL = "qwen2:7b";

    // --- UI Configuration ---
    public static final String CHARACTER_IMAGE_URL = "src/pngegg.png";

    // --- Personality Configuration ---
    public static final String PERSONALITIES_FOLDER = "data";
    private static List<Personality> availablePersonalities = new ArrayList<>();
    public static volatile Personality selectedPersonality = null;

    // --- Shared State (volatile ensures thread safety) ---
    public static volatile String selectedTtsCharacterVoice = null;
    public static volatile String selectedLanguage = "English";
    public static volatile boolean isRunning = false;

    /**
     * Loads all personality JSON files from the data folder
     */
    public static void loadPersonalities() {
        availablePersonalities.clear();
        Gson gson = new Gson();

        File personalitiesDir = new File(PERSONALITIES_FOLDER);
        if (!personalitiesDir.exists() || !personalitiesDir.isDirectory()) {
            System.err.println("Personalities folder not found: " + PERSONALITIES_FOLDER);
            return;
        }

        File[] jsonFiles = personalitiesDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        if (jsonFiles == null || jsonFiles.length == 0) {
            System.err.println("No personality JSON files found in: " + PERSONALITIES_FOLDER);
            return;
        }

        for (File file : jsonFiles) {
            try (FileReader reader = new FileReader(file)) {
                Personality personality = gson.fromJson(reader, Personality.class);
                if (personality != null && personality.getName() != null && personality.getPrompt() != null) {
                    availablePersonalities.add(personality);
                    System.out.println("Loaded personality: " + personality.getName());
                } else {
                    System.err.println("Invalid personality file: " + file.getName());
                }
            } catch (IOException e) {
                System.err.println("Error loading personality from " + file.getName() + ": " + e.getMessage());
            }
        }

        // Set default personality (first one found, preferably tsundere)
        if (!availablePersonalities.isEmpty()) {
            selectedPersonality = availablePersonalities.stream()
                .filter(p -> "Tsundere".equalsIgnoreCase(p.getName()))
                .findFirst()
                .orElse(availablePersonalities.get(0));
            System.out.println("Default personality set to: " + selectedPersonality.getName());
        }
    }

    /**
     * Gets all available personalities
     */
    public static List<Personality> getAvailablePersonalities() {
        return new ArrayList<>(availablePersonalities);
    }

    /**
     * Sets the selected personality
     */
    public static void setSelectedPersonality(Personality personality) {
        selectedPersonality = personality;
        System.out.println("Personality changed to: " + personality.getName());
    }

    /**
     * Gets the current personality's prompt
     */
    public static String getCurrentPersonalityPrompt() {
        return selectedPersonality != null ? selectedPersonality.getPrompt() : null;
    }
}