package org.openelisglobal.analyzer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.analyzer.form.AnalyzerForm;
import org.openelisglobal.analyzer.service.AnalyzerConfigurationService;
import org.openelisglobal.analyzer.service.AnalyzerFieldService;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.service.FileImportService;
import org.openelisglobal.analyzer.service.SerialPortService;
import org.openelisglobal.analyzer.util.NetworkValidationUtil;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerConfiguration;
import org.openelisglobal.analyzer.valueholder.FileImportConfiguration;
import org.openelisglobal.analyzer.valueholder.SerialPortConfiguration;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.rest.BaseRestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Analyzer management Handles CRUD operations for analyzers
 * and analyzer configurations
 */
@RestController
@RequestMapping("/rest/analyzer")
public class AnalyzerRestController extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzerRestController.class);

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private AnalyzerConfigurationService analyzerConfigurationService;

    @Autowired
    private AnalyzerFieldService analyzerFieldService;

    @Autowired
    private FileImportService fileImportService;

    @Autowired
    private SerialPortService serialPortService;

    @Autowired
    private org.openelisglobal.analyzer.service.AnalyzerQueryService analyzerQueryService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** ASTM LIS2-A2 Enquiry — initiates transmission. */
    private static final byte ENQ = 0x05;

    /** ASTM LIS2-A2 Acknowledge — positive response. */
    private static final byte ACK = 0x06;

    /**
     * GET /rest/analyzer/analyzers Retrieve all analyzers with their configurations
     */
    @GetMapping("/analyzers")
    public ResponseEntity<List<Map<String, Object>>> getAnalyzers(@RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        try {
            List<Analyzer> analyzers = analyzerService.getAll();
            List<Map<String, Object>> response = new ArrayList<>();

            for (Analyzer analyzer : analyzers) {
                Map<String, Object> analyzerMap = analyzerToMap(analyzer);

                // Skip DELETED analyzers (soft-deleted with 90-day window)
                String analyzerStatus = (String) analyzerMap.get("status");
                if ("DELETED".equals(analyzerStatus)) {
                    continue;
                }

                // Apply search filter
                if (search != null && !search.isEmpty()) {
                    String searchLower = search.toLowerCase();
                    if (!analyzer.getName().toLowerCase().contains(searchLower) && (analyzer.getType() == null
                            || !analyzer.getType().toLowerCase().contains(searchLower))) {
                        continue;
                    }
                }

                // Apply lifecycle status filter (SETUP, ACTIVE, INACTIVE, DELETED)
                if (status != null && !status.isEmpty()) {
                    if (analyzerStatus == null || !analyzerStatus.equalsIgnoreCase(status)) {
                        continue;
                    }
                }

                response.add(analyzerMap);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error retrieving analyzers", e);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }

    /**
     * POST /rest/analyzer/analyzers Create new analyzer with configuration
     */
    @PostMapping("/analyzers")
    public ResponseEntity<Map<String, Object>> createAnalyzer(@RequestBody AnalyzerForm form,
            HttpServletRequest request) {
        try {
            // Collect all validation errors instead of failing on the first one
            List<String> validationErrors = new ArrayList<>();
            if (form.getName() == null || form.getName().trim().isEmpty()) {
                validationErrors.add("Analyzer name is required");
            }
            if (form.getAnalyzerType() == null || form.getAnalyzerType().trim().isEmpty()) {
                validationErrors.add("Analyzer type is required");
            }
            if (form.getIpAddress() != null && !form.getIpAddress().matches("^(\\d{1,3}\\.){3}\\d{1,3}$")) {
                validationErrors.add("Invalid IPv4 address format");
            }
            if (form.getIpAddress() != null && NetworkValidationUtil.isBlockedAddress(form.getIpAddress())) {
                validationErrors.add("Connection to this address is not permitted");
            }
            if (form.getPort() != null && (form.getPort() < 1 || form.getPort() > 65535)) {
                validationErrors.add("Port must be between 1 and 65535");
            }
            if (!validationErrors.isEmpty()) {
                Map<String, Object> error = AnalyzerControllerHelper.wrapError(String.join("; ", validationErrors));
                error.put("validationErrors", validationErrors);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            // Check for duplicate name
            List<Analyzer> existingAnalyzers = analyzerService.getAll();
            for (Analyzer existing : existingAnalyzers) {
                if (existing.getName().equalsIgnoreCase(form.getName())) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(AnalyzerControllerHelper
                            .wrapError("Analyzer with name '" + form.getName() + "' already exists"));
                }
            }

            // Create Analyzer entity
            Analyzer analyzer = new Analyzer();
            analyzer.setName(form.getName());
            analyzer.setType(form.getAnalyzerType());
            analyzer.setSysUserId(getSysUserId(request));

            String analyzerId = analyzerService.insert(analyzer);

            // Retrieve created analyzer
            Analyzer createdAnalyzer = analyzerService.get(analyzerId);
            if (createdAnalyzer == null) {
                throw new LIMSRuntimeException("Failed to retrieve created analyzer");
            }

            // Create AnalyzerConfiguration (tracks lifecycle: SETUP → ACTIVE → INACTIVE →
            // DELETED)
            // Always create configuration to store status, even if IP/Port not provided
            try {
                List<String> testUnitIds = form.getTestUnitIds() != null ? form.getTestUnitIds() : new ArrayList<>();
                AnalyzerConfiguration config = new AnalyzerConfiguration();
                config.setAnalyzer(createdAnalyzer);
                config.setIpAddress(form.getIpAddress());
                config.setPort(form.getPort());
                config.setProtocolVersion(form.getProtocolVersion() != null ? form.getProtocolVersion() : "LIS2-A2");
                config.setTestUnitIds(testUnitIds);
                if (form.getIdentifierPattern() != null) {
                    config.setIdentifierPattern(form.getIdentifierPattern());
                }
                if (form.getGenericPlugin() != null) {
                    config.setGenericPlugin(form.getGenericPlugin());
                }
                if (form.getPreferGenericPlugin() != null) {
                    config.setPreferGenericPlugin(form.getPreferGenericPlugin());
                }
                // Set status (use form status or default to SETUP)
                String status = form.getStatus() != null ? form.getStatus() : "SETUP";
                try {
                    config.setStatus(AnalyzerConfiguration.AnalyzerStatus.valueOf(status));
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid status value: {}, defaulting to SETUP", status);
                    config.setStatus(AnalyzerConfiguration.AnalyzerStatus.SETUP);
                }
                config.setSysUserId(getSysUserId(request));
                analyzerConfigurationService.insert(config);
            } catch (LIMSRuntimeException e) {
                // If configuration creation fails, log but don't fail the analyzer creation
                logger.warn("Failed to create analyzer configuration: {}", e.getMessage());
                // Continue - analyzer is created, configuration can be added later
            }

            Map<String, Object> response = analyzerToMap(createdAnalyzer);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (LIMSRuntimeException e) {
            logger.error("Error creating analyzer: {}", e.getMessage(), e);
            return AnalyzerControllerHelper.mapExceptionToResponse(e);
        } catch (Exception e) {
            logger.error("Error creating analyzer", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalyzerControllerHelper.wrapError(e.getMessage()));
        }
    }

    /**
     * POST /rest/analyzer/analyzers/{id}/test-connection Test TCP connection to
     * analyzer
     */
    @PostMapping("/analyzers/{id}/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection(@PathVariable String id) {
        try {
            Analyzer analyzer = analyzerService.get(id);
            if (analyzer == null) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("error", "Analyzer not found: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            Optional<AnalyzerConfiguration> configOpt = analyzerConfigurationService.getByAnalyzerId(id);

            if (!configOpt.isPresent()) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("error", "Analyzer configuration not found");
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
            }

            AnalyzerConfiguration config = configOpt.get();
            String protocol = config.getProtocolVersion();

            // Protocol-aware connection testing
            Map<String, Object> response;
            if (protocol != null && protocol.toUpperCase().startsWith("HL7")) {
                response = testHl7Connection(config);
            } else if (protocol != null && (protocol.toUpperCase().startsWith("ASTM")
                    || protocol.toUpperCase().contains("E1381") || protocol.toUpperCase().contains("LIS2"))) {
                response = testAstmTcpConnection(config);
            } else if (protocol != null && protocol.toUpperCase().contains("FILE")) {
                response = testFileConfiguration(config, analyzer);
            } else if (protocol != null && protocol.toUpperCase().contains("RS232")) {
                response = testSerialConfiguration(id);
            } else {
                // Fallback: try TCP connection if IP/port available
                if (config.getIpAddress() != null && config.getPort() != null) {
                    response = testTcpConnection(config.getIpAddress(), config.getPort());
                } else {
                    response = new LinkedHashMap<>();
                    response.put("success", false);
                    response.put("message", "Unknown protocol or missing connection details: " + protocol);
                }
            }

            response.put("analyzerId", id);
            response.put("analyzerName", analyzer.getName());
            response.put("protocol", protocol);
            if (config.getIpAddress() != null) {
                response.put("ipAddress", config.getIpAddress());
            }
            if (config.getPort() != null) {
                response.put("port", config.getPort());
            }

            // Always return 200 with success status in response body
            // Client should check response.success to determine if connection worked
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error testing connection", e);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * GET /rest/analyzer/analyzers/{id}/fields Get all fields for an analyzer
     */
    @GetMapping("/analyzers/{id}/fields")
    public ResponseEntity<List<Map<String, Object>>> getFields(@PathVariable String id) {
        try {
            List<org.openelisglobal.analyzer.valueholder.AnalyzerField> fields = analyzerFieldService
                    .getFieldsByAnalyzerId(id);
            List<Map<String, Object>> response = new ArrayList<>();
            for (org.openelisglobal.analyzer.valueholder.AnalyzerField field : fields) {
                Map<String, Object> fieldMap = new LinkedHashMap<>();
                fieldMap.put("id", field.getId());
                fieldMap.put("fieldName", field.getFieldName());
                fieldMap.put("astmRef", field.getAstmRef());
                fieldMap.put("fieldType", field.getFieldType() != null ? field.getFieldType().toString() : null);
                fieldMap.put("unit", field.getUnit());
                fieldMap.put("isActive", field.getIsActive());
                response.add(fieldMap);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error retrieving fields for analyzer: {}", id, e);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }

    /**
     * GET /rest/analyzer/analyzers/{id} Retrieve analyzer by ID
     */
    @GetMapping("/analyzers/{id}")
    public ResponseEntity<Map<String, Object>> getAnalyzer(@PathVariable String id) {
        try {
            Analyzer analyzer = analyzerService.get(id);
            if (analyzer == null) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("error", "Analyzer not found: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            Map<String, Object> response = analyzerToMap(analyzer);
            return ResponseEntity.ok(response);
        } catch (org.hibernate.ObjectNotFoundException e) {
            // Hibernate may throw instead of returning null for missing IDs
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "Analyzer not found: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            logger.error("Error retrieving analyzer", e);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * PUT /rest/analyzer/analyzers/{id} Update analyzer
     */
    @PutMapping("/analyzers/{id}")
    public ResponseEntity<Map<String, Object>> updateAnalyzer(@PathVariable String id, @RequestBody AnalyzerForm form,
            HttpServletRequest request) {
        try {
            Analyzer analyzer = analyzerService.get(id);
            if (analyzer == null) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("error", "Analyzer not found: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            // Manual validation for optional fields
            if (form.getIpAddress() != null && !form.getIpAddress().matches("^(\\d{1,3}\\.){3}\\d{1,3}$")) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("error", "Invalid IPv4 address format");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            if (form.getIpAddress() != null && NetworkValidationUtil.isBlockedAddress(form.getIpAddress())) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("error", "Connection to this address is not permitted");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            if (form.getPort() != null && (form.getPort() < 1 || form.getPort() > 65535)) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("error", "Port must be between 1 and 65535");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            // Update analyzer fields
            if (form.getName() != null && !form.getName().trim().isEmpty()) {
                analyzer.setName(form.getName());
            }
            if (form.getAnalyzerType() != null && !form.getAnalyzerType().trim().isEmpty()) {
                analyzer.setType(form.getAnalyzerType());
            }

            analyzerService.update(analyzer);

            // Update or create configuration
            Optional<AnalyzerConfiguration> configOpt = analyzerConfigurationService.getByAnalyzerId(id);
            AnalyzerConfiguration config;
            if (configOpt.isPresent()) {
                config = configOpt.get();
                if (form.getIpAddress() != null) {
                    config.setIpAddress(form.getIpAddress());
                }
                if (form.getPort() != null) {
                    config.setPort(form.getPort());
                }
                if (form.getProtocolVersion() != null) {
                    config.setProtocolVersion(form.getProtocolVersion());
                }
                if (form.getTestUnitIds() != null) {
                    config.setTestUnitIds(form.getTestUnitIds());
                }
                if (form.getIdentifierPattern() != null) {
                    config.setIdentifierPattern(form.getIdentifierPattern());
                }
                if (form.getGenericPlugin() != null) {
                    config.setGenericPlugin(form.getGenericPlugin());
                }
                if (form.getPreferGenericPlugin() != null) {
                    config.setPreferGenericPlugin(form.getPreferGenericPlugin());
                }
                // Update lifecycle status if provided (SETUP → ACTIVE → INACTIVE → DELETED)
                if (form.getStatus() != null) {
                    try {
                        config.setStatus(AnalyzerConfiguration.AnalyzerStatus.valueOf(form.getStatus()));
                    } catch (IllegalArgumentException e) {
                        logger.warn("Invalid status value: {}, keeping existing status", form.getStatus());
                    }
                }
                analyzerConfigurationService.update(config);
            } else {
                // Create new configuration if doesn't exist
                List<String> testUnitIds = form.getTestUnitIds() != null ? form.getTestUnitIds() : new ArrayList<>();
                config = new AnalyzerConfiguration();
                config.setAnalyzer(analyzer);
                config.setIpAddress(form.getIpAddress());
                config.setPort(form.getPort());
                config.setProtocolVersion(form.getProtocolVersion() != null ? form.getProtocolVersion() : "LIS2-A2");
                config.setTestUnitIds(testUnitIds);
                if (form.getIdentifierPattern() != null) {
                    config.setIdentifierPattern(form.getIdentifierPattern());
                }
                if (form.getGenericPlugin() != null) {
                    config.setGenericPlugin(form.getGenericPlugin());
                }
                if (form.getPreferGenericPlugin() != null) {
                    config.setPreferGenericPlugin(form.getPreferGenericPlugin());
                }
                // Set status
                if (form.getStatus() != null) {
                    try {
                        config.setStatus(AnalyzerConfiguration.AnalyzerStatus.valueOf(form.getStatus()));
                    } catch (IllegalArgumentException e) {
                        logger.warn("Invalid status value: {}, defaulting to SETUP", form.getStatus());
                        config.setStatus(AnalyzerConfiguration.AnalyzerStatus.SETUP);
                    }
                } else {
                    config.setStatus(AnalyzerConfiguration.AnalyzerStatus.SETUP);
                }
                config.setSysUserId(getSysUserId(request));
                analyzerConfigurationService.insert(config);
            }

            // Retrieve updated analyzer
            Analyzer updatedAnalyzer = analyzerService.get(id);
            Map<String, Object> response = analyzerToMap(updatedAnalyzer);
            return ResponseEntity.ok(response);
        } catch (LIMSRuntimeException e) {
            logger.error("Error updating analyzer: {}", e.getMessage(), e);
            return AnalyzerControllerHelper.mapExceptionToResponse(e);
        } catch (Exception e) {
            logger.error("Error updating analyzer", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalyzerControllerHelper.wrapError(e.getMessage()));
        }
    }

    /**
     * POST /rest/analyzer/analyzers/{id}/delete Delete analyzer
     * 
     * Implements 90-day soft delete window per spec requirement: - If analyzer has
     * recent results (within 90 days): soft delete (status = DELETED) - If analyzer
     * has no recent results: hard delete (remove from database)
     * 
     * Note: Uses POST instead of DELETE HTTP method due to Spring Security 6 CSRF
     * protection blocking DELETE requests even with valid CSRF tokens and ignore
     * matchers configured. POST works correctly with the same security
     * configuration.
     * 
     * @param id Analyzer ID to delete
     * @return 204 No Content on success, 404 if analyzer not found, 409 if cannot
     *         delete (recent results), 500 on error
     */
    @PostMapping("/analyzers/{id}/delete")
    public ResponseEntity<Map<String, Object>> deleteAnalyzerLegacy(@PathVariable String id) {
        try {
            Analyzer analyzer = analyzerService.get(id);
            if (analyzer == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            // Check for recent results (within 90-day window)
            boolean hasRecentResults = analyzerConfigurationService.hasRecentResults(id);

            Optional<AnalyzerConfiguration> configOpt = analyzerConfigurationService.getByAnalyzerId(id);

            if (hasRecentResults) {
                // Soft delete: set status to DELETED (90-day window)
                if (configOpt.isPresent()) {
                    AnalyzerConfiguration config = configOpt.get();
                    config.setStatus(AnalyzerConfiguration.AnalyzerStatus.DELETED);
                    analyzerConfigurationService.update(config);
                }

                Map<String, Object> response = new LinkedHashMap<>();
                response.put("message", "Analyzer soft-deleted (has recent results within 90-day window)");
                response.put("deleted", false); // Soft delete, not hard delete
                return ResponseEntity.ok(response);
            } else {
                // Hard delete: remove from database (no recent results)
                if (configOpt.isPresent()) {
                    analyzerConfigurationService.delete(configOpt.get());
                }
                analyzerService.delete(analyzer);

                Map<String, Object> response = new LinkedHashMap<>();
                response.put("message", "Analyzer permanently deleted");
                response.put("deleted", true); // Hard delete
                return ResponseEntity.ok(response);
            }
        } catch (org.hibernate.ObjectNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            logger.error("Error deleting analyzer", e);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Convert Analyzer entity to Map for JSON response
     */
    private Map<String, Object> analyzerToMap(Analyzer analyzer) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", analyzer.getId());
        map.put("name", analyzer.getName());
        map.put("type", analyzer.getType());
        map.put("description", analyzer.getDescription());
        map.put("location", analyzer.getLocation());

        // Add configuration if exists (catch exceptions to avoid breaking response)
        try {
            Optional<AnalyzerConfiguration> configOpt = analyzerConfigurationService.getByAnalyzerId(analyzer.getId());
            if (configOpt.isPresent()) {
                AnalyzerConfiguration config = configOpt.get();
                map.put("ipAddress", config.getIpAddress());
                map.put("port", config.getPort());
                map.put("protocolVersion", config.getProtocolVersion());
                map.put("testUnitIds", config.getTestUnitIds());
                map.put("identifierPattern", config.getIdentifierPattern());
                map.put("genericPlugin", config.isGenericPlugin());
                map.put("preferGenericPlugin", config.isPreferGenericPlugin());
                // Include lifecycle status (SETUP → ACTIVE → INACTIVE → DELETED)
                if (config.getStatus() != null) {
                    map.put("status", config.getStatus().toString());
                } else {
                    map.put("status", "SETUP");
                }
            } else {
                // No configuration exists, default status to SETUP
                map.put("status", "SETUP");
            }
        } catch (Exception e) {
            // Configuration not found or error - just don't include it in response
            logger.debug("Could not load analyzer configuration for analyzer {}: {}", analyzer.getId(), e.getMessage());
            // Default status to SETUP if configuration cannot be loaded
            map.put("status", "SETUP");
        }

        return map;
    }

    /**
     * Test TCP connection to analyzer with ASTM handshake (ENQ/ACK)
     * 
     * @param ipAddress IP address of the analyzer
     * @param port      Port number of the analyzer
     * @return Map with success status, message, and connection details
     */
    private Map<String, Object> testTcpConnection(String ipAddress, Integer port) {
        Map<String, Object> response = new LinkedHashMap<>();
        Socket socket = null;

        if (NetworkValidationUtil.isBlockedAddress(ipAddress)) {
            response.put("success", false);
            response.put("message", "Connection to this address is not permitted");
            return response;
        }

        try {
            // Attempt TCP connection with 5 second timeout
            socket = new Socket();
            socket.connect(new java.net.InetSocketAddress(ipAddress, port), 5000);
            socket.setSoTimeout(5000); // Read timeout

            // Send ENQ
            OutputStream out = socket.getOutputStream();
            out.write(ENQ);
            out.flush();

            // Wait for ACK response
            InputStream in = socket.getInputStream();
            int responseByte = in.read();

            if (responseByte == ACK) {
                response.put("success", true);
                response.put("message", "Connection successful - ACK received");
                logger.info("Connection test successful for {}:{} - ACK received", ipAddress, port);
            } else {
                response.put("success", false);
                response.put("message", "Connection established but invalid response: 0x"
                        + String.format("%02X", responseByte & 0xFF) + " (expected ACK 0x06)");
                logger.warn("Connection test failed for {}:{} - Invalid response: 0x{}", ipAddress, port,
                        String.format("%02X", responseByte & 0xFF));
            }

        } catch (SocketTimeoutException e) {
            response.put("success", false);
            response.put("message", "Connection timeout - No response from analyzer");
            logger.warn("Connection test timeout for {}:{}", ipAddress, port, e);
        } catch (java.net.ConnectException e) {
            response.put("success", false);
            response.put("message", "Connection refused - Analyzer not reachable at " + ipAddress + ":" + port);
            logger.warn("Connection test failed for {}:{} - Connection refused", ipAddress, port, e);
        } catch (java.net.UnknownHostException e) {
            response.put("success", false);
            response.put("message", "Unknown host - Cannot resolve " + ipAddress);
            logger.warn("Connection test failed for {}:{} - Unknown host", ipAddress, port, e);
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Connection error: " + e.getMessage());
            logger.error("Connection test error for {}:{}", ipAddress, port, e);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Unexpected error: " + e.getMessage());
            logger.error("Unexpected error during connection test for {}:{}", ipAddress, port, e);
        } finally {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    logger.debug("Error closing socket", e);
                }
            }
        }

        return response;
    }

    /**
     * Test HL7 analyzer connection.
     *
     * In OpenELIS, HL7 analyzers are typically push-based (results are posted to
     * OpenELIS), so there is no reliable outbound \"connection test\" from OpenELIS
     * to the analyzer. This returns success when the analyzer is configured.
     *
     * @param config Analyzer configuration (protocol metadata)
     * @return Map with success status and message
     */
    private Map<String, Object> testHl7Connection(AnalyzerConfiguration config) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "HL7 analyzers are push-based; validate by sending an HL7 message to OpenELIS");
        return response;
    }

    /**
     * Test ASTM analyzer connection over TCP/IP. ASTM requires ENQ/ACK handshake
     * for connection validation.
     *
     * @param config Analyzer configuration with IP/port
     * @return Map with success status and message
     */
    private Map<String, Object> testAstmTcpConnection(AnalyzerConfiguration config) {
        if (config.getIpAddress() == null || config.getPort() == null) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", false);
            response.put("message", "ASTM configuration incomplete - missing IP address or port");
            return response;
        }

        // Use existing testTcpConnection which implements ASTM ENQ/ACK
        Map<String, Object> response = testTcpConnection(config.getIpAddress(), config.getPort());
        response.put("connectionType", "ASTM");
        return response;
    }

    /**
     * Test FILE analyzer configuration Verifies that the file import directory
     * exists and is accessible
     * 
     * @param config   Analyzer configuration
     * @param analyzer Analyzer entity
     * @return Map with success status and message
     */
    private Map<String, Object> testFileConfiguration(AnalyzerConfiguration config, Analyzer analyzer) {
        Map<String, Object> response = new LinkedHashMap<>();

        try {
            // Check if file import configuration exists
            Optional<FileImportConfiguration> fileConfigOpt = fileImportService
                    .getByAnalyzerId(Integer.valueOf(analyzer.getId()));

            if (!fileConfigOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "File import configuration not found for analyzer");
                return response;
            }

            FileImportConfiguration fileConfig = fileConfigOpt.get();
            String importDir = fileConfig.getImportDirectory();

            if (importDir == null || importDir.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Import directory not configured");
                return response;
            }

            // Verify directory exists
            Path directory = Path.of(importDir);
            if (Files.exists(directory) && Files.isDirectory(directory) && Files.isReadable(directory)) {
                response.put("success", true);
                response.put("message", "File import directory accessible: " + importDir);
                response.put("importDirectory", importDir);
                response.put("filePattern", fileConfig.getFilePattern());
            } else {
                response.put("success", false);
                response.put("message", "Import directory not accessible: " + importDir);
                response.put("importDirectory", importDir);
            }

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error checking file configuration: " + e.getMessage());
            logger.error("Error testing file configuration for analyzer {}", analyzer.getId(), e);
        }

        return response;
    }

    /**
     * Test serial analyzer configuration. Verifies that the serial port
     * configuration exists and the device node is accessible.
     *
     * <p>
     * <b>Platform note:</b> Serial port detection relies on *NIX device nodes (e.g.
     * {@code /dev/ttyS0}, {@code /dev/ttyUSB0}). On Windows, detection would
     * require javax.comm or jSerialComm; this method will always report the port as
     * inaccessible on non-*NIX systems.
     *
     * @param analyzerId Analyzer ID
     * @return Map with success status and message
     */
    private Map<String, Object> testSerialConfiguration(String analyzerId) {
        Map<String, Object> response = new LinkedHashMap<>();

        try {
            Optional<SerialPortConfiguration> serialConfigOpt = serialPortService
                    .getByAnalyzerId(Integer.valueOf(analyzerId));

            if (!serialConfigOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "Serial port configuration not found for analyzer");
                return response;
            }

            SerialPortConfiguration serialConfig = serialConfigOpt.get();
            String portName = serialConfig.getPortName();

            if (portName == null || portName.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Serial port name not configured");
                return response;
            }

            // Check if port device node exists (*NIX only — see Javadoc)
            boolean portExists = Files.exists(Path.of(portName));

            if (portExists) {
                response.put("success", true);
                response.put("message", "Serial port accessible: " + portName);
                response.put("portName", portName);
                response.put("baudRate", serialConfig.getBaudRate());
                response.put("dataBits", serialConfig.getDataBits());
                response.put("parity", serialConfig.getParity());
                response.put("stopBits", serialConfig.getStopBits());
            } else {
                response.put("success", false);
                response.put("message", "Serial port not accessible: " + portName
                        + " (check hardware connection or virtual serial setup)");
                response.put("portName", portName);
            }

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error checking serial configuration: " + e.getMessage());
            logger.error("Error testing serial configuration for analyzer {}", analyzerId, e);
        }

        return response;
    }

    /**
     * POST /rest/analyzer/analyzers/{id}/query Start an asynchronous query job for
     * an analyzer
     */
    @PostMapping("/analyzers/{id}/query")
    public ResponseEntity<Map<String, Object>> queryAnalyzer(@PathVariable String id) {
        try {
            String jobId = analyzerQueryService.startQuery(id);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("jobId", jobId);
            response.put("analyzerId", id);
            response.put("status", "started");
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } catch (LIMSRuntimeException e) {
            // Push-only analyzers or missing TCP config → 422
            logger.warn("Cannot query analyzer {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(AnalyzerControllerHelper.wrapError(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error starting query job for analyzer: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalyzerControllerHelper.wrapError(e.getMessage()));
        }
    }

    /**
     * GET /rest/analyzer/analyzers/{id}/query/{jobId}/status Get query job status
     */
    @GetMapping("/analyzers/{id}/query/{jobId}/status")
    public ResponseEntity<Map<String, Object>> getQueryStatus(@PathVariable String id, @PathVariable String jobId) {
        try {
            Map<String, Object> status = analyzerQueryService.getStatus(id, jobId);
            if (status == null) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("error", "Query job not found or expired: " + jobId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("Error getting query status for analyzer: {}, job: {}", id, jobId, e);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * GET /rest/analyzer/defaults List available default configuration templates
     * from filesystem.
     *
     * <p>
     * Returns minimal metadata for each template: - id (e.g., "astm/mindray-ba88a")
     * - protocol ("ASTM" or "HL7") - analyzer_name (from JSON)
     *
     * <p>
     * Task Reference: T215 (M20) - Create REST endpoint for listing defaults
     */
    @GetMapping("/defaults")
    public ResponseEntity<?> getDefaults() {
        try {
            String defaultsDir = System.getenv("ANALYZER_DEFAULTS_DIR");
            if (defaultsDir == null || defaultsDir.isEmpty()) {
                defaultsDir = "/data/analyzer-defaults";
            }

            Path baseDir = Path.of(defaultsDir);
            if (!Files.exists(baseDir) || !Files.isDirectory(baseDir)) {
                logger.warn("Analyzer defaults directory not found: {}", defaultsDir);
                return ResponseEntity.ok(new ArrayList<>());
            }

            List<Map<String, Object>> templates = new ArrayList<>();

            // Scan ASTM directory
            Path astmDir = baseDir.resolve("astm");
            if (Files.exists(astmDir) && Files.isDirectory(astmDir)) {
                scanTemplates(astmDir, "astm", templates);
            }

            // Scan HL7 directory
            Path hl7Dir = baseDir.resolve("hl7");
            if (Files.exists(hl7Dir) && Files.isDirectory(hl7Dir)) {
                scanTemplates(hl7Dir, "hl7", templates);
            }

            return ResponseEntity.ok(templates);
        } catch (Exception e) {
            logger.error("Error listing default configs", e);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "Failed to list default configurations: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * GET /rest/analyzer/defaults/{protocol}/{name} Load specific default
     * configuration template from filesystem.
     *
     * <p>
     * Implements strict security controls:
     * <ul>
     * <li>Protocol allowlist: only "astm" or "hl7" (case-insensitive)</li>
     * <li>Filename regex: {@code ^[a-zA-Z0-9\-_.]+$} — rejects path separators,
     * {@code ..}, and special characters to prevent path traversal</li>
     * <li>Normalized path verification: resolved path must start with the defaults
     * base directory</li>
     * </ul>
     */
    @GetMapping("/defaults/{protocol}/{name}")
    public ResponseEntity<?> getDefaultConfig(@PathVariable String protocol, @PathVariable String name) {
        try {
            // Validate protocol (allowlist)
            if (!protocol.equalsIgnoreCase("astm") && !protocol.equalsIgnoreCase("hl7")) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("error", "Invalid protocol: must be 'astm' or 'hl7'");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            // Sanitize filename: only alphanumeric, dash, underscore, period
            if (!name.matches("^[a-zA-Z0-9\\-_.]+$")) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("error", "Invalid filename: only alphanumeric, dash, underscore, and period allowed");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            // Ensure .json extension
            String filename = name.endsWith(".json") ? name : name + ".json";

            // Build path using Path.resolve() (handles separators automatically)
            String defaultsDir = System.getenv("ANALYZER_DEFAULTS_DIR");
            if (defaultsDir == null || defaultsDir.isEmpty()) {
                defaultsDir = "/data/analyzer-defaults";
            }

            Path baseDir = Path.of(defaultsDir);
            Path templateFile = baseDir.resolve(protocol).resolve(filename);

            // Verify normalized path stays within base directory (prevents path traversal)
            Path normalizedPath = templateFile.normalize();
            Path normalizedBase = baseDir.normalize();
            if (!normalizedPath.startsWith(normalizedBase)) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("error", "Invalid path: template must be within defaults directory");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            // Check file exists
            if (!Files.exists(templateFile) || !Files.isRegularFile(templateFile)) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("error", "Template not found: " + protocol + "/" + name);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            // Read and parse JSON
            String jsonContent = Files.readString(templateFile, StandardCharsets.UTF_8);

            Map<String, Object> config = objectMapper.readValue(jsonContent, Map.class);
            return ResponseEntity.ok(config);

        } catch (IOException e) {
            logger.error("Error reading default config: {}/{}", protocol, name, e);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "Failed to read template: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        } catch (Exception e) {
            logger.error("Error loading default config", e);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "Failed to load template: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Scan directory for JSON template files and add to list.
     *
     * @param directory Protocol directory (astm/ or hl7/)
     * @param protocol  Protocol name ("astm" or "hl7")
     * @param templates List to populate with template metadata
     */
    private void scanTemplates(Path directory, String protocol, List<Map<String, Object>> templates) {
        try (java.util.stream.Stream<Path> paths = Files.list(directory)) {
            paths.filter(p -> p.toString().endsWith(".json")).forEach(file -> {
                try {
                    String jsonContent = Files.readString(file, StandardCharsets.UTF_8);
                    Map<String, Object> config = objectMapper.readValue(jsonContent, Map.class);

                    Map<String, Object> template = new LinkedHashMap<>();
                    String filename = file.getFileName().toString().replace(".json", "");
                    template.put("id", protocol + "/" + filename);
                    template.put("protocol", protocol.toUpperCase());
                    template.put("analyzerName", config.get("analyzer_name"));
                    template.put("manufacturer", config.get("manufacturer"));
                    template.put("category", config.get("category"));

                    templates.add(template);
                } catch (Exception e) {
                    logger.warn("Failed to parse template file: {}", file.getFileName(), e);
                }
            });
        } catch (IOException e) {
            logger.warn("Failed to list template files in {}: {}", directory, e.getMessage());
        }
    }
}
