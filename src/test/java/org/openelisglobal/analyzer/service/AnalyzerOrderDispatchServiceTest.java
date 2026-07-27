package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.CommunicationMode;
import org.openelisglobal.analyzer.valueholder.ProtocolVersion;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;

/**
 * OE2 dispatch is analyzer-agnostic. dispatchOrder(analyzerId, accession)
 * resolves the accession's ordered tests, reads each test's LOINC (Test.loinc),
 * and POSTs a LOINC-coded order to the bridge's /api/orders. OE2 builds NO
 * ASTM/HL7 and emits NO analyzer codes — the bridge owns that.
 *
 * Subclasses the service to capture what it would POST (no live bridge).
 */
public class AnalyzerOrderDispatchServiceTest {

    private org.openelisglobal.analyzer.service.AnalyzerService analyzerService;
    private SampleService sampleService;
    private AnalysisService analysisService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private AtomicReference<String> capturedEndpoint;
    private AtomicReference<String> capturedBody;
    private Map<String, Object> bridgeResponse;

    private AnalyzerOrderDispatchService service;

    @Before
    public void setUp() throws Exception {
        analyzerService = Mockito.mock(org.openelisglobal.analyzer.service.AnalyzerService.class);
        sampleService = Mockito.mock(SampleService.class);
        analysisService = Mockito.mock(AnalysisService.class);
        capturedEndpoint = new AtomicReference<>();
        capturedBody = new AtomicReference<>();
        bridgeResponse = new LinkedHashMap<>();
        bridgeResponse.put("dispatched", true);
        bridgeResponse.put("protocol", "HL7");

        service = new AnalyzerOrderDispatchService() {
            @Override
            protected Map<String, Object> sendToBridge(String endpoint, String body) {
                capturedEndpoint.set(endpoint);
                capturedBody.set(body);
                return bridgeResponse;
            }
        };
        inject("analyzerBridgeUrl", "http://bridge.test:8443");
        inject("analyzerService", analyzerService);
        inject("sampleService", sampleService);
        inject("analysisService", analysisService);
    }

    private void inject(String field, Object value) throws Exception {
        Field f = AnalyzerOrderDispatchService.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(service, value);
    }

    private Analyzer analyzer(ProtocolVersion pv, String ip, int port) {
        Analyzer a = new Analyzer();
        a.setProtocolVersion(pv);
        a.setIpAddress(ip);
        a.setPort(port);
        // Dispatchable by default — the service now rejects non-LIS-initiated
        // analyzers.
        a.setCommunicationMode(CommunicationMode.BOTH);
        return a;
    }

    private Analysis analysisWithLoinc(String loinc) {
        Analysis a = Mockito.mock(Analysis.class);
        org.openelisglobal.test.valueholder.Test t = Mockito.mock(org.openelisglobal.test.valueholder.Test.class);
        when(t.getLoinc()).thenReturn(loinc);
        when(a.getTest()).thenReturn(t);
        return a;
    }

    private void seedAccession(String accession, String sampleId, Analysis... analyses) {
        Sample s = Mockito.mock(Sample.class);
        when(s.getId()).thenReturn(sampleId);
        when(sampleService.getSampleByAccessionNumber(accession)).thenReturn(s);
        when(analysisService.getAnalysesBySampleId(sampleId)).thenReturn(List.of(analyses));
    }

    @Test
    public void resolvesAccessionTestsToLoincAndPostsOrdersEndpoint() throws Exception {
        when(analyzerService.get("mr-1")).thenReturn(analyzer(ProtocolVersion.HL7_V2_3_1, "10.0.0.5", 5380));
        seedAccession("ACC-1", "S1", analysisWithLoinc("6690-2"), analysisWithLoinc("718-7"));

        AnalyzerOrderDispatchService.DispatchResult r = service.dispatchOrder("mr-1", "ACC-1");

        assertTrue(r.success);
        assertEquals("http://bridge.test:8443/api/orders", capturedEndpoint.get());
        Map<String, Object> body = objectMapper.readValue(capturedBody.get(),
                new TypeReference<Map<String, Object>>() {});
        assertEquals("10.0.0.5", body.get("host"));
        assertEquals(5380, ((Number) body.get("port")).intValue());
        assertEquals("HL7", body.get("protocol"));
        assertEquals("ACC-1", body.get("accessionNumber"));
        // The crux: OE2 sends LOINCs, never analyzer codes.
        assertEquals(List.of("6690-2", "718-7"), body.get("loincCodes"));
    }

    @Test
    public void astmAnalyzer_sendsProtocolAstm_stillOnlyLoinc() throws Exception {
        when(analyzerService.get("gx-1")).thenReturn(analyzer(ProtocolVersion.ASTM_LIS2_A2, "10.0.0.6", 9600));
        seedAccession("ACC-2", "S2", analysisWithLoinc("85362-2"));

        service.dispatchOrder("gx-1", "ACC-2");

        Map<String, Object> body = objectMapper.readValue(capturedBody.get(),
                new TypeReference<Map<String, Object>>() {});
        assertEquals("ASTM", body.get("protocol"));
        assertEquals(List.of("85362-2"), body.get("loincCodes"));
    }

    @Test
    public void blankLoincsAreSkipped() throws Exception {
        when(analyzerService.get("mr-1")).thenReturn(analyzer(ProtocolVersion.HL7_V2_3_1, "h", 1));
        seedAccession("ACC-3", "S3", analysisWithLoinc("6690-2"), analysisWithLoinc(null), analysisWithLoinc(""));

        service.dispatchOrder("mr-1", "ACC-3");

        Map<String, Object> body = objectMapper.readValue(capturedBody.get(),
                new TypeReference<Map<String, Object>>() {});
        assertEquals(List.of("6690-2"), body.get("loincCodes"));
    }

    @Test
    public void unknownAnalyzer_throwsIllegalArgument() {
        when(analyzerService.get("nope")).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> service.dispatchOrder("nope", "ACC-1"));
    }

    @Test
    public void unknownAccession_throwsIllegalArgument() {
        when(analyzerService.get("mr-1")).thenReturn(analyzer(ProtocolVersion.HL7_V2_3_1, "h", 1));
        when(sampleService.getSampleByAccessionNumber("NOPE")).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> service.dispatchOrder("mr-1", "NOPE"));
    }

    @Test
    public void noLoincCodedTests_throwsIllegalState() {
        when(analyzerService.get("mr-1")).thenReturn(analyzer(ProtocolVersion.HL7_V2_3_1, "h", 1));
        seedAccession("ACC-4", "S4", analysisWithLoinc(null));
        assertThrows(IllegalStateException.class, () -> service.dispatchOrder("mr-1", "ACC-4"));
    }

    @Test
    public void analyzerMissingIpPort_throwsIllegalState() {
        Analyzer a = new Analyzer();
        a.setProtocolVersion(ProtocolVersion.HL7_V2_3_1);
        a.setCommunicationMode(CommunicationMode.BOTH); // dispatchable, so we reach the IP/port check
        when(analyzerService.get("mr-2")).thenReturn(a);
        assertThrows(IllegalStateException.class, () -> service.dispatchOrder("mr-2", "ACC-1"));
    }

    @Test
    public void pushOnlyAnalyzer_throwsIllegalState() {
        // ANALYZER_INITIATED (push-only) must not be dispatchable, even via a direct
        // service call that bypasses the UI's dropdown filter.
        Analyzer a = analyzer(ProtocolVersion.HL7_V2_3_1, "10.0.0.9", 5380);
        a.setCommunicationMode(CommunicationMode.ANALYZER_INITIATED);
        when(analyzerService.get("push-1")).thenReturn(a);
        assertThrows(IllegalStateException.class, () -> service.dispatchOrder("push-1", "ACC-1"));
    }
}
