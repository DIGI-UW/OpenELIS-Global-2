package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.ProtocolVersion;

/**
 * Unit tests for {@link AnalyzerOrderDispatchService}.
 *
 * <p>
 * Subclasses the service in each test to override {@code sendToBridge} —
 * captures the endpoint URL, body, and content-type the service would have
 * POSTed, without needing a live bridge process.
 */
public class AnalyzerOrderDispatchServiceTest {

    private AnalyzerService analyzerService;
    private HL7MessageService hl7MessageService;
    private ASTMOrderBuilder astmOrderBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private AtomicReference<String> capturedEndpoint;
    private AtomicReference<String> capturedBody;
    private AtomicReference<String> capturedContentType;
    private Map<String, Object> bridgeResponse;

    private AnalyzerOrderDispatchService service;

    @Before
    public void setUp() throws Exception {
        analyzerService = Mockito.mock(AnalyzerService.class);
        hl7MessageService = Mockito.mock(HL7MessageService.class);
        astmOrderBuilder = new ASTMOrderBuilder();
        capturedEndpoint = new AtomicReference<>();
        capturedBody = new AtomicReference<>();
        capturedContentType = new AtomicReference<>();
        bridgeResponse = new LinkedHashMap<>();
        bridgeResponse.put("success", true);

        service = new AnalyzerOrderDispatchService() {
            @Override
            protected Map<String, Object> sendToBridge(String endpoint, String body, String contentType) {
                capturedEndpoint.set(endpoint);
                capturedBody.set(body);
                capturedContentType.set(contentType);
                return bridgeResponse;
            }
        };

        inject(service, "analyzerBridgeUrl", "http://bridge.test:8443");
        inject(service, "analyzerService", analyzerService);
        inject(service, "astmOrderBuilder", astmOrderBuilder);
        inject(service, "hl7MessageService", hl7MessageService);
    }

    @Test
    public void astmAnalyzer_postsForwarderEndpointWithAstmRecords() throws Exception {
        when(analyzerService.get("gx-1")).thenReturn(
                buildAnalyzer(ProtocolVersion.ASTM_LIS2_A2, "analyzer-mock", 9600));

        AnalyzerOrderDispatchService.DispatchResult r =
                service.dispatchOrder("gx-1", "ACC-555", "PAT-9", List.of("MTB-RIF"));

        assertEquals("ASTM", r.protocol);
        assertTrue(r.success);

        // Endpoint targets the bridge's generic forwarder with forwardAddress/Port
        String url = capturedEndpoint.get();
        assertNotNull(url);
        assertTrue("expected bridge root with forwardAddress, got: " + url,
                url.startsWith("http://bridge.test:8443/?"));
        assertTrue(url.contains("forwardAddress=analyzer-mock"));
        assertTrue(url.contains("forwardPort=9600"));
        assertTrue(url.contains("forwardAstmVersion=LIS01_A"));

        // Body is the ASTM record content from ASTMOrderBuilder
        String body = capturedBody.get();
        assertTrue("body should be ASTM records, got: " + body, body.startsWith("H|\\^&|"));
        assertTrue(body.contains("O|1|ACC-555||^^^MTB-RIF|R"));
        assertEquals("text/plain", capturedContentType.get());
    }

    @Test
    public void hl7Analyzer_postsSendHl7WithOrmO01Body() throws Exception {
        when(analyzerService.get("mr-1")).thenReturn(
                buildAnalyzer(ProtocolVersion.HL7_V2_3_1, "analyzer-mock", 5380));
        when(hl7MessageService.generateOrmO01(any()))
                .thenReturn("MSH|...|ORM^O01|CTRL|P|2.3.1\rORC|NW|ACC-77|ACC-77\rOBR|1|ACC-77|ACC-77\r");

        AnalyzerOrderDispatchService.DispatchResult r =
                service.dispatchOrder("mr-1", "ACC-77", "PAT-3", List.of("WBC", "RBC", "HGB", "HCT"));

        assertEquals("HL7", r.protocol);
        assertTrue(r.success);
        assertEquals("http://bridge.test:8443/api/send-hl7", capturedEndpoint.get());
        assertEquals("application/json", capturedContentType.get());

        // Body is a JSON payload with host/port/hl7Message
        Map<String, Object> sent = objectMapper.readValue(capturedBody.get(),
                new TypeReference<Map<String, Object>>() {});
        assertEquals("analyzer-mock", sent.get("host"));
        assertEquals(5380, ((Number) sent.get("port")).intValue());
        assertTrue(((String) sent.get("hl7Message")).startsWith("MSH|"));
    }

    @Test
    public void hl7v25Analyzer_alsoRoutesAsHl7() throws Exception {
        when(analyzerService.get("mr-2")).thenReturn(
                buildAnalyzer(ProtocolVersion.HL7_V2_5, "host", 1234));
        when(hl7MessageService.generateOrmO01(any())).thenReturn("MSH|stub\r");

        AnalyzerOrderDispatchService.DispatchResult r =
                service.dispatchOrder("mr-2", "ACC-1", null, List.of("CBC"));

        assertEquals("HL7", r.protocol);
    }

    @Test
    public void bridgeReturnsFailure_isPropagated() throws Exception {
        when(analyzerService.get("mr-3")).thenReturn(buildAnalyzer(ProtocolVersion.HL7_V2_5, "h", 1));
        when(hl7MessageService.generateOrmO01(any())).thenReturn("MSH|stub\r");
        bridgeResponse.clear();
        bridgeResponse.put("success", false);
        bridgeResponse.put("error", "Connection refused");

        AnalyzerOrderDispatchService.DispatchResult r =
                service.dispatchOrder("mr-3", "ACC-1", null, List.of("X"));

        assertEquals("HL7", r.protocol);
        assertEquals(false, r.success);
        assertEquals("Connection refused", r.error);
    }

    @Test
    public void unknownAnalyzer_throwsIllegalArgument() {
        when(analyzerService.get("nope")).thenReturn(null);
        assertThrows(IllegalArgumentException.class,
                () -> service.dispatchOrder("nope", "ACC-1", null, List.of("X")));
    }

    @Test
    public void missingBridgeUrl_throwsIllegalState() throws Exception {
        inject(service, "analyzerBridgeUrl", "");
        when(analyzerService.get("gx-1")).thenReturn(buildAnalyzer(ProtocolVersion.ASTM_LIS2_A2, "h", 1));
        assertThrows(IllegalStateException.class, () -> service.dispatchOrder("gx-1", "ACC-1", null, List.of("X")));
    }

    @Test
    public void missingAccession_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.dispatchOrder("gx-1", "", null, List.of("X")));
    }

    @Test
    public void emptyTestCodes_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.dispatchOrder("gx-1", "ACC-1", null, List.of()));
    }

    @Test
    public void analyzerMissingIpPort_throwsIllegalState() {
        Analyzer a = new Analyzer();
        a.setProtocolVersion(ProtocolVersion.ASTM_LIS2_A2);
        // ipAddress + port intentionally unset
        when(analyzerService.get("gx-2")).thenReturn(a);
        assertThrows(IllegalStateException.class, () -> service.dispatchOrder("gx-2", "ACC-1", null, List.of("X")));
    }

    @Test
    public void bridgeThrowsIoException_bubbledUp() throws Exception {
        when(analyzerService.get("gx-1")).thenReturn(buildAnalyzer(ProtocolVersion.ASTM_LIS2_A2, "h", 1));
        service = new AnalyzerOrderDispatchService() {
            @Override
            protected Map<String, Object> sendToBridge(String endpoint, String body, String contentType)
                    throws IOException {
                throw new IOException("connection refused");
            }
        };
        inject(service, "analyzerBridgeUrl", "http://bridge.test:8443");
        inject(service, "analyzerService", analyzerService);
        inject(service, "astmOrderBuilder", astmOrderBuilder);
        inject(service, "hl7MessageService", hl7MessageService);

        assertThrows(IOException.class,
                () -> service.dispatchOrder("gx-1", "ACC-1", null, List.of("X")));
    }

    private Analyzer buildAnalyzer(ProtocolVersion protocol, String ip, int port) {
        Analyzer a = new Analyzer();
        a.setProtocolVersion(protocol);
        a.setIpAddress(ip);
        a.setPort(port);
        return a;
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field f = AnalyzerOrderDispatchService.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}
