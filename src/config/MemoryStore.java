package config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * MemoryStore holds short-term (in-memory) and long-term (disk-backed) memory.
 * Long-term memory is persisted to data/system/userMemory.json across sessions.
 */
public class MemoryStore {
    private static final Gson GSON = new Gson();
    private static final Path MEMORY_FILE = Paths.get("data", "system", "userMemory.json");

    private static String shortTerm = ""; // volatile session-only
    private static String longTerm = "";  // persisted across sessions

    /**
     * Initialize persistence for long-term memory: read from disk or create empty file.
     */
    public static synchronized void initialize() {
        try {
            // Ensure folder exists
            Files.createDirectories(MEMORY_FILE.getParent());

            if (Files.exists(MEMORY_FILE)) {
                String json = Files.readString(MEMORY_FILE, StandardCharsets.UTF_8);
                if (json != null && !json.isBlank()) {
                    try {
                        JsonObject obj = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
                        if (obj.has("long_term_memory")) {
                            longTerm = safeString(obj.get("long_term_memory").getAsString());
                        }
                    } catch (Exception parseErr) {
                        System.err.println("userMemory.json parse error, resetting file: " + parseErr.getMessage());
                        writeFile("");
                    }
                }
            } else {
                writeFile("");
            }
        } catch (IOException ioe) {
            System.err.println("Failed to initialize userMemory.json: " + ioe.getMessage());
        }
    }

    public static synchronized String getShortTerm() { return shortTerm; }
    public static synchronized String getLongTerm() { return longTerm; }

    public static synchronized void setShortTerm(String s) { shortTerm = safeString(s); }

    public static synchronized void setLongTerm(String s) {
        longTerm = safeString(s);
        // Persist immediately on update
        try {
            writeFile(longTerm);
        } catch (Exception e) {
            System.err.println("Failed to save userMemory.json: " + e.getMessage());
        }
    }

    private static String safeString(String s) { return s != null ? s : ""; }

    private static void writeFile(String longTermContent) throws IOException {
        JsonObject obj = new JsonObject();
        obj.addProperty("long_term_memory", safeString(longTermContent));
        String jsonOut = GSON.toJson(obj);
        Files.writeString(MEMORY_FILE, jsonOut, StandardCharsets.UTF_8);
    }
}
