package org.openelisglobal.addons.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Calls an OpenAI-style chat API (OpenRouter by default). Streams (SSE) when
 * asked: each piece of text goes to {@code onDelta}. Never logs the API key or
 * what is sent. Only hosts the manifest declares are called ({@link Egress}).
 */
public class LlmClient {

    public record Reply(String text, int promptTokens, int completionTokens, int totalTokens, String model,
            String sessionId) {
        public Reply(String text, int promptTokens, int completionTokens, int totalTokens, String model) {
            this(text, promptTokens, completionTokens, totalTokens, model, null);
        }
    }

    public static class LlmException extends Exception {
        private static final long serialVersionUID = 1L;
        public final int status;

        public LlmException(String message, int status) {
            super(message);
            this.status = status;
        }
    }

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final HttpClient HTTP = Egress.client(15);
    private final Settings settings;
    private final HttpClient http;

    public LlmClient(Settings settings) {
        this(settings, HTTP);
    }

    LlmClient(Settings settings, HttpClient http) {
        this.settings = settings;
        this.http = http;
    }

    /** @param onDelta null = không stream */
    public Reply chat(List<Map<String, String>> messages, Consumer<String> onDelta)
            throws LlmException, InterruptedException {
        if (!settings.openAiConfigured()) {
            throw new LlmException("no API key configured", 503);
        }
        String url = settings.endpoint + "/chat/completions";
        if (!Egress.permits(url)) {
            throw new LlmException("endpoint host is not in the add-on's network.egress", 503);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", settings.model);
        body.put("messages", messages);
        body.put("temperature", settings.temperature);
        body.put("max_tokens", settings.maxTokens);
        boolean stream = onDelta != null;
        if (stream) {
            body.put("stream", true);
            body.put("stream_options", Map.of("include_usage", true));
        }
        HttpRequest req;
        try {
            req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(settings.timeoutSeconds))
                    .header("Authorization", "Bearer " + settings.apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", stream ? "text/event-stream" : "application/json")
                    .header("X-Title", settings.productName)
                    .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();
        } catch (IOException e) {
            throw new LlmException("cannot encode request", 500);
        }
        HttpResponse<InputStream> res;
        try {
            res = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException e) {
            throw new LlmException("provider unreachable: " + e.getClass().getSimpleName(), 502);
        }
        try (InputStream in = res.body()) {
            if (res.statusCode() / 100 != 2) {
                String err = new String(in.readNBytes(2000), StandardCharsets.UTF_8);
                throw new LlmException("provider HTTP " + res.statusCode() + ": " + shortError(err), res.statusCode());
            }
            return stream ? readStream(in, onDelta) : readJson(in);
        } catch (IOException e) {
            throw new LlmException("provider read failed: " + e.getClass().getSimpleName(), 502);
        }
    }

    private Reply readJson(InputStream in) throws IOException, LlmException {
        JsonNode root = JSON.readTree(in);
        if (root.has("error")) {
            throw new LlmException("provider error: " + shortError(root.get("error").toString()), 502);
        }
        String text = root.path("choices").path(0).path("message").path("content").asText("");
        JsonNode u = root.path("usage");
        return new Reply(text, u.path("prompt_tokens").asInt(0), u.path("completion_tokens").asInt(0),
                u.path("total_tokens").asInt(0), root.path("model").asText(settings.model));
    }

    private Reply readStream(InputStream in, Consumer<String> onDelta) throws IOException, LlmException {
        StringBuilder text = new StringBuilder();
        int pt = 0;
        int ct = 0;
        int tt = 0;
        String model = settings.model;
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        while ((line = r.readLine()) != null) {
            if (!line.startsWith("data:")) {
                continue; // ": OPENROUTER PROCESSING" keep-alive, empty lines
            }
            String data = line.substring(5).trim();
            if (data.equals("[DONE]")) {
                break;
            }
            JsonNode chunk = JSON.readTree(data);
            if (chunk.has("error")) {
                throw new LlmException("provider error: " + shortError(chunk.get("error").toString()), 502);
            }
            String piece = chunk.path("choices").path(0).path("delta").path("content").asText("");
            if (!piece.isEmpty()) {
                text.append(piece);
                onDelta.accept(piece);
            }
            if (chunk.hasNonNull("model")) {
                model = chunk.get("model").asText(model);
            }
            JsonNode u = chunk.path("usage");
            if (u.isObject()) {
                pt = u.path("prompt_tokens").asInt(pt);
                ct = u.path("completion_tokens").asInt(ct);
                tt = u.path("total_tokens").asInt(tt);
            }
        }
        return new Reply(text.toString(), pt, ct, tt, model);
    }

    private static String shortError(String s) {
        String t = s.replaceAll("\\s+", " ");
        return t.length() > 300 ? t.substring(0, 300) : t;
    }
}
