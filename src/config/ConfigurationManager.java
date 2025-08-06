package config;

import com.google.gson.Gson;
import java.io.FileReader;
import java.io.IOException;

/**
 * Manages all configuration loading and access for the application.
 * Centralizes system.json and userSettings.json management.
 */
public class ConfigurationManager {
    private static SystemConfig systemConfig = null;
    private static UserSettings userSettings = null;
    private static final Gson gson = new Gson();

    /**
     * Initializes all configuration files.
     * Should be called once at application startup.
     */
    public static void initialize() {
        loadSystemConfig();
        loadUserSettings();
    }

    /**
     * Loads system configuration from system.json
     */
    private static void loadSystemConfig() {
        systemConfig = SystemConfig.loadSystemConfig();
    }

    /**
     * Loads user settings from userSettings.json
     */
    private static void loadUserSettings() {
        userSettings = UserSettings.loadUserSettings();
    }

    /**
     * Saves current user settings to file
     */
    public static void saveUserSettings() {
        if (userSettings != null) {
            userSettings.saveUserSettings();
        }
    }

    // === System Config Access ===

    /**
     * Gets the vision API configuration
     */
    public static SystemConfig.ApiConfig getVisionApiConfig() {
        return systemConfig != null ? systemConfig.getVision() : null;
    }

    /**
     * Gets the analysis API configuration
     */
    public static SystemConfig.ApiConfig getAnalysisApiConfig() {
        return systemConfig != null ? systemConfig.getAnalysis() : null;
    }

    /**
     * Checks if vision API configuration is available
     */
    public static boolean isVisionApiAvailable() {
        SystemConfig.ApiConfig vision = getVisionApiConfig();
        return vision != null && vision.getKey() != null && vision.getUrl() != null;
    }

    /**
     * Checks if analysis API configuration is available
     */
    public static boolean isAnalysisApiAvailable() {
        SystemConfig.ApiConfig analysis = getAnalysisApiConfig();
        return analysis != null && analysis.getKey() != null && analysis.getUrl() != null;
    }

    // === User Settings Access ===

    public static UserSettings getUserSettings() {
        return userSettings;
    }

    public static String getSelectedTtsVoice() {
        return userSettings != null ? userSettings.getSelectedTtsCharacterVoice() : null;
    }

    public static String getSelectedLanguage() {
        return userSettings != null ? userSettings.getSelectedLanguage() : "English";
    }

    public static String getSelectedPersonalityName() {
        return userSettings != null ? userSettings.getSelectedPersonalityName() : "Tsundere";
    }

    public static boolean useApiVision() {
        return userSettings != null && userSettings.isUseApiVision();
    }

    public static boolean useApiAnalysis() {
        return userSettings != null && userSettings.isUseApiAnalysis();
    }

    // === User Settings Updates ===

    public static void setSelectedTtsVoice(String voice) {
        if (userSettings != null) {
            userSettings.setSelectedTtsCharacterVoice(voice);
            saveUserSettings();
        }
    }

    public static void setSelectedLanguage(String language) {
        if (userSettings != null) {
            userSettings.setSelectedLanguage(language);
            saveUserSettings();
        }
    }

    public static void setSelectedPersonality(String personalityName) {
        if (userSettings != null) {
            userSettings.setSelectedPersonalityName(personalityName);
            saveUserSettings();
        }
    }

    public static void setUseApiVision(boolean useApi) {
        if (userSettings != null) {
            userSettings.setUseApiVision(useApi);
            saveUserSettings();
        }
    }

    public static void setUseApiAnalysis(boolean useApi) {
        if (userSettings != null) {
            userSettings.setUseApiAnalysis(useApi);
            saveUserSettings();
        }
    }

    /**
     * Gets the vision prompt from system configuration
     */
    public static String getVisionPrompt() {
        return systemConfig != null && systemConfig.getPrompts() != null ?
               systemConfig.getPrompts().getVisionPrompt() :
               "Describe the user's activity in this image. Focus on the content and what they are doing. Do NOT use the words 'screenshot', 'screen', or 'image'.";
    }

    /**
     * Gets the fallback prompt from system configuration
     */
    public static String getFallbackPrompt() {
        return systemConfig != null && systemConfig.getPrompts() != null ?
               systemConfig.getPrompts().getFallbackPrompt() :
               "Based on this screen description: \"%s\" Give a SHORT comment (maximum 15 words).";
    }
}
