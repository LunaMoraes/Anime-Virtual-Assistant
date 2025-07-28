import com.google.gson.Gson;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Represents the system configuration loaded from data/system/system.json
 */
public class SystemConfig {
    private ModelConfig analysis;
    private ModelConfig vision;

    // Default constructor for Gson
    public SystemConfig() {}

    public ModelConfig getAnalysis() {
        return analysis;
    }

    public ModelConfig getVision() {
        return vision;
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
     * Represents a model configuration (analysis or vision)
     */
    public static class ModelConfig {
        private String key;
        private String model_name;
        private String url;

        public ModelConfig() {}

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
}
