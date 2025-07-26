/**
 * Holds all the shared configuration and state for the application.
 * This version defines both the vision and language models.
 */
public class AppState {

    // --- Ollama Configuration ---
    public static final String OLLAMA_API_URL = "http://localhost:11434/api/generate";

    // Define both models for our two-step process
    public static final String VISION_MODEL = "qwen2.5vl:3b"; // For analyzing images
    public static final String LANGUAGE_MODEL = "deepseek-r1:8b"; // For generating the final response

    // --- TTS Configuration ---
    public static final String TTS_API_URL = "http://localhost:5001";

    // --- UI Configuration ---
    public static final String CHARACTER_IMAGE_URL = "src/pngegg.png";

    // --- Shared State (volatile ensures thread safety) ---
    public static volatile String selectedTtsCharacterVoice = null;
    public static volatile String selectedLanguage = "English";
    public static volatile boolean isRunning = false;
}
