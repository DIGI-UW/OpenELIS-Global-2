package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.ProtocolVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Dispatches an outbound LIS-initiated order to an analyzer via the bridge.
 *
 * <p>
 * OE2 never opens a direct socket to an analyzer — payload bytes go to the
 * bridge over HTTP, and the bridge owns the wire-level transport (ASTM
 * ENQ/ACK/framing for LIS01-A targets, MLLP for HL7 targets).
 *
 * <p>
 * Protocol selection is driven by {@code Analyzer.protocol_version}: any value
 * starting with {@code HL7} routes through the bridge's {@code /api/send-hl7}
 * endpoint with an ORM^O01 message; anything else is treated as ASTM and POSTed
 * to the bridge's existing generic forwarder with
 * {@code forwardAddress / forwardPort} query parameters.
 *
 * <p>
 * This is the first production caller of
 * {@link HL7MessageService#generateOrmO01} and the first OE2 code path to
 * exercise the
 * {@link org.openelisglobal.analyzer.valueholder.CommunicationMode#LIS_INITIATED}
 * mode end-to-end.
 */
@Service
public class AnalyzerOrderDispatchService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzerOrderDispatchService.class);

    private static final String DEFAULT_PATIENT_ID = "PAT-OE-UNKNOWN";
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 40_000;

    @Value("${analyzer.bridge.url:}")
    private String analyzerBridgeUrl;

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private ASTMOrderBuilder astmOrderBuilder;

    @Autowired
    private HL7MessageService hl7MessageService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public DispatchResult dispatchOrder(String analyzerId, String accessionNumber, String patientId,
            List<String> testCodes) throws IOException {
        if (analyzerBridgeUrl == null || analyzerBridgeUrl.isBlank()) {
            throw new IllegalStateException("Bridge URL not configured (analyzer.bridge.url)");
        }
        if (accessionNumber == null || accessionNumber.isBlank()) {
            throw new IllegalArgumentException("accessionNumber required");
        }
        if (testCodes == null || testCodes.isEmpty()) {
            throw new IllegalArgumentException("at least one test code required");
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

        ProtocolVersion protocolVersion = analyzer.getProtocolVersion();
        if (protocolVersion != null && protocolVersion.isHl7()) {
            return dispatchHl7(analyzerId, host, port, accessionNumber, patientId, testCodes);
        }
        return dispatchAstm(analyzerId, host, port, accessionNumber, patientId, testCodes);
    }

    private DispatchResult dispatchAstm(String analyzerId, String host, Integer port, String accession,
            String patientId, List<String> testCodes) throws IOException {
        String astmBytes = astmOrderBuilder.build(accession, patientId, testCodes);
        String endpoint = analyzerBridgeUrl.replaceAll("/+$", "") + "/?forwardAddress="
                + URLEncoder.encode(host, StandardCharsets.UTF_8) + "&forwardPort=" + port
                + "&forwardAstmVersion=LIS01_A";
        logger.info("[ORDER_OUT] Dispatching ASTM order for analyzer={} accession={} tests={}", analyzerId, accession,
                testCodes);
        Map<String, Object> bridgeResponse = sendToBridge(endpoint, astmBytes, "text/plain");

        DispatchResult r = new DispatchResult();
        r.protocol = "ASTM";
        r.bridgeResponse = bridgeResponse;
        r.success = true; // bridge generic forwarder returns 200 on accepted forward
        return r;
    }

    private DispatchResult dispatchHl7(String analyzerId, String host, Integer port, String accession, String patientId,
            List<String> testCodes) throws IOException {
        String effectivePatientId = patientId == null || patientId.isBlank() ? DEFAULT_PATIENT_ID : patientId;
        List<HL7MessageService.OrmOrderItem> orderItems = testCodes.stream()
                .filter(c -> c != null && !c.isBlank()).<HL7MessageService.OrmOrderItem>map(
                        code -> new HL7MessageService.OrmOrderItem() {
                            @Override
                            public String getTestCode() {
                                return code;
                            }

                            @Override
                            public String getTestName() {
                                return code;
                            }
                        })
                .toList();
        HL7MessageService.OrmO01Request request = new HL7MessageService.OrmO01Request() {
            @Override
            public String getPatientId() {
                return effectivePatientId;
            }

            @Override
            public String getPatientLastName() {
                return "";
            }

            @Override
            public String getPatientFirstName() {
                return "";
            }

            @Override
            public String getPatientDob() {
                return "";
            }

            @Override
            public String getPatientGender() {
                return "U";
            }

            @Override
            public String getPlacerOrderNumber() {
                return accession;
            }

            @Override
            public String getFillerOrderNumber() {
                return accession;
            }

            @Override
            public String getReceivingApplication() {
                return "";
            }

            @Override
            public String getReceivingFacility() {
                return "";
            }

            @Override
            public List<HL7MessageService.OrmOrderItem> getOrders() {
                return orderItems;
            }
        };
        String hl7Message = hl7MessageService.generateOrmO01(request);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("host", host);
        payload.put("port", port);
        payload.put("hl7Message", hl7Message);

        String endpoint = analyzerBridgeUrl.replaceAll("/+$", "") + "/api/send-hl7";
        logger.info("[ORDER_OUT] Dispatching HL7 order for analyzer={} accession={} target={}:{}", analyzerId,
                accession, host, port);
        Map<String, Object> bridgeResponse = sendToBridge(endpoint, objectMapper.writeValueAsString(payload),
                "application/json");

        DispatchResult r = new DispatchResult();
        r.protocol = "HL7";
        r.bridgeResponse = bridgeResponse;
        r.success = Boolean.TRUE.equals(bridgeResponse.get("success"));
        if (!r.success) {
            r.error = String.valueOf(bridgeResponse.getOrDefault("error", "Unknown bridge error"));
        }
        return r;
    }

    /**
     * POST to the bridge. Protected so tests can override and avoid actual HTTP.
     * Returns the parsed JSON body for JSON endpoints, or {{@code {"success":
     * true}}} for plain-text endpoints that return 2xx without a JSON body.
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

            if (httpStatus >= 400) {
                throw new IOException("Bridge returned HTTP " + httpStatus + ": "
                        + (respBody.length() > 500 ? respBody.substring(0, 500) : respBody));
            }

            if (respBody.isBlank() || !respBody.trim().startsWith("{")) {
                Map<String, Object> stub = new LinkedHashMap<>();
                stub.put("success", true);
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
        public Map<String, Object> bridgeResponse;
    }
}
