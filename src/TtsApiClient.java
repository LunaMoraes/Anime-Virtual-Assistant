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
 */
public class TtsApiClient {

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final Gson gson = new Gson();

    /**
     * Fetches the list of available character voices from the TTS API.
     * @return A list of character names, or null if an error occurs.
     */
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

    /**
     * Sends text to the TTS API to be converted to speech.
     * @param text The text to synthesize.
     * @param characterName The voice to use.
     * @param speed The speed of the speech.
     * @param language The language of the text.
     */
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
                byte[] audioBytes = response.body().readAllBytes();
                InputStream byteArrayInputStream = new ByteArrayInputStream(audioBytes);

                try (AudioInputStream sourceStream = AudioSystem.getAudioInputStream(byteArrayInputStream)) {
                    AudioFormat sourceFormat = sourceStream.getFormat();
                    // Define a standard, compatible audio format
                    AudioFormat targetFormat = new AudioFormat(
                            AudioFormat.Encoding.PCM_SIGNED,
                            sourceFormat.getSampleRate(),
                            16,
                            sourceFormat.getChannels(),
                            sourceFormat.getChannels() * 2,
                            sourceFormat.getSampleRate(),
                            false
                    );

                    // Convert the audio stream to the compatible format
                    try (AudioInputStream convertedStream = AudioSystem.getAudioInputStream(targetFormat, sourceStream)) {
                        Clip clip = AudioSystem.getClip();

                        // Use a LineListener to robustly wait for playback to finish
                        final Object lock = new Object();
                        clip.addLineListener(event -> {
                            if (event.getType() == LineEvent.Type.STOP) {
                                synchronized (lock) {
                                    lock.notify();
                                }
                            }
                        });

                        clip.open(convertedStream);
                        clip.start();

                        // Wait here until the LineListener notifies us that playback is complete
                        synchronized (lock) {
                            lock.wait();
                        }
                        clip.close();
                    }
                }
            } else {
                System.err.println("Error from TTS API: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
