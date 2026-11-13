package org.openelisglobal.dataexchange.fhir;

import static org.junit.Assert.*;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsExchange;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.TrustManagerFactory;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Practitioner;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openelisglobal.config.HttpClientConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.env.MockPropertySource;

/**
 * Exercises the real Spring wiring, HAPI client and TLS transport against
 * synthetic receivers.
 */
public class FhirClientFactoryTest {
    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();
    private HttpsServer receiver;
    private AnnotationConfigApplicationContext application;
    private MockPropertySource properties;
    private final List<Received> received = new CopyOnWriteArrayList<>();
    private volatile String next;
    private volatile String redirect;
    private String origin;
    private volatile String tokenBody;
    private final String internalPassword = java.util.UUID.randomUUID().toString();
    private final String partnerPassword = java.util.UUID.randomUUID().toString();
    private final String registryToken = java.util.UUID.randomUUID().toString();
    private final String facilityToken = java.util.UUID.randomUUID().toString();
    private final String explicitPassword = java.util.UUID.randomUUID().toString();

    record Received(String path, List<String> authorization, boolean clientCertificate, List<String> cookies) {
    }

    @Before
    public void startReceiver() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        X500Name name = new X500Name("CN=localhost");
        Instant now = Instant.now();
        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(name,
                new BigInteger(128, new SecureRandom()), Date.from(now.minusSeconds(60)),
                Date.from(now.plusSeconds(3600)), name, pair.getPublic());
        builder.addExtension(Extension.subjectAlternativeName, false,
                new GeneralNames(new GeneralName(GeneralName.dNSName, "localhost")));
        X509Certificate cert = new JcaX509CertificateConverter()
                .getCertificate(builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(pair.getPrivate())));
        char[] password = java.util.UUID.randomUUID().toString().toCharArray();
        KeyStore keys = KeyStore.getInstance("PKCS12");
        keys.load(null, null);
        keys.setKeyEntry("identity", pair.getPrivate(), password, new X509Certificate[] { cert });
        KeyStore trust = KeyStore.getInstance("PKCS12");
        trust.load(null, null);
        trust.setCertificateEntry("receiver", cert);
        Path keyPath = temporary.getRoot().toPath().resolve("keys.p12");
        Path trustPath = temporary.getRoot().toPath().resolve("trust.p12");
        try (var output = Files.newOutputStream(keyPath)) {
            keys.store(output, password);
        }
        try (var output = Files.newOutputStream(trustPath)) {
            trust.store(output, password);
        }
        KeyManagerFactory km = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        km.init(keys, password);
        TrustManagerFactory tm = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tm.init(trust);
        SSLContext tls = SSLContext.getInstance("TLS");
        tls.init(km.getKeyManagers(), tm.getTrustManagers(), null);
        receiver = HttpsServer.create(new InetSocketAddress("localhost", 0), 0);
        receiver.setHttpsConfigurator(new HttpsConfigurator(tls) {
            @Override
            public void configure(HttpsParameters parameters) {
                SSLParameters ssl = tls.getDefaultSSLParameters();
                ssl.setWantClientAuth(true);
                parameters.setSSLParameters(ssl);
            }
        });
        receiver.createContext("/", exchange -> {
            boolean clientCertificate;
            try {
                ((HttpsExchange) exchange).getSSLSession().getPeerCertificates();
                clientCertificate = true;
            } catch (SSLPeerUnverifiedException absent) {
                clientCertificate = false;
            }
            String path = exchange.getRequestURI().getPath();
            received.add(new Received(path,
                    new ArrayList<>(exchange.getRequestHeaders().getOrDefault("Authorization", List.of())),
                    clientCertificate,
                    new ArrayList<>(exchange.getRequestHeaders().getOrDefault("Cookie", List.of()))));
            byte[] requestBody = exchange.getRequestBody().readAllBytes();
            if (path.startsWith("/partner/")) {
                exchange.getResponseHeaders().add("Set-Cookie", "partner-session=synthetic-session; Path=/; Secure");
            }
            String body;
            int status = 200;
            if (path.endsWith("/metadata")) {
                body = "{\"resourceType\":\"CapabilityStatement\",\"status\":\"active\",\"kind\":\"instance\","
                        + "\"fhirVersion\":\"4.0.1\",\"format\":[\"json\"]}";
            } else if (redirect != null) {
                status = 302;
                exchange.getResponseHeaders().add("Location", redirect);
                body = "{\"resourceType\":\"OperationOutcome\"}";
            } else if (path.equals("/token")) {
                tokenBody = new String(requestBody, StandardCharsets.UTF_8);
                body = "{\"access_token\":\"" + facilityToken + "\"}";
            } else if (exchange.getRequestMethod().equals("PUT")) {
                body = new String(requestBody, StandardCharsets.UTF_8);
            } else {
                body = "{\"resourceType\":\"Bundle\",\"type\":\"searchset\",\"entry\":["
                        + "{\"resource\":{\"resourceType\":\"Practitioner\",\"id\":\"synthetic\"}}]"
                        + (next == null ? "" : ",\"link\":[{\"relation\":\"next\",\"url\":\"" + next + "\"}]") + "}";
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/fhir+json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        receiver.start();
        origin = "https://localhost:" + receiver.getAddress().getPort();
        properties = new MockPropertySource().withProperty("server.ssl.key-store", keyPath.toUri().toString())
                .withProperty("server.ssl.trust-store", trustPath.toUri().toString())
                .withProperty("server.ssl.key-store-password", new String(password))
                .withProperty("server.ssl.key-password", new String(password))
                .withProperty("server.ssl.trust-store-password", new String(password))
                .withProperty("org.openelisglobal.fhirstore.uri", origin + "/local/")
                .withProperty("org.openelisglobal.fhirstore.username", "internal")
                .withProperty("org.openelisglobal.fhirstore.password", internalPassword)
                .withProperty("org.openelisglobal.remote.source.uri", origin + "/partner")
                .withProperty("org.openelisglobal.remote.source.identifier", "Practitioner/*");
    }

    private FhirClientFactory factory() {
        application = new AnnotationConfigApplicationContext();
        application.getEnvironment().getPropertySources().addFirst(properties);
        application.register(HttpClientConfig.class, FhirConfig.class, FhirClientFactory.class);
        application.refresh();
        return application.getBean(FhirClientFactory.class);
    }

    private void configureBasic() {
        properties.withProperty("org.openelisglobal.fhir.partners", "lab")
                .withProperty("org.openelisglobal.fhir.partner.lab.uri", origin + "/partner/")
                .withProperty("org.openelisglobal.fhir.partner.lab.username", "partner")
                .withProperty("org.openelisglobal.fhir.partner.lab.password", partnerPassword);
    }

    private Bundle search(IGenericClient client) {
        return client.search().forResource(Practitioner.class).returnBundle(Bundle.class).execute();
    }

    private void assertIdentity(String prefix, String expected, boolean certificate) {
        List<Received> requests = received.stream().filter(r -> r.path.startsWith(prefix + "/")).toList();
        assertFalse("Receiver must observe actual requests", requests.isEmpty());
        for (Received request : requests) {
            assertEquals(expected == null ? List.of() : List.of(expected), request.authorization);
            assertEquals("TLS client identity at " + request.path, certificate, request.clientCertificate);
            assertTrue("No implicit cookie identity at " + request.path, request.cookies.isEmpty());
        }
    }

    private String basic(String user, String password) {
        return "Basic " + java.util.Base64.getEncoder()
                .encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void internalIdentityStaysLocalAndUnconfiguredPartnersAreAnonymous() {
        FhirClientFactory clients = factory();
        search(clients.create(origin + "/local"));
        search(clients.create(origin + "/partner"));
        assertIdentity("/local", basic("internal", internalPassword), true);
        assertIdentity("/partner", null, false);
    }

    @Test
    public void partnersUseDistinctBasicAndBearerIdentities() {
        configureBasic();
        properties.withProperty("org.openelisglobal.fhir.partners", "lab,registry")
                .withProperty("org.openelisglobal.fhir.partner.registry.uri", origin + "/registry")
                .withProperty("org.openelisglobal.fhir.partner.registry.token", registryToken);
        FhirClientFactory clients = factory();
        search(clients.create(origin + "/partner"));
        search(clients.create(origin + "/registry/"));
        search(clients.create(origin + "/other"));
        assertIdentity("/partner", basic("partner", partnerPassword), false);
        assertIdentity("/registry", "Bearer " + registryToken, false);
        assertIdentity("/other", null, false);
    }

    @Test
    public void existingExplicitRegistryCredentialsArePreserved() {
        FhirClientFactory clients = factory();
        search(clients.create(origin + "/registry", "registry", explicitPassword));
        search(clients.createBearer(origin + "/facility", facilityToken));
        assertIdentity("/registry", basic("registry", explicitPassword), false);
        assertIdentity("/facility", "Bearer " + facilityToken, false);
    }

    @Test
    public void practitionerLookupAndSameBasePaginationUsePartnerCredentials() {
        configureBasic();
        factory();
        next = origin + "/partner/page2";
        IGenericClient client = application.getBean(FhirClientFactory.class).create(origin + "/partner");
        Bundle first = search(client);
        next = null;
        assertEquals(1, client.loadPage().next(first).execute().getEntry().size());
        assertEquals(List.of("Practitioner/synthetic"),
                application.getBean(FhirConfig.class).getRemoteStoreIdentifier());
        assertTrue(received.stream().anyMatch(r -> r.path.equals("/partner/page2")));
        assertIdentity("/partner", basic("partner", partnerPassword), false);
    }

    @Test
    public void paginationCannotMoveCredentialsToAnotherBaseOrOrigin() {
        configureBasic();
        IGenericClient client = factory().create(origin + "/partner");
        for (String target : List.of(origin + "/other/page", origin.replace("localhost", "127.0.0.1") + "/partner/page",
                origin + "/partner/%2e%2e/other", origin + "/partner/%252e%252e/other",
                origin + "/partner%2fother/page", origin + "/partner/..;x/other")) {
            next = target;
            Bundle first = search(client);
            int before = received.size();
            assertThrows(RuntimeException.class, () -> client.loadPage().next(first).execute());
            assertEquals("Escaped destination must never be contacted", before, received.size());
        }
        assertIdentity("/partner", basic("partner", partnerPassword), false);
    }

    @Test
    public void redirectsNeverTransmitToTheSecondDestination() {
        configureBasic();
        FhirClientFactory clients = factory();
        redirect = origin + "/other/Practitioner";
        for (String path : List.of("/local", "/partner")) {
            assertThrows(RuntimeException.class, () -> search(clients.create(origin + path)));
        }
        assertFalse(received.stream().anyMatch(r -> r.path.startsWith("/other")));
        assertIdentity("/local", basic("internal", internalPassword), true);
        assertIdentity("/partner", basic("partner", partnerPassword), false);
    }

    @Test
    public void invalidOrAmbiguousPartnerConfigurationFailsAtStartup() {
        configureBasic();
        properties.withProperty("org.openelisglobal.fhir.partner.lab.token", registryToken);
        assertThrows(org.springframework.beans.BeansException.class, this::factory);
        assertTrue(received.isEmpty());
    }

    @Test
    public void localUrlCannotBeRegisteredAsAPartner() {
        configureBasic();
        properties.withProperty("org.openelisglobal.fhir.partner.lab.uri", origin + "/local/");
        assertThrows(org.springframework.beans.BeansException.class, this::factory);
    }

    @Test
    public void baseUrlMatchingNormalizesOnlyEquivalentUrls() {
        assertEquals(FhirClientFactory.baseUri("https://EXAMPLE.org:443/fhir/"),
                FhirClientFactory.baseUri("https://example.org/fhir"));
        for (String url : List.of("https://user:secret@example.org/fhir", "https://example.org/fhir?token=x",
                "https://example.org/fhir#fragment", "https://example.org/fhir/../other",
                "https://example.org/fhir/%2e%2e/other")) {
            assertThrows(IllegalArgumentException.class, () -> FhirClientFactory.baseUri(url));
        }
    }

    @Test
    public void plaintextPartnerCredentialsAreRefusedBeforeContact() {
        FhirClientFactory clients = factory();
        assertThrows(IllegalArgumentException.class,
                () -> clients.create("http://localhost:1/fhir", "partner", partnerPassword));
        assertTrue(received.isEmpty());
    }

    @Test
    public void facilitySynchronizationDoesNotAddInternalAuthentication() {
        configureBasic();
        FhirClientFactory clients = factory();
        FhirUtil util = new FhirUtil();
        org.springframework.test.util.ReflectionTestUtils.setField(util, "clients", clients);
        var service = new org.openelisglobal.dataexchange.fhir.service.FhirFacilityOrganizationServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "fhirUtil", util);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "fhirConfig",
                application.getBean(FhirConfig.class));
        var organization = new org.hl7.fhir.r4.model.Organization();
        organization.setId("synthetic-facility");
        organization.setName("Synthetic facility");
        org.springframework.test.util.ReflectionTestUtils.setField(service, "facilityOrganization", organization);
        service.syncToRemoteFhirServers();
        assertTrue(received.stream().anyMatch(r -> r.path.equals("/partner/Organization/synthetic-facility")));
        assertIdentity("/partner", basic("partner", partnerPassword), false);
    }

    @Test
    public void tokenExchangeUsesExplicitCredentialsWithoutLocalClientCertificate() throws Exception {
        factory();
        FhirUtil util = new FhirUtil();
        org.springframework.test.util.ReflectionTestUtils.setField(util, "partnerHttpClient",
                application.getBean("partnerFhirHttpClient"));
        assertEquals(facilityToken,
                util.getAccessToken(origin + "/token?realm=facility", "synthetic-\"user", explicitPassword + "é"));
        var body = new com.fasterxml.jackson.databind.ObjectMapper().readTree(tokenBody);
        assertEquals("synthetic-\"user", body.get("username").asText());
        assertEquals(explicitPassword + "é", body.get("password").asText());
        Received request = received.get(0);
        assertEquals("/token", request.path);
        assertEquals(List.of(), request.authorization);
        assertFalse(request.clientCertificate);
        redirect = origin + "/other/token";
        assertThrows(java.io.IOException.class, () -> util.getAccessToken(origin + "/token", "user", partnerPassword));
        assertFalse(received.stream().anyMatch(r -> r.path.startsWith("/other")));
    }

    @Test
    public void explicitlyAllowedTestSubscriberRemainsAnonymous() {
        properties.withProperty("org.openelisglobal.fhir.subscriber", "http://localhost:1/fhir/")
                .withProperty("org.openelisglobal.fhir.subscriber.allowHTTP", "true");
        FhirClientFactory clients = factory();
        assertNotNull(clients.create("http://localhost:1/fhir"));
        assertThrows(IllegalArgumentException.class,
                () -> clients.create("http://localhost:1/fhir", "partner", partnerPassword));
        assertThrows(IllegalArgumentException.class, () -> clients.create("http://localhost:1/other"));
    }

    @Test
    public void partnerTransportVerifiesTheCertificateHostname() {
        IGenericClient client = factory().create(origin.replace("localhost", "127.0.0.1") + "/partner");
        assertThrows(RuntimeException.class, () -> search(client));
        assertTrue("No HTTP request before verified TLS", received.isEmpty());
    }

    @After
    public void close() {
        if (application != null)
            application.close();
        if (receiver != null)
            receiver.stop(0);
    }
}
