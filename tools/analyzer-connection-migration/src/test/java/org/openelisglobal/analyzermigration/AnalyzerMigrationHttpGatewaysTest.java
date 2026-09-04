package org.openelisglobal.analyzermigration;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AnalyzerMigrationHttpGatewaysTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private HttpServer server;
    private String baseUrl;
    private final List<ObjectNode> requests = new ArrayList<>();
    private final List<String> cookies = new ArrayList<>();

    @Before
    public void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/bridge/api/connections", this::handleBridge);
        server.createContext("/oe/rest/analyzer/migration/analyzers/42/reference", this::handleOpenElis);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @After
    public void stopServer() {
        server.stop(0);
    }

    @Test
    public void bridgeGatewayUsesTheVersionedCreateAndReadEndpoints() {
        HttpBridgeGateway gateway = new HttpBridgeGateway(baseUrl + "/bridge");
        ObjectNode create = JSON.createObjectNode();
        create.put("schemaVersion", "1.0");
        create.put("requestId", "migration:42:aaaaaaaa");
        create.put("clientAnalyzerId", "42");
        create.put("displayName", "FluoroCycler XT");
        create.set("profileRef", profileRef());
        create.putObject("values").put("directory", "/srv/analyzers/fluoro");

        ObjectNode created = gateway.createConnection(create);
        ObjectNode read = gateway.getConnection("bridge-42");

        assertEquals("bridge-42", created.path("connectionId").asText());
        assertEquals("bridge-42", read.path("connectionId").asText());
        assertEquals("migration:42:aaaaaaaa", requests.get(0).path("requestId").asText());
    }

    @Test
    public void openElisGatewayUsesTheMigrationReferenceEndpointsAndSessionCookie() {
        HttpOpenElisGateway gateway = new HttpOpenElisGateway(baseUrl + "/oe", "JSESSIONID=migration-session");

        gateway.attachBridgeConnection("42", profileRef(), "bridge-42", "migration-operator");
        ObjectNode reference = gateway.getAnalyzerReference("42");

        assertEquals("bridge-42", requests.get(0).path("bridgeConnectionId").asText());
        assertEquals("fluorocycler-xt", requests.get(0).path("profileId").asText());
        assertEquals("bridge-42", reference.path("bridgeConnectionId").asText());
        assertEquals(List.of("JSESSIONID=migration-session", "JSESSIONID=migration-session"), cookies);
    }

    private void handleBridge(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            requests.add((ObjectNode) JSON.readTree(exchange.getRequestBody()));
        }
        writeJson(exchange, connection());
    }

    private void handleOpenElis(HttpExchange exchange) throws IOException {
        cookies.add(exchange.getRequestHeaders().getFirst("Cookie"));
        if ("PUT".equals(exchange.getRequestMethod())) {
            requests.add((ObjectNode) JSON.readTree(exchange.getRequestBody()));
        }
        ObjectNode reference = JSON.createObjectNode();
        reference.put("sourceAnalyzerId", "42");
        reference.put("bridgeConnectionId", "bridge-42");
        reference.set("profileRef", profileRef());
        writeJson(exchange, reference);
    }

    private static void writeJson(HttpExchange exchange, ObjectNode body) throws IOException {
        byte[] bytes = JSON.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static ObjectNode connection() {
        ObjectNode connection = JSON.createObjectNode();
        connection.put("schemaVersion", "1.0");
        connection.put("connectionId", "bridge-42");
        connection.put("clientAnalyzerId", "42");
        connection.set("profileRef", profileRef());
        connection.put("configRevision", 1);
        return connection;
    }

    private static ObjectNode profileRef() {
        ObjectNode profileRef = JSON.createObjectNode();
        profileRef.put("profileId", "fluorocycler-xt");
        profileRef.put("revision", 1);
        profileRef.put("fingerprint", "sha256:" + "a".repeat(64));
        return profileRef;
    }
}
