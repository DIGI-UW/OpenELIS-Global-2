package org.openelisglobal.qc.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.openelisglobal.qc.service.QCFrequencyService;
import org.openelisglobal.qc.service.QCService;
import org.openelisglobal.qc.service.QCSignatureService;
import org.openelisglobal.qc.service.WestgardRuleEngine;
import org.openelisglobal.qc.valueholder.QCFrequencyConfig;
import org.openelisglobal.qc.valueholder.QCSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/qc")
public class QCRestController {

    @Autowired
    private QCService qcService;

    @Autowired
    private QCSignatureService signatureService;

    @Autowired
    private QCFrequencyService frequencyService;

    @Autowired
    private WestgardRuleEngine westgardRuleEngine;

    @GetMapping(value = "/westgard-rules", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> getWestgardRules() {
        return ResponseEntity.ok(westgardRuleEngine.getRuleDescriptions());
    }

    @GetMapping(value = "/westgard-rules/config/{testTypeId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getRuleConfig(@PathVariable Long testTypeId) {
        return ResponseEntity.ok(qcService.getRuleConfigs(testTypeId));
    }

    @PutMapping(value = "/westgard-rules/config/{testTypeId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateRuleConfig(@PathVariable Long testTypeId, @RequestBody Map<String, Object> body) {
        try {
            String ruleCode = (String) body.get("ruleCode");
            Boolean enabled = (Boolean) body.get("enabled");

            if (ruleCode == null || enabled == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "ruleCode and enabled are required"));
            }

            qcService.updateRuleConfig(testTypeId, ruleCode, enabled);
            return ResponseEntity.ok(qcService.getRuleConfigs(testTypeId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/controls/{controlId}/evaluate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> evaluateQCResults(@PathVariable Long controlId, @RequestBody Map<String, Object> body) {
        try {
            Number testTypeId = (Number) body.get("testTypeId");
            @SuppressWarnings("unchecked")
            List<Number> rawValues = (List<Number>) body.get("values");
            Number mean = (Number) body.get("mean");
            Number sd = (Number) body.get("sd");

            if (testTypeId == null || rawValues == null || mean == null || sd == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "testTypeId, values, mean, and sd are required"));
            }

            List<BigDecimal> values = rawValues.stream().map(v -> new BigDecimal(v.toString()))
                    .collect(Collectors.toList());

            Map<String, Object> result = qcService.evaluateQCResult(testTypeId.longValue(), values,
                    new BigDecimal(mean.toString()), new BigDecimal(sd.toString()));

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/controls/{controlId}/chart-data", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getChartData(@PathVariable Long controlId, @RequestBody Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            List<Number> rawValues = (List<Number>) body.get("values");
            if (rawValues == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "values list is required"));
            }

            List<BigDecimal> values = rawValues.stream().map(v -> new BigDecimal(v.toString()))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(qcService.calculateChartData(values));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/reports/{reportId}/sign", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> signReport(@PathVariable Long reportId, @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        try {
            String userId = body.get("userId");
            String comment = body.get("comment");

            if (userId == null || userId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "userId is required"));
            }

            String ipAddress = request.getRemoteAddr();
            QCSignature signature = signatureService.signReport(reportId, userId, ipAddress, comment);

            Map<String, Object> dto = new HashMap<>();
            dto.put("id", signature.getId());
            dto.put("reportId", signature.getReportId());
            dto.put("userId", signature.getUserId());
            dto.put("signedAt", signature.getSignedAt());
            dto.put("ipAddress", signature.getIpAddress());
            dto.put("comment", signature.getComment());

            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping(value = "/reports/{reportId}/signatures", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getSignatures(@PathVariable Long reportId) {
        List<QCSignature> signatures = signatureService.getSignaturesByReportId(reportId);
        List<Map<String, Object>> dtos = signatures.stream().map(s -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", s.getId());
            dto.put("reportId", s.getReportId());
            dto.put("userId", s.getUserId());
            dto.put("signedAt", s.getSignedAt());
            dto.put("ipAddress", s.getIpAddress());
            dto.put("comment", s.getComment());
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping(value = "/instruments/{instrumentId}/frequency", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getFrequencyConfig(@PathVariable Long instrumentId) {
        Optional<QCFrequencyConfig> config = frequencyService.getFrequencyConfig(instrumentId);
        if (config.isPresent()) {
            QCFrequencyConfig c = config.get();
            Map<String, Object> dto = new HashMap<>();
            dto.put("instrumentId", c.getInstrumentId());
            dto.put("frequencyType", c.getFrequencyType());
            dto.put("frequencyValue", c.getFrequencyValue());
            dto.put("lastUpdated", c.getLastupdated());
            return ResponseEntity.ok(dto);
        } else {
            return ResponseEntity.ok(Map.of("instrumentId", instrumentId, "configured", false));
        }
    }

    @PutMapping(value = "/instruments/{instrumentId}/frequency", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateFrequencyConfig(@PathVariable Long instrumentId,
            @RequestBody Map<String, Object> body) {
        try {
            String frequencyType = (String) body.get("frequencyType");
            Number frequencyValue = (Number) body.get("frequencyValue");

            if (frequencyType == null || frequencyType.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "frequencyType is required"));
            }

            QCFrequencyConfig config = frequencyService.updateFrequencyConfig(instrumentId, frequencyType,
                    frequencyValue != null ? frequencyValue.intValue() : null);

            Map<String, Object> dto = new HashMap<>();
            dto.put("instrumentId", config.getInstrumentId());
            dto.put("frequencyType", config.getFrequencyType());
            dto.put("frequencyValue", config.getFrequencyValue());
            dto.put("lastUpdated", config.getLastupdated());
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping(value = "/instruments/{instrumentId}/compliance", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getComplianceMetrics(@PathVariable Long instrumentId) {
        return ResponseEntity.ok(frequencyService.getComplianceMetrics(instrumentId));
    }
}
