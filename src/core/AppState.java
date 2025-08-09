package core;

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
    public static volatile boolean isTtsApiAvailable = false;
    // Indicates when an action (e.g., screen analysis) is performing background work
    public static volatile boolean isActionProcessing = false;
    // Indicates when TTS playback is ongoing
    public static volatile boolean isSpeaking = false;
    // Global tick counter for thinking cycles
    public static volatile long tickCounter = 0L;

    // --- UI Configuration ---
    public static final String FALLBACK_IMAGE_URL = "";

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

        // Initialize persistent memory store (creates/loads data/system/userMemory.json)
        try {
            config.MemoryStore.initialize();
        } catch (Throwable t) {
            System.err.println("Memory store failed to initialize: " + t.getMessage());
        }

        // Load personalities
        PersonalityManager.loadPersonalities();

        // Initialize levels system
        try {
            levels.LevelManager.initialize();
        } catch (Throwable t) {
            System.err.println("Levels system failed to initialize: " + t.getMessage());
        }

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

    // === Chat frequency accessors ===
    public static String getChatFrequency() { return ConfigurationManager.getChatFrequency(); }
    public static void setChatFrequency(String freq) { ConfigurationManager.setChatFrequency(freq); }
    public static int getChatFrequencyDivisor() { return ConfigurationManager.getChatFrequencyDivisor(); }

    // === Personality Delegation Methods ===

    public static List<Personality> getAvailablePersonalities() {
        return PersonalityManager.getAvailablePersonalities();
    }

    public static void setSelectedPersonality(Personality personality) {
        PersonalityManager.setSelectedPersonality(personality);
        // Update UI after personality change (avoid hard dependency if Main not initialized)
        try {
            if (Main.characterUI != null) {
                Main.characterUI.updatePersonalityImages();
            }
        } catch (Throwable ignored) {
            // Main might not be initialized in some contexts; ignore
        }
    }

    public static Personality getSelectedPersonality() {
        return PersonalityManager.getSelectedPersonality();
    }

    // === Configuration Delegation Methods ===

    public static boolean isVisionApiConfigAvailable() {
        return ConfigurationManager.isVisionApiAvailable();
    }

    public static boolean isAnalysisApiConfigAvailable() {
        return ConfigurationManager.isAnalysisApiAvailable();
    }

    public static boolean isMultimodalApiConfigAvailable() {
        return ConfigurationManager.isMultimodalApiConfigAvailable();
    }

    public static void setUseApiVision(boolean useApi) {
        ConfigurationManager.setUseApiVision(useApi);
        System.out.println("Vision model changed to: " + (useApi ? "API" : "Local"));
    }

    public static void setUseApiAnalysis(boolean useApi) {
        ConfigurationManager.setUseApiAnalysis(useApi);
        System.out.println("Analysis model changed to: " + (useApi ? "API" : "Local"));
    }

    public static void setUseMultimodal(boolean useMultimodal) {
        ConfigurationManager.setUseMultimodal(useMultimodal);
        System.out.println("Multimodal mode changed to: " + (useMultimodal ? "Enabled" : "Disabled"));
    }

    public static void setUseApiMultimodal(boolean useApi) {
        ConfigurationManager.setUseApiMultimodal(useApi);
        System.out.println("Multimodal model changed to: " + (useApi ? "API" : "Local"));
    }

    public static boolean useApiVision() {
        return ConfigurationManager.useApiVision();
    }

    public static boolean useApiAnalysis() {
        return ConfigurationManager.useApiAnalysis();
    }

    public static boolean useMultimodal() {
        return ConfigurationManager.useMultimodal();
    }

    public static boolean useApiMultimodal() {
        return ConfigurationManager.useApiMultimodal();
    }

    public static boolean useTTS() {
        return ConfigurationManager.useTTS();
    }

    public static void setUseTTS(boolean useTTS) {
        ConfigurationManager.setUseTTS(useTTS);
        System.out.println("TTS " + (useTTS ? "Enabled" : "Disabled"));
    }
}
