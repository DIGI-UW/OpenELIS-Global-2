package org.openelisglobal.analyzer.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzer.service.AnalyzerBridgeSyncService;
import org.openelisglobal.analyzer.service.AnalyzerQcRuleService;
import org.openelisglobal.analyzer.valueholder.AnalyzerQcRule;
import org.openelisglobal.analyzer.valueholder.AnalyzerQcRule.RuleType;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
public class AnalyzerQcRuleRestController extends BaseRestController {

    private static final String CLASS_NAME = "AnalyzerQcRuleRestController";

    @Autowired
    private AnalyzerQcRuleService analyzerQcRuleService;

    @Autowired(required = false)
    private AnalyzerBridgeSyncService analyzerBridgeSyncService;

    @GetMapping("/analyzers/{analyzerId}/qc-rules")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getQcRules(@PathVariable String analyzerId) {
        try {
            List<AnalyzerQcRule> rules = analyzerQcRuleService.getRulesForAnalyzer(analyzerId);
            List<Map<String, Object>> response = rules.stream().map(this::ruleToMap).toList();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LogEvent.logError(CLASS_NAME, "getQcRules", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    @PostMapping("/analyzers/{analyzerId}/qc-rules")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<Map<String, Object>> createQcRule(@PathVariable String analyzerId,
            @RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            AnalyzerQcRule rule = mapToRule(body);
            AnalyzerQcRule created = analyzerQcRuleService.createRule(analyzerId, rule, getSysUserId(request));
            pushToBridge(analyzerId);
            return ResponseEntity.status(HttpStatus.CREATED).body(ruleToMap(created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AnalyzerControllerHelper.wrapError(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(AnalyzerControllerHelper.wrapError(e.getMessage()));
        } catch (Exception e) {
            LogEvent.logError(CLASS_NAME, "createQcRule", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalyzerControllerHelper.wrapError("Failed to create QC rule: " + e.getMessage()));
        }
    }

    @PutMapping("/analyzers/{analyzerId}/qc-rules/{ruleId}")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<Map<String, Object>> updateQcRule(@PathVariable String analyzerId,
            @PathVariable String ruleId, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            AnalyzerQcRule updates = mapToRule(body);
            AnalyzerQcRule updated = analyzerQcRuleService.updateRule(analyzerId, ruleId, updates,
                    getSysUserId(request));
            pushToBridge(analyzerId);
            return ResponseEntity.ok(ruleToMap(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AnalyzerControllerHelper.wrapError(e.getMessage()));
        } catch (Exception e) {
            LogEvent.logError(CLASS_NAME, "updateQcRule", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalyzerControllerHelper.wrapError("Failed to update QC rule: " + e.getMessage()));
        }
    }

    @DeleteMapping("/analyzers/{analyzerId}/qc-rules/{ruleId}")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<Void> deleteQcRule(@PathVariable String analyzerId, @PathVariable String ruleId) {
        try {
            analyzerQcRuleService.deleteRule(analyzerId, ruleId);
            pushToBridge(analyzerId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            LogEvent.logError(CLASS_NAME, "deleteQcRule", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private void pushToBridge(String analyzerId) {
        if (analyzerBridgeSyncService != null) {
            analyzerBridgeSyncService.pushAnalyzer(analyzerId);
        }
    }

    private Map<String, Object> ruleToMap(AnalyzerQcRule rule) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", rule.getId());
        map.put("analyzerId", rule.getAnalyzerId());
        map.put("ruleType", rule.getRuleType().name());
        map.put("targetField", rule.getTargetField());
        map.put("operand", rule.getOperand());
        map.put("isActive", rule.isActive());
        map.put("displayOrder", rule.getDisplayOrder());
        map.put("description", rule.getDescription());
        return map;
    }

    private AnalyzerQcRule mapToRule(Map<String, Object> body) {
        AnalyzerQcRule rule = new AnalyzerQcRule();
        if (body.get("ruleType") != null) {
            rule.setRuleType(RuleType.valueOf(String.valueOf(body.get("ruleType"))));
        }
        rule.setTargetField((String) body.get("targetField"));
        rule.setOperand((String) body.get("operand"));
        if (body.containsKey("isActive")) {
            rule.setActive(Boolean.TRUE.equals(body.get("isActive")));
        }
        if (body.get("displayOrder") instanceof Number n) {
            rule.setDisplayOrder(n.intValue());
        }
        rule.setDescription((String) body.get("description"));
        return rule;
    }
}
