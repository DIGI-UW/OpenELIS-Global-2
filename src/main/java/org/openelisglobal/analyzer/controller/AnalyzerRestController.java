package org.openelisglobal.analyzer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.analyzer.form.AnalyzerForm;
import org.openelisglobal.analyzer.service.AnalyzerConfigurationService;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.service.AnalyzerQueryService;
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
 * REST Controller for Analyzer management
 * Handles CRUD operations for analyzers and analyzer configurations
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
    private AnalyzerQueryService analyzerQueryService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * GET /rest/analyzer/analyzers
     * Retrieve all analyzers with their configurations
     */
    @GetMapping("/analyzers")
    public ResponseEntity<List<Map<String, Object>>> getAnalyzers(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        try {
            List<Analyzer> analyzers = analyzerService.getAll();
            List<Map<String, Object>> response = new ArrayList<>();

            for (Analyzer analyzer : analyzers) {
                // Apply filters
                if (status != null && !status.isEmpty()) {
                    Boolean activeStatus = "active".equalsIgnoreCase(status) ? true
                            : "inactive".equalsIgnoreCase(status) ? false : null;
                    if (activeStatus != null && analyzer.isActive() != activeStatus) {
                        continue;
                    }
                }

                if (search != null && !search.isEmpty()) {
                    String searchLower = search.toLowerCase();
                    if (!analyzer.getName().toLowerCase().contains(searchLower)
                            && (analyzer.getType() == null || !analyzer.getType().toLowerCase().contains(searchLower))) {
                        continue;
                    }
                }

                Map<String, Object> analyzerMap = analyzerToMap(analyzer);
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
     * POST /rest/analyzer/analyzers/{id}/query
     * Start asynchronous query job to retrieve available analyzer fields
     */
    @PostMapping(value = "/analyzers/{id}/query", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> startQuery(@PathVariable String id) {
        try {
            String jobId = analyzerQueryService.startQuery(id);
            Map<String, Object> response = new HashMap<>();
            response.put("jobId", jobId);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } catch (Exception e) {
            logger.error("Error starting analyzer query", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * GET /rest/analyzer/analyzers/{id}/query/{jobId}/status
     * Get status for analyzer query job
     */
    @GetMapping(value = "/analyzers/{id}/query/{jobId}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getQueryStatus(@PathVariable String id, @PathVariable String jobId) {
        try {
            Map<String, Object> status = analyzerQueryService.getStatus(id, jobId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("Error fetching analyzer query status", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * POST /rest/analyzer/analyzers
     * Create new analyzer with configuration
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
            analyzer.setActive(form.getActive() != null ? form.getActive() : true);
            analyzer.setSysUserId("1"); // Default system user (should come from security context)

            String analyzerId = analyzerService.insert(analyzer);

            // Retrieve created analyzer
            Analyzer createdAnalyzer = analyzerService.get(analyzerId);
            if (createdAnalyzer == null) {
                throw new LIMSRuntimeException("Failed to retrieve created analyzer");
            }

            // Create AnalyzerConfiguration if IP/Port provided
            // Note: This happens in a separate transaction to ensure analyzer is fully persisted
            if (form.getIpAddress() != null && form.getPort() != null) {
                try {
                    List<String> testUnitIds = form.getTestUnitIds() != null
                            ? form.getTestUnitIds()
                            : new ArrayList<>();
                    analyzerConfigurationService.createConfiguration(createdAnalyzer, form.getIpAddress(), form.getPort(),
                            testUnitIds);
                } catch (LIMSRuntimeException e) {
                    // If configuration creation fails, log but don't fail the analyzer creation
                    logger.warn("Failed to create analyzer configuration: " + e.getMessage());
                    // Continue - analyzer is created, configuration can be added later
                }
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
     * POST /rest/analyzer/analyzers/{id}/test-connection
     * Test TCP connection to analyzer
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

            if (!configOpt.isPresent() || configOpt.get().getIpAddress() == null
                    || configOpt.get().getPort() == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Analyzer configuration not found or incomplete");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            AnalyzerConfiguration config = configOpt.get();

            // TODO: Implement actual TCP connection test
            // For now, return success response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("analyzerId", id);
            response.put("analyzerName", analyzer.getName());
            response.put("ipAddress", config.getIpAddress());
            response.put("port", config.getPort());
            response.put("message", "Connection test not yet implemented");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error testing connection", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * GET /rest/analyzer/analyzers/{id}
     * Retrieve analyzer by ID
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
     * PUT /rest/analyzer/analyzers/{id}
     * Update analyzer
     */
    @PutMapping("/analyzers/{id}")
    public ResponseEntity<Map<String, Object>> updateAnalyzer(@PathVariable String id,
            @RequestBody AnalyzerForm form) {
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
            if (form.getActive() != null) {
                analyzer.setActive(form.getActive());
            }

            analyzerService.update(analyzer);

            // Update configuration if IP/Port provided
            if (form.getIpAddress() != null && form.getPort() != null) {
                Optional<AnalyzerConfiguration> configOpt = analyzerConfigurationService.getByAnalyzerId(id);
                if (configOpt.isPresent()) {
                    AnalyzerConfiguration config = configOpt.get();
                    config.setIpAddress(form.getIpAddress());
                    config.setPort(form.getPort());
                    if (form.getProtocolVersion() != null) {
                        config.setProtocolVersion(form.getProtocolVersion());
                    }
                    if (form.getTestUnitIds() != null) {
                        config.setTestUnitIds(form.getTestUnitIds());
                    }
                    analyzerConfigurationService.update(config);
                } else {
                    // Create new configuration if doesn't exist
                    List<String> testUnitIds = form.getTestUnitIds() != null ? form.getTestUnitIds() : new ArrayList<>();
                    analyzerConfigurationService.createConfiguration(analyzer, form.getIpAddress(), form.getPort(),
                            testUnitIds);
                }
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
     * DELETE /rest/analyzer/analyzers/{id}
     * Delete analyzer (soft delete - sets active=false)
     */
    @DeleteMapping("/analyzers/{id}")
    public ResponseEntity<Void> deleteAnalyzer(@PathVariable String id) {
        try {
            Analyzer analyzer = analyzerService.get(id);
            // Soft delete - set active to false
            analyzer.setActive(false);
            analyzerService.update(analyzer);
            return ResponseEntity.noContent().build();
        } catch (org.hibernate.ObjectNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            logger.error("Error deleting analyzer", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
        map.put("active", analyzer.isActive());
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
            }
        } catch (Exception e) {
            // Configuration not found or error - just don't include it in response
            logger.debug("Could not load analyzer configuration for analyzer " + analyzer.getId() + ": " + e.getMessage());
        }

        return map;
    }
}

