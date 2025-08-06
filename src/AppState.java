import java.util.List;
import config.ConfigurationManager;
import personality.PersonalityManager;
import personality.Personality;

/**
 * Simplified AppState - now only holds the current application state.
 * Configuration management and personality loading are delegated to specialized managers.
 */
public class AppState {

    // --- Essential Application State ---
    public static volatile boolean isRunning = false;

    // --- UI Configuration ---
    public static final String FALLBACK_IMAGE_URL = "src/pngegg.png";

    // --- Current Settings (delegated to ConfigurationManager) ---
    public static volatile String selectedTtsCharacterVoice = null;
    public static volatile String selectedLanguage = "English";

    /**
     * Initializes the application state by loading all configurations and personalities.
     * This should be called once at application startup.
     */
    public static void initialize() {
        // Initialize configuration management
        ConfigurationManager.initialize();

        // Load personalities
        PersonalityManager.loadPersonalities();

        // Apply user settings to AppState variables
        applyUserSettings();

        // Apply personality selection from user settings
        PersonalityManager.applyPersonalityFromSettings();

        System.out.println("AppState initialized successfully.");
    }

    /**
     * Applies user settings to the AppState variables.
     */
    private static void applyUserSettings() {
        selectedTtsCharacterVoice = ConfigurationManager.getSelectedTtsVoice();
        selectedLanguage = ConfigurationManager.getSelectedLanguage();
    }

    /**
     * Saves all current settings to user settings file.
     */
    public static void saveCurrentSettings() {
        ConfigurationManager.setSelectedTtsVoice(selectedTtsCharacterVoice);
        ConfigurationManager.setSelectedLanguage(selectedLanguage);
    }

    // === Personality Delegation Methods ===

    public static List<Personality> getAvailablePersonalities() {
        return PersonalityManager.getAvailablePersonalities();
    }

    public static void setSelectedPersonality(Personality personality) {
        PersonalityManager.setSelectedPersonality(personality);
        // Update UI after personality change
        if (Main.characterUI != null) {
            Main.characterUI.updatePersonalityImages();
        }
    }

    public static Personality getSelectedPersonality() {
        return PersonalityManager.getSelectedPersonality();
    }

    public static String getCurrentPersonalityPrompt() {
        return PersonalityManager.getCurrentPersonalityPrompt();
    }

    // === Configuration Delegation Methods ===

    public static boolean isVisionApiConfigAvailable() {
        return ConfigurationManager.isVisionApiAvailable();
    }

    public static boolean isAnalysisApiConfigAvailable() {
        return ConfigurationManager.isAnalysisApiAvailable();
    }

    public static void setUseApiVision(boolean useApi) {
        ConfigurationManager.setUseApiVision(useApi);
        System.out.println("Vision model changed to: " + (useApi ? "API" : "Local"));
    }

    public static void setUseApiAnalysis(boolean useApi) {
        ConfigurationManager.setUseApiAnalysis(useApi);
        System.out.println("Analysis model changed to: " + (useApi ? "API" : "Local"));
    }

    public static boolean useApiVision() {
        return ConfigurationManager.useApiVision();
    }

    public static boolean useApiAnalysis() {
        return ConfigurationManager.useApiAnalysis();
    }
}
