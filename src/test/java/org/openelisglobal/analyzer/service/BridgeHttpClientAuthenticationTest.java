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
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = BridgeHttpClient.class)
@TestPropertySource(properties = { "analyzer.bridge.username=bridge-user", "analyzer.bridge.password=bridge-password" })
public class BridgeHttpClientAuthenticationTest {

    @Autowired
    private BridgeHttpClient bridgeHttpClient;

    private HttpServer server;
    private final AtomicReference<String> authorization = new AtomicReference<>();

    @Before
    public void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/analyzers", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();
    }

    @After
    public void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    public void sendsConfiguredBasicAuthenticationToBridge() throws Exception {
        String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/api/analyzers";

        BridgeHttpClient.BridgeResponse response = bridgeHttpClient.get(url, Duration.ofSeconds(2));

        String token = Base64.getEncoder()
                .encodeToString("bridge-user:bridge-password".getBytes(StandardCharsets.UTF_8));
        assertEquals(204, response.status);
        assertEquals("Basic " + token, authorization.get());
    }
}
