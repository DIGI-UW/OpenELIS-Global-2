package org.openelisglobal.facilityregistry.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.dataexchange.fhir.FhirClientFactory;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.service.FhirFacilityOrganizationService;
import org.openelisglobal.facilityregistry.form.FacilityLocationStatus;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.organization.locations.LocationsApi.Detail;
import org.openelisglobal.organization.locations.LocationsApi.SaveRequest;
import org.openelisglobal.organization.locations.LocationsService;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.service.OrganizationTypeService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.organization.valueholder.OrganizationType;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

public class FacilityRegistryServiceTest extends BaseWebContextSensitiveTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private FacilityRegistryService service;

    @Autowired
    private FhirFacilityOrganizationService facilityOrganizationService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private LocalizationService localizationService;

    @Autowired
    private LocationsService locationsService;

    @Autowired
    private OrganizationTypeService organizationTypeService;

    private Object target;
    private Object serviceClient;
    private Object contextFhirUtil;
    private HttpsServer server;
    private HttpServer ipServer;
    private CloseableHttpClient partnerTransport;
    private String base;
    private final List<Recorded> requests = new CopyOnWriteArrayList<>();
    private final AtomicInteger registryStatus = new AtomicInteger(200);
    private final AtomicInteger tokenStatus = new AtomicInteger(200);
    private volatile String ipResponse = "{\"latitude\": 0.3476, \"longitude\": 32.5825}";

    record Recorded(String method, String path, String contentType, String authorization, String body) {
    }

    @Before
    public void setUp() throws Exception {
        target = service instanceof Advised ? ((Advised) service).getTargetSource().getTarget() : service;
        facilityOrganizationService.initialize();
        SSLContext tls = receiverTls();
        server = HttpsServer.create(new InetSocketAddress("localhost", 0), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(tls));
        server.createContext("/", this::handle);
        server.start();
        base = "https://localhost:" + server.getAddress().getPort();
        ipServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        ipServer.createContext("/ip", this::handle);
        ipServer.start();
        partnerTransport = HttpClients.custom().setSSLContext(tls).disableRedirectHandling().disableCookieManagement()
                .build();
        serviceClient = ReflectionTestUtils.getField(target, "httpClient");
        assertSame("Registry uploads must use the partner transport from Spring",
                webApplicationContext.getBean("partnerFhirHttpClient"), serviceClient);
        contextFhirUtil = ReflectionTestUtils.getField(target, "fhirUtil");
        FhirUtil fhirUtil = new FhirUtil();
        FhirContext context = FhirContext.forR4Cached();
        FhirClientFactory clients = new FhirClientFactory(new FhirConfig(), context, partnerTransport,
                new MockEnvironment());
        clients.configurePartners();
        ReflectionTestUtils.setField(fhirUtil, "fhirContext", context);
        ReflectionTestUtils.setField(fhirUtil, "clients", clients);
        ReflectionTestUtils.setField(fhirUtil, "partnerHttpClient", partnerTransport);
        ReflectionTestUtils.setField(target, "httpClient", partnerTransport);
        ReflectionTestUtils.setField(target, "fhirUtil", fhirUtil);
        configure("", "", "");
        ReflectionTestUtils.setField(target, "registryUrl", base + "/fhir/DEFAULT");
        ReflectionTestUtils.setField(target, "registryAuth", "");
        ReflectionTestUtils.setField(target, "autolocationUrl",
                "http://127.0.0.1:" + ipServer.getAddress().getPort() + "/ip");
        resetState();
        clearStoredCoordinates();
    }

    @After
    public void tearDown() throws Exception {
        if (server != null)
            server.stop(0);
        if (ipServer != null)
            ipServer.stop(0);
        if (partnerTransport != null)
            partnerTransport.close();
        ReflectionTestUtils.setField(target, "httpClient", serviceClient);
        ReflectionTestUtils.setField(target, "fhirUtil", contextFhirUtil);
        ReflectionTestUtils.setField(target, "registryUrl", "");
        configure("", "", "");
        resetState();
        clearStoredCoordinates();
    }

    @Test
    public void registersTheGofrFacilityOrganizationThenLocationAsJson() throws Exception {
        configure("", "0.3476", "32.5825");

        assertTrue(service.register());

        List<Recorded> puts = puts();
        assertEquals(2, puts.size());
        String uuid = facilityOrganizationService.getFacilityUuid();
        assertEquals("/fhir/DEFAULT/Organization/" + uuid, puts.get(0).path());
        assertTrue(puts.get(1).path().startsWith("/fhir/DEFAULT/Location/"));
        assertEquals("application/json", puts.get(0).contentType());
        assertEquals("application/json", puts.get(1).contentType());

        JsonNode organization = JSON.readTree(puts.get(0).body());
        assertEquals(FacilityRegistryService.FACILITY_ORGANIZATION_PROFILE,
                organization.at("/meta/profile/0").asText());
        assertFacilityTypes(organization);
        assertEquals(facilityOrganizationService.getFacilityIdentifierSystem(),
                organization.at("/identifier/0/system").asText());

        JsonNode location = JSON.readTree(puts.get(1).body());
        assertEquals(FacilityRegistryService.FACILITY_LOCATION_PROFILE, location.at("/meta/profile/0").asText());
        assertEquals("active", location.at("/status").asText());
        assertFacilityTypes(location);
        assertEquals("http://terminology.hl7.org/CodeSystem/location-physical-type",
                location.at("/physicalType/coding/0/system").asText());
        assertEquals("bu", location.at("/physicalType/coding/0/code").asText());
        assertEquals(0.3476, location.at("/position/latitude").asDouble(), 0.0000001);
        assertEquals(32.5825, location.at("/position/longitude").asDouble(), 0.0000001);
        assertEquals("Organization/" + uuid, location.at("/managingOrganization/reference").asText());
        assertEquals(organization.at("/identifier/0/system").asText(), location.at("/identifier/0/system").asText());
        assertEquals(organization.at("/identifier/0/value").asText(), location.at("/identifier/0/value").asText());
        assertEquals(organization.at("/name").asText(), location.at("/name").asText());
    }

    @Test
    public void tokenAuthenticationFetchesATokenAndSendsItAsBearer() throws Exception {
        configureAuth("token");

        assertTrue(service.register());

        Recorded tokenRequest = requests.get(0);
        assertEquals("/auth/token", tokenRequest.path());
        JsonNode credentials = JSON.readTree(tokenRequest.body());
        assertEquals("registry-user", credentials.get("username").asText());
        assertEquals("pa\"ss", credentials.get("password").asText());
        assertEquals("Bearer token-123", puts().get(0).authorization());
        assertEquals("Bearer token-123", puts().get(1).authorization());
    }

    @Test
    public void refusedTokenSaysWhyAndSendsNothing() {
        configureAuth("token");
        tokenStatus.set(401);

        assertFalse(service.register());

        assertTrue(puts().isEmpty());
        FacilityLocationStatus status = service.getStatus();
        assertEquals(Boolean.FALSE, status.getLastRegistrationSuccess());
        assertTrue(status.getLastRegistrationMessage(), status.getLastRegistrationMessage()
                .contains("FHIR authentication endpoint did not accept the request"));
    }

    @Test
    public void basicAuthenticationSendsTheCredentials() {
        configureAuth("basic");

        assertTrue(service.register());

        assertEquals("Basic cmVnaXN0cnktdXNlcjpwYSJzcw==", puts().get(0).authorization());
    }

    @Test
    public void aFailedRegistrationIsRetriedUntilItSucceeds() {
        configure("", "0.3476", "32.5825");
        registryStatus.set(500);

        service.synchronize();

        FacilityLocationStatus failed = service.getStatus();
        assertEquals(Boolean.FALSE, failed.getLastRegistrationSuccess());
        assertTrue(failed.getLastRegistrationMessage(), failed.getLastRegistrationMessage().contains("HTTP 500"));

        registryStatus.set(200);
        requests.clear();
        service.synchronize();

        assertEquals(Boolean.TRUE, service.getStatus().getLastRegistrationSuccess());
        assertEquals(2, puts().size());

        requests.clear();
        service.synchronize();
        assertTrue("nothing changed, nothing sent again", puts().isEmpty());
    }

    @Test
    public void theOutcomeMessageDoesNotRepeatTheOutcome() {
        configure("", "0.3476", "32.5825");
        service.register();
        assertFalse(service.getStatus().getLastRegistrationMessage(),
                service.getStatus().getLastRegistrationMessage().startsWith("Registered"));

        registryStatus.set(500);
        service.register();
        assertFalse(service.getStatus().getLastRegistrationMessage(),
                service.getStatus().getLastRegistrationMessage().startsWith("Registration"));
    }

    @Test
    public void manualModeUsesOnlyTheProperties() throws Exception {
        configure("false", "-1.2921", "36.8219");

        service.synchronize();

        FacilityLocationStatus status = service.getStatus();
        assertEquals(FacilityRegistryService.MODE_MANUAL, status.getMode());
        assertEquals(FacilityRegistryService.SOURCE_PROPERTIES, status.getSource());
        assertEquals(0, new BigDecimal("-1.2921").compareTo(status.getLatitude()));
        assertTrue("no IP lookup in manual mode", ipLookups().isEmpty());
        assertEquals(-1.2921, JSON.readTree(puts().get(1).body()).at("/position/latitude").asDouble(), 0.0000001);
        try {
            service.recordBrowserLocation(new BigDecimal("1"), new BigDecimal("2"), "1");
            fail("manual mode refuses a browser position");
        } catch (FacilityLocationModeException e) {
            assertTrue(e.getMessage().contains("manual mode"));
        }
    }

    @Test
    public void manualModeWithoutPropertiesRegistersWithoutAPosition() throws Exception {
        configure("", "", "");

        service.synchronize();

        assertNull(service.getStatus().getLatitude());
        assertTrue(JSON.readTree(puts().get(1).body()).at("/position").isMissingNode());
        assertTrue(ipLookups().isEmpty());
    }

    @Test
    public void autoModeTakesTheIpLookupAndIgnoresTheProperties() throws Exception {
        configure("true", "-1.2921", "36.8219");

        service.synchronize();

        FacilityLocationStatus status = service.getStatus();
        assertEquals(FacilityRegistryService.MODE_AUTO, status.getMode());
        assertEquals(FacilityRegistryService.SOURCE_IP, status.getSource());
        assertTrue(status.isManualCoordinatesIgnored());
        assertEquals(0, new BigDecimal("0.3476").compareTo(status.getLatitude()));
        assertEquals(1, ipLookups().size());
        assertEquals(0.3476, JSON.readTree(puts().get(1).body()).at("/position/latitude").asDouble(), 0.0000001);
    }

    @Test
    public void theIpLookupDoesNotUseTheClientLimitedToTheOpenElisTruststore() throws Exception {
        configure("true", "", "");
        ReflectionTestUtils.setField(target, "registryUrl", "");
        ReflectionTestUtils.setField(target, "httpClient",
                HttpClients.custom().addInterceptorFirst((HttpRequestInterceptor) (request, context) -> {
                    throw new IOException("PKIX path building failed");
                }).build());

        service.synchronize();

        assertEquals(1, ipLookups().size());
        assertEquals(FacilityRegistryService.SOURCE_IP, service.getStatus().getSource());
        assertEquals(0, new BigDecimal("0.3476").compareTo(service.getStatus().getLatitude()));
    }

    @Test
    public void theDefaultLookupServiceAnswerWithCoordinatesAsStringsIsRead() throws Exception {
        configure("true", "", "");
        ipResponse = "{\"latitude\": \"0.321\", \"longitude\": \"32.5714\", \"city\": \"Kampala\", \"accuracy\": 20}";

        service.synchronize();

        assertEquals(0, new BigDecimal("0.321").compareTo(service.getStatus().getLatitude()));
        assertEquals(0, new BigDecimal("32.5714").compareTo(service.getStatus().getLongitude()));
    }

    @Test
    public void aRenamedFacilityIsRegisteredAgainUnderItsNewName() throws Exception {
        configure("", "0.3476", "32.5825");
        service.synchronize();
        requests.clear();
        String bannerId = ConfigurationProperties.getInstance().getPropertyValue(Property.BANNER_TEXT);
        Localization renamed = new Localization();
        renamed.setDescription("facility registry rename test");
        renamed.setEnglish("Renamed Reference Laboratory");
        renamed.setFrench("Renamed Reference Laboratory");
        ConfigurationProperties.getInstance().setPropertyValue(Property.BANNER_TEXT,
                localizationService.insert(renamed));
        try {
            service.synchronize();

            assertEquals(2, puts().size());
            assertEquals("Renamed Reference Laboratory", JSON.readTree(puts().get(0).body()).get("name").asText());
            assertEquals("Renamed Reference Laboratory", JSON.readTree(puts().get(1).body()).get("name").asText());
        } finally {
            ConfigurationProperties.getInstance().setPropertyValue(Property.BANNER_TEXT, bannerId);
            facilityOrganizationService.initialize();
        }
    }

    @Test
    public void coordinatesReadBackFromTheDatabaseAreReportedWithoutPaddingZeros() {
        assertEquals("0.4244,33.2042 (browser)",
                new FacilityRegistryServiceImpl.Coordinates(new BigDecimal("0.4244000"), new BigDecimal("33.2042000"),
                        "browser").toString());
    }

    @Test
    public void autoModeBrowserPositionReplacesTheIpOneAndSurvivesARestart() throws Exception {
        configure("true", "", "");
        service.synchronize();
        requests.clear();

        FacilityLocationStatus status = service.recordBrowserLocation(new BigDecimal("0.0512"),
                new BigDecimal("32.4635"), "1");

        assertEquals(FacilityRegistryService.SOURCE_BROWSER, status.getSource());
        assertEquals(0.0512, JSON.readTree(puts().get(1).body()).at("/position/latitude").asDouble(), 0.0000001);

        resetState();
        requests.clear();
        service.synchronize();

        assertTrue("a browser position is not replaced by a new IP lookup", ipLookups().isEmpty());
        assertEquals(FacilityRegistryService.SOURCE_BROWSER, service.getStatus().getSource());
        assertEquals(0.0512, JSON.readTree(puts().get(1).body()).at("/position/latitude").asDouble(), 0.0000001);
    }

    @Test
    public void autoModeWhenTheLookupFailsRegistersWithoutAPosition() throws Exception {
        configure("true", "", "");
        ipResponse = null;

        service.synchronize();

        assertNull(service.getStatus().getLatitude());
        assertEquals(Boolean.TRUE, service.getStatus().getLastRegistrationSuccess());
        assertTrue(JSON.readTree(puts().get(1).body()).at("/position").isMissingNode());
    }

    @Test
    public void aFailedLookupAtStartIsTriedAgainOnTheNextCycle() throws Exception {
        configure("true", "", "");
        ipResponse = null;
        service.synchronize();
        assertNull(service.getStatus().getLatitude());

        ipResponse = "{\"latitude\": 0.3476, \"longitude\": 32.5825}";
        requests.clear();
        service.synchronize();

        assertEquals(1, ipLookups().size());
        assertEquals(FacilityRegistryService.SOURCE_IP, service.getStatus().getSource());
        assertEquals(0.3476, JSON.readTree(puts().get(1).body()).at("/position/latitude").asDouble(), 0.0000001);

        requests.clear();
        service.synchronize();
        assertTrue("a successful lookup is not repeated", ipLookups().isEmpty());
    }

    @Test
    public void switchingModesAndRestartingRegistersTheNewCoordinates() throws Exception {
        configure("true", "", "");
        service.synchronize();
        service.recordBrowserLocation(new BigDecimal("0.0512"), new BigDecimal("32.4635"), "1");

        configure("false", "-1.2921", "36.8219");
        resetState();
        requests.clear();
        service.synchronize();

        assertEquals(FacilityRegistryService.SOURCE_PROPERTIES, service.getStatus().getSource());
        assertEquals(-1.2921, JSON.readTree(puts().get(1).body()).at("/position/latitude").asDouble(), 0.0000001);

        configure("true", "", "");
        resetState();
        requests.clear();
        service.synchronize();

        assertEquals(FacilityRegistryService.SOURCE_IP, service.getStatus().getSource());
        assertEquals(1, ipLookups().size());
        assertEquals(0.3476, JSON.readTree(puts().get(1).body()).at("/position/latitude").asDouble(), 0.0000001);
    }

    @Test
    public void theLocationsScreenKeepsThePositionTheFacilityLocationOwns() {
        configure("true", "", "");
        service.recordBrowserLocation(new BigDecimal("0.0512"), new BigDecimal("32.4635"), "1");
        Organization row = organizationService
                .getOrganizationByShortName(FhirFacilityOrganizationService.FACILITY_ORG_SHORT_NAME, false);
        Detail detail = locationsService.get(row.getId());
        assertTrue(detail.positionManaged());

        OrganizationType type = organizationTypeService.getOrganizationTypeByName("registry test lab");
        if (type == null) {
            type = new OrganizationType();
            type.setName("registry test lab");
            type.setDescription("registry test lab");
            type.setSysUserId("1");
            organizationTypeService.insert(type);
        }
        locationsService.save(
                new SaveRequest(row.getId(), null, detail.row().name(), detail.row().shortName(), List.of(type.getId()),
                        null, null, null, null, null, null, null, null, new BigDecimal("-1.2921"),
                        new BigDecimal("36.8219"), null, null, null, null, null, List.of(), null, null, null, null),
                "1");

        Organization saved = organizationService
                .getOrganizationByShortName(FhirFacilityOrganizationService.FACILITY_ORG_SHORT_NAME, false);
        assertEquals(0, new BigDecimal("0.0512").compareTo(saved.getGpsLatitude()));
        assertEquals(0, new BigDecimal("32.4635").compareTo(saved.getGpsLongitude()));
        assertEquals(FacilityRegistryService.SOURCE_BROWSER, saved.getGpsSource());
    }

    @Test
    public void aBrowserPositionOutOfRangeIsRefused() {
        configure("true", "", "");
        try {
            service.recordBrowserLocation(new BigDecimal("91"), new BigDecimal("10"), "1");
            fail("latitude 91 is refused");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("between -90 and 90"));
        }
    }

    private static void assertFacilityTypes(JsonNode resource) {
        assertEquals(FacilityRegistryService.ROLE_CODE_SYSTEM, resource.at("/type/0/coding/0/system").asText());
        assertEquals("HLAB", resource.at("/type/0/coding/0/code").asText());
        assertEquals(FacilityRegistryService.MCSD_TYPE_SYSTEM, resource.at("/type/1/coding/0/system").asText());
        assertEquals(FacilityRegistryService.MCSD_FACILITY_CODE, resource.at("/type/1/coding/0/code").asText());
    }

    private void configure(String autolocation, String latitude, String longitude) {
        ReflectionTestUtils.setField(target, "autolocation", autolocation);
        ReflectionTestUtils.setField(target, "configuredLatitude", latitude);
        ReflectionTestUtils.setField(target, "configuredLongitude", longitude);
    }

    private void configureAuth(String auth) {
        ReflectionTestUtils.setField(target, "registryAuth", auth);
        ReflectionTestUtils.setField(target, "registryAuthUrl", base + "/auth/token");
        ReflectionTestUtils.setField(target, "registryUsername", "registry-user");
        ReflectionTestUtils.setField(target, "registryPassword", "pa\"ss");
    }

    private void resetState() {
        ReflectionTestUtils.setField(target, "ipLookupDone", false);
        ReflectionTestUtils.setField(target, "registeredFingerprint", null);
        ReflectionTestUtils.setField(target, "lastRegistrationSuccess", null);
        ReflectionTestUtils.setField(target, "lastRegistrationAt", null);
        ReflectionTestUtils.setField(target, "lastRegistrationMessage", null);
    }

    private void clearStoredCoordinates() {
        Organization row = organizationService
                .getOrganizationByShortName(FhirFacilityOrganizationService.FACILITY_ORG_SHORT_NAME, false);
        if (row != null) {
            row.setGpsLatitude(null);
            row.setGpsLongitude(null);
            row.setGpsSource(null);
            row.setSysUserId("1");
            organizationService.update(row);
        }
    }

    private List<Recorded> puts() {
        return requests.stream().filter(r -> "PUT".equals(r.method())).toList();
    }

    private List<Recorded> ipLookups() {
        return requests.stream().filter(r -> "/ip".equals(r.path())).toList();
    }

    private void handle(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String path = exchange.getRequestURI().getPath();
        requests.add(
                new Recorded(exchange.getRequestMethod(), path, exchange.getRequestHeaders().getFirst("Content-Type"),
                        exchange.getRequestHeaders().getFirst("Authorization"), body));
        if (path.endsWith("/metadata")) {
            respond(exchange, 200,
                    "{\"resourceType\":\"CapabilityStatement\",\"status\":\"active\",\"date\":\"2026-01-01\",\"kind\":\"instance\",\"fhirVersion\":\"4.0.1\",\"format\":[\"json\"]}");
        } else if ("/auth/token".equals(path)) {
            respond(exchange, tokenStatus.get(), "{\"access_token\":\"token-123\"}");
        } else if ("/ip".equals(path)) {
            respond(exchange, ipResponse == null ? 503 : 200, ipResponse == null ? "{}" : ipResponse);
        } else {
            respond(exchange, registryStatus.get(), body);
        }
    }

    private static SSLContext receiverTls() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var pair = generator.generateKeyPair();
        X500Name name = new X500Name("CN=localhost");
        Instant now = Instant.now();
        var builder = new JcaX509v3CertificateBuilder(name, BigInteger.valueOf(System.nanoTime()),
                Date.from(now.minusSeconds(60)), Date.from(now.plusSeconds(3600)), name, pair.getPublic());
        builder.addExtension(Extension.subjectAlternativeName, false,
                new GeneralNames(new GeneralName(GeneralName.dNSName, "localhost")));
        X509Certificate certificate = new JcaX509CertificateConverter()
                .getCertificate(builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(pair.getPrivate())));
        char[] password = java.util.UUID.randomUUID().toString().toCharArray();
        KeyStore keys = KeyStore.getInstance("PKCS12");
        keys.load(null, null);
        keys.setKeyEntry("receiver", pair.getPrivate(), password, new X509Certificate[] { certificate });
        KeyManagerFactory km = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        km.init(keys, password);
        KeyStore trust = KeyStore.getInstance("PKCS12");
        trust.load(null, null);
        trust.setCertificateEntry("receiver", certificate);
        TrustManagerFactory tm = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tm.init(trust);
        SSLContext tls = SSLContext.getInstance("TLS");
        tls.init(km.getKeyManagers(), tm.getTrustManagers(), null);
        return tls;
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
