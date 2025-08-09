package config;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.io.FileReader;

/**
 * Loads prompts from data/system/prompts.json to keep system.json modular.
 */
public class PromptsConfig {
    private static final String PROMPTS_FILE = "data/system/prompts.json";

    @SerializedName("vision_prompt")
    private String visionPrompt;

    @SerializedName("fallback_personality_prompt")
    private String fallbackPrompt;

    @SerializedName("multimodal_prompt")
    private String multimodalPrompt;

    @SerializedName("tasks")
    private String tasksInstruction;

    @SerializedName("speak_task_prompt")
    private String speakTaskPrompt;

    public String getVisionPrompt() { return visionPrompt; }
    public String getFallbackPrompt() { return fallbackPrompt; }
    public String getMultimodalPrompt() { return multimodalPrompt; }
    public String getTasksInstruction() { return tasksInstruction; }
    public String getSpeakTaskPrompt() { return speakTaskPrompt; }

    public static PromptsConfig load() {
        try (FileReader reader = new FileReader(new File(PROMPTS_FILE))) {
            return new Gson().fromJson(reader, PromptsConfig.class);
        } catch (Exception e) {
            System.err.println("Could not load prompts.json: " + e.getMessage());
            return new PromptsConfig();
        }
    }
}
