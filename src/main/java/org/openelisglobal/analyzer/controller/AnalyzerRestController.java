package org.openelisglobal.analyzer.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.openelisglobal.analyzer.form.AnalyzerForm;
import org.openelisglobal.analyzer.service.AnalyzerErrorService;
import org.openelisglobal.analyzer.service.AnalyzerFieldService;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.service.AnalyzerTypeService;
import org.openelisglobal.analyzer.util.NetworkValidationUtil;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.Analyzer.AnalyzerStatus;
import org.openelisglobal.analyzer.valueholder.AnalyzerError;
import org.openelisglobal.analyzer.valueholder.AnalyzerType;
import org.openelisglobal.analyzer.valueholder.CommunicationMode;
import org.openelisglobal.analyzer.valueholder.ProtocolVersion;
import org.openelisglobal.analyzerimport.util.AnalyzerTestNameCache;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.common.services.PluginMenuService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Analyzer management. Handles CRUD operations for
 * analyzers using the 2-table model (Analyzer + AnalyzerType).
 */
@RestController
@RequestMapping("/rest/analyzer")
public class AnalyzerRestController extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzerRestController.class);

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private AnalyzerFieldService analyzerFieldService;

    @Autowired
    private org.openelisglobal.analyzer.service.AnalyzerQueryService analyzerQueryService;

    @Autowired
    private org.openelisglobal.analyzer.service.AnalyzerOrderDispatchService analyzerOrderDispatchService;

    @Autowired
    private PluginAnalyzerService pluginAnalyzerService;

    @Autowired
    private AnalyzerTypeService analyzerTypeService;

    @Autowired
    private PluginMenuService pluginService;

    @Autowired
    private AnalyzerErrorService analyzerErrorService;

    /**
     * GET /rest/analyzer/analyzers Retrieve all analyzers with their
     * configurations.
     */
    @GetMapping("/analyzers")
    public ResponseEntity<Map<String, Object>> getAnalyzers(@RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        try {
            List<Analyzer> analyzers = analyzerService.getAllWithTypes();
            Set<String> loadedPlugins = getLoadedPluginClassNames();
            List<Map<String, Object>> analyzerList = new ArrayList<>();

            for (Analyzer analyzer : analyzers) {
                Map<String, Object> analyzerMap = analyzerToMap(analyzer, loadedPlugins);

                // Skip DELETED analyzers (soft-deleted with 90-day window)
                String analyzerStatus = (String) analyzerMap.get("status");
                if ("DELETED".equals(analyzerStatus)) {
                    continue;
                }

                if (search != null && !search.isEmpty()) {
                    String searchLower = search.toLowerCase();
                    if (!analyzer.getName().toLowerCase().contains(searchLower) && (analyzer.getType() == null
                            || !analyzer.getType().toLowerCase().contains(searchLower))) {
                        continue;
                    }
                }

                if (status != null && !status.isEmpty()) {
                    if (analyzerStatus == null || !analyzerStatus.equalsIgnoreCase(status)) {
                        continue;
                    }
                }

                analyzerList.add(analyzerMap);
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("analyzers", analyzerList);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error retrieving analyzers", e);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("analyzers", new ArrayList<>());
            error.put("error", "Error retrieving analyzers");
            if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                error.put("message", e.getMessage());
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * POST /rest/analyzer/analyzers Create new analyzer.
     */
    @PostMapping("/analyzers")
    public ResponseEntity<Map<String, Object>> createAnalyzer(@RequestBody AnalyzerForm form,
            HttpServletRequest request) {
        try {
            if (form.getStatus() != null && !AnalyzerStatus.SETUP.name().equals(form.getStatus())) {
                return lifecycleStatusManaged();
            }
            // Collect all validation errors instead of failing on the first one
            List<String> validationErrors = new ArrayList<>();
            if (form.getName() == null || form.getName().trim().isEmpty()) {
                validationErrors.add("Analyzer name is required");
            }
            if (form.getAnalyzerType() == null || form.getAnalyzerType().trim().isEmpty()) {
                validationErrors.add("Analyzer type is required");
            }
            if (form.getIpAddress() != null && !form.getIpAddress().trim().isEmpty()
                    && !form.getIpAddress().matches("^(\\d{1,3}\\.){3}\\d{1,3}$")) {
                validationErrors.add("Invalid IPv4 address format");
            }
            if (form.getIpAddress() != null && !form.getIpAddress().trim().isEmpty()
                    && NetworkValidationUtil.isBlockedAddress(form.getIpAddress())) {
                validationErrors.add("Connection to this address is not permitted");
            }
            if (form.getPort() != null && (form.getPort() < 1 || form.getPort() > 65535)) {
                validationErrors.add("Port must be between 1 and 65535");
            }
            if (form.getProtocolVersion() != null && ProtocolVersion.fromValue(form.getProtocolVersion()) == null) {
                String validValues = java.util.Arrays.stream(ProtocolVersion.values()).map(ProtocolVersion::name)
                        .collect(Collectors.joining(", "));
                validationErrors.add(
                        "Invalid protocol version: " + form.getProtocolVersion() + ". Valid values: " + validValues);
            }
            if (form.getCommunicationMode() != null && !form.getCommunicationMode().trim().isEmpty()
                    && CommunicationMode.fromValue(form.getCommunicationMode()) == null) {
                String validValues = java.util.Arrays.stream(CommunicationMode.values()).map(CommunicationMode::name)
                        .collect(Collectors.joining(", "));
                validationErrors.add("Invalid communication mode: " + form.getCommunicationMode() + ". Valid values: "
                        + validValues);
            }
            if (!validationErrors.isEmpty()) {
                Map<String, Object> error = AnalyzerControllerHelper.wrapError(String.join("; ", validationErrors));
                error.put("validationErrors", validationErrors);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            // Create Analyzer entity — names are display labels, not unique constraints.
            // Multiple analyzers can share a name (e.g., two instruments of the same
            // model).
            Analyzer analyzer = new Analyzer();
            analyzer.ensureFhirUuid();
            analyzer.setActive(true);
            analyzer.setName(form.getName());
            analyzer.setType(form.getAnalyzerType());
            analyzer.setIpAddress(
                    form.getIpAddress() != null && !form.getIpAddress().trim().isEmpty() ? form.getIpAddress() : null);
            analyzer.setPort(form.getPort());
            if (form.getProtocolVersion() != null && !form.getProtocolVersion().trim().isEmpty()) {
                analyzer.setProtocolVersion(ProtocolVersion.fromValue(form.getProtocolVersion()));
            }
            if (form.getCommunicationMode() != null && !form.getCommunicationMode().trim().isEmpty()) {
                CommunicationMode cm = CommunicationMode.fromValue(form.getCommunicationMode());
                analyzer.setCommunicationMode(cm);
            }
            analyzer.setTestUnitIds(form.getTestUnitIds() != null ? form.getTestUnitIds() : new ArrayList<>());
            if (form.getIdentifierPattern() != null) {
                analyzer.setIdentifierPattern(form.getIdentifierPattern());
            }

            if (form.getPluginTypeId() != null && !form.getPluginTypeId().trim().isEmpty()) {
                AnalyzerType pluginType = resolvePluginType(form.getPluginTypeId());
                if (pluginType != null) {
                    analyzer.setAnalyzerType(pluginType);
                }
            }

            analyzer.setStatus(AnalyzerStatus.SETUP);

            // File import fields — allow the frontend to set these at creation time
            // so FILE analyzers can be fully configured in a single form submission.
            if (form.getImportDirectory() != null) {
                analyzer.setImportDirectory(form.getImportDirectory());
            }
            if (form.getFilePattern() != null) {
                analyzer.setFilePattern(form.getFilePattern());
            }
            if (form.getColumnMappings() != null) {
                analyzer.setColumnMappings(form.getColumnMappings());
            }
            if (form.getFileFormat() != null) {
                analyzer.setFileFormat(form.getFileFormat());
            }
            if (form.getDelimiter() != null) {
                analyzer.setDelimiter(form.getDelimiter());
            }
            if (form.getHasHeader() != null) {
                analyzer.setHasHeader(form.getHasHeader());
            }
            if (form.getSkipRows() != null) {
                analyzer.setSkipRows(form.getSkipRows());
            }

            analyzer.setSysUserId(getSysUserId(request));
            String analyzerId = analyzerService.insert(analyzer);
            pluginService.registerAnalyzerMenuAndPermission(analyzer.getName(), analyzerId);

            // Use getWithType() to eagerly fetch AnalyzerType within the service
            // transaction — prevents LazyInitializationException in analyzerToMap()
            Analyzer createdAnalyzer = analyzerService.getWithType(analyzerId).orElse(null);
            if (createdAnalyzer == null) {
                throw new LIMSRuntimeException("Failed to retrieve created analyzer");
            }

            Map<String, Object> response = analyzerToMap(createdAnalyzer, getLoadedPluginClassNames());
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
     * GET /rest/analyzer/analyzers/{id}/fields Get all fields for an analyzer.
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
     * GET /rest/analyzer/analyzers/{id} Retrieve analyzer by ID.
     */
    @GetMapping("/analyzers/{id}")
    public ResponseEntity<Map<String, Object>> getAnalyzer(@PathVariable String id) {
        try {
            Optional<Analyzer> opt = analyzerService.getWithType(id);
            if (opt.isEmpty()) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("error", "Analyzer not found: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            Map<String, Object> response = analyzerToMap(opt.get(), getLoadedPluginClassNames());
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
     * PUT /rest/analyzer/analyzers/{id} Update analyzer.
     */
    @PutMapping("/analyzers/{id}")
    public ResponseEntity<Map<String, Object>> updateAnalyzer(@PathVariable String id, @RequestBody AnalyzerForm form,
            HttpServletRequest request) {
        try {
            Optional<Analyzer> analyzerToUpdate = analyzerService.getWithType(id);
            if (analyzerToUpdate.isEmpty()) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("error", "Analyzer not found: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            Analyzer analyzer = analyzerToUpdate.get();
            if (form.getStatus() != null && !analyzer.getStatus().name().equals(form.getStatus())) {
                return lifecycleStatusManaged();
            }

            // Manual validation for optional fields
            if (form.getIpAddress() != null && !form.getIpAddress().trim().isEmpty()
                    && !form.getIpAddress().matches("^(\\d{1,3}\\.){3}\\d{1,3}$")) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("error", "Invalid IPv4 address format");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            if (form.getIpAddress() != null && !form.getIpAddress().trim().isEmpty()
                    && NetworkValidationUtil.isBlockedAddress(form.getIpAddress())) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("error", "Connection to this address is not permitted");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            if (form.getPort() != null && (form.getPort() < 1 || form.getPort() > 65535)) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("error", "Port must be between 1 and 65535");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            // Update analyzer fields (2-table model: all fields on Analyzer directly)
            if (form.getName() != null && !form.getName().trim().isEmpty()) {
                analyzer.setName(form.getName());
            }
            if (form.getAnalyzerType() != null && !form.getAnalyzerType().trim().isEmpty()) {
                analyzer.setType(form.getAnalyzerType());
            }
            if (form.getIpAddress() != null && !form.getIpAddress().trim().isEmpty()) {
                analyzer.setIpAddress(form.getIpAddress());
            }
            if (form.getPort() != null) {
                analyzer.setPort(form.getPort());
            }
            if (form.getProtocolVersion() != null) {
                ProtocolVersion updatedPv = ProtocolVersion.fromValue(form.getProtocolVersion());
                if (updatedPv == null) {
                    String validValues = java.util.Arrays.stream(ProtocolVersion.values()).map(ProtocolVersion::name)
                            .collect(Collectors.joining(", "));
                    Map<String, Object> error = new LinkedHashMap<>();
                    error.put("error", "analyzer.form.error.invalidProtocolVersion");
                    error.put("errorKey", "analyzer.form.error.invalidProtocolVersion");
                    error.put("errorArgs", Map.of("value", form.getProtocolVersion(), "validValues", validValues));
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
                }
                analyzer.setProtocolVersion(updatedPv);
            }
            if (form.getCommunicationMode() != null && !form.getCommunicationMode().trim().isEmpty()) {
                CommunicationMode cm = CommunicationMode.fromValue(form.getCommunicationMode());
                if (cm == null) {
                    String validValues = java.util.Arrays.stream(CommunicationMode.values())
                            .map(CommunicationMode::name).collect(Collectors.joining(", "));
                    Map<String, Object> error = new LinkedHashMap<>();
                    error.put("error", "analyzer.form.error.invalidCommunicationMode");
                    error.put("errorKey", "analyzer.form.error.invalidCommunicationMode");
                    error.put("errorArgs", Map.of("value", form.getCommunicationMode(), "validValues", validValues));
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
                }
                analyzer.setCommunicationMode(cm);
            }
            if (form.getTestUnitIds() != null) {
                analyzer.setTestUnitIds(form.getTestUnitIds());
            }
            if (form.getIdentifierPattern() != null) {
                analyzer.setIdentifierPattern(form.getIdentifierPattern());
            }
            if (form.getPluginTypeId() != null && !form.getPluginTypeId().trim().isEmpty()) {
                AnalyzerType pluginType = resolvePluginType(form.getPluginTypeId());
                if (pluginType != null) {
                    analyzer.setAnalyzerType(pluginType);
                }
            }
            if (form.getImportDirectory() != null) {
                analyzer.setImportDirectory(form.getImportDirectory());
            }
            if (form.getFilePattern() != null) {
                analyzer.setFilePattern(form.getFilePattern());
            }
            if (form.getColumnMappings() != null) {
                analyzer.setColumnMappings(form.getColumnMappings());
            }
            if (form.getFileFormat() != null) {
                analyzer.setFileFormat(form.getFileFormat());
            }
            if (form.getDelimiter() != null) {
                analyzer.setDelimiter(form.getDelimiter());
            }
            if (form.getHasHeader() != null) {
                analyzer.setHasHeader(form.getHasHeader());
            }
            if (form.getSkipRows() != null) {
                analyzer.setSkipRows(form.getSkipRows());
            }
            if (form.getProfileId() != null) {
                analyzerProfileBindingService.assignProfile(analyzer, form.getProfileId(), form.getProfileRevision(),
                        getSysUserId(request));
            }
            analyzer.setSysUserId(getSysUserId(request));
            analyzerService.update(analyzer);

            Analyzer updatedAnalyzer = analyzerService.getWithType(id)
                    .orElseThrow(() -> new LIMSRuntimeException("Failed to retrieve updated analyzer"));
            Map<String, Object> response = analyzerToMap(updatedAnalyzer, getLoadedPluginClassNames());
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
     * POST /rest/analyzer/analyzers/{id}/delete Delete analyzer.
     *
     * <p>
     * Always performs a soft delete: sets status to DELETED and active to false.
     * The analyzer record is retained for audit trail purposes. Uses POST instead
     * of DELETE due to Spring Security 6 CSRF protection.
     *
     * @param id      Analyzer ID to delete
     * @param request HTTP request (used to resolve the current user id for the
     *                audit trail)
     * @return 200 on success with deletion details, 404 if analyzer not found
     */
    @PostMapping("/analyzers/{id}/delete")
    public ResponseEntity<Map<String, Object>> deleteAnalyzer(@PathVariable String id, HttpServletRequest request) {
        try {
            Analyzer analyzer = analyzerService.get(id);
            if (analyzer == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            analyzer.setStatus(AnalyzerStatus.DELETED);
            analyzer.setActive(false);
            analyzer.setSysUserId(getSysUserId(request));
            analyzerService.update(analyzer);

            AnalyzerTestNameCache.getInstance().reloadCache();

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("message", "analyzer.delete.success");
            response.put("messageKey", "analyzer.delete.success");
            response.put("deleted", true);
            return ResponseEntity.ok(response);
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
     * Convert Analyzer entity to Map for JSON response. Reads all configuration
     * fields directly from the Analyzer entity (2-table model).
     */
    private Map<String, Object> analyzerToMap(Analyzer analyzer, Set<String> loadedPlugins) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", analyzer.getId());
        map.put("name", analyzer.getName());
        map.put("type", analyzer.getType());
        map.put("description", analyzer.getDescription());
        map.put("location", analyzer.getLocation());

        boolean pluginLoaded;
        if (analyzer.getAnalyzerType() != null) {
            String className = analyzer.getAnalyzerType().getPluginClassName();
            pluginLoaded = className != null && loadedPlugins.contains(className);
        } else {
            pluginLoaded = pluginAnalyzerService.getPluginByAnalyzerId(analyzer.getId()) != null;
        }
        map.put("pluginLoaded", pluginLoaded);

        // Configuration fields (stored directly on Analyzer in 2-table model)
        map.put("ipAddress", analyzer.getIpAddress());
        map.put("port", analyzer.getPort());
        map.put("protocolVersion", analyzer.getProtocolVersion() != null ? analyzer.getProtocolVersion().name() : null);
        map.put("communicationMode",
                analyzer.getCommunicationMode() != null ? analyzer.getCommunicationMode().name() : null);
        map.put("effectiveCommunicationMode", analyzer.getEffectiveCommunicationMode().name());
        map.put("testUnitIds", analyzer.getTestUnitIds());
        map.put("identifierPattern", analyzer.getIdentifierPattern());

        // FILE transport fields (unified on analyzer table — same as TCP fields above).
        // The bridge is strictly read-only with respect to watched directories since
        // plan mellow-honking-cascade Phase 1, so archive/error directories no longer
        // exist — processing state lives in the bridge's FileStateStore instead.
        map.put("importDirectory", analyzer.getImportDirectory());
        map.put("filePattern", analyzer.getFilePattern());
        map.put("columnMappings", analyzer.getColumnMappings());
        map.put("fileFormat", analyzer.getFileFormat());
        map.put("delimiter", analyzer.getDelimiter());
        map.put("hasHeader", analyzer.getHasHeader());
        map.put("skipRows", analyzer.getSkipRows());

        // Derive plugin type info from analyzer_type FK
        boolean isGeneric = analyzer.getAnalyzerType() != null && analyzer.getAnalyzerType().isGenericPlugin();
        map.put("genericPlugin", isGeneric);
        if (analyzer.getAnalyzerType() != null) {
            map.put("pluginTypeId", analyzer.getAnalyzerType().getId());
            map.put("pluginTypeName", analyzer.getAnalyzerType().getName());
        }

        // Lifecycle status (SETUP → ACTIVE → INACTIVE → DELETED)
        if (analyzer.getStatus() != null) {
            map.put("status", analyzer.getStatus().toString());
        } else {
            map.put("status", "SETUP");
        }

        // Audit field from BaseObject — surfaces "Last Modified" column in the
        // dashboard. Jackson serializes Timestamp as epoch millis; the frontend
        // formats with toLocaleDateString().
        map.put("lastModified", analyzer.getLastupdated());

        return map;
    }

    /**
     * Precompute the set of loaded plugin class names for O(1) lookups. Same
     * pattern as {@link AnalyzerTypeRestController#getLoadedPluginClassNames()}.
     */
    private Set<String> getLoadedPluginClassNames() {
        return pluginAnalyzerService.getAnalyzerPlugins().stream().map(plugin -> plugin.getClass().getName())
                .collect(Collectors.toSet());
    }

    private static ResponseEntity<Map<String, Object>> lifecycleStatusManaged() {
        Map<String, Object> body = AnalyzerControllerHelper.wrapError("analyzer.lifecycle.statusManaged");
        body.put("errorKey", "analyzer.lifecycle.statusManaged");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * POST /rest/analyzer/analyzers/{id}/query Start an asynchronous query job for
     * an analyzer.
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
     * POST /rest/analyzer/analyzers/{id}/send-order Dispatch an outbound LIS-
     * initiated order to the given analyzer via the bridge.
     *
     * <p>
     * Body: {@code { accessionNumber: string, patientId?: string, testCodes:
     * string[] }}. Returns HTTP 200 on successful bridge accept, 502 on bridge-side
     * failure (failed ACK, connection refused), 400 on validation, 422 on
     * configuration problems (missing IP/port, missing bridge URL).
     */
    @PostMapping("/analyzers/{id}/send-order")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> sendOrder(@PathVariable String id,
            @RequestBody Map<String, Object> body) {
        // OE2 is analyzer-agnostic: it sends only {accessionNumber}. The backend
        // resolves the accession's ordered tests → their LOINCs and posts a
        // LOINC order to the bridge, which owns LOINC→analyzer-code + message
        // building. No test codes cross this boundary.
        String accessionNumber = body.get("accessionNumber") instanceof String s ? s : null;
        try {
            org.openelisglobal.analyzer.service.AnalyzerOrderDispatchService.DispatchResult result = analyzerOrderDispatchService
                    .dispatchOrder(id, accessionNumber);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", result.success ? "DISPATCHED" : "FAILED");
            response.put("protocol", result.protocol);
            response.put("analyzerId", id);
            response.put("accessionNumber", accessionNumber);
            response.put("loincCodes", result.loincCodes);
            if (!result.success) {
                response.put("error", result.error);
            }
            return result.success ? ResponseEntity.ok(response)
                    : ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AnalyzerControllerHelper.wrapError(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(AnalyzerControllerHelper.wrapError(e.getMessage()));
        } catch (java.io.IOException e) {
            logger.warn("Bridge IO failure dispatching order for analyzer {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(AnalyzerControllerHelper.wrapError(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error dispatching order for analyzer {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalyzerControllerHelper.wrapError(e.getMessage()));
        }
    }

    /**
     * GET /rest/analyzer/analyzers/{id}/query/{jobId}/status Get query job status.
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
     * Resolve a pluginTypeId that may be numeric (database ID) or a well-known
     * alias like "generic-astm". Returns null if unresolvable.
     *
     * <p>
     * The frontend fallback list historically used string IDs ("generic-astm",
     * "generic-hl7") instead of database numeric IDs. This method gracefully
     * handles both formats to prevent NumberFormatException.
     */
    private AnalyzerType resolvePluginType(String pluginTypeId) {
        if (pluginTypeId == null || pluginTypeId.trim().isEmpty()) {
            return null;
        }

        // Try numeric ID first (normal path when frontend has real DB IDs)
        try {
            Integer.parseInt(pluginTypeId.trim());
            return analyzerTypeService.get(pluginTypeId);
        } catch (NumberFormatException e) {
            logger.info("Non-numeric pluginTypeId '{}', attempting name-based lookup", pluginTypeId);
        }

        // Map well-known frontend aliases to database names
        String lookupName;
        switch (pluginTypeId.toLowerCase()) {
        case "generic-astm":
            lookupName = "Generic ASTM";
            break;
        case "generic-file":
            lookupName = "Generic File";
            break;
        case "generic-hl7":
            lookupName = "Generic HL7";
            break;
        default:
            lookupName = pluginTypeId;
        }

        AnalyzerType type = analyzerTypeService.getAnalyzerTypeByName(lookupName);
        if (type == null) {
            logger.warn("Could not resolve pluginTypeId '{}' (tried name '{}')", pluginTypeId, lookupName);
        }
        return type;
    }

    /**
     * POST /rest/analyzer/discovered-sources Report an unknown analyzer source
     * discovered by the bridge. Creates a PENDING_REGISTRATION stub if no analyzer
     * with this sourceId exists. Idempotent via UNIQUE constraint on
     * discovered_source_id: duplicate inserts return the existing stub.
     */
    @PostMapping("/discovered-sources")
    public ResponseEntity<Map<String, Object>> reportDiscoveredSource(@RequestBody Map<String, String> body) {
        String sourceId = body.get("sourceId");
        String protocol = body.get("protocol");
        String protocolHint = body.get("protocolHint");
        String transport = body.get("transport");

        if (sourceId == null || sourceId.isBlank()) {
            return ResponseEntity.badRequest().body(AnalyzerControllerHelper.wrapError("sourceId is required"));
        }

        // Build display name with length safety (Analyzer.name is VARCHAR(100))
        String displayName = (protocolHint != null && !protocolHint.isBlank()) ? protocolHint
                : "Unknown (" + sourceId + ")";
        if (displayName.length() > 100) {
            displayName = displayName.substring(0, 97) + "...";
        }

        Analyzer stub = new Analyzer();
        stub.ensureFhirUuid();
        stub.setName(displayName);
        stub.setStatus(AnalyzerStatus.PENDING_REGISTRATION);
        stub.setDiscoveredSourceId(sourceId);

        // Try insert. UNIQUE index on discovered_source_id handles races.
        // On duplicate, catch the constraint violation and return existing stub.
        String analyzerId;
        try {
            analyzerId = analyzerService.insert(stub);
        } catch (Exception e) {
            if (isDuplicateKeyViolation(e)) {
                Optional<Analyzer> existing = analyzerService.findByDiscoveredSourceId(sourceId);
                if (existing.isPresent()) {
                    Analyzer found = existing.get();
                    Map<String, Object> response = new LinkedHashMap<>();
                    response.put("analyzerId", found.getId());
                    response.put("status", found.getStatus().name());
                    response.put("alreadyExists", true);
                    return ResponseEntity.ok(response);
                }
            }
            throw e;
        }

        // Error dashboard entry — best-effort (stub is the critical data)
        try {
            Analyzer created = analyzerService.get(analyzerId);
            String errorMsg = String.format(
                    "Unregistered source discovered: sourceId=%s, protocol=%s, transport=%s, hint=%s", sourceId,
                    protocol, transport, protocolHint);
            analyzerErrorService.createError(created, AnalyzerError.ErrorType.UNREGISTERED_SOURCE,
                    AnalyzerError.Severity.WARNING, errorMsg, null);
        } catch (Exception e) {
            logger.warn("Failed to create error entry for discovered source {}: {}", sourceId, e.getMessage());
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("analyzerId", analyzerId);
        response.put("status", AnalyzerStatus.PENDING_REGISTRATION.name());
        response.put("alreadyExists", false);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private boolean isDuplicateKeyViolation(Throwable e) {
        while (e != null) {
            String msg = e.getMessage();
            if (msg != null && (msg.contains("duplicate key") || msg.contains("unique constraint"))) {
                return true;
            }
            e = e.getCause();
        }
        return false;
    }

}
