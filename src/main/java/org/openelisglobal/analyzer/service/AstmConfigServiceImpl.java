package org.openelisglobal.analyzer.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.analyzer.dao.AstmAnalyzerConfigDAO;
import org.openelisglobal.analyzer.dao.AstmFieldExtractionConfigDAO;
import org.openelisglobal.analyzer.dao.AstmFlagMappingDAO;
import org.openelisglobal.analyzer.dao.AstmQcRuleDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AstmAnalyzerConfig;
import org.openelisglobal.analyzer.valueholder.AstmFieldExtractionConfig;
import org.openelisglobal.analyzer.valueholder.AstmFlagMapping;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AstmConfigServiceImpl implements AstmConfigService {

    private static final int AGGREGATION_WINDOW_MIN = 5;
    private static final int AGGREGATION_WINDOW_MAX = 300;
    private static final String ROLE_SERVER = "SERVER";
    private static final String ROLE_CLIENT = "CLIENT";
    private static final String AGGREGATION_BY_SESSION = "BY_SESSION";
    private static final Set<String> STANDARD_EXTRACTION_KEYS = Set.of("SPECIMEN_ID_FIELD", "TEST_ID_FIELD",
            "TEST_ID_COMPONENT", "RESULT_VALUE_FIELD", "RESULT_UNITS_FIELD", "ABNORMAL_FLAG_FIELD",
            "RESULT_STATUS_FIELD", "RESULT_TIMESTAMP_FIELD", "SENDER_FIELD");

    @Autowired
    private AstmAnalyzerConfigDAO configDAO;

    @Autowired
    private AstmFieldExtractionConfigDAO extractionDAO;

    @Autowired
    private AstmFlagMappingDAO flagMappingDAO;

    @Autowired
    private AstmQcRuleDAO qcRuleDAO;

    @Autowired
    private AnalyzerService analyzerService;

    @Override
    @Transactional(readOnly = true)
    public AstmAnalyzerConfig getOrCreateConfig(String analyzerId) {
        return configDAO.findByAnalyzerId(analyzerId).orElseGet(() -> createDefaultConfig(analyzerId));
    }

    @Override
    @Transactional(readOnly = true)
    public AstmAnalyzerConfig getConfig(String analyzerId) {
        return configDAO.findByAnalyzerId(analyzerId).orElse(null);
    }

    @Override
    @Transactional
    public AstmAnalyzerConfig updateConfig(String analyzerId, Map<String, Object> update) {
        Analyzer analyzer = analyzerService.get(analyzerId);
        if (analyzer == null) {
            throw new LIMSRuntimeException("Analyzer not found: " + analyzerId);
        }

        AstmAnalyzerConfig config = getOrCreateConfig(analyzerId);
        if (config.getAnalyzer() == null) {
            config.setAnalyzer(analyzer);
        }

        if (update.containsKey("connectionRole")) {
            config.setConnectionRole((String) update.get("connectionRole"));
        }
        if (update.containsKey("serverListenPort")) {
            config.setServerListenPort(
                    update.get("serverListenPort") != null ? ((Number) update.get("serverListenPort")).intValue()
                            : null);
        }
        if (update.containsKey("clientTargetIp")) {
            config.setClientTargetIp((String) update.get("clientTargetIp"));
        }
        if (update.containsKey("clientTargetPort")) {
            config.setClientTargetPort(
                    update.get("clientTargetPort") != null ? ((Number) update.get("clientTargetPort")).intValue()
                            : null);
        }
        if (update.containsKey("aggregationMode")) {
            config.setAggregationMode((String) update.get("aggregationMode"));
        }
        if (update.containsKey("aggregationWindowSeconds")) {
            config.setAggregationWindowSeconds(update.get("aggregationWindowSeconds") != null
                    ? ((Number) update.get("aggregationWindowSeconds")).intValue()
                    : null);
        }

        validateConfig(config);
        config.setSysUserId("1");
        config.setLastupdatedFields();
        if (config.getId() == null) {
            configDAO.insert(config);
        } else {
            configDAO.update(config);
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> extractionOverrides = (List<Map<String, Object>>) update.get("extractionOverrides");
        if (extractionOverrides != null) {
            replaceExtractionConfigs(analyzerId, extractionOverrides);
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flagMappings = (List<Map<String, Object>>) update.get("flagMappings");
        if (flagMappings != null) {
            replaceFlagMappings(analyzerId, flagMappings);
        }

        return configDAO.findByAnalyzerId(analyzerId).orElse(config);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AstmFieldExtractionConfig> getExtractionConfigs(String analyzerId) {
        List<AstmFieldExtractionConfig> configured = extractionDAO.findByAnalyzerId(analyzerId);
        if (!configured.isEmpty()) {
            return configured;
        }
        return buildDefaultExtractionConfigs(analyzerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AstmFlagMapping> getFlagMappings(String analyzerId) {
        return flagMappingDAO.findByAnalyzerId(analyzerId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveQcRules(String analyzerId) {
        return !qcRuleDAO.findActiveByAnalyzerId(analyzerId).isEmpty();
    }

    private AstmAnalyzerConfig createDefaultConfig(String analyzerId) {
        Analyzer analyzer = analyzerService.get(analyzerId);
        if (analyzer == null) {
            throw new LIMSRuntimeException("Analyzer not found: " + analyzerId);
        }
        AstmAnalyzerConfig config = new AstmAnalyzerConfig();
        config.setAnalyzer(analyzer);
        config.setConnectionRole("SERVER");
        config.setAggregationMode("PER_MESSAGE");
        configDAO.insert(config);
        return config;
    }

    private void validateConfig(AstmAnalyzerConfig config) {
        if (ROLE_SERVER.equals(config.getConnectionRole()) && config.getServerListenPort() == null) {
            throw new LIMSRuntimeException("SERVER role requires serverListenPort");
        }
        if (ROLE_CLIENT.equals(config.getConnectionRole())) {
            if (config.getClientTargetIp() == null || config.getClientTargetIp().trim().isEmpty()
                    || config.getClientTargetPort() == null) {
                throw new LIMSRuntimeException("CLIENT role requires clientTargetIp and clientTargetPort");
            }
        }
        if (AGGREGATION_BY_SESSION.equals(config.getAggregationMode())) {
            if (config.getAggregationWindowSeconds() == null) {
                throw new LIMSRuntimeException("BY_SESSION requires aggregationWindowSeconds");
            }
            int w = config.getAggregationWindowSeconds();
            if (w < AGGREGATION_WINDOW_MIN || w > AGGREGATION_WINDOW_MAX) {
                throw new LIMSRuntimeException("aggregationWindowSeconds must be between " + AGGREGATION_WINDOW_MIN
                        + " and " + AGGREGATION_WINDOW_MAX);
            }
        }
        validateServerPortConflict(config);
    }

    private void replaceExtractionConfigs(String analyzerId, List<Map<String, Object>> overrides) {
        Analyzer analyzer = analyzerService.get(analyzerId);
        if (analyzer == null) {
            throw new LIMSRuntimeException("Analyzer not found: " + analyzerId);
        }
        List<AstmFieldExtractionConfig> pending = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();
        for (Map<String, Object> o : overrides) {
            String key = (String) o.get("key");
            Number fi = (Number) o.get("fieldIndex");
            Number ci = (Number) o.get("componentIndex");

            if (key == null || key.trim().isEmpty()) {
                throw new LIMSRuntimeException("extractionOverrides.key is required");
            }
            if (!STANDARD_EXTRACTION_KEYS.contains(key)) {
                throw new LIMSRuntimeException("Unsupported extraction key: " + key);
            }
            if (!seenKeys.add(key)) {
                throw new LIMSRuntimeException("Duplicate extraction override key: " + key);
            }
            if (fi == null || fi.intValue() < 1) {
                throw new LIMSRuntimeException("fieldIndex must be >= 1 for key: " + key);
            }
            if (ci != null && ci.intValue() < 1) {
                throw new LIMSRuntimeException("componentIndex must be >= 1 for key: " + key);
            }

            AstmFieldExtractionConfig c = new AstmFieldExtractionConfig();
            c.setAnalyzer(analyzer);
            c.setKey(key);
            c.setFieldIndex(fi.intValue());
            if (ci != null) {
                c.setComponentIndex(ci.intValue());
            }
            c.setIsDefault(false);
            pending.add(c);
        }

        List<AstmFieldExtractionConfig> existing = extractionDAO.findByAnalyzerId(analyzerId);
        for (AstmFieldExtractionConfig e : existing) {
            extractionDAO.delete(e);
        }
        for (AstmFieldExtractionConfig config : pending) {
            extractionDAO.insert(config);
        }
    }

    private void replaceFlagMappings(String analyzerId, List<Map<String, Object>> mappings) {
        Analyzer analyzer = analyzerService.get(analyzerId);
        if (analyzer == null) {
            throw new LIMSRuntimeException("Analyzer not found: " + analyzerId);
        }
        Set<String> seenFlags = new HashSet<>();
        List<AstmFlagMapping> pending = new ArrayList<>();
        for (Map<String, Object> m : mappings) {
            String af = (String) m.get("analyzerFlag");
            String of = (String) m.get("openelisFlag");
            if (af == null || af.trim().isEmpty() || of == null || of.trim().isEmpty()) {
                throw new LIMSRuntimeException("analyzerFlag and openelisFlag are required");
            }
            if (!seenFlags.add(af)) {
                throw new LIMSRuntimeException("Duplicate analyzerFlag in request: " + af);
            }
            AstmFlagMapping fm = new AstmFlagMapping();
            fm.setAnalyzer(analyzer);
            fm.setAnalyzerFlag(af);
            fm.setOpenelisFlag(of);
            fm.setIsCustom(m.containsKey("isCustom") ? (Boolean) m.get("isCustom") : true);
            pending.add(fm);
        }

        List<AstmFlagMapping> existing = flagMappingDAO.findByAnalyzerId(analyzerId);
        for (AstmFlagMapping e : existing) {
            flagMappingDAO.delete(e);
        }
        for (AstmFlagMapping mapping : pending) {
            flagMappingDAO.insert(mapping);
        }
    }

    private void validateServerPortConflict(AstmAnalyzerConfig config) {
        if (!ROLE_SERVER.equals(config.getConnectionRole()) || config.getServerListenPort() == null
                || config.getAnalyzer() == null || config.getAnalyzer().getId() == null) {
            return;
        }
        for (AstmAnalyzerConfig existing : configDAO.getAll()) {
            if (existing.getAnalyzer() == null || existing.getAnalyzer().getId() == null) {
                continue;
            }
            if (existing.getAnalyzer().getId().equals(config.getAnalyzer().getId())) {
                continue;
            }
            if (!ROLE_SERVER.equals(existing.getConnectionRole()) || existing.getServerListenPort() == null) {
                continue;
            }
            if (!existing.getServerListenPort().equals(config.getServerListenPort())) {
                continue;
            }
            if (isAnalyzerActive(existing.getAnalyzer())) {
                throw new LIMSRuntimeException("serverListenPort conflict with active analyzer "
                        + existing.getAnalyzer().getId() + ": " + config.getServerListenPort());
            }
        }
    }

    private boolean isAnalyzerActive(Analyzer analyzer) {
        if (analyzer == null) {
            return false;
        }
        return analyzer.isActive() || Analyzer.AnalyzerStatus.ACTIVE.equals(analyzer.getStatus());
    }

    private List<AstmFieldExtractionConfig> buildDefaultExtractionConfigs(String analyzerId) {
        Analyzer analyzer = analyzerService.get(analyzerId);
        List<AstmFieldExtractionConfig> defaults = new ArrayList<>();
        defaults.add(defaultExtraction(analyzer, "SPECIMEN_ID_FIELD", 3, null));
        defaults.add(defaultExtraction(analyzer, "TEST_ID_FIELD", 3, null));
        defaults.add(defaultExtraction(analyzer, "TEST_ID_COMPONENT", 3, 4));
        defaults.add(defaultExtraction(analyzer, "RESULT_VALUE_FIELD", 4, null));
        defaults.add(defaultExtraction(analyzer, "RESULT_UNITS_FIELD", 5, null));
        defaults.add(defaultExtraction(analyzer, "ABNORMAL_FLAG_FIELD", 7, null));
        defaults.add(defaultExtraction(analyzer, "RESULT_STATUS_FIELD", 9, null));
        defaults.add(defaultExtraction(analyzer, "RESULT_TIMESTAMP_FIELD", 13, null));
        defaults.add(defaultExtraction(analyzer, "SENDER_FIELD", 5, null));
        return defaults;
    }

    private AstmFieldExtractionConfig defaultExtraction(Analyzer analyzer, String key, int fieldIndex,
            Integer componentIndex) {
        AstmFieldExtractionConfig config = new AstmFieldExtractionConfig();
        config.setAnalyzer(analyzer);
        config.setKey(key);
        config.setFieldIndex(fieldIndex);
        config.setComponentIndex(componentIndex);
        config.setIsDefault(true);
        return config;
    }
}
