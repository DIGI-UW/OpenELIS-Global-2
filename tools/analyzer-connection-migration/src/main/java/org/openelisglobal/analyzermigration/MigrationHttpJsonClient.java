package org.openelisglobal.analyzermigration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

final class MigrationHttpJsonClient {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    ObjectNode request(String method, String url, ObjectNode body, Map<String, String> headers) {
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url)).timeout(TIMEOUT).header("Accept",
                    "application/json");
            headers.forEach(request::header);
            if (body == null) {
                request.method(method, HttpRequest.BodyPublishers.noBody());
            } else {
                request.header("Content-Type", "application/json").method(method,
                        HttpRequest.BodyPublishers.ofByteArray(JSON.writeValueAsBytes(body)));
            }
            HttpResponse<byte[]> response = client.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("HTTP " + response.statusCode() + " from " + url);
            }
            JsonNode document = JSON.readTree(response.body());
            if (!(document instanceof ObjectNode object)) {
                throw new IllegalStateException("Expected a JSON object from " + url);
            }
            return object;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("HTTP request interrupted: " + url, exception);
        } catch (Exception exception) {
            throw new IllegalStateException("HTTP request failed: " + url, exception);
        }
    }
}
