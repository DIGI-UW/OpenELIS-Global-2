package org.openelisglobal.analyzer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import org.openelisglobal.analyzer.form.AnalyzerForm;
import org.openelisglobal.analyzer.service.AnalyzerErrorService;
import org.openelisglobal.analyzer.service.AnalyzerFieldService;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.service.AnalyzerTypeService;
import org.openelisglobal.analyzer.service.BridgeRegistrationService;
import org.openelisglobal.analyzer.service.FileImportService;
import org.openelisglobal.analyzer.service.SerialPortService;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    private FileImportService fileImportService;

    @Autowired
    private SerialPortService serialPortService;

    @Autowired
    private org.openelisglobal.analyzer.service.AnalyzerQueryService analyzerQueryService;

    @Autowired
    private PluginAnalyzerService pluginAnalyzerService;

    @Autowired
    private AnalyzerTypeService analyzerTypeService;

    @Autowired
    private PluginMenuService pluginService;

    @Autowired
    private BridgeRegistrationService bridgeRegistrationService;

    @Autowired
    private AnalyzerErrorService analyzerErrorService;

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("bridgeRegistrationExecutor")
    private Executor bridgeRegistrationExecutor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Bridge URL for outbound analyzer communication. When set, all test-connection
     * and query operations route through the bridge instead of direct TCP. This is
     * the production architecture — OE never connects directly to analyzers.
     *
     * <p>
     * Set via Spring property {@code analyzer.bridge.url} or env var
     * {@code ANALYZER_BRIDGE_URL}. The bridge is mandatory — OE never connects
     * directly to analyzers.
     */
    @Value("${analyzer.bridge.url:}")
    private String analyzerBridgeUrl;

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
            analyzer.setName(form.getName());
            analyzer.setType(form.getAnalyzerType());
            analyzer.setIpAddress(
                    form.getIpAddress() != null && !form.getIpAddress().trim().isEmpty() ? form.getIpAddress() : null);
            analyzer.setPort(form.getPort());
            if (form.getProtocolVersion() != null && !form.getProtocolVersion().trim().isEmpty()) {
                ProtocolVersion pv = ProtocolVersion.fromValue(form.getProtocolVersion());
                analyzer.setProtocolVersion(pv != null ? pv : ProtocolVersion.ASTM_LIS2_A2);
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

            String statusStr = form.getStatus() != null ? form.getStatus() : "SETUP";
            try {
                analyzer.setStatus(AnalyzerStatus.valueOf(statusStr));
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid status value: {}, defaulting to SETUP", statusStr);
                analyzer.setStatus(AnalyzerStatus.SETUP);
            }

            analyzer.setSysUserId(getSysUserId(request));
            String analyzerId = analyzerService.insert(analyzer);
            pluginService.registerAnalyzerMenuAndPermission(analyzer.getName(), analyzerId);

            // Auto-create test mappings and file import config from default profile if
            // provided
            if (form.getDefaultConfigId() != null && !form.getDefaultConfigId().isEmpty()) {
                Map<String, Object> configData = loadDefaultConfigFile(form.getDefaultConfigId());
                if (configData != null) {
                    analyzerService.autoCreateTestMappings(analyzerId, configData, getSysUserId(request));

                    // For FILE protocol profiles, auto-create FileImportConfiguration
                    if (isFileProtocol(configData)) {
                        fileImportService.autoCreateFromProfile(analyzerId, configData, form.getName(),
                                getSysUserId(request));
                    }
                } else {
                    logger.warn("Could not load default config '{}' for test mapping auto-creation",
                            form.getDefaultConfigId());
                }
            }

            // Use getWithType() to eagerly fetch AnalyzerType within the service
            // transaction — prevents LazyInitializationException in analyzerToMap()
            Analyzer createdAnalyzer = analyzerService.getWithType(analyzerId).orElse(null);
            if (createdAnalyzer == null) {
                throw new LIMSRuntimeException("Failed to retrieve created analyzer");
            }

            // Register with bridge synchronously — analyzer is not fully operational
            // until the bridge confirms it can route results for it.
            boolean bridgeRegistered = registerWithBridge(createdAnalyzer);

            Map<String, Object> response = analyzerToMap(createdAnalyzer, getLoadedPluginClassNames());
            response.put("bridgeRegistered", bridgeRegistered);
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
     * analyzer.
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

            // All connectivity checks route through the bridge — OE never
            // directly tests analyzer transports (bridge is mandatory).
            Map<String, Object> response;
            Integer analyzerIdInt = Integer.valueOf(id);
            var fileConfig = fileImportService.getByAnalyzerId(analyzerIdInt);
            var serialConfig = serialPortService.getByAnalyzerId(analyzerIdInt);

            if (fileConfig.isPresent()) {
                response = testFileViaBridge(fileConfig.get().getImportDirectory());
            } else if (serialConfig.isPresent()) {
                response = testSerialViaBridge(serialConfig.get().getPortName());
            } else if (analyzer.getIpAddress() != null && analyzer.getPort() != null) {
                response = testTcpAnalyzerConnection(analyzer);
            } else {
                response = new LinkedHashMap<>();
                response.put("success", false);
                response.put("message", "No transport configured (missing IP/port, file import, or serial config)");
            }

            response.put("analyzerId", id);
            response.put("analyzerName", analyzer.getName());
            response.put("protocol",
                    analyzer.getProtocolVersion() != null ? analyzer.getProtocolVersion().name() : null);
            response.put("communicationMode", analyzer.getEffectiveCommunicationMode().name());
            if (analyzer.getIpAddress() != null) {
                response.put("ipAddress", analyzer.getIpAddress());
            }
            if (analyzer.getPort() != null) {
                response.put("port", analyzer.getPort());
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
            Analyzer analyzer = analyzerService.get(id);
            if (analyzer == null) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("error", "Analyzer not found: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
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
            // Update lifecycle status if provided (SETUP → ACTIVE → INACTIVE → DELETED)
            if (form.getStatus() != null) {
                try {
                    analyzer.setStatus(AnalyzerStatus.valueOf(form.getStatus()));
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid status value: {}, keeping existing status", form.getStatus());
                }
            }

            analyzer.setSysUserId(getSysUserId(request));
            analyzerService.update(analyzer);

            // Re-register transport mapping with bridge synchronously.
            Analyzer updatedAnalyzer = analyzerService.get(id);
            boolean bridgeRegistered = registerWithBridge(updatedAnalyzer);
            Map<String, Object> response = analyzerToMap(updatedAnalyzer, getLoadedPluginClassNames());
            response.put("bridgeRegistered", bridgeRegistered);
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
     * Implements 90-day soft delete window per spec requirement:
     * <ul>
     * <li>If analyzer has recent results (within 90 days): soft delete (status =
     * DELETED)</li>
     * <li>If analyzer has no recent results: hard delete (remove from
     * database)</li>
     * </ul>
     *
     * <p>
     * Note: Uses POST instead of DELETE HTTP method due to Spring Security 6 CSRF
     * protection blocking DELETE requests even with valid CSRF tokens.
     *
     * @param id Analyzer ID to delete
     * @return 200 on success with deletion details, 404 if analyzer not found
     */
    @PostMapping("/analyzers/{id}/delete")
    public ResponseEntity<Map<String, Object>> deleteAnalyzer(@PathVariable String id) {
        try {
            Analyzer analyzer = analyzerService.get(id);
            if (analyzer == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            analyzer.setStatus(AnalyzerStatus.DELETED);
            analyzer.setActive(false);
            analyzerService.update(analyzer);

            unregisterFromBridgeAsync(id, analyzer.getName());
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

        // Plugin loaded check — O(1) via pre-computed Set
        boolean pluginLoaded;
        if (analyzer.getAnalyzerType() != null) {
            String className = analyzer.getAnalyzerType().getPluginClassName();
            pluginLoaded = className != null && loadedPlugins.contains(className);
        } else {
            pluginLoaded = pluginAnalyzerService.getPluginByAnalyzerId(analyzer.getId()) != null;
            if (!pluginLoaded) {
                // Fallback: match analyzer name against loaded plugin class simple names.
                // Handles analyzers not yet linked to an AnalyzerType (e.g., fixture data
                // inserted after startup, or multi-analyzer plugins like Cobas4800).
                String analyzerName = analyzer.getName();
                pluginLoaded = loadedPlugins.stream().anyMatch(cn -> {
                    String simpleName = cn.substring(cn.lastIndexOf('.') + 1);
                    return simpleName.equals(analyzerName) || simpleName.equals(analyzerName + "Analyzer")
                            || analyzerName.startsWith(simpleName.replaceAll("Analyzer$", ""));
                });
            }
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

        // FILE transport fields (unified on analyzer table — same as TCP fields above)
        map.put("importDirectory", analyzer.getImportDirectory());
        map.put("filePattern", analyzer.getFilePattern());
        map.put("columnMappings", analyzer.getColumnMappings());
        map.put("fileFormat", analyzer.getFileFormat());

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

    /**
     * Unified TCP analyzer test-connection. Always checks bridge health and always
     * attempts TCP to the analyzer. The communication mode determines how results
     * are interpreted:
     *
     * <p>
     * Success requires both bridge health AND TCP reachability when IP/port are
     * configured, regardless of communication mode. If the user configured IP/port,
     * a network failure should be surfaced. The communication mode determines the
     * messaging context (push vs pull), not whether TCP matters.
     * </p>
     *
     * <ul>
     * <li>{@code ANALYZER_INITIATED}: Analyzer pushes to bridge. TCP failure
     * messaging notes that the analyzer may still reach the bridge even if OE
     * cannot reach the analyzer directly.</li>
     * <li>{@code LIS_INITIATED}: OE/bridge reaches the analyzer for queries/orders.
     * TCP failure is critical.</li>
     * <li>{@code BOTH}: Bidirectional — both paths must work.</li>
     * </ul>
     */
    private Map<String, Object> testTcpAnalyzerConnection(Analyzer analyzer) {
        Map<String, Object> response = new LinkedHashMap<>();
        long startTime = System.currentTimeMillis();

        CommunicationMode mode = analyzer.getEffectiveCommunicationMode();
        String ip = analyzer.getIpAddress();
        Integer port = analyzer.getPort();

        // Step 1: Bridge health (bridge is mandatory for all analyzer communication)
        boolean bridgeHealthy = false;
        String bridgeMessage = null;
        if (analyzerBridgeUrl != null && !analyzerBridgeUrl.isBlank()) {
            Map<String, Object> bridgeResult = checkBridgeHealth();
            bridgeHealthy = Boolean.TRUE.equals(bridgeResult.get("healthy"));
            bridgeMessage = (String) bridgeResult.get("message");
            response.put("bridgeHealthy", bridgeHealthy);
            response.put("bridgeMessage", bridgeMessage);
        } else {
            response.put("bridgeHealthy", false);
            response.put("bridgeMessage", "Bridge URL not configured (analyzer.bridge.url)");
            logger.warn("No analyzer.bridge.url configured — bridge is required for production.");
        }

        // Step 2: Test analyzer reachability via bridge (bridge is on analyzer
        // networks)
        boolean tcpReachable = false;
        String tcpMessage = null;
        if (ip != null && port != null && analyzerBridgeUrl != null && !analyzerBridgeUrl.isBlank()) {
            // TCP-only for connectivity test — protocol handshakes (ASTM ENQ, MLLP)
            // can cause contention with active analyzers. Reachability is what matters.
            Map<String, Object> tcpResult = testConnectivityViaBridge(ip, port, "TCP");
            tcpReachable = Boolean.TRUE.equals(tcpResult.get("reachable"));
            tcpMessage = (String) tcpResult.get("message");
            response.put("tcpReachable", tcpReachable);
            response.put("tcpMessage", tcpMessage);
        } else if (ip != null && port != null) {
            response.put("tcpReachable", false);
            response.put("tcpMessage", "analyzer.testConnection.tcp.bridgeNotConfigured");
            response.put("tcpMessageKey", "analyzer.testConnection.tcp.bridgeNotConfigured");
        }

        // Step 3: Interpret results based on communication mode
        boolean success;
        StringBuilder message = new StringBuilder();

        // If IP/port is configured, TCP must succeed — regardless of mode.
        // Mode affects the messaging context, not whether TCP matters.
        boolean tcpConfigured = ip != null && port != null;
        success = bridgeHealthy && (!tcpConfigured || tcpReachable);

        switch (mode) {
        case ANALYZER_INITIATED:
            response.put("connectionType", "Analyzer-initiated via bridge");
            if (success) {
                message.append("Bridge listener ready.");
                if (tcpReachable) {
                    message.append(" Analyzer reachable at ").append(ip).append(":").append(port).append(".");
                }
                message.append(" Analyzer will connect to bridge when sending results.");
            } else {
                if (!bridgeHealthy) {
                    message.append("Bridge not healthy — analyzer cannot connect. ");
                    message.append(bridgeMessage != null ? bridgeMessage : "");
                }
                if (tcpConfigured && !tcpReachable) {
                    message.append("Analyzer not reachable at ").append(ip).append(":").append(port).append(". ");
                    message.append(tcpMessage != null ? tcpMessage : "");
                }
            }
            break;

        case LIS_INITIATED:
            response.put("connectionType", "LIS-initiated via bridge");
            if (success) {
                message.append("Bridge ready. Analyzer reachable at ").append(ip).append(":").append(port)
                        .append(" — ready for LIS-initiated communication.");
            } else {
                if (!bridgeHealthy) {
                    message.append("Bridge not healthy — cannot route to analyzer. ");
                    message.append(bridgeMessage != null ? bridgeMessage : "");
                }
                if (tcpConfigured && !tcpReachable) {
                    message.append("Cannot reach analyzer at ").append(ip).append(":").append(port)
                            .append(" — verify analyzer is powered on and listening. ");
                    message.append(tcpMessage != null ? tcpMessage : "");
                }
            }
            break;

        case BOTH:
            response.put("connectionType", "Bidirectional via bridge");
            if (success) {
                message.append("Bidirectional communication verified. Bridge ready, analyzer reachable at ").append(ip)
                        .append(":").append(port).append(".");
            } else {
                if (!bridgeHealthy) {
                    message.append("Bridge not healthy. ");
                }
                if (tcpConfigured && !tcpReachable) {
                    message.append("Analyzer not reachable at ").append(ip).append(":").append(port).append(". ");
                }
            }
            break;

        default:
            message.append("Unknown communication mode: ").append(mode);
        }

        response.put("success", success);
        response.put("message", message.toString().trim());
        response.put("responseTimeMs", System.currentTimeMillis() - startTime);
        return response;
    }

    /**
     * Test analyzer connectivity by delegating to the bridge's
     * {@code /api/test-connectivity} endpoint. The bridge is on analyzer networks
     * and performs the actual TCP/ASTM/MLLP check. OE never opens direct sockets to
     * analyzer IPs.
     *
     * @param host     Analyzer IP address
     * @param port     Analyzer port
     * @param protocol "HL7", "ASTM", or "TCP" (determines handshake type)
     * @return Map with reachable (boolean) and message (String)
     */
    private Map<String, Object> testConnectivityViaBridge(String host, Integer port, String protocol) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("transport", "TCP");
            payload.put("host", host);
            payload.put("port", port);
            payload.put("protocol", protocol != null ? protocol : "TCP");
            return callBridgeTestConnectivity(objectMapper.writeValueAsString(payload));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            return Map.of("reachable", false, "message", "analyzer.testConnection.requestBuildFailed", "messageKey",
                    "analyzer.testConnection.requestBuildFailed", "messageArgs",
                    Map.of("detail", String.valueOf(e.getMessage())));
        }
    }

    /**
     * Call the bridge's {@code /api/test-connectivity} endpoint with arbitrary JSON
     * payload. Used for TCP, FILE, and SERIAL transports.
     */
    private Map<String, Object> callBridgeTestConnectivity(String json) {
        Map<String, Object> result = new LinkedHashMap<>();
        String endpoint = analyzerBridgeUrl.replaceAll("/+$", "") + "/api/test-connectivity";

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
            conn.setReadTimeout(10000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            String body = "";
            try (InputStream is = (status < 400) ? conn.getInputStream() : conn.getErrorStream()) {
                if (is != null) {
                    body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            }

            if (status == 200) {
                try {
                    Map<String, Object> bridgeResponse = objectMapper.readValue(body,
                            new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                            });
                    result.putAll(bridgeResponse);
                } catch (Exception parseEx) {
                    result.put("reachable", false);
                    result.put("message", "analyzer.testConnection.bridge.unparseableResponse");
                    result.put("messageKey", "analyzer.testConnection.bridge.unparseableResponse");
                }
            } else {
                result.put("reachable", false);
                result.put("message", "analyzer.testConnection.bridge.httpStatus");
                result.put("messageKey", "analyzer.testConnection.bridge.httpStatus");
                result.put("messageArgs", Map.of("status", status));
            }

            logger.info("Bridge test-connectivity: reachable={}", result.get("reachable"));
        } catch (Exception e) {
            result.put("reachable", false);
            result.put("message", "analyzer.testConnection.bridge.unreachable");
            result.put("messageKey", "analyzer.testConnection.bridge.unreachable");
            result.put("messageArgs", Map.of("detail", String.valueOf(e.getMessage())));
            logger.error("Bridge test-connectivity failed", e);
        }

        return result;
    }

    /**
     * Test FILE analyzer connectivity via bridge. The bridge checks if the import
     * directory exists and is accessible from its filesystem.
     */
    private Map<String, Object> testFileViaBridge(String importDirectory) {
        Map<String, Object> response = new LinkedHashMap<>();
        if (analyzerBridgeUrl == null || analyzerBridgeUrl.isBlank()) {
            response.put("success", false);
            response.put("message", "analyzer.testConnection.bridge.notConfigured");
            response.put("messageKey", "analyzer.testConnection.bridge.notConfigured");
            return response;
        }

        Map<String, Object> result;
        try {
            result = callBridgeTestConnectivity(
                    objectMapper.writeValueAsString(Map.of("transport", "FILE", "path", importDirectory)));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            response.put("success", false);
            response.put("message", "analyzer.testConnection.requestBuildFailed");
            response.put("messageKey", "analyzer.testConnection.requestBuildFailed");
            response.put("messageArgs", Map.of("detail", String.valueOf(e.getMessage())));
            return response;
        }
        response.put("success", Boolean.TRUE.equals(result.get("reachable")));
        response.put("message", result.getOrDefault("message", ""));
        response.put("connectionType", "FILE via bridge");
        return response;
    }

    /**
     * Test SERIAL analyzer connectivity via bridge. The bridge checks if the serial
     * device path exists and is accessible.
     */
    private Map<String, Object> testSerialViaBridge(String portName) {
        Map<String, Object> response = new LinkedHashMap<>();
        if (analyzerBridgeUrl == null || analyzerBridgeUrl.isBlank()) {
            response.put("success", false);
            response.put("message", "analyzer.testConnection.bridge.notConfigured");
            response.put("messageKey", "analyzer.testConnection.bridge.notConfigured");
            return response;
        }

        Map<String, Object> result;
        try {
            result = callBridgeTestConnectivity(
                    objectMapper.writeValueAsString(Map.of("transport", "SERIAL", "path", portName)));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            response.put("success", false);
            response.put("message", "analyzer.testConnection.requestBuildFailed");
            response.put("messageKey", "analyzer.testConnection.requestBuildFailed");
            response.put("messageArgs", Map.of("detail", String.valueOf(e.getMessage())));
            return response;
        }
        response.put("success", Boolean.TRUE.equals(result.get("reachable")));
        response.put("message", result.getOrDefault("message", ""));
        response.put("connectionType", "Serial via bridge");
        return response;
    }

    /**
     * Check bridge health via Spring Boot Actuator endpoint.
     *
     * @return Map with {@code healthy} (boolean) and {@code message} (String)
     */
    private Map<String, Object> checkBridgeHealth() {
        Map<String, Object> result = new LinkedHashMap<>();
        String healthUrl = analyzerBridgeUrl.replaceAll("/+$", "") + "/actuator/health";

        try {
            URL url = new URL(healthUrl);
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
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int status = conn.getResponseCode();
            String body = "";
            try (InputStream is = (status < 400) ? conn.getInputStream() : conn.getErrorStream()) {
                if (is != null) {
                    body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            }

            boolean healthy = false;
            if (status == 200) {
                try {
                    Map<String, Object> healthJson = objectMapper.readValue(body,
                            new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                            });
                    healthy = "UP".equals(healthJson.get("status"));
                } catch (Exception parseEx) {
                    logger.warn("Could not parse bridge health JSON: {}", parseEx.getMessage());
                }
            }
            result.put("healthy", healthy);
            String healthMessage = healthy ? "Bridge healthy (status UP)"
                    : "Bridge returned HTTP " + status + " (status: "
                            + (body.length() > 200 ? body.substring(0, 200) + "..." : body) + ")";
            result.put("message", healthMessage);
            logger.info("Bridge health check: {} (HTTP {})", healthy ? "UP" : "NOT UP", status);
        } catch (Exception e) {
            result.put("healthy", false);
            result.put("message", "analyzer.testConnection.bridge.healthCheckFailed");
            result.put("messageKey", "analyzer.testConnection.bridge.healthCheckFailed");
            result.put("messageArgs", Map.of("url", healthUrl, "detail", String.valueOf(e.getMessage())));
            logger.error("Bridge health check failed: {}", healthUrl, e);
        }

        return result;
    }

    // testConnectionViaBridge() removed — bridge health is now checked via
    // checkBridgeHealth() in the unified testTcpAnalyzerConnection() method.
    // The bridge ASTM forwarding endpoint (POST /?forwardAddress=&forwardPort=)
    // is still available for direct use by the bridge's own ASTM test tooling.
    /**
     * Register analyzer with the bridge for transport-level identification. Runs in
     * background — failures are logged but don't prevent analyzer creation.
     */
    /**
     * Register analyzer with the bridge synchronously. Returns true if the bridge
     * confirmed registration, false if the bridge was unreachable or rejected.
     *
     * <p>
     * Called during create/update — the analyzer is not fully operational until the
     * bridge confirms it can route results for it.
     */
    private boolean registerWithBridge(Analyzer analyzer) {
        try {
            String id = analyzer.getId();
            String name = analyzer.getName();
            boolean registered = false;

            // TCP/ASTM/HL7 analyzers: register by IP
            if (analyzer.getIpAddress() != null && !analyzer.getIpAddress().isBlank()) {
                String protocol = analyzer.getProtocolVersion() != null && analyzer.getProtocolVersion().isHl7() ? "HL7"
                        : "ASTM";
                registered = bridgeRegistrationService.registerTcp(id, name, analyzer.getIpAddress(),
                        analyzer.getPort(), protocol);
            }

            // FILE analyzers: register by watch directory (use unified fields on Analyzer)
            if (analyzer.getImportDirectory() != null && !analyzer.getImportDirectory().isBlank()) {
                registered = bridgeRegistrationService.registerFile(id, name, analyzer.getImportDirectory(),
                        analyzer.getFilePattern(), analyzer.getColumnMappings());
            } else {
                // Fallback: check legacy FileImportConfiguration table
                try {
                    Integer analyzerIdInt = Integer.valueOf(id);
                    var fileConfig = fileImportService.getByAnalyzerId(analyzerIdInt);
                    if (fileConfig.isPresent()) {
                        var fc = fileConfig.get();
                        registered = bridgeRegistrationService.registerFile(id, name, fc.getImportDirectory(),
                                fc.getFilePattern(), fc.getColumnMappings());
                    }
                } catch (NumberFormatException e) {
                    logger.debug("Analyzer id '{}' is not numeric; skipping legacy FileImportConfiguration fallback",
                            id);
                }
            }

            if (registered) {
                logger.info("Bridge registration confirmed for analyzer '{}' (id={})", name, id);
            }
            return registered;
        } catch (Exception e) {
            logger.warn("Bridge registration failed for analyzer '{}': {}", analyzer.getName(), e.getMessage());
            return false;
        }
    }

    private void unregisterFromBridgeAsync(String analyzerId, String analyzerName) {
        CompletableFuture.runAsync(() -> bridgeRegistrationService.unregister(analyzerId), bridgeRegistrationExecutor)
                .exceptionally(e -> {
                    logger.warn("Async bridge unregister failed for analyzer {} ({})", analyzerName, analyzerId, e);
                    return null;
                });
    }

    // testFileConfiguration() and testSerialConfiguration() removed — replaced
    // by testFileViaBridge() and testSerialViaBridge() which route through the
    // bridge's /api/test-connectivity endpoint. OE never checks file/serial
    // transports directly — the bridge owns those transports.

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
     * GET /rest/analyzer/profiles List available analyzer profile templates from
     * filesystem.
     *
     * <p>
     * Returns minimal metadata for each template: id (e.g., "astm/mindray-ba88a"),
     * protocol ("ASTM" or "HL7"), analyzer_name (from JSON).
     *
     * <p>
     * Built-in profiles are intentionally immutable and exposed via read-only GET
     * endpoints. There are no write endpoints for /profiles/**.
     */
    @GetMapping({ "/profiles", "/defaults" })
    public ResponseEntity<?> getDefaults() {
        try {
            String defaultsDir = System.getenv("ANALYZER_PROFILES_DIR");
            if (defaultsDir == null || defaultsDir.isEmpty()) {
                defaultsDir = "/data/analyzer-profiles";
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

            // Scan FILE directory
            Path fileDir = baseDir.resolve("file");
            if (Files.exists(fileDir) && Files.isDirectory(fileDir)) {
                scanTemplates(fileDir, "file", templates);
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
     * GET /rest/analyzer/profiles/{protocol}/{name} Load specific profile
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
    @GetMapping({ "/profiles/{protocol}/{name}", "/defaults/{protocol}/{name}" })
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> getDefaultConfig(@PathVariable String protocol,
            @PathVariable String name) {
        try {
            Path templateFile = resolveConfigFilePath(protocol, name);
            if (templateFile == null) {
                // Determine specific error for HTTP response
                if (!protocol.equalsIgnoreCase("astm") && !protocol.equalsIgnoreCase("hl7")
                        && !protocol.equalsIgnoreCase("file")) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                            AnalyzerControllerHelper.wrapError("Invalid protocol: must be 'astm', 'hl7', or 'file'"));
                }
                if (!name.matches("^[a-zA-Z0-9\\-_.]+$")) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(AnalyzerControllerHelper
                            .wrapError("Invalid filename: only alphanumeric, dash, underscore, and period allowed"));
                }
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(AnalyzerControllerHelper.wrapError("Template not found: " + protocol + "/" + name));
            }

            String jsonContent = Files.readString(templateFile, StandardCharsets.UTF_8);
            Map<String, Object> config = objectMapper.readValue(jsonContent, Map.class);
            String schemaValidationError = validateProfileMeta(config, protocol + "/" + name);
            if (schemaValidationError != null) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body(AnalyzerControllerHelper.wrapError(schemaValidationError));
            }
            return ResponseEntity.ok(config);

        } catch (IOException e) {
            logger.error("Error reading default config: {}/{}", protocol, name, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalyzerControllerHelper.wrapError("Failed to read template: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Error loading default config", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalyzerControllerHelper.wrapError("Failed to load template: " + e.getMessage()));
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
     * Resolve and validate a config template file path. Shared validation logic
     * used by both the HTTP endpoint ({@code getDefaultConfig}) and internal
     * callers ({@code loadDefaultConfigFile}).
     *
     * @param protocol Protocol name ("astm" or "hl7")
     * @param name     Template filename (with or without .json extension)
     * @return Validated Path to the template file, or null if validation fails or
     *         file not found
     */
    private Path resolveConfigFilePath(String protocol, String name) {
        if (!protocol.equalsIgnoreCase("astm") && !protocol.equalsIgnoreCase("hl7")
                && !protocol.equalsIgnoreCase("file")) {
            return null;
        }
        if (!name.matches("^[a-zA-Z0-9\\-_.]+$")) {
            return null;
        }

        String filename = name.endsWith(".json") ? name : name + ".json";
        String defaultsDir = System.getenv("ANALYZER_PROFILES_DIR");
        if (defaultsDir == null || defaultsDir.isEmpty()) {
            defaultsDir = "/data/analyzer-profiles";
        }

        Path baseDir = Path.of(defaultsDir);
        Path templateFile = baseDir.resolve(protocol).resolve(filename).normalize();
        if (!templateFile.startsWith(baseDir.normalize())) {
            return null;
        }

        if (!Files.exists(templateFile) || !Files.isRegularFile(templateFile)) {
            return null;
        }

        return templateFile;
    }

    /**
     * Load a default config JSON file from the filesystem. Returns null if
     * validation fails or file not found.
     *
     * @param configId Config ID in "protocol/name" format (e.g.,
     *                 "astm/genexpert-astm")
     * @return Parsed JSON as Map, or null
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> loadDefaultConfigFile(String configId) {
        if (configId == null || !configId.contains("/")) {
            return null;
        }

        String[] parts = configId.split("/", 2);
        Path templateFile = resolveConfigFilePath(parts[0], parts[1]);
        if (templateFile == null) {
            return null;
        }

        try {
            String jsonContent = Files.readString(templateFile, StandardCharsets.UTF_8);
            return objectMapper.readValue(jsonContent, Map.class);
        } catch (IOException e) {
            logger.error("Error reading default config file: {}", configId, e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private boolean isFileProtocol(Map<String, Object> configData) {
        Object protocol = configData.get("protocol");
        if (protocol instanceof Map) {
            Object name = ((Map<String, Object>) protocol).get("name");
            return "FILE".equalsIgnoreCase(name instanceof String ? (String) name : null);
        }
        return false;
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
                    String schemaValidationError = validateProfileMeta(config, protocol + "/" + file.getFileName());
                    if (schemaValidationError != null) {
                        logger.warn("Skipping profile template due to invalid schema: {}", schemaValidationError);
                        return;
                    }

                    Map<String, Object> template = new LinkedHashMap<>();
                    String filename = file.getFileName().toString().replace(".json", "");
                    template.put("id", protocol + "/" + filename);
                    template.put("protocol", protocol.toUpperCase());

                    // Top-level keys (ASTM/HL7 profiles)
                    String analyzerName = (String) config.get("analyzer_name");
                    String manufacturer = (String) config.get("manufacturer");
                    String category = (String) config.get("category");

                    // Fallback to profileMeta (FILE profiles store data there)
                    @SuppressWarnings("unchecked")
                    Map<String, Object> profileMeta = (Map<String, Object>) config.get("profileMeta");
                    if (profileMeta != null) {
                        if (analyzerName == null) {
                            analyzerName = (String) profileMeta.get("displayName");
                        }
                        if (manufacturer == null) {
                            manufacturer = (String) profileMeta.get("manufacturer");
                        }
                    }

                    template.put("analyzerName", analyzerName);
                    template.put("manufacturer", manufacturer);
                    template.put("category", category);

                    templates.add(template);
                } catch (Exception e) {
                    logger.warn("Failed to parse template file: {}", file.getFileName(), e);
                }
            });
        } catch (IOException e) {
            logger.warn("Failed to list template files in {}: {}", directory, e.getMessage());
        }
    }

    private String validateProfileMeta(Map<String, Object> config, String templateId) {
        if (config == null) {
            return "Invalid profile template '" + templateId + "': content is empty";
        }
        Object profileMetaObj = config.get("profileMeta");
        if (!(profileMetaObj instanceof Map<?, ?> profileMeta)) {
            return "Invalid profile template '" + templateId + "': missing required profileMeta object";
        }
        if (isBlank(profileMeta.get("id")) || isBlank(profileMeta.get("version"))
                || isBlank(profileMeta.get("displayName"))) {
            return "Invalid profile template '" + templateId
                    + "': profileMeta must include non-empty id, version, and displayName";
        }
        return null;
    }

    private boolean isBlank(Object value) {
        return value == null || String.valueOf(value).trim().isEmpty();
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
        stub.setName(displayName);
        stub.setStatus(AnalyzerStatus.PENDING_REGISTRATION);
        stub.setDiscoveredSourceId(sourceId);
        stub.setSysUserId("bridge-discovery");

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
