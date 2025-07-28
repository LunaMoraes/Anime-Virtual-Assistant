import java.util.List;

/**
 * Represents a personality configuration for the AI assistant.
 * Each personality has a name, a specific prompt, and attributes for future features.
 */
public class Personality {
    private String name;
    private String prompt;
    private List<String> attributes;

    // Default constructor for Gson
    public Personality() {}

    public Personality(String name, String prompt, List<String> attributes) {
        this.name = name;
        this.prompt = prompt;
        this.attributes = attributes;
    }

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

    @Override
    public String toString() {
        return name; // This will be used in UI components like radio buttons
    }
}
