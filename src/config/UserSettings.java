package config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;

/**
 * Manages user settings that are persisted across application sessions.
 * This class handles loading and saving user preferences to userSettings.json.
 */
public class UserSettings {
    private static final String USER_SETTINGS_FILE = "data/system/userSettings.json";

    // User configurable settings
    private String selectedTtsCharacterVoice = null;
    private String selectedLanguage = "English";
    private String selectedPersonalityName = "Tsundere";
    private boolean useApiVision = true;
    private boolean useApiAnalysis = true;
    private boolean useMultimodal = true;
    private boolean useApiMultimodal = true;
    private boolean useTTS = true;

    // Default constructor
    public UserSettings() {}

    /**
     * Loads user settings from the JSON file.
     * If the file doesn't exist, creates a new one with default values.
     */
    public static UserSettings loadUserSettings() {
        File settingsFile = new File(USER_SETTINGS_FILE);

        if (!settingsFile.exists()) {
            System.out.println("User settings file not found. Creating with default values...");
            UserSettings defaultSettings = new UserSettings();
            defaultSettings.saveUserSettings();
            return defaultSettings;
        }

        try (FileReader reader = new FileReader(settingsFile)) {
            Gson gson = new Gson();
            UserSettings settings = gson.fromJson(reader, UserSettings.class);
            System.out.println("User settings loaded successfully.");
            return settings != null ? settings : new UserSettings();
        } catch (IOException e) {
            System.err.println("Error loading user settings: " + e.getMessage());
            return new UserSettings();
        }
    }

    /**
     * Saves the current user settings to the JSON file.
     */
    public void saveUserSettings() {
        File settingsFile = new File(USER_SETTINGS_FILE);

        // Ensure the directory exists
        File parentDir = settingsFile.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (FileWriter writer = new FileWriter(settingsFile)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(this, writer);
            System.out.println("User settings saved successfully.");
        } catch (IOException e) {
            System.err.println("Error saving user settings: " + e.getMessage());
        }
    }

    // Getters and setters
    public String getSelectedTtsCharacterVoice() {
        return selectedTtsCharacterVoice;
    }

    public void setSelectedTtsCharacterVoice(String selectedTtsCharacterVoice) {
        this.selectedTtsCharacterVoice = selectedTtsCharacterVoice;
    }

    public String getSelectedLanguage() {
        return selectedLanguage;
    }

    public void setSelectedLanguage(String selectedLanguage) {
        this.selectedLanguage = selectedLanguage;
    }

    public String getSelectedPersonalityName() {
        return selectedPersonalityName;
    }

    public void setSelectedPersonalityName(String selectedPersonalityName) {
        this.selectedPersonalityName = selectedPersonalityName;
    }

    public boolean isUseApiVision() {
        return useApiVision;
    }

    public void setUseApiVision(boolean useApiVision) {
        this.useApiVision = useApiVision;
    }

    public boolean isUseApiAnalysis() {
        return useApiAnalysis;
    }

    public void setUseApiAnalysis(boolean useApiAnalysis) {
        this.useApiAnalysis = useApiAnalysis;
    }

    public boolean isUseMultimodal() {
        return useMultimodal;
    }

    public void setUseMultimodal(boolean useMultimodal) {
        this.useMultimodal = useMultimodal;
    }

    public boolean isUseApiMultimodal() {
        return useApiMultimodal;
    }

    public void setUseApiMultimodal(boolean useApiMultimodal) {
        this.useApiMultimodal = useApiMultimodal;
    }

    public boolean isUseTTS() {
        return useTTS;
    }

    public void setUseTTS(boolean useTTS) {
        this.useTTS = useTTS;
    }
}
