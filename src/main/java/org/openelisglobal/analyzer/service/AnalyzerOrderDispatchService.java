package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.ProtocolVersion;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.test.valueholder.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Dispatches an outbound LIS-initiated order to an analyzer via the bridge —
 * analyzer-agnostically. OE2 resolves the accession's ordered tests, reads each
 * test's LOINC ({@link Test#getLoinc()}), and POSTs a LOINC-coded order to the
 * bridge's {@code /api/orders}. The bridge owns everything analyzer-specific:
 * translating LOINC → the analyzer's own code (from the profile mapping it was
 * pushed) and building the ASTM/HL7 wire message.
 *
 * <p>
 * OE2 builds no protocol messages and emits no vendor codes. LOINC is the
 * interlingua — the same one OE2 uses to import external FHIR orders.
 */
@Service
public class AnalyzerOrderDispatchService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzerOrderDispatchService.class);

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 40_000;

    @Value("${analyzer.bridge.url:}")
    private String analyzerBridgeUrl;

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private AnalysisService analysisService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public DispatchResult dispatchOrder(String analyzerId, String accessionNumber) throws IOException {
        if (analyzerBridgeUrl == null || analyzerBridgeUrl.isBlank()) {
            throw new IllegalStateException("Bridge URL not configured (analyzer.bridge.url)");
        }
        if (accessionNumber == null || accessionNumber.isBlank()) {
            throw new IllegalArgumentException("accessionNumber required");
        }

        Analyzer analyzer = analyzerService.get(analyzerId);
        if (analyzer == null) {
            throw new IllegalArgumentException("Analyzer not found: " + analyzerId);
        }
        String host = analyzer.getIpAddress();
        Integer port = analyzer.getPort();
        if (host == null || host.isBlank() || port == null) {
            throw new IllegalStateException(
                    "Analyzer " + analyzerId + " has no IP/port configured — outbound dispatch requires both");
        }

        Sample sample = sampleService.getSampleByAccessionNumber(accessionNumber);
        if (sample == null) {
            throw new IllegalArgumentException("No sample for accession: " + accessionNumber);
        }

        // Resolve the accession's ordered tests → their LOINCs. OE2 sends LOINC;
        // the bridge maps LOINC → the analyzer's code. Preserve order, de-dupe.
        List<String> loincCodes = new ArrayList<>();
        for (Analysis analysis : analysisService.getAnalysesBySampleId(sample.getId())) {
            Test test = analysis.getTest();
            String loinc = test != null ? test.getLoinc() : null;
            if (loinc != null && !loinc.isBlank() && !loincCodes.contains(loinc)) {
                loincCodes.add(loinc);
            }
        }
        if (loincCodes.isEmpty()) {
            throw new IllegalStateException(
                    "No LOINC-coded tests on accession " + accessionNumber + " — cannot build an order");
        }

        ProtocolVersion pv = analyzer.getProtocolVersion();
        String protocol = (pv != null && pv.isHl7()) ? "HL7" : "ASTM";

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("host", host);
        payload.put("port", port);
        payload.put("protocol", protocol);
        payload.put("accessionNumber", accessionNumber);
        payload.put("patientId", resolvePatientId(sample));
        payload.put("loincCodes", loincCodes);

        String endpoint = analyzerBridgeUrl.replaceAll("/+$", "") + "/api/orders";
        logger.info("[ORDER_OUT] analyzer={} accession={} protocol={} loincCodes={} → {}", analyzerId, accessionNumber,
                protocol, loincCodes, endpoint);
        Map<String, Object> bridgeResponse = sendToBridge(endpoint, objectMapper.writeValueAsString(payload),
                "application/json");

        DispatchResult r = new DispatchResult();
        r.protocol = protocol;
        r.loincCodes = loincCodes;
        r.bridgeResponse = bridgeResponse;
        r.success = Boolean.TRUE.equals(bridgeResponse.get("dispatched"));
        if (!r.success) {
            r.error = String.valueOf(bridgeResponse.getOrDefault("error", "Unknown bridge error"));
        }
        return r;
    }

    private String resolvePatientId(Sample sample) {
        // Optional — the analyzer/mock doesn't key on it; accession is the
        // correlation key. Kept best-effort and null-safe.
        try {
            return sample.getId();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * POST to the bridge. Protected so tests can override and avoid real HTTP.
     */
    protected Map<String, Object> sendToBridge(String endpoint, String body, String contentType) throws IOException {
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", contentType);
            conn.setDoOutput(true);
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
            int httpStatus = conn.getResponseCode();
            String respBody = "";
            try (InputStream is = httpStatus < 400 ? conn.getInputStream() : conn.getErrorStream()) {
                if (is != null) {
                    respBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
            if (respBody.isBlank() || !respBody.trim().startsWith("{")) {
                Map<String, Object> stub = new LinkedHashMap<>();
                stub.put("dispatched", httpStatus < 400);
                stub.put("rawResponse", respBody);
                return stub;
            }
            return objectMapper.readValue(respBody, new TypeReference<Map<String, Object>>() {
            });
        } finally {
            conn.disconnect();
        }
    }

    public static class DispatchResult {
        public boolean success;
        public String protocol;
        public String error;
        public List<String> loincCodes;
        public Map<String, Object> bridgeResponse;
    }
}
