import java.util.List;

/**
 * Represents a personality configuration for the AI assistant.
 * Each personality has a name, a specific prompt, and attributes for future features.
 * UPDATED: Now includes paths for static and speaking images.
 */
public class Personality {
    // These fields are loaded from JSON
    private String name;
    private String prompt;
    private List<String> attributes;

    // These fields are set programmatically after loading
    private transient String staticImagePath;
    private transient String speakingImagePath;


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

    public List<String> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<String> attributes) {
        this.attributes = attributes;
    }

    // --- Image Path Getters and Setters ---

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


    @Override
    public String toString() {
        return name; // This will be used in UI components like radio buttons
    }
}
