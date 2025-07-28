/**
 * Holds all the shared configuration and state for the application.
 * This version uses a separate Python service for vision and Ollama for language.
 */
public class AppState {

    // --- Service URLs ---
    public static final String OLLAMA_API_URL = "http://localhost:11434/api/generate";
    public static final String VISION_API_URL = "http://localhost:5002/describe"; // URL for the new Python vision server
    public static final String TTS_API_URL = "http://localhost:5001";

    // --- Model Configuration ---
    // The vision model is now handled by the Python service, so we only define the language model here.
    public static final String LANGUAGE_MODEL = "qwen2:7b";

    // --- UI Configuration ---
    public static final String CHARACTER_IMAGE_URL = "src/pngegg.png";

    // --- Shared State (volatile ensures thread safety) ---
    public static volatile String selectedTtsCharacterVoice = null;
    public static volatile String selectedLanguage = "English";
    public static volatile boolean isRunning = false;
}