package org.openelisglobal.analyzer.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzer.service.AstmConfigService;
import org.openelisglobal.analyzer.service.AstmPendingCodeService;
import org.openelisglobal.analyzer.service.AstmQcRuleService;
import org.openelisglobal.analyzer.service.AstmTestMappingConfigService;
import org.openelisglobal.analyzer.valueholder.AstmAnalyzerConfig;
import org.openelisglobal.analyzer.valueholder.AstmFieldExtractionConfig;
import org.openelisglobal.analyzer.valueholder.AstmFlagMapping;
import org.openelisglobal.analyzer.valueholder.AstmPendingCode;
import org.openelisglobal.analyzer.valueholder.AstmQcRule;
import org.openelisglobal.analyzer.valueholder.AstmTestMappingConfig;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.userrole.service.UserRoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/analyzer")
public class AstmConfigRestController extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(AstmConfigRestController.class);

    @Autowired
    private AstmConfigService astmConfigService;

    @Autowired
    private AstmQcRuleService astmQcRuleService;

    @Autowired
    private AstmTestMappingConfigService astmTestMappingConfigService;

    @Autowired
    private AstmPendingCodeService astmPendingCodeService;

    @Autowired
    private UserRoleService userRoleService;

    @GetMapping("/analyzers/{analyzerId}/astm-config")
    public ResponseEntity<Map<String, Object>> getAstmConfig(@PathVariable String analyzerId,
            HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> authorizationError = ensureAnalyzerConfigAccess(request);
        if (authorizationError != null) {
            return authorizationError;
        }
        try {
            AstmAnalyzerConfig config = astmConfigService.getConfig(analyzerId);
            if (config == null) {
                config = astmConfigService.getOrCreateConfig(analyzerId);
            }
            Map<String, Object> body = configToMap(config);
            body.put("extractionOverrides", extractionToMaps(astmConfigService.getExtractionConfigs(analyzerId)));
            body.put("flagMappings", flagMappingsToMaps(astmConfigService.getFlagMappings(analyzerId)));
            return ResponseEntity.ok(body);
        } catch (LIMSRuntimeException e) {
            return AnalyzerControllerHelper.mapExceptionToResponse(e);
        } catch (Exception e) {
            logger.error("Error getting ASTM config", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalyzerControllerHelper.error("ASTM_CONFIG_READ_ERROR", e.getMessage()));
        }
    }

    @PutMapping("/analyzers/{analyzerId}/astm-config")
    public ResponseEntity<Map<String, Object>> updateAstmConfig(@PathVariable String analyzerId,
            @RequestBody Map<String, Object> body, HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> authorizationError = ensureAnalyzerConfigAccess(request);
        if (authorizationError != null) {
            return authorizationError;
        }
        try {
            AstmAnalyzerConfig config = astmConfigService.updateConfig(analyzerId, body);
            Map<String, Object> response = configToMap(config);
            response.put("extractionOverrides", extractionToMaps(astmConfigService.getExtractionConfigs(analyzerId)));
            response.put("flagMappings", flagMappingsToMaps(astmConfigService.getFlagMappings(analyzerId)));
            return ResponseEntity.ok(response);
        } catch (LIMSRuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AnalyzerControllerHelper.error("ASTM_CONFIG_VALIDATION_ERROR", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating ASTM config", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalyzerControllerHelper.error("ASTM_CONFIG_UPDATE_ERROR", e.getMessage()));
        }
    }

    @GetMapping("/analyzers/{analyzerId}/qc-rules")
    public ResponseEntity<?> getQcRules(@PathVariable String analyzerId, HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> authorizationError = ensureAnalyzerConfigAccess(request);
        if (authorizationError != null) {
            return authorizationError;
        }
        try {
            List<AstmQcRule> rules = astmQcRuleService.findByAnalyzerId(analyzerId);
            List<Map<String, Object>> list = new ArrayList<>();
            for (AstmQcRule r : rules) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", r.getId());
                m.put("ruleType", r.getRuleType());
                m.put("targetField", r.getTargetField());
                m.put("operand", r.getOperand());
                m.put("isActive", r.getIsActive());
                m.put("sortOrder", r.getSortOrder());
                list.add(m);
            }
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            logger.error("Error getting QC rules", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalyzerControllerHelper.error("ASTM_QC_RULE_READ_ERROR", e.getMessage()));
        }
    }

    @PostMapping("/analyzers/{analyzerId}/qc-rules")
    public ResponseEntity<Map<String, Object>> createQcRule(@PathVariable String analyzerId,
            @RequestBody Map<String, Object> body, HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> authorizationError = ensureAnalyzerConfigAccess(request);
        if (authorizationError != null) {
            return authorizationError;
        }
        try {
            AstmQcRule rule = astmQcRuleService.create(analyzerId, body);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", rule.getId());
            m.put("ruleType", rule.getRuleType());
            m.put("targetField", rule.getTargetField());
            m.put("operand", rule.getOperand());
            m.put("isActive", rule.getIsActive());
            m.put("sortOrder", rule.getSortOrder());
            return ResponseEntity.status(HttpStatus.CREATED).body(m);
        } catch (LIMSRuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AnalyzerControllerHelper.error("ASTM_QC_RULE_VALIDATION_ERROR", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating QC rule", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalyzerControllerHelper.error("ASTM_QC_RULE_CREATE_ERROR", e.getMessage()));
        }
    }

    @PutMapping("/analyzers/{analyzerId}/qc-rules/{ruleId}")
    public ResponseEntity<Map<String, Object>> updateQcRule(@PathVariable String analyzerId,
            @PathVariable String ruleId, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> authorizationError = ensureAnalyzerConfigAccess(request);
        if (authorizationError != null) {
            return authorizationError;
        }
        try {
            AstmQcRule rule = astmQcRuleService.update(ruleId, body);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", rule.getId());
            m.put("ruleType", rule.getRuleType());
            m.put("targetField", rule.getTargetField());
            m.put("operand", rule.getOperand());
            m.put("isActive", rule.getIsActive());
            m.put("sortOrder", rule.getSortOrder());
            return ResponseEntity.ok(m);
        } catch (LIMSRuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AnalyzerControllerHelper.error("ASTM_QC_RULE_VALIDATION_ERROR", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating QC rule", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalyzerControllerHelper.error("ASTM_QC_RULE_UPDATE_ERROR", e.getMessage()));
        }
    }

    @DeleteMapping("/analyzers/{analyzerId}/qc-rules/{ruleId}")
    public ResponseEntity<?> deleteQcRule(@PathVariable String analyzerId, @PathVariable String ruleId,
            HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> authorizationError = ensureAnalyzerConfigAccess(request);
        if (authorizationError != null) {
            return authorizationError;
        }
        try {
            astmQcRuleService.delete(ruleId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Error deleting QC rule", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalyzerControllerHelper.error("ASTM_QC_RULE_DELETE_ERROR", e.getMessage()));
        }
    }

    @GetMapping("/analyzers/{analyzerId}/test-mapping-configs")
    public ResponseEntity<?> getTestMappingConfigs(@PathVariable String analyzerId, HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> authorizationError = ensureAnalyzerConfigAccess(request);
        if (authorizationError != null) {
            return authorizationError;
        }
        try {
            List<AstmTestMappingConfig> configs = astmTestMappingConfigService.findByAnalyzerId(analyzerId);
            List<Map<String, Object>> list = new ArrayList<>();
            for (AstmTestMappingConfig c : configs) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", c.getId());
                m.put("analyzerTestName", c.getAnalyzerTestName());
                m.put("transformType", c.getTransformType());
                m.put("transformConfig", c.getTransformConfigJson());
                m.put("isActive", c.getIsActive());
                list.add(m);
            }
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            logger.error("Error getting test mapping configs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalyzerControllerHelper.error("ASTM_MAPPING_CONFIG_READ_ERROR", e.getMessage()));
        }
    }

    @PostMapping("/analyzers/{analyzerId}/test-mapping-configs")
    public ResponseEntity<Map<String, Object>> createTestMappingConfig(@PathVariable String analyzerId,
            @RequestBody Map<String, Object> body, HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> authorizationError = ensureAnalyzerConfigAccess(request);
        if (authorizationError != null) {
            return authorizationError;
        }
        try {
            AstmTestMappingConfig config = astmTestMappingConfigService.create(analyzerId, body);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", config.getId());
            m.put("analyzerTestName", config.getAnalyzerTestName());
            m.put("transformType", config.getTransformType());
            m.put("transformConfig", config.getTransformConfigJson());
            m.put("isActive", config.getIsActive());
            return ResponseEntity.status(HttpStatus.CREATED).body(m);
        } catch (LIMSRuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AnalyzerControllerHelper.error("ASTM_MAPPING_CONFIG_VALIDATION_ERROR", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating test mapping config", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalyzerControllerHelper.error("ASTM_MAPPING_CONFIG_CREATE_ERROR", e.getMessage()));
        }
    }

    @PutMapping("/analyzers/{analyzerId}/test-mapping-configs/{configId}")
    public ResponseEntity<Map<String, Object>> updateTestMappingConfig(@PathVariable String analyzerId,
            @PathVariable String configId, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> authorizationError = ensureAnalyzerConfigAccess(request);
        if (authorizationError != null) {
            return authorizationError;
        }
        try {
            AstmTestMappingConfig config = astmTestMappingConfigService.update(configId, body);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", config.getId());
            m.put("analyzerTestName", config.getAnalyzerTestName());
            m.put("transformType", config.getTransformType());
            m.put("transformConfig", config.getTransformConfigJson());
            m.put("isActive", config.getIsActive());
            return ResponseEntity.ok(m);
        } catch (LIMSRuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AnalyzerControllerHelper.error("ASTM_MAPPING_CONFIG_VALIDATION_ERROR", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating test mapping config", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalyzerControllerHelper.error("ASTM_MAPPING_CONFIG_UPDATE_ERROR", e.getMessage()));
        }
    }

    @DeleteMapping("/analyzers/{analyzerId}/test-mapping-configs/{configId}")
    public ResponseEntity<?> deleteTestMappingConfig(@PathVariable String analyzerId, @PathVariable String configId,
            HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> authorizationError = ensureAnalyzerConfigAccess(request);
        if (authorizationError != null) {
            return authorizationError;
        }
        try {
            astmTestMappingConfigService.delete(configId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Error deleting test mapping config", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/analyzers/{analyzerId}/pending-codes")
    public ResponseEntity<?> getPendingCodes(@PathVariable String analyzerId, HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> authorizationError = ensureAnalyzerConfigAccess(request);
        if (authorizationError != null) {
            return authorizationError;
        }
        try {
            List<AstmPendingCode> list = astmPendingCodeService.findPendingByAnalyzerId(analyzerId);
            List<Map<String, Object>> result = new ArrayList<>();
            for (AstmPendingCode pc : list) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", pc.getId());
                m.put("analyzerTestName", pc.getAnalyzerTestName());
                m.put("firstSeenAt", pc.getFirstSeenAt());
                m.put("lastSeenAt", pc.getLastSeenAt());
                m.put("seenCount", pc.getSeenCount());
                m.put("samplePayload", pc.getSamplePayload());
                m.put("status", pc.getStatus());
                result.add(m);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error getting pending codes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalyzerControllerHelper.error("ASTM_PENDING_CODE_READ_ERROR", e.getMessage()));
        }
    }

    @PostMapping("/analyzers/{analyzerId}/pending-codes/{pendingCodeId}/map")
    public ResponseEntity<Map<String, Object>> mapPendingCode(@PathVariable String analyzerId,
            @PathVariable String pendingCodeId, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> authorizationError = ensureAnalyzerConfigAccess(request);
        if (authorizationError != null) {
            return authorizationError;
        }
        try {
            String openelisTestId = (String) body.get("openelisTestId");
            if (openelisTestId == null || openelisTestId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(AnalyzerControllerHelper
                        .error("ASTM_PENDING_CODE_REQUIRED_FIELD", "openelisTestId is required"));
            }
            astmPendingCodeService.resolveByMapping(pendingCodeId, openelisTestId);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("success", true);
            m.put("code", "ASTM_PENDING_CODE_RESOLVED");
            return ResponseEntity.ok(m);
        } catch (LIMSRuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AnalyzerControllerHelper.error("ASTM_PENDING_CODE_RESOLVE_ERROR", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error mapping pending code", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalyzerControllerHelper.error("ASTM_PENDING_CODE_RESOLVE_ERROR", e.getMessage()));
        }
    }

    @PutMapping("/analyzers/{analyzerId}/pending-codes/{pendingCodeId}/status")
    public ResponseEntity<Map<String, Object>> updatePendingCodeStatus(@PathVariable String analyzerId,
            @PathVariable String pendingCodeId, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> authorizationError = ensureAnalyzerConfigAccess(request);
        if (authorizationError != null) {
            return authorizationError;
        }
        try {
            String statusStr = (String) body.get("status");
            if (statusStr == null || statusStr.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(AnalyzerControllerHelper.error("ASTM_PENDING_CODE_REQUIRED_FIELD", "status is required"));
            }
            AstmPendingCode.Status status = AstmPendingCode.Status.valueOf(statusStr);
            astmPendingCodeService.updateStatus(pendingCodeId, status);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("success", true);
            m.put("code", "ASTM_PENDING_CODE_STATUS_UPDATED");
            return ResponseEntity.ok(m);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AnalyzerControllerHelper.error("ASTM_PENDING_CODE_INVALID_STATUS", e.getMessage()));
        } catch (LIMSRuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AnalyzerControllerHelper.error("ASTM_PENDING_CODE_STATUS_ERROR", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating pending code status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalyzerControllerHelper.error("ASTM_PENDING_CODE_STATUS_ERROR", e.getMessage()));
        }
    }

    private ResponseEntity<Map<String, Object>> ensureAnalyzerConfigAccess(HttpServletRequest request) {
        String userId = getSysUserId(request);
        if (userId == null || userId.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    AnalyzerControllerHelper.error("AUTH_FORBIDDEN", "User is not authorized for analyzer config"));
        }

        try {
            if (!userRoleService.userInRole(userId, Constants.ROLE_GLOBAL_ADMIN)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        AnalyzerControllerHelper.error("AUTH_FORBIDDEN", "User is not authorized for analyzer config"));
            }
            return null;
        } catch (Exception e) {
            logger.warn("Failed analyzer config authorization check for user {}", userId, e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(AnalyzerControllerHelper.error("AUTH_FORBIDDEN", "Unable to validate authorization"));
        }
    }

    private Map<String, Object> configToMap(AstmAnalyzerConfig c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("connectionRole", c.getConnectionRole());
        m.put("serverListenPort", c.getServerListenPort());
        m.put("clientTargetIp", c.getClientTargetIp());
        m.put("clientTargetPort", c.getClientTargetPort());
        m.put("aggregationMode", c.getAggregationMode());
        m.put("aggregationWindowSeconds", c.getAggregationWindowSeconds());
        return m;
    }

    private List<Map<String, Object>> extractionToMaps(List<AstmFieldExtractionConfig> list) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (AstmFieldExtractionConfig e : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("key", e.getKey());
            m.put("fieldIndex", e.getFieldIndex());
            m.put("componentIndex", e.getComponentIndex());
            result.add(m);
        }
        return result;
    }

    private List<Map<String, Object>> flagMappingsToMaps(List<AstmFlagMapping> list) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (AstmFlagMapping fm : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", fm.getId());
            m.put("analyzerFlag", fm.getAnalyzerFlag());
            m.put("openelisFlag", fm.getOpenelisFlag());
            m.put("isCustom", fm.getIsCustom());
            result.add(m);
        }
        return result;
    }
}
