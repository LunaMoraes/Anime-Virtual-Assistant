import com.google.gson.Gson;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds all the shared configuration and state for the application.
 * UPDATED: Loads personalities from a subdirectory and constructs image paths.
 */
public class AppState {

    // --- Service URLs ---
    public static final String OLLAMA_API_URL = "http://localhost:11434/api/generate";
    public static final String VISION_API_URL = "http://localhost:5002/describe";
    public static final String TTS_API_URL = "http://localhost:5005";

    // --- Model Configuration ---
    // The vision model is now handled by the Python service, so we only define the language model here.
    public static final String LANGUAGE_MODEL = "qwen3:4b";

    // --- New: Model Type Configuration ---
    public static volatile boolean useApiVision = false; // false = local Python, true = API
    public static volatile boolean useApiAnalysis = false; // false = local Ollama, true = API
    private static SystemConfig systemConfig = null;

    // --- UI Configuration ---
    // This is now a fallback image if a personality's image is not found.
    public static final String FALLBACK_IMAGE_URL = "src/pngegg.png";

    // --- Personality Configuration ---
    // UPDATED: Path now points to the 'personalities' subfolder.
    public static final String PERSONALITIES_FOLDER = "data/personalities";
    private static final List<Personality> availablePersonalities = new ArrayList<>();
    public static volatile Personality selectedPersonality = null;

    // --- Shared State ---
    public static volatile String selectedTtsCharacterVoice = null;
    public static volatile String selectedLanguage = "English";
    public static volatile boolean isRunning = false;

    /**
     * Loads all personality JSON files and constructs their image paths.
     */
    public static void loadPersonalities() {
        // Load system configuration first
        systemConfig = SystemConfig.loadSystemConfig();

        availablePersonalities.clear();
        Gson gson = new Gson();

        File personalitiesDir = new File(PERSONALITIES_FOLDER);
        if (!personalitiesDir.exists() || !personalitiesDir.isDirectory()) {
            System.err.println("Personalities folder not found: " + personalitiesDir.getAbsolutePath());
            return;
        }

        File[] jsonFiles = personalitiesDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        if (jsonFiles == null || jsonFiles.length == 0) {
            System.err.println("No personality JSON files found in: " + personalitiesDir.getAbsolutePath());
            return;
        }

        for (File file : jsonFiles) {
            try (FileReader reader = new FileReader(file)) {
                Personality personality = gson.fromJson(reader, Personality.class);
                if (personality != null && personality.getName() != null) {
                    // Programmatically set the image paths based on the personality name
                    String imageFolderName = personality.getName();
                    String imageFolderPath = PERSONALITIES_FOLDER + File.separator + imageFolderName;
                    personality.setStaticImagePath(imageFolderPath + File.separator + "static.png");
                    personality.setSpeakingImagePath(imageFolderPath + File.separator + "speaking.png");

                    availablePersonalities.add(personality);
                    System.out.println("Loaded personality: " + personality.getName());
                }
            } catch (IOException e) {
                System.err.println("Error loading personality from " + file.getName() + ": " + e.getMessage());
            }
        }

        if (!availablePersonalities.isEmpty()) {
            selectedPersonality = availablePersonalities.stream()
                    .filter(p -> "Tsundere".equalsIgnoreCase(p.getName()))
                    .findFirst()
                    .orElse(availablePersonalities.getFirst());
            System.out.println("Default personality set to: " + selectedPersonality.getName());
        }
    }

    public static List<Personality> getAvailablePersonalities() {
        return new ArrayList<>(availablePersonalities);
    }

    public static void setSelectedPersonality(Personality personality) {
        if (personality != null) {
            selectedPersonality = personality;
            System.out.println("Personality changed to: " + personality.getName());
            // Notify the UI to update the character image
            if (Main.characterUI != null) {
                Main.characterUI.updatePersonalityImages();
            }
        }
    }

    public static String getCurrentPersonalityPrompt() {
        return selectedPersonality != null ? selectedPersonality.getPrompt() : null;
    }

    // --- New: Model Type Management ---

    /**
     * Gets the current model name based on the selected mode (local or API)
     */
    public static String getCurrentModelName() {
        if (useApiAnalysis && systemConfig != null && systemConfig.getAnalysis() != null) {
            return systemConfig.getAnalysis().getModelName();
        }
        return LANGUAGE_MODEL;
    }

    /**
     * Gets the API URL for analysis model requests
     */
    public static String getApiUrl() {
        return systemConfig != null && systemConfig.getAnalysis() != null ?
               systemConfig.getAnalysis().getUrl() : null;
    }

    /**
     * Gets the API key for analysis model requests
     */
    public static String getApiKey() {
        return systemConfig != null && systemConfig.getAnalysis() != null ?
               systemConfig.getAnalysis().getKey() : null;
    }

    /**
     * Gets the vision API URL for vision model requests
     */
    public static String getVisionApiUrl() {
        return systemConfig != null && systemConfig.getVision() != null ?
               systemConfig.getVision().getUrl() : null;
    }

    /**
     * Gets the vision API key for vision model requests
     */
    public static String getVisionApiKey() {
        return systemConfig != null && systemConfig.getVision() != null ?
               systemConfig.getVision().getKey() : null;
    }

    /**
     * Gets the vision model name
     */
    public static String getVisionModelName() {
        return systemConfig != null && systemConfig.getVision() != null ?
               systemConfig.getVision().getModelName() : null;
    }

    /**
     * Sets whether to use API for vision processing
     */
    public static void setUseApiVision(boolean useApi) {
        useApiVision = useApi;
        System.out.println("Vision model changed to: " + (useApi ? "API" : "Local"));
    }

    /**
     * Sets whether to use API for analysis processing
     */
    public static void setUseApiAnalysis(boolean useApi) {
        useApiAnalysis = useApi;
        System.out.println("Analysis model changed to: " + (useApi ? "API" : "Local"));
    }

    /**
     * Checks if vision API configuration is available
     */
    public static boolean isVisionApiConfigAvailable() {
        return systemConfig != null &&
               systemConfig.getVision() != null &&
               systemConfig.getVision().getKey() != null &&
               systemConfig.getVision().getUrl() != null;
    }

    /**
     * Checks if analysis API configuration is available
     */
    public static boolean isAnalysisApiConfigAvailable() {
        return systemConfig != null &&
               systemConfig.getAnalysis() != null &&
               systemConfig.getAnalysis().getKey() != null &&
               systemConfig.getAnalysis().getUrl() != null;
    }

    /**
     * Checks if system configuration is available for API usage
     */
    public static boolean isApiConfigAvailable() {
        return isVisionApiConfigAvailable() && isAnalysisApiConfigAvailable();
    }
}
