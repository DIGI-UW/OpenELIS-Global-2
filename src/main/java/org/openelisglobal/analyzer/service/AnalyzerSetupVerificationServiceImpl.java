package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerPendingCode;
import org.openelisglobal.analyzer.valueholder.AnalyzerPluginConfig;
import org.openelisglobal.analyzer.valueholder.AnalyzerQcRule;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.openelisglobal.audittrail.dao.AuditTrailService;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.qc.service.QCControlLotService;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AnalyzerSetupVerificationServiceImpl implements AnalyzerSetupVerificationService {

    private static final String VERIFICATION_KEY = "setupVerification";
    private static final String PLUGIN_CONFIG_TABLE = "analyzer_plugin_config";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private AnalyzerTestMappingService analyzerTestMappingService;

    @Autowired
    private AnalyzerFieldMappingService analyzerFieldMappingService;

    @Autowired
    private AnalyzerPluginConfigService analyzerPluginConfigService;

    @Autowired
    private AnalyzerPendingCodeService analyzerPendingCodeService;

    @Autowired
    private AnalyzerQcRuleService analyzerQcRuleService;

    @Autowired
    private QCControlLotService qcControlLotService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private AuditTrailService auditTrailService;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getVerificationStatus(String analyzerId) {
        requireAnalyzer(analyzerId);
        return snapshot(analyzerId).toResponse();
    }

    @Override
    public Map<String, Object> verifySetup(String analyzerId, Map<String, Object> request, String sysUserId) {
        requireAnalyzer(analyzerId);
        VerificationSnapshot snapshot = snapshot(analyzerId);
        if (!snapshot.mappingReady || !snapshot.qcReady) {
            throw new IllegalStateException("Analyzer setup is incomplete: " + String.join(", ", snapshot.blockers));
        }

        List<String> requestedMappingIds = stringList(request != null ? request.get("mappingIds") : null);
        List<String> requestedQcIds = stringList(request != null ? request.get("qcIds") : null);
        if (!snapshot.mappingIds.equals(requestedMappingIds) || !snapshot.qcIds.equals(requestedQcIds)) {
            throw new IllegalArgumentException(
                    "Confirmed mapping or QC records changed; reload setup before verifying");
        }

        Map<String, Object> originalConfig = new LinkedHashMap<>(
                analyzerPluginConfigService.getConfigAsMap(analyzerId));
        Map<String, Object> config = new LinkedHashMap<>(originalConfig);
        Map<String, Object> verification = new LinkedHashMap<>();
        verification.put("mappingIds", snapshot.mappingIds);
        verification.put("qcIds", snapshot.qcIds);
        verification.put("mappingFingerprint", snapshot.mappingFingerprint);
        verification.put("qcFingerprint", snapshot.qcFingerprint);
        verification.put("verifiedBy", sysUserId);
        verification.put("verifiedAt", Instant.now().toString());
        config.put(VERIFICATION_KEY, verification);
        analyzerPluginConfigService.upsert(analyzerId, config, sysUserId);

        auditTrailService.saveHistory(auditConfig(analyzerId, config, sysUserId),
                auditConfig(analyzerId, originalConfig, sysUserId), sysUserId, IActionConstants.AUDIT_TRAIL_UPDATE,
                PLUGIN_CONFIG_TABLE);
        eventPublisher.publishEvent(new AnalyzerSetupVerifiedEvent(this, analyzerId));

        return snapshot(analyzerId).toResponse();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCurrentlyVerifiedAndReady(String analyzerId) {
        VerificationSnapshot snapshot = snapshot(analyzerId);
        return snapshot.mappingReady && snapshot.qcReady && snapshot.currentlyVerified;
    }

    private Analyzer requireAnalyzer(String analyzerId) {
        Analyzer analyzer = analyzerService.get(analyzerId);
        if (analyzer == null) {
            throw new IllegalArgumentException("Analyzer not found: " + analyzerId);
        }
        return analyzer;
    }

    private VerificationSnapshot snapshot(String analyzerId) {
        Map<String, Object> config = analyzerPluginConfigService.getConfigAsMap(analyzerId);
        List<AnalyzerTestMapping> testMappings = analyzerTestMappingService.getAllForAnalyzer(analyzerId);
        List<Map<String, Object>> fieldMappings = analyzerFieldMappingService.getMappingsForAnalyzer(analyzerId, true);
        List<Map<String, Object>> resultMappings = analyzerPluginConfigService.getResultValueMappings(analyzerId);
        List<Map<String, Object>> pendingResultValues = analyzerPluginConfigService.getPendingResultValues(analyzerId);
        List<AnalyzerPendingCode> pendingCodes = analyzerPendingCodeService.findByAnalyzerId(analyzerId);
        List<AnalyzerQcRule> rules = analyzerQcRuleService.getActiveRulesForAnalyzer(analyzerId);
        List<QCControlLot> lots = qcControlLotService.getActiveControlLotsByInstrument(analyzerId);

        List<String> mappingRecords = new ArrayList<>();
        List<String> mappingIds = new ArrayList<>();
        for (AnalyzerTestMapping mapping : testMappings) {
            String code = nullSafe(mapping.getAnalyzerTestName());
            mappingIds.add("TEST:" + code);
            mappingRecords.add(
                    "TEST|" + code + "|" + nullSafe(mapping.getTestId()) + "|" + nullSafe(mapping.getComponentId()));
        }
        for (Map<String, Object> mapping : fieldMappings) {
            if (Boolean.FALSE.equals(mapping.get("isActive"))) {
                continue;
            }
            String id = value(mapping.get("id"));
            mappingIds.add("FIELD:" + id);
            mappingRecords.add("FIELD|" + canonicalMap(mapping));
        }
        for (Map<String, Object> mapping : resultMappings) {
            if (Boolean.FALSE.equals(mapping.get("active"))) {
                continue;
            }
            String id = "RESULT:" + value(mapping.get("testCode")) + ":" + value(mapping.get("analyzerValue"));
            mappingIds.add(id);
            mappingRecords.add("RESULT|" + canonicalMap(mapping));
        }
        mappingIds.sort(String::compareTo);
        mappingRecords.sort(String::compareTo);

        List<String> qcRecords = new ArrayList<>();
        List<String> qcIds = new ArrayList<>();
        for (AnalyzerQcRule rule : rules) {
            qcIds.add("RULE:" + nullSafe(rule.getId()));
            qcRecords.add("RULE|" + nullSafe(rule.getId()) + "|" + value(rule.getRuleType()) + "|"
                    + nullSafe(rule.getTargetField()) + "|" + nullSafe(rule.getOperand()) + "|" + rule.isActive());
        }
        for (QCControlLot lot : lots) {
            qcIds.add("LOT:" + nullSafe(lot.getId()));
            qcRecords.add("LOT|" + nullSafe(lot.getId()) + "|" + nullSafe(lot.getTestId()) + "|"
                    + nullSafe(lot.getLotNumber()) + "|" + nullSafe(lot.getControlLevel()) + "|"
                    + nullSafe(lot.getStatus()));
        }
        qcIds.sort(String::compareTo);
        qcRecords.sort(String::compareTo);

        List<String> blockers = new ArrayList<>();
        if (testMappings.isEmpty()) {
            blockers.add("NO_TEST_MAPPINGS");
        }
        if (pendingCodes.stream().anyMatch(code -> code.getStatus() == AnalyzerPendingCode.Status.PENDING)) {
            blockers.add("PENDING_ANALYZER_CODES");
        }
        if (pendingResultValues.stream().anyMatch(value -> "PENDING".equals(value(value.get("status"))))) {
            blockers.add("PENDING_RESULT_VALUES");
        }
        if (resultMappings.stream().filter(mapping -> !Boolean.FALSE.equals(mapping.get("active")))
                .anyMatch(mapping -> !"BOUND".equals(value(mapping.get("bindingStatus"))))) {
            blockers.add("UNBOUND_RESULT_VALUES");
        }
        boolean mappingReady = blockers.isEmpty();

        boolean qcApplicable = qcApplicable(config);
        if (qcApplicable && rules.isEmpty()) {
            blockers.add("NO_ACTIVE_QC_RULE");
        }
        if (qcApplicable && lots.isEmpty()) {
            blockers.add("NO_ACTIVE_CONTROL_LOT");
        }
        boolean qcReady = !qcApplicable || (!rules.isEmpty() && !lots.isEmpty());

        String mappingFingerprint = fingerprint(mappingRecords);
        String qcFingerprint = fingerprint(qcRecords);
        Map<String, Object> verification = map(config.get(VERIFICATION_KEY));
        boolean hasVerification = !verification.isEmpty();
        boolean mappingCurrent = hasVerification
                && Objects.equals(mappingFingerprint, value(verification.get("mappingFingerprint")))
                && mappingIds.equals(stringList(verification.get("mappingIds")));
        boolean qcCurrent = hasVerification && Objects.equals(qcFingerprint, value(verification.get("qcFingerprint")))
                && qcIds.equals(stringList(verification.get("qcIds")));
        boolean currentlyVerified = mappingCurrent && qcCurrent;
        if (mappingReady && qcReady && !currentlyVerified) {
            if (!hasVerification) {
                blockers.add("SETUP_NOT_VERIFIED");
            } else {
                if (!mappingCurrent) {
                    blockers.add("MAPPINGS_CHANGED");
                }
                if (!qcCurrent) {
                    blockers.add("QC_CHANGED");
                }
            }
        }

        String state = !mappingReady || !qcReady ? "INCOMPLETE"
                : currentlyVerified ? "CURRENT" : hasVerification ? "STALE" : "UNVERIFIED";
        return new VerificationSnapshot(mappingIds, qcIds, mappingFingerprint, qcFingerprint, mappingReady,
                qcApplicable, qcReady, currentlyVerified, state, blockers, verification);
    }

    private boolean qcApplicable(Map<String, Object> config) {
        Map<String, Object> profile = map(config.get("profile"));
        if (profile.get("qcApplicable") instanceof Boolean applicable) {
            return applicable;
        }
        Object configuredRules = config.get("qcRules");
        return configuredRules instanceof List<?> rules && !rules.isEmpty();
    }

    private String canonicalMap(Map<String, Object> source) {
        TreeMap<String, String> canonical = new TreeMap<>();
        source.forEach((key, value) -> canonical.put(key, value(value)));
        return canonical.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(java.util.stream.Collectors.joining("|"));
    }

    private String fingerprint(List<String> records) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(String.join("\n", records).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to fingerprint analyzer setup", e);
        }
    }

    private AnalyzerPluginConfig auditConfig(String analyzerId, Map<String, Object> config, String sysUserId) {
        AnalyzerPluginConfig entity = new AnalyzerPluginConfig();
        entity.setAnalyzerId(analyzerId);
        entity.setConfig(toJson(config));
        entity.setSysUserId(sysUserId);
        return entity;
    }

    private String toJson(Map<String, Object> config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to serialize analyzer setup verification for audit", e);
        }
    }

    private Map<String, Object> map(Object value) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (value instanceof Map<?, ?> source) {
            source.forEach((key, item) -> result.put(String.valueOf(key), item));
        }
        return result;
    }

    private List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().map(this::value).sorted(Comparator.naturalOrder()).toList();
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private record VerificationSnapshot(List<String> mappingIds, List<String> qcIds, String mappingFingerprint,
            String qcFingerprint, boolean mappingReady, boolean qcApplicable, boolean qcReady,
            boolean currentlyVerified, String state, List<String> blockers, Map<String, Object> verification) {

        private Map<String, Object> toResponse() {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("mappingIds", mappingIds);
            response.put("qcIds", qcIds);
            response.put("mappingFingerprint", mappingFingerprint);
            response.put("qcFingerprint", qcFingerprint);
            response.put("mappingReady", mappingReady);
            response.put("qcApplicable", qcApplicable);
            response.put("qcReady", qcReady);
            response.put("currentlyVerified", currentlyVerified);
            response.put("readyForActivation", mappingReady && qcReady && currentlyVerified);
            response.put("verificationState", state);
            response.put("blockers", blockers);
            response.put("verifiedBy", verification.get("verifiedBy"));
            response.put("verifiedAt", verification.get("verifiedAt"));
            return response;
        }
    }
}
