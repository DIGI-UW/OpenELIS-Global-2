package org.openelisglobal.analyzer.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
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

    // ASTM LIS2-A2 Control Characters
    private static final byte ENQ = 0x05; // Enquiry - Start transmission
    private static final byte ACK = 0x06; // Acknowledge - Positive response
    private static final byte NAK = 0x15; // Negative Acknowledge
    private static final byte EOT = 0x04; // End of Transmission
    private static final byte STX = 0x02; // Start of Text (frame start)
    private static final byte ETX = 0x03; // End of Text (frame end)
    private static final byte CR = 0x0D; // Carriage Return
    private static final byte LF = 0x0A; // Line Feed

    private final Map<String, Map<String, Object>> jobStore = new ConcurrentHashMap<>();

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

            List<Map<String, Object>> fields = queryAnalyzerASTM(ipAddress, port, timeoutMs, status);

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
     * Query analyzer via ASTM protocol and return parsed fields
     */
    private List<Map<String, Object>> queryAnalyzerASTM(String ipAddress, Integer port, int timeoutMs,
            Map<String, Object> status) throws IOException {
        Socket socket = null;
        List<Map<String, Object>> fields = new ArrayList<>();

        try {
            socket = new Socket();
            socket.connect(new java.net.InetSocketAddress(ipAddress, port), 5000);
            socket.setSoTimeout(timeoutMs);

            addLog(status, "TCP connection established");
            status.put("progress", 30);

            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            // Phase 1: ENQ/ACK handshake
            addLog(status, "Sending ENQ (Enquiry)");
            out.write(ENQ);
            out.flush();

            int response = in.read();
            if (response != ACK) {
                throw new IOException("Expected ACK (0x06), received: 0x" + String.format("%02X", response & 0xFF));
            }
            addLog(status, "Received ACK (Acknowledge)");
            status.put("progress", 40);

            // Phase 2: Send query message (header only - indicates query)
            addLog(status, "Sending query message (header record)");
            String headerRecord = "H|\\^&|||OpenELIS^Query^1.0|||||||LIS2-A2";
            sendFrame(out, headerRecord, 1);

            // Wait for ACK
            response = in.read();
            if (response != ACK) {
                throw new IOException("Frame not ACKed, received: 0x" + String.format("%02X", response & 0xFF));
            }

            // Send EOT to end transmission
            addLog(status, "Sending EOT (End of Transmission)");
            out.write(EOT);
            out.flush();
            status.put("progress", 50);

            // Phase 3: Wait for server response (server initiates with ENQ)
            addLog(status, "Waiting for server response");
            response = in.read();
            if (response == ENQ) {
                addLog(status, "Received ENQ from server, sending ACK");
                out.write(ACK);
                out.flush();
            } else {
                throw new IOException(
                        "Expected ENQ from server, received: 0x" + String.format("%02X", response & 0xFF));
            }

            // Phase 4: Receive frames from server
            addLog(status, "Receiving field data frames");
            status.put("progress", 60);

            int frameNumber = 1;
            List<String> records = new ArrayList<>();

            while (true) {
                // Read frame: <STX><FN><data><ETX><checksum><CR><LF>
                int stx = in.read();
                if (stx == EOT) {
                    addLog(status, "Received EOT, end of transmission");
                    break;
                }
                if (stx != STX) {
                    throw new IOException("Expected STX (0x02), received: 0x" + String.format("%02X", stx & 0xFF));
                }

                // Read frame number
                int fn = in.read();
                if (fn < '1' || fn > '7') {
                    throw new IOException("Invalid frame number: " + (char) fn);
                }

                // Read until ETX
                StringBuilder frameData = new StringBuilder();
                int b;
                while ((b = in.read()) != ETX) {
                    if (b == -1) {
                        throw new IOException("Unexpected end of stream");
                    }
                    frameData.append((char) b);
                }

                // Read checksum (2 hex digits)
                byte[] checksumBytes = new byte[2];
                in.read(checksumBytes);

                // Read CR/LF
                in.read(); // CR
                in.read(); // LF

                String record = frameData.toString();
                records.add(record);
                addLog(status, String.format("Received frame %d: %s", frameNumber,
                        record.length() > 50 ? record.substring(0, 50) + "..." : record));

                // ACK the frame
                out.write(ACK);
                out.flush();

                frameNumber++;
                if (frameNumber > 7) {
                    frameNumber = 1;
                }
            }

            addLog(status, String.format("Parsing %d records", records.size()));
            status.put("progress", 80);

            fields = parseFieldRecords(records);
            addLog(status, String.format("Extracted %d fields from response", fields.size()));

            logger.info("[QUERY_ASTM] Parsed {} fields from analyzer response", fields.size());
            for (int i = 0; i < fields.size(); i++) {
                Map<String, Object> field = fields.get(i);
                logger.info("[QUERY_ASTM] Parsed field {}: fieldName='{}', astmRef='{}', unit='{}', type='{}'", i + 1,
                        field.get("fieldName"), field.get("astmRef"), field.get("unit"), field.get("fieldType"));
            }

        } finally {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    logger.debug("Error closing socket", e);
                }
            }
        }

        return fields;
    }

    /**
     * Send an ASTM frame
     */
    private void sendFrame(OutputStream out, String content, int frameNumber) throws IOException {
        byte[] frameNumBytes = String.valueOf(frameNumber).getBytes();
        byte[] contentBytes = content.getBytes("UTF-8");

        // Calculate checksum: sum of frame number + content + ETX, mod 256
        int checksum = 0;
        for (byte b : frameNumBytes) {
            checksum += b & 0xFF;
        }
        for (byte b : contentBytes) {
            checksum += b & 0xFF;
        }
        checksum += ETX;
        checksum = checksum % 256;

        String checksumStr = String.format("%02X", checksum);

        // Build frame: <STX><FN><content><ETX><checksum><CR><LF>
        out.write(STX);
        out.write(frameNumBytes);
        out.write(contentBytes);
        out.write(ETX);
        out.write(checksumStr.getBytes());
        out.write(CR);
        out.write(LF);
        out.flush();
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
