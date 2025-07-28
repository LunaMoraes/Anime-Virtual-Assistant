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
 * Handles all communication with the Python TTS API server.
 * UPDATED: Now controls the character image state (speaking/static).
 */
public class TtsApiClient {

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final Gson gson = new Gson();

    public static List<String> getAvailableCharacters() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(AppState.TTS_API_URL + "/characters"))
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
            Map<String, Object> payloadMap = Map.of(
                    "text", text,
                    "character", characterName,
                    "speed", speed,
                    "language", language
            );
            String jsonPayload = gson.toJson(payloadMap);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(AppState.TTS_API_URL + "/synthesize"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() == 200) {
                // Show speaking image and bubble as soon as we get a valid response
                if (Main.characterUI != null) {
                    Main.characterUI.showSpeakingImage();
                    Main.characterUI.showSpeechBubble(text);
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
                                // Revert to static image and hide bubble when done
                                if (Main.characterUI != null) {
                                    Main.characterUI.showStaticImage();
                                    Main.characterUI.hideSpeechBubble();
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
                if (Main.characterUI != null) {
                    Main.characterUI.showStaticImage();
                    Main.characterUI.hideSpeechBubble();
                }
            }
        } catch (Exception e) {
            System.err.println("Error during TTS playback: " + e.getMessage());
            e.printStackTrace();
            // Ensure UI resets on error
            if (Main.characterUI != null) {
                Main.characterUI.showStaticImage();
                Main.characterUI.hideSpeechBubble();
            }
        }
    }
}
