package org.openelisglobal.analyzer.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.openelisglobal.analyzer.util.NetworkValidationUtil;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerField;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Service implementation for querying analyzers via ASTM protocol.
 *
 * <p>
 * Implements asynchronous query workflow per FR-002: background job pattern
 * with job ID, TCP connection, ASTM LIS2-A2 protocol, response parsing, and
 * field storage. Not @Transactional at class level because methods are
 * primarily in-memory job management; DB operations use TransactionTemplate
 * explicitly.
 */
@Service
public class AnalyzerQueryServiceImpl implements AnalyzerQueryService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzerQueryServiceImpl.class);

    private final Map<String, Map<String, Object>> jobStore = new ConcurrentHashMap<>();

    @org.springframework.beans.factory.annotation.Value("${analyzer.bridge.url:}")
    private String analyzerBridgeUrl;

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private AnalyzerFieldService analyzerFieldService;

    @Autowired
    private FileImportService fileImportService;

    @Autowired
    private SerialPortService serialPortService;

    @Autowired
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

    @Override
    public String startQuery(String analyzerId) {
        if (analyzerId == null || analyzerId.trim().isEmpty()) {
            throw new LIMSRuntimeException("Analyzer ID required");
        }

        // Verify the analyzer exists and is queryable (has TCP/IP configuration).
        // Push-only analyzers (file-based, serial/RS-232) cannot be actively queried
        // because they deliver results to OpenELIS rather than accepting inbound
        // requests.
        Analyzer analyzer = null;
        try {
            analyzer = analyzerService.get(analyzerId);
        } catch (Exception e) {
            logger.debug("Analyzer {} not found in database, skipping transport validation", analyzerId);
        }
        if (analyzer != null) {
            // Block push-only transports — derive from config entities, not
            // protocolVersion (which tracks message format, not transport).
            try {
                Integer analyzerIdInt = Integer.valueOf(analyzerId);
                if (fileImportService.getByAnalyzerId(analyzerIdInt).isPresent()) {
                    throw new LIMSRuntimeException(
                            "Analyzer uses a push-only transport (file import) and cannot be queried");
                }
                if (serialPortService.getByAnalyzerId(analyzerIdInt).isPresent()) {
                    throw new LIMSRuntimeException(
                            "Analyzer uses a push-only transport (RS-232 serial) and cannot be queried");
                }
            } catch (NumberFormatException e) {
                logger.warn("Analyzer ID [{}] is not numeric; unable to perform transport validation", analyzerId, e);
                throw new LIMSRuntimeException("Analyzer ID must be numeric for transport validation", e);
            }
            if (analyzer.getIpAddress() == null || analyzer.getPort() == null) {
                throw new LIMSRuntimeException("Analyzer has no TCP/IP connection details configured");
            }
        }

        String jobId = UUID.randomUUID().toString();

        Map<String, Object> status = new HashMap<>();
        status.put("analyzerId", analyzerId);
        status.put("jobId", jobId);
        status.put("createdAt", Instant.now().toString());
        status.put("state", "pending");
        status.put("progress", 0);
        status.put("logs", new ArrayList<String>());
        status.put("fieldsCount", 0); // Just track count, not the data
        status.put("error", null);

        jobStore.put(jobKey(analyzerId, jobId), status);

        executorService.submit(() -> executeQuery(analyzerId, jobId));

        return jobId;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getStatus(String analyzerId, String jobId) {
        Map<String, Object> status = jobStore.get(jobKey(analyzerId, jobId));
        if (status == null) {
            Map<String, Object> notFound = new HashMap<>();
            notFound.put("analyzerId", analyzerId);
            notFound.put("jobId", jobId);
            notFound.put("state", "not_found");
            notFound.put("progress", 0);
            return notFound;
        }
        Map<String, Object> response = new HashMap<>(status); // Return copy to prevent modification

        // If job is completed, load fields from database (single source of truth)
        if ("completed".equals(status.get("state"))) {
            List<org.openelisglobal.analyzer.valueholder.AnalyzerField> savedFields = analyzerFieldService
                    .getFieldsByAnalyzerId(analyzerId);

            List<Map<String, Object>> fieldsList = new ArrayList<>();
            for (org.openelisglobal.analyzer.valueholder.AnalyzerField field : savedFields) {
                Map<String, Object> fieldMap = new HashMap<>();
                fieldMap.put("id", field.getId());
                fieldMap.put("fieldName", field.getFieldName());
                fieldMap.put("astmRef", field.getAstmRef());
                fieldMap.put("fieldType", field.getFieldType() != null ? field.getFieldType().toString() : null);
                fieldMap.put("unit", field.getUnit());
                fieldMap.put("isActive", field.getIsActive());
                fieldsList.add(fieldMap);
            }
            response.put("fields", fieldsList);

            logger.info("[QUERY_STATUS] Job completed, returning {} fields from database for analyzer {} job {}",
                    fieldsList.size(), analyzerId, jobId);
        } else {
            // Job not completed yet - no fields to return
            response.put("fields", new ArrayList<>());
        }

        return response;
    }

    @Override
    public void cancel(String analyzerId, String jobId) {
        Map<String, Object> status = jobStore.get(jobKey(analyzerId, jobId));
        if (status != null && !"completed".equals(status.get("state")) && !"failed".equals(status.get("state"))) {
            status.put("state", "cancelled");
            addLog(status, "Query cancelled by user");
        }
    }

    /**
     * Execute the query job in background thread
     */
    private void executeQuery(String analyzerId, String jobId) {
        Map<String, Object> status = jobStore.get(jobKey(analyzerId, jobId));
        if (status == null) {
            return;
        }

        try {
            status.put("state", "in_progress");
            status.put("progress", 10);
            addLog(status, "Starting query job");

            Analyzer analyzer = analyzerService.get(analyzerId);
            if (analyzer == null) {
                throw new LIMSRuntimeException("Analyzer not found: " + analyzerId);
            }

            String ipAddress = analyzer.getIpAddress();
            Integer port = analyzer.getPort();

            if (ipAddress == null || port == null) {
                throw new LIMSRuntimeException("Analyzer IP address or port not configured");
            }
            if (NetworkValidationUtil.isBlockedAddress(ipAddress)) {
                throw new LIMSRuntimeException("Connection to this address is not permitted");
            }

            addLog(status, String.format("Connecting to analyzer at %s:%d", ipAddress, port));
            status.put("progress", 20);

            int timeoutMinutes = getQueryTimeout();
            int timeoutMs = timeoutMinutes * 60 * 1000;

            List<Map<String, Object>> fields = queryViaBridge(ipAddress, port, timeoutMs, status);

            // Store fields directly in database (single source of truth)
            addLog(status, String.format("Storing %d fields in database", fields.size()));
            status.put("progress", 90);

            logger.info("[STORE_FIELDS] About to store {} fields for analyzer {}", fields.size(), analyzerId);
            for (int i = 0; i < fields.size(); i++) {
                Map<String, Object> field = fields.get(i);
                logger.info("[STORE_FIELDS] Field {} before store: fieldName='{}', astmRef='{}', unit='{}', type='{}'",
                        i + 1, field.get("fieldName"), field.get("astmRef"), field.get("unit"), field.get("fieldType"));
            }

            // Store fields in a new transaction (background thread needs explicit
            // transaction)
            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
            int fieldsStored = transactionTemplate.execute(transactionStatus -> {
                return storeFields(analyzerId, fields);
            });

            logger.info("[STORE_FIELDS] Stored {} fields to database for analyzer {}", fieldsStored, analyzerId);

            // Update job status - don't store fields in jobStore, they're in the database
            status.put("state", "completed");
            status.put("progress", 100);
            status.put("fieldsCount", fieldsStored); // Just store count, not the data
            addLog(status, String.format("Query completed successfully. %d fields saved to database.", fieldsStored));

        } catch (Exception e) {
            logger.error("Error executing query for analyzer: " + analyzerId, e);
            status.put("state", "failed");
            status.put("error", e.getMessage());
            addLog(status, "Query failed: " + e.getMessage());
        }
    }

    /**
     * Query analyzer via the bridge's /api/query endpoint. The bridge handles the
     * ASTM ENQ/ACK/frame exchange — OE never opens direct sockets to analyzers.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> queryViaBridge(String ipAddress, Integer port, int timeoutMs,
            Map<String, Object> status) throws IOException {

        if (analyzerBridgeUrl == null || analyzerBridgeUrl.isBlank()) {
            throw new IOException("Bridge URL not configured (analyzer.bridge.url) — cannot query analyzer");
        }

        String endpoint = analyzerBridgeUrl.replaceAll("/+$", "") + "/api/query";
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("host", ipAddress);
        payload.put("port", port);
        payload.put("timeoutMs", timeoutMs);
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IOException("Failed to build query request JSON", e);
        }

        addLog(status, "Sending query via bridge: " + endpoint);
        status.put("progress", 30);

        try {
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            if (conn instanceof javax.net.ssl.HttpsURLConnection) {
                javax.net.ssl.HttpsURLConnection httpsConn = (javax.net.ssl.HttpsURLConnection) conn;
                javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
                sslContext.init(null, new javax.net.ssl.TrustManager[] { new javax.net.ssl.X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[0];
                    }

                    public void checkClientTrusted(java.security.cert.X509Certificate[] c, String s) {
                    }

                    public void checkServerTrusted(java.security.cert.X509Certificate[] c, String s) {
                    }
                } }, new java.security.SecureRandom());
                httpsConn.setSSLSocketFactory(sslContext.getSocketFactory());
                httpsConn.setHostnameVerifier((hostname, session) -> true);
            }
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(timeoutMs + 10000); // bridge timeout + buffer

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            int httpStatus = conn.getResponseCode();
            String body = "";
            try (InputStream is = (httpStatus < 400) ? conn.getInputStream() : conn.getErrorStream()) {
                if (is != null) {
                    body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            }

            status.put("progress", 60);

            if (httpStatus != 200) {
                throw new IOException("Bridge query returned HTTP " + httpStatus + ": " + body);
            }

            Map<String, Object> bridgeResponse = objectMapper.readValue(body,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                    });

            Boolean success = (Boolean) bridgeResponse.get("success");
            if (!Boolean.TRUE.equals(success)) {
                String error = (String) bridgeResponse.getOrDefault("error", "Unknown bridge error");
                throw new IOException("Bridge query failed: " + error);
            }

            // Forward bridge logs to job status
            Object logsObj = bridgeResponse.get("logs");
            if (logsObj instanceof List) {
                for (Object log : (List<Object>) logsObj) {
                    addLog(status, "[bridge] " + log);
                }
            }

            // Parse records from bridge response
            List<String> records = new ArrayList<>();
            Object recordsObj = bridgeResponse.get("records");
            if (recordsObj instanceof List) {
                for (Object r : (List<Object>) recordsObj) {
                    records.add(String.valueOf(r));
                }
            }

            addLog(status, String.format("Parsing %d records from bridge response", records.size()));
            status.put("progress", 80);

            List<Map<String, Object>> fields = parseFieldRecords(records);
            addLog(status, String.format("Extracted %d fields", fields.size()));

            logger.info("[QUERY] Parsed {} fields via bridge for {}:{}", fields.size(), ipAddress, port);
            return fields;

        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Bridge query error: " + e.getMessage(), e);
        }
    }

    /**
     * Parse ASTM records to extract field information
     * 
     * ASTM R (Result) record format per LIS2-A2 specification:
     * R|sequence|test_id^test_name|value|units|reference_range|abnormal_flags|status|...
     * 
     * For query responses (no values), format:
     * R|seq|test_id^test_name||units|||field_type
     * 
     * Example: R|1|^^^WBC^White Blood Cell Count||10^3/μL|||NUMERIC Split by |:
     * ["R", "1", "^^^WBC^White Blood Cell Count", "", "10^3/μL", "", "", "NUMERIC"]
     * 
     * Field indices: - 0: "R" (segment type) - 1: sequence number - 2:
     * test_id^test_name (composite field with ^ delimiter) - 3: value (empty for
     * query responses) - 4: units - 5: reference_range (empty for query responses)
     * - 6: abnormal_flags (empty for query responses) - 7: field_type (NUMERIC,
     * QUALITATIVE, etc.)
     */
    private List<Map<String, Object>> parseFieldRecords(List<String> records) {
        List<Map<String, Object>> fields = new ArrayList<>();

        // Constants matching ASTMQSegmentParserImpl pattern
        final String FIELD_DELIMITER = "|";
        final String COMPOSITE_DELIMITER = "^";
        final String R_SEGMENT_PREFIX = "R|";

        for (String record : records) {
            if (record == null || !record.startsWith(R_SEGMENT_PREFIX)) {
                continue;
            }

            // Split by field delimiter (same pattern as ASTMQSegmentParserImpl)
            String[] fields_array = record.split("\\" + FIELD_DELIMITER, -1);

            if (fields_array.length < 4) {
                logger.warn("R record too short, skipping: {}", record);
                continue;
            }

            try {
                Map<String, Object> field = new HashMap<>();

                // Field 0: R (segment type) - already validated
                // Field 1: sequence number
                String sequence = fields_array.length > 1 ? fields_array[1] : "";

                // Field 2: test_id^test_name (composite field)
                String testIdField = fields_array.length > 2 ? fields_array[2] : "";
                String astmRef = testIdField;

                logger.debug("Parsing R record - sequence: {}, testIdField: '{}', full record: {}", sequence,
                        testIdField, record);

                // Extract field name from test_id^test_name
                // Format: ^^^WBC or ^^^WBC^White Blood Cell Count
                // Per ASTM spec: test_id is typically the 4th component (index 3), test_name is
                // optional 5th component
                // We want the test_id (WBC) not the test_name (White Blood Cell Count)
                String fieldName = "";
                if (testIdField.contains(COMPOSITE_DELIMITER)) {
                    String[] components = testIdField.split("\\" + COMPOSITE_DELIMITER, -1);
                    logger.debug("Split testIdField into {} components: {}", components.length,
                            java.util.Arrays.toString(components));
                    // ASTM format: ^^^WBC^White Blood Cell Count
                    // components[0] = "" (empty)
                    // components[1] = "" (empty)
                    // components[2] = "" (empty)
                    // components[3] = "WBC" (test_id - this is what we want)
                    // components[4] = "White Blood Cell Count" (test_name - optional)
                    // Find first non-empty component after the leading empty ones (typically index
                    // 3)
                    for (int i = 0; i < components.length; i++) {
                        if (components[i] != null && !components[i].trim().isEmpty()) {
                            fieldName = components[i].trim();
                            logger.debug("Extracted fieldName '{}' from component index {}", fieldName, i);
                            break; // Take first non-empty (the test_id)
                        }
                    }
                } else if (!testIdField.trim().isEmpty()) {
                    fieldName = testIdField.trim();
                    logger.debug("No composite delimiter, using testIdField as fieldName: '{}'", fieldName);
                }

                // Fallback: use sequence number if field name not found
                if (fieldName.isEmpty()) {
                    fieldName = "Field_" + sequence;
                    logger.warn("Could not extract field name from test_id field, using fallback: {}", record);
                }

                logger.info(
                        "[PARSE_FIELD] Extracted field - sequence: {}, fieldName: '{}', astmRef: '{}', unit: '{}', type: '{}'",
                        sequence, fieldName, astmRef, fields_array.length > 4 ? fields_array[4] : "",
                        fields_array.length > 7 ? fields_array[7] : "");
                logger.debug("[PARSE_FIELD] Full record: {}", record);
                logger.debug("[PARSE_FIELD] fields_array[2] (testIdField): '{}'", testIdField);
                logger.debug("[PARSE_FIELD] Split components: {}",
                        java.util.Arrays.toString(testIdField.split("\\^", -1)));

                // Field 4: units (may be empty for qualitative fields)
                String unit = "";
                if (fields_array.length > 4 && fields_array[4] != null && !fields_array[4].trim().isEmpty()) {
                    unit = fields_array[4].trim();
                }

                // Field 7: field_type (NUMERIC, QUALITATIVE, etc.)
                // For query responses, field_type is at index 7
                String fieldType = "NUMERIC"; // Default
                if (fields_array.length > 7 && fields_array[7] != null && !fields_array[7].trim().isEmpty()) {
                    String candidate = fields_array[7].trim().toUpperCase();
                    try {
                        AnalyzerField.FieldType.valueOf(candidate);
                        fieldType = candidate;
                    } catch (IllegalArgumentException e) {
                        logger.warn("Invalid field type '{}' in record, using default NUMERIC: {}", candidate, record);
                    }
                }

                // If unit is empty and no explicit type found, infer QUALITATIVE
                if (unit.isEmpty() && fieldType.equals("NUMERIC")) {
                    fieldType = "QUALITATIVE";
                }

                field.put("fieldName", fieldName);
                field.put("astmRef", astmRef);
                field.put("fieldType", fieldType);
                field.put("unit", unit);

                fields.add(field);

            } catch (Exception e) {
                logger.warn("Error parsing R record: {}", record, e);
            }
        }

        return fields;
    }

    /**
     * Store parsed fields in database
     * 
     * @return Number of fields successfully stored
     */
    private int storeFields(String analyzerId, List<Map<String, Object>> fields) {
        List<AnalyzerField> existingFields = analyzerFieldService.getFieldsByAnalyzerId(analyzerId);
        Map<String, AnalyzerField> existingByAstmRef = new HashMap<>();
        for (AnalyzerField existing : existingFields) {
            if (existing.getAstmRef() != null) {
                existingByAstmRef.put(existing.getAstmRef(), existing);
            }
        }

        Analyzer analyzer = analyzerService.get(analyzerId);

        int storedCount = 0;
        for (Map<String, Object> fieldData : fields) {
            String astmRef = (String) fieldData.get("astmRef");

            if (existingByAstmRef.containsKey(astmRef)) {
                continue;
            }

            AnalyzerField field = new AnalyzerField();

            // CRITICAL: Entity uses "assigned" ID strategy - must set ID manually before
            // persist
            // @PrePersist runs too late - Hibernate checks for ID before @PrePersist
            // executes
            String fieldId = java.util.UUID.randomUUID().toString();
            field.setId(fieldId);

            field.setAnalyzer(analyzer);

            String fieldNameValue = (String) fieldData.get("fieldName");
            if (fieldNameValue == null || fieldNameValue.trim().isEmpty()) {
                logger.error("[STORE_FIELD] CRITICAL: fieldName is null or empty in fieldData! fieldData={}",
                        fieldData);
                continue; // Skip this field
            }
            field.setFieldName(fieldNameValue.trim());
            field.setAstmRef(astmRef);

            String fieldTypeStr = (String) fieldData.get("fieldType");
            if (fieldTypeStr == null || fieldTypeStr.trim().isEmpty()) {
                logger.error("[STORE_FIELD] CRITICAL: fieldType is null or empty in fieldData! fieldData={}",
                        fieldData);
                continue; // Skip this field
            }
            AnalyzerField.FieldType fieldType = AnalyzerField.FieldType.valueOf(fieldTypeStr.trim());
            field.setFieldType(fieldType);

            String unit = (String) fieldData.get("unit");
            if (unit != null && !unit.isEmpty()) {
                field.setUnit(unit.trim());
            }

            field.setIsActive(true);
            field.setSysUserId("1");

            logger.info(
                    "[STORE_FIELD] Creating field: id={}, fieldName='{}', astmRef='{}', unit='{}', type='{}', analyzerId={}",
                    field.getId(), field.getFieldName(), field.getAstmRef(), field.getUnit(), field.getFieldType(),
                    analyzerId);

            if (field.getFieldName() == null || field.getFieldName().trim().isEmpty()) {
                logger.error("[STORE_FIELD] CRITICAL: fieldName is null after setting! field={}", field);
                continue;
            }
            if (field.getFieldType() == null) {
                logger.error("[STORE_FIELD] CRITICAL: fieldType is null after setting! field={}", field);
                continue;
            }
            if (field.getAnalyzer() == null) {
                logger.error("[STORE_FIELD] CRITICAL: analyzer is null! analyzerId={}", analyzerId);
                continue;
            }

            try {
                // Validate field type and unit compatibility (same validation as createField)
                // NUMERIC fields must have unit, non-NUMERIC fields must not have unit
                if (field.getFieldType() == AnalyzerField.FieldType.NUMERIC) {
                    if (field.getUnit() == null || field.getUnit().trim().isEmpty()) {
                        logger.error("[STORE_FIELD] NUMERIC field requires unit: fieldName='{}'", field.getFieldName());
                        continue; // Skip this field
                    }
                } else {
                    // QUALITATIVE, TEXT, etc. must not have unit
                    if (field.getUnit() != null && !field.getUnit().trim().isEmpty()) {
                        logger.error("[STORE_FIELD] Non-NUMERIC field must not have unit: fieldName='{}', type='{}'",
                                field.getFieldName(), field.getFieldType());
                        continue; // Skip this field
                    }
                }

                // Use insert() directly (same as integration tests) - this is what actually
                // persists
                String createdId = analyzerFieldService.insert(field);
                storedCount++;
                logger.info("[STORE_FIELD] Successfully created field: id={}, fieldName='{}'", createdId,
                        field.getFieldName());
            } catch (Exception e) {
                logger.error(
                        "[STORE_FIELD] Failed to store field: id={}, fieldName='{}', astmRef='{}', unit='{}', type='{}', analyzerId={}",
                        field.getId(), field.getFieldName(), field.getAstmRef(), field.getUnit(), field.getFieldType(),
                        analyzerId, e);
                // Log full stack trace for debugging
                logger.error("[STORE_FIELD] Exception type: {}, message: {}", e.getClass().getName(), e.getMessage());
                if (e.getCause() != null) {
                    logger.error("[STORE_FIELD] Caused by: {} - {}", e.getCause().getClass().getName(),
                            e.getCause().getMessage());
                }
            }
        }

        return storedCount;
    }

    /**
     * Get query timeout from SystemConfiguration (default: 5 minutes)
     */
    private int getQueryTimeout() {
        try {
            ConfigurationProperties config = ConfigurationProperties.getInstance();
            String timeoutStr = config.getPropertyValue("analyzer.query.timeout.minutes");
            if (timeoutStr != null && !timeoutStr.trim().isEmpty()) {
                int timeout = Integer.parseInt(timeoutStr.trim());
                if (timeout > 0) {
                    return timeout;
                }
            }
        } catch (Exception e) {
            logger.debug("Error reading query timeout from SystemConfiguration, using default", e);
        }
        return 5; // Default: 5 minutes
    }

    /**
     * Add log entry to job status
     */
    @SuppressWarnings("unchecked")
    private void addLog(Map<String, Object> status, String message) {
        String timestamp = java.time.LocalTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        String logEntry = String.format("[%s] %s", timestamp, message);

        List<String> logs = (List<String>) status.get("logs");
        if (logs != null) {
            logs.add(logEntry);
        }

        logger.info("Query job {}: {}", status.get("jobId"), message);
    }

    private String jobKey(String analyzerId, String jobId) {
        return analyzerId + "::" + jobId;
    }
}
