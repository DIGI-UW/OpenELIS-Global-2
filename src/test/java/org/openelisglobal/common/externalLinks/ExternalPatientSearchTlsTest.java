package org.openelisglobal.common.externalLinks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openelisglobal.config.HttpClientConfig;
import org.springframework.core.io.FileSystemResource;
import org.springframework.test.util.ReflectionTestUtils;

public class ExternalPatientSearchTlsTest {
    @ClassRule
    public static TemporaryFolder certificates = new TemporaryFolder();
    private static final String PASSWORD = "test-" + java.util.UUID.randomUUID();
    private static final String PATIENT_XML = "<Response><Patients><Patient><firstName>Test</firstName>"
            + "<lastName>Patient</lastName><GUID>synthetic-patient</GUID></Patient></Patients></Response>";
    private final List<HttpServer> servers = new ArrayList<>();
    private CloseableHttpClient client;

    @BeforeClass
    public static void createCertificates() throws Exception {
        for (String name : List.of("trusted", "untrusted", "wrong-host")) {
            keytool("-genkeypair", "-alias", name, "-keyalg", "RSA", "-keysize", "2048", "-validity", "2", "-dname",
                    "CN=" + (name.equals("wrong-host") ? "other.invalid" : "localhost"), "-ext",
                    "SAN=dns:" + (name.equals("wrong-host") ? "other.invalid" : "localhost"), "-keystore",
                    file(name + ".p12").toString(), "-storetype", "PKCS12", "-storepass", PASSWORD);
            if (!name.equals("untrusted")) {
                Path cert = file(name + ".cer");
                keytool("-exportcert", "-alias", name, "-keystore", file(name + ".p12").toString(), "-storepass",
                        PASSWORD, "-file", cert.toString());
                keytool("-importcert", "-noprompt", "-alias", name, "-keystore", file("trust.p12").toString(),
                        "-storetype", "PKCS12", "-storepass", PASSWORD, "-file", cert.toString());
            }
        }
    }

    @Before
    public void createClientFromTheInstallationTrustSettings() throws Exception {
        HttpClientConfig config = new HttpClientConfig();
        ReflectionTestUtils.setField(config, "trustStore", new FileSystemResource(file("trust.p12")));
        ReflectionTestUtils.setField(config, "trustStorePassword", PASSWORD);
        // No client-key or other destination credentials are configured.
        client = config.patientSearchHttpClient();
    }

    @After
    public void close() throws Exception {
        for (HttpServer server : servers)
            server.stop(0);
        client.close();
    }

    @Test
    public void explicitlyTrustedCertificateAndMatchingHostname_returnPatientResults() throws Exception {
        AtomicInteger received = new AtomicInteger();
        HttpsServer server = server("trusted", received, 200, null);
        ExternalPatientSearch search = search(url(server));
        assertEquals(200, search.runExternalSearch().get().intValue());
        assertEquals(1, received.get());
        assertEquals(1, search.getSearchResults().size());
        assertEquals("Test", search.getSearchResults().get(0).getFirstName());
        assertEquals("synthetic-patient", search.getSearchResults().get(0).getGUID());
    }

    @Test
    public void untrustedSelfSignedCertificate_refusesBeforeSendingTheQuery() throws Exception {
        assertTlsRefused("untrusted");
    }

    @Test
    public void trustedCertificateForAnotherHost_refusesBeforeSendingTheQuery() throws Exception {
        assertTlsRefused("wrong-host");
    }

    private void assertTlsRefused(String identity) throws Exception {
        AtomicInteger received = new AtomicInteger();
        ExternalPatientSearch search = search(url(server(identity, received, 200, null)));
        assertEquals(502, search.runExternalSearch().get().intValue());
        assertEquals(0, received.get());
        assertEquals(0, search.getSearchResults().size());
        assertFalse(search.errors.isEmpty());
    }

    @Test
    public void plaintextEndpoint_sendsNeitherPatientTermsNorCredentials() throws Exception {
        AtomicInteger received = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servers.add(server);
        server.createContext("/", exchange -> {
            received.incrementAndGet();
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
        ExternalPatientSearch search = search("http://localhost:" + server.getAddress().getPort() + "/search");
        assertEquals(502, search.runExternalSearch().get().intValue());
        assertEquals(0, received.get());
    }

    @Test
    public void redirect_doesNotSendTheQueryToAnotherEndpoint() throws Exception {
        AtomicInteger redirected = new AtomicInteger();
        HttpsServer target = server("trusted", redirected, 200, null);
        AtomicInteger received = new AtomicInteger();
        HttpsServer source = server("trusted", received, 302, url(target) + "?pwd=partner-test-secret");
        ExternalPatientSearch search = search(url(source));
        assertEquals(302, search.runExternalSearch().get().intValue());
        assertEquals(1, received.get());
        assertEquals(0, redirected.get());
        assertEquals(0, search.getSearchResults().size());
    }

    private ExternalPatientSearch search(String endpoint) {
        ExternalPatientSearch search = new ExternalPatientSearch();
        ReflectionTestUtils.setField(search, "timeout", 2000);
        ReflectionTestUtils.setField(search, "httpClient", client);
        search.setConnectionCredentials(endpoint, "partner-test-user", "partner-test-secret");
        search.setSearchCriteria("Patient", "Test", null, null, null, null);
        return search;
    }

    private HttpsServer server(String identity, AtomicInteger received, int status, String location) throws Exception {
        KeyStore keys = KeyStore.getInstance(file(identity + ".p12").toFile(), PASSWORD.toCharArray());
        KeyManagerFactory managers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        managers.init(keys, PASSWORD.toCharArray());
        SSLContext tls = SSLContext.getInstance("TLS");
        tls.init(managers.getKeyManagers(), null, null);
        HttpsServer server = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servers.add(server);
        server.setHttpsConfigurator(new HttpsConfigurator(tls));
        server.createContext("/search", exchange -> {
            received.incrementAndGet();
            assertTrue(exchange.getRequestURI().getQuery().contains("pwd=partner-test-secret"));
            if (location != null)
                exchange.getResponseHeaders().set("Location", location);
            byte[] body = PATIENT_XML.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        return server;
    }

    private static String url(HttpServer server) {
        return "https://localhost:" + server.getAddress().getPort() + "/search";
    }

    private static Path file(String name) {
        return certificates.getRoot().toPath().resolve(name);
    }

    private static void keytool(String... args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "keytool").toString());
        command.addAll(List.of(args));
        Path output = file("keytool.log");
        Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(output.toFile()).start();
        assertTrue("keytool timed out", process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS));
        assertEquals(Files.readString(output), 0, process.exitValue());
    }
}
