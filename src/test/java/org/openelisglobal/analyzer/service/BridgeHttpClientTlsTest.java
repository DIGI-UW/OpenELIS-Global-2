package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.time.Duration;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = BridgeHttpClient.class)
public class BridgeHttpClientTlsTest {

    private static final char[] KEYSTORE_PASSWORD = "changeit".toCharArray();
    private static final Path SELF_SIGNED_KEYSTORE = Path.of(
            "tools/openelis-analyzer-bridge/src/test/resources/test-server.p12");

    @Autowired
    private BridgeHttpClient bridgeHttpClient;

    private HttpsServer server;

    @Before
    public void startSelfSignedServer() throws Exception {
        assertTrue("pinned Bridge TLS fixture is missing", Files.isRegularFile(SELF_SIGNED_KEYSTORE));
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream input = Files.newInputStream(SELF_SIGNED_KEYSTORE)) {
            keyStore.load(input, KEYSTORE_PASSWORD);
        }

        KeyManagerFactory keyManagerFactory = KeyManagerFactory
                .getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, KEYSTORE_PASSWORD);
        SSLContext serverContext = SSLContext.getInstance("TLS");
        serverContext.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());

        server = HttpsServer.create(new InetSocketAddress("localhost", 0), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(serverContext));
        server.createContext("/actuator/health", exchange -> {
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
    public void rejectsCertificateOutsideConfiguredTruststore() throws Exception {
        String url = "https://localhost:" + server.getAddress().getPort() + "/actuator/health";

        try {
            bridgeHttpClient.get(url, Duration.ofSeconds(2));
            fail("Bridge client accepted a certificate outside the configured truststore");
        } catch (IOException exception) {
            assertNotNull(exception.getMessage());
            assertTrue("expected a TLS failure but got: " + exception, hasCause(exception, SSLException.class));
        }
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> expectedType) {
        Throwable current = throwable;
        while (current != null) {
            if (expectedType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
