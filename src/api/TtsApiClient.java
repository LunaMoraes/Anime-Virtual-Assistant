package api;

import com.google.gson.Gson;
import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * Handles all communication with the Python VITS TTS API server.
 * UPDATED: Now works with VITS models and supports multiple languages/characters.
 */
public class TtsApiClient {

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final Gson gson = new Gson();
    private static final String TTS_API_URL = "http://localhost:5005";

    // Interface for UI callbacks to avoid circular dependencies
    public interface UICallback {
        void showSpeakingImage();
        void showSpeechBubble(String text);
        void showStaticImage();
        void hideSpeechBubble();
    }

    private static UICallback uiCallback = null;

    /**
     * Sets the UI callback for TTS events
     */
    public static void setUICallback(UICallback callback) {
        uiCallback = callback;
    }

    public static List<String> getAvailableCharacters() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TTS_API_URL + "/characters"))
                    .GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return gson.fromJson(response.body(), new com.google.gson.reflect.TypeToken<List<String>>() {}.getType());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void speak(String text, String characterName, double speed, String language) {
        try {
            // Use the characterName directly from the API (no more language mapping)
            // The characterName now comes from the Coqui TTS API speakers list

            Map<String, Object> payloadMap = Map.of(
                    "text", text,
                    "character", characterName,  // Use characterName directly (jenny_female, bella_female, etc.)
                    "speed", speed
            );
            String jsonPayload = gson.toJson(payloadMap);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TTS_API_URL + "/synthesize"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() == 200) {
                // Show speaking image and bubble as soon as we get a valid response (when TTS starts)
                if (uiCallback != null) {
                    uiCallback.showSpeakingImage();
                    uiCallback.showSpeechBubble(text);
                }

                byte[] audioBytes = response.body().readAllBytes();
                InputStream byteArrayInputStream = new ByteArrayInputStream(audioBytes);

                try (AudioInputStream sourceStream = AudioSystem.getAudioInputStream(byteArrayInputStream)) {
                    AudioFormat sourceFormat = sourceStream.getFormat();
                    AudioFormat targetFormat = new AudioFormat(
                            AudioFormat.Encoding.PCM_SIGNED,
                            sourceFormat.getSampleRate(),
                            16,
                            sourceFormat.getChannels(),
                            sourceFormat.getChannels() * 2,
                            sourceFormat.getSampleRate(),
                            false
                    );

                    try (AudioInputStream convertedStream = AudioSystem.getAudioInputStream(targetFormat, sourceStream)) {
                        Clip clip = AudioSystem.getClip();
                        final Object lock = new Object();

                        clip.addLineListener(event -> {
                            if (event.getType() == LineEvent.Type.STOP) {
                                // Revert to static image and hide bubble when TTS finishes
                                if (uiCallback != null) {
                                    uiCallback.showStaticImage();
                                    uiCallback.hideSpeechBubble();
                                }
                                synchronized (lock) {
                                    lock.notify();
                                }
                            }
                        });

                        clip.open(convertedStream);
                        clip.start();
                        synchronized (lock) {
                            lock.wait();
                        }
                        clip.close();
                    }
                }
            } else {
                System.err.println("TTS request failed with status: " + response.statusCode());
                // Ensure UI resets on failure
                if (uiCallback != null) {
                    uiCallback.showStaticImage();
                    uiCallback.hideSpeechBubble();
                }
            }
        } catch (Exception e) {
            System.err.println("Error during TTS playback: " + e.getMessage());
            e.printStackTrace();
            // Ensure UI resets on error
            if (uiCallback != null) {
                uiCallback.showStaticImage();
                uiCallback.hideSpeechBubble();
            }
        }
    }
}
