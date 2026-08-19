package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BridgeHttpClientAuthenticationTest {

    private HttpServer server;
    private String endpoint;

    @Before
    public void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/api/profiles";
    }

    @After
    public void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    public void getSendsConfiguredBridgeBasicAuthentication() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        server.createContext("/api/profiles", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().close();
        });
        server.start();

        BridgeHttpClient client = new BridgeHttpClient("bridge-user", "bridge-secret");
        BridgeHttpClient.BridgeResponse response = client.get(endpoint, Duration.ofSeconds(2));

        String credentials = Base64.getEncoder()
                .encodeToString("bridge-user:bridge-secret".getBytes(StandardCharsets.UTF_8));
        assertEquals(200, response.status);
        assertEquals("Basic " + credentials, authorization.get());
    }
}
