package personality;

import com.google.gson.Gson;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages all personality-related operations including loading, selection, and access.
 */
public class PersonalityManager {
    private static final String PERSONALITIES_FOLDER = "data/personalities";
    private static final List<Personality> availablePersonalities = new ArrayList<>();
    private static Personality selectedPersonality = null;
    private static final Gson gson = new Gson();

    /**
     * Loads all personality JSON files and constructs their image paths.
     */
    public static void loadPersonalities() {
        availablePersonalities.clear();

        File personalitiesDir = new File(PERSONALITIES_FOLDER);
        if (!personalitiesDir.exists() || !personalitiesDir.isDirectory()) {
            System.err.println("Personalities folder not found: " + personalitiesDir.getAbsolutePath());
            return;
        }

        File[] jsonFiles = personalitiesDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        if (jsonFiles == null || jsonFiles.length == 0) {
            System.err.println("No personality JSON files found in: " + personalitiesDir.getAbsolutePath());
            return;
        }

        for (File file : jsonFiles) {
            try (FileReader reader = new FileReader(file)) {
                Personality personality = gson.fromJson(reader, Personality.class);
                if (personality != null && personality.getName() != null) {
                    // Programmatically set the image paths based on the personality name
                    String imageFolderName = personality.getName();
                    String imageFolderPath = PERSONALITIES_FOLDER + File.separator + imageFolderName;
                    personality.setStaticImagePath(imageFolderPath + File.separator + "static.png");
                    personality.setSpeakingImagePath(imageFolderPath + File.separator + "speaking.png");

                    availablePersonalities.add(personality);
                    System.out.println("Loaded personality: " + personality.getName());
                }
            } catch (IOException e) {
                System.err.println("Error loading personality from " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Sets the selected personality and applies it to user settings.
     */
    public static void setSelectedPersonality(Personality personality) {
        if (personality != null) {
            selectedPersonality = personality;
            System.out.println("Personality changed to: " + personality.getName());

            // Save to user settings
            config.ConfigurationManager.setSelectedPersonality(personality.getName());

            // Note: UI updates are handled by the calling code to avoid circular dependencies
            // The caller should call updatePersonalityImages() on the UI after this method
        }
    }

    /**
     * Applies the selected personality from user settings after personalities are loaded.
     */
    public static void applyPersonalityFromSettings() {
        String personalityName = config.ConfigurationManager.getSelectedPersonalityName();

        if (personalityName != null) {
            selectedPersonality = availablePersonalities.stream()
                    .filter(p -> personalityName.equalsIgnoreCase(p.getName()))
                    .findFirst()
                    .orElse(null);

            if (selectedPersonality != null) {
                System.out.println("Personality set from user settings: " + selectedPersonality.getName());
                return;
            }
        }

        // Fallback if no personality was set from settings
        if (!availablePersonalities.isEmpty()) {
            selectedPersonality = availablePersonalities.stream()
                    .filter(p -> "Tsundere".equalsIgnoreCase(p.getName()))
                    .findFirst()
                    .orElse(availablePersonalities.getFirst());
            System.out.println("Default personality set to: " + selectedPersonality.getName());
        }
    }

    // === Getters ===

    public static List<Personality> getAvailablePersonalities() {
        return new ArrayList<>(availablePersonalities);
    }

    public static Personality getSelectedPersonality() {
        return selectedPersonality;
    }

    public static String getCurrentPersonalityPrompt() {
        return selectedPersonality != null ? selectedPersonality.getPrompt() : null;
    }

    /**
     * Gets the current personality's multimodal prompt for single-request processing.
     */
    public static String getCurrentMultimodalPrompt() {
        return selectedPersonality != null ? selectedPersonality.getMultimodalPrompt() : null;
    }

    /**
     * Gets the last response from the selected personality for memory purposes.
     */
    public static String getLastResponse() {
        return selectedPersonality != null ? selectedPersonality.getLastResponse() : null;
    }

    /**
     * Gets the last up to 5 responses, newest last.
     */
    public static java.util.List<String> getLastResponses() {
        return selectedPersonality != null ? selectedPersonality.getLastResponses() : java.util.List.of();
    }

    /**
     * Saves a response to the selected personality's memory.
     */
    public static void saveResponseToMemory(String response) {
        if (selectedPersonality != null) {
            selectedPersonality.addResponse(response);
            System.out.println("Saved to memory: \"" + response + "\"");
        }
    }
}
