package personality;

import java.util.List;

/**
 * Represents a personality configuration for the AI assistant.
 * UPDATED: Now includes a 'lastResponse' field to act as short-term memory and multimodal_prompt support.
 */
public class Personality {
    // These fields are loaded from JSON
    private String name;
    private String prompt;
    private String multimodal_prompt; // Changed to match JSON field name
    private List<String> attributes;

    // These fields are set programmatically
    private transient String staticImagePath;
    private transient String speakingImagePath;
    private transient String lastResponse = ""; // Short-term memory for the last spoken line

    // Default constructor for Gson
    public Personality() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getMultimodalPrompt() {
        return multimodal_prompt; // Return the correctly named field
    }

    public void setMultimodalPrompt(String multimodalPrompt) {
        this.multimodal_prompt = multimodalPrompt;
    }

    public List<String> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<String> attributes) {
        this.attributes = attributes;
    }

    public String getStaticImagePath() {
        return staticImagePath;
    }

    public void setStaticImagePath(String staticImagePath) {
        this.staticImagePath = staticImagePath;
    }

    public String getSpeakingImagePath() {
        return speakingImagePath;
    }

    public void setSpeakingImagePath(String speakingImagePath) {
        this.speakingImagePath = speakingImagePath;
    }

    public String getLastResponse() {
        return lastResponse;
    }

    public void setLastResponse(String lastResponse) {
        this.lastResponse = lastResponse;
    }

    @Override
    public String toString() {
        return name;
    }
}
