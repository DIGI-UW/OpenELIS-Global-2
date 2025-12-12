package org.openelisglobal.analyzer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.analyzer.form.AnalyzerForm;
import org.openelisglobal.analyzer.service.AnalyzerConfigurationService;
import org.openelisglobal.analyzer.service.AnalyzerFieldService;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerConfiguration;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.rest.BaseRestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

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

                // Apply unified status filter
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
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }

    /**
     * POST /rest/analyzer/analyzers Create new analyzer with configuration
     */
    @PostMapping("/analyzers")
    public ResponseEntity<Map<String, Object>> createAnalyzer(@RequestBody AnalyzerForm form) {
        try {
            // Manual validation for optional fields
            if (form.getIpAddress() != null && !form.getIpAddress().matches("^(\\d{1,3}\\.){3}\\d{1,3}$")) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Invalid IPv4 address format");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            if (form.getPort() != null && (form.getPort() < 1 || form.getPort() > 65535)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Port must be between 1 and 65535");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            if (form.getName() == null || form.getName().trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Analyzer name is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            if (form.getAnalyzerType() == null || form.getAnalyzerType().trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Analyzer type is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            // Check for duplicate name
            List<Analyzer> existingAnalyzers = analyzerService.getAll();
            for (Analyzer existing : existingAnalyzers) {
                if (existing.getName().equalsIgnoreCase(form.getName())) {
                    Map<String, Object> error = new HashMap<>();
                    error.put("error", "Analyzer with name '" + form.getName() + "' already exists");
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
                }
            }

            // Create Analyzer entity
            Analyzer analyzer = new Analyzer();
            analyzer.setName(form.getName());
            analyzer.setType(form.getAnalyzerType());
            analyzer.setSysUserId("1"); // Default system user (should come from security context)

            String analyzerId = analyzerService.insert(analyzer);

            // Retrieve created analyzer
            Analyzer createdAnalyzer = analyzerService.get(analyzerId);
            if (createdAnalyzer == null) {
                throw new LIMSRuntimeException("Failed to retrieve created analyzer");
            }

            // Create AnalyzerConfiguration with unified status
            // Always create configuration to store status, even if IP/Port not provided
            try {
                List<String> testUnitIds = form.getTestUnitIds() != null ? form.getTestUnitIds() : new ArrayList<>();
                AnalyzerConfiguration config = new AnalyzerConfiguration();
                config.setAnalyzer(createdAnalyzer);
                config.setIpAddress(form.getIpAddress());
                config.setPort(form.getPort());
                config.setProtocolVersion(
                        form.getProtocolVersion() != null ? form.getProtocolVersion() : "ASTM LIS2-A2");
                config.setTestUnitIds(testUnitIds);
                // Set unified status (use form status or default to SETUP)
                String status = form.getStatus() != null ? form.getStatus() : "SETUP";
                try {
                    config.setStatus(AnalyzerConfiguration.AnalyzerStatus.valueOf(status));
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid status value: " + status + ", defaulting to SETUP");
                    config.setStatus(AnalyzerConfiguration.AnalyzerStatus.SETUP);
                }
                config.setSysUserId("1");
                analyzerConfigurationService.insert(config);
            } catch (LIMSRuntimeException e) {
                // If configuration creation fails, log but don't fail the analyzer creation
                logger.warn("Failed to create analyzer configuration: " + e.getMessage());
                // Continue - analyzer is created, configuration can be added later
            }

            Map<String, Object> response = analyzerToMap(createdAnalyzer);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (LIMSRuntimeException e) {
            logger.error("Error creating analyzer: " + e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Error creating analyzer", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
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
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Analyzer not found: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            Optional<AnalyzerConfiguration> configOpt = analyzerConfigurationService.getByAnalyzerId(id);

            if (!configOpt.isPresent() || configOpt.get().getIpAddress() == null || configOpt.get().getPort() == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Analyzer configuration not found or incomplete");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            AnalyzerConfiguration config = configOpt.get();

            // Perform actual TCP connection test with ASTM handshake
            Map<String, Object> response = testTcpConnection(config.getIpAddress(), config.getPort());
            response.put("analyzerId", id);
            response.put("analyzerName", analyzer.getName());
            response.put("ipAddress", config.getIpAddress());
            response.put("port", config.getPort());

            // Always return 200 with success status in response body
            // Client should check response.success to determine if connection worked
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error testing connection", e);
            Map<String, Object> error = new HashMap<>();
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
                Map<String, Object> fieldMap = new HashMap<>();
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
            logger.error("Error retrieving fields for analyzer: " + id, e);
            Map<String, Object> error = new HashMap<>();
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
            Map<String, Object> response = analyzerToMap(analyzer);
            return ResponseEntity.ok(response);
        } catch (org.hibernate.ObjectNotFoundException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Analyzer not found: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            logger.error("Error retrieving analyzer", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * PUT /rest/analyzer/analyzers/{id} Update analyzer
     */
    @PutMapping("/analyzers/{id}")
    public ResponseEntity<Map<String, Object>> updateAnalyzer(@PathVariable String id, @RequestBody AnalyzerForm form) {
        try {
            Analyzer analyzer = analyzerService.get(id);
            if (analyzer == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Analyzer not found: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            // Manual validation for optional fields
            if (form.getIpAddress() != null && !form.getIpAddress().matches("^(\\d{1,3}\\.){3}\\d{1,3}$")) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Invalid IPv4 address format");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            if (form.getPort() != null && (form.getPort() < 1 || form.getPort() > 65535)) {
                Map<String, Object> error = new HashMap<>();
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
                // Update unified status if provided
                if (form.getStatus() != null) {
                    try {
                        config.setStatus(AnalyzerConfiguration.AnalyzerStatus.valueOf(form.getStatus()));
                    } catch (IllegalArgumentException e) {
                        logger.warn("Invalid status value: " + form.getStatus() + ", keeping existing status");
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
                config.setProtocolVersion(
                        form.getProtocolVersion() != null ? form.getProtocolVersion() : "ASTM LIS2-A2");
                config.setTestUnitIds(testUnitIds);
                // Set unified status
                if (form.getStatus() != null) {
                    try {
                        config.setStatus(AnalyzerConfiguration.AnalyzerStatus.valueOf(form.getStatus()));
                    } catch (IllegalArgumentException e) {
                        logger.warn("Invalid status value: " + form.getStatus() + ", defaulting to SETUP");
                        config.setStatus(AnalyzerConfiguration.AnalyzerStatus.SETUP);
                    }
                } else {
                    config.setStatus(AnalyzerConfiguration.AnalyzerStatus.SETUP);
                }
                config.setSysUserId("1");
                analyzerConfigurationService.insert(config);
            }

            // Retrieve updated analyzer
            Analyzer updatedAnalyzer = analyzerService.get(id);
            Map<String, Object> response = analyzerToMap(updatedAnalyzer);
            return ResponseEntity.ok(response);
        } catch (LIMSRuntimeException e) {
            logger.error("Error updating analyzer: " + e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Error updating analyzer", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * DELETE /rest/analyzer/analyzers/{id} Soft delete analyzer
     * 
     * Implements soft delete: sets status to INACTIVE (analyzer can be
     * reactivated).
     * 
     * @param id Analyzer ID to delete
     * @return 204 No Content on success, 404 if analyzer not found, 500 on error
     */
    @DeleteMapping("/analyzers/{id}")
    public ResponseEntity<Void> softDeleteAnalyzer(@PathVariable String id) {
        try {
            Analyzer analyzer = analyzerService.get(id);
            if (analyzer == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            // Soft delete: set status to INACTIVE
            Optional<AnalyzerConfiguration> configOpt = analyzerConfigurationService.getByAnalyzerId(id);
            if (configOpt.isPresent()) {
                AnalyzerConfiguration config = configOpt.get();
                config.setStatus(AnalyzerConfiguration.AnalyzerStatus.INACTIVE);
                analyzerConfigurationService.update(config);
            }

            // Also mark the analyzer as inactive
            analyzer.setActive(false);
            analyzerService.update(analyzer);

            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Error deleting analyzer: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /rest/analyzer/analyzers/{id}/delete Delete analyzer (legacy endpoint)
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

                Map<String, Object> response = new HashMap<>();
                response.put("message", "Analyzer soft-deleted (has recent results within 90-day window)");
                response.put("deleted", false); // Soft delete, not hard delete
                return ResponseEntity.ok(response);
            } else {
                // Hard delete: remove from database (no recent results)
                if (configOpt.isPresent()) {
                    analyzerConfigurationService.delete(configOpt.get());
                }
                analyzerService.delete(analyzer);

                Map<String, Object> response = new HashMap<>();
                response.put("message", "Analyzer permanently deleted");
                response.put("deleted", true); // Hard delete
                return ResponseEntity.ok(response);
            }
        } catch (org.hibernate.ObjectNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            logger.error("Error deleting analyzer", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Convert Analyzer entity to Map for JSON response
     */
    private Map<String, Object> analyzerToMap(Analyzer analyzer) {
        Map<String, Object> map = new HashMap<>();
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
                // Include unified status (defaults to SETUP if not set)
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
            logger.debug(
                    "Could not load analyzer configuration for analyzer " + analyzer.getId() + ": " + e.getMessage());
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
        Map<String, Object> response = new HashMap<>();
        Socket socket = null;

        try {
            // Attempt TCP connection with 5 second timeout
            socket = new Socket();
            socket.connect(new java.net.InetSocketAddress(ipAddress, port), 5000);
            socket.setSoTimeout(5000); // Read timeout

            // ASTM LIS2-A2 Control Characters
            byte ENQ = 0x05; // Enquiry - Start transmission
            byte ACK = 0x06; // Acknowledge - Positive response

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
}
