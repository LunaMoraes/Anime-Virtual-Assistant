package config;

import com.google.gson.Gson;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Represents the system configuration loaded from data/system/system.json
 */
public class SystemConfig {
    private ApiConfig analysis;
    private ApiConfig vision;
    private ApiConfig multimodal;
    private PromptsConfig prompts;

    // Default constructor for Gson
    public SystemConfig() {}

    public ApiConfig getAnalysis() {
        return analysis;
    }

    public ApiConfig getVision() {
        return vision;
    }

    public ApiConfig getMultimodal() {
        return multimodal;
    }

    public PromptsConfig getPrompts() {
        return prompts;
    }

    /**
     * Loads system configuration from the system.json file
     */
    public static SystemConfig loadSystemConfig() {
        File configFile = new File("data/system/system.json");
        if (!configFile.exists()) {
            System.err.println("System config file not found: " + configFile.getAbsolutePath());
            return null;
        }

        try (FileReader reader = new FileReader(configFile)) {
            Gson gson = new Gson();
            SystemConfig config = gson.fromJson(reader, SystemConfig.class);
            System.out.println("System configuration loaded successfully");
            return config;
        } catch (IOException e) {
            System.err.println("Error loading system config: " + e.getMessage());
            return null;
        }
    }

    /**
     * Represents an API configuration (analysis or vision)
     */
    public static class ApiConfig {
        private String key;
        private String model_name;
        private String url;

        // Default constructor for Gson
        public ApiConfig() {}

        public String getKey() {
            return key;
        }

        public String getModelName() {
            return model_name;
        }

        public String getUrl() {
            return url;
        }
    }

    /**
     * Represents prompts configuration
     */
    public static class PromptsConfig {
        private String visionPrompt;
        private String fallbackPrompt;
        private String multimodalPrompt;
    private String tasks; // instruction on how to split and format responses

        // Default constructor for Gson
        public PromptsConfig() {}

        public String getVisionPrompt() {
            return visionPrompt != null ? visionPrompt :
                "Describe the user's activity in this image. Focus on the content and what they are doing. Do NOT use the words 'screenshot', 'screen', or 'image'.";
        }

        public String getFallbackPrompt() {
            return fallbackPrompt != null ? fallbackPrompt :
                "Based on this screen description: \"%s\" Give a SHORT comment (maximum 15 words).";
        }

        public String getMultimodalPrompt() {
            return multimodalPrompt != null ? multimodalPrompt :
                "The attached screenshot shows a user activity, based on this and the later on personality quote give a response to the user.";
        }

        public String getTasksInstruction() {
            return tasks != null ? tasks :
                "You will receive a few tasks, for each task you will provide a different response. Use '[]' to wrap the response asked by the specific task. An ideal response will consist of multiple sequences of [] with the response inside for each.";
        }
    }
}
