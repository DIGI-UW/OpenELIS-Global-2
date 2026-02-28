package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzer.dao.AstmTestMappingConfigDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AstmTestMappingConfig;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AstmTestMappingConfigServiceImpl implements AstmTestMappingConfigService {

    private static final String[] VALID_TRANSFORM_TYPES = { "PASS_THROUGH", "GREATER_LESS_FLAG", "VALUE_MAP",
            "THRESHOLD_CLASSIFY", "CODED_LOOKUP" };

    @Autowired
    private AstmTestMappingConfigDAO configDAO;

    @Autowired
    private AnalyzerService analyzerService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(readOnly = true)
    public List<AstmTestMappingConfig> findByAnalyzerId(String analyzerId) {
        return configDAO.findByAnalyzerId(analyzerId);
    }

    @Override
    @Transactional
    public AstmTestMappingConfig create(String analyzerId, Map<String, Object> payload) {
        Analyzer analyzer = analyzerService.get(analyzerId);
        if (analyzer == null) {
            throw new LIMSRuntimeException("Analyzer not found: " + analyzerId);
        }
        String analyzerTestName = (String) payload.get("analyzerTestName");
        if (analyzerTestName == null || analyzerTestName.trim().isEmpty()) {
            throw new LIMSRuntimeException("analyzerTestName is required");
        }
        String transformType = (String) payload.get("transformType");
        validateTransformType(transformType);
        validateTransformConfig(transformType, payload.get("transformConfig"));

        AstmTestMappingConfig config = new AstmTestMappingConfig();
        config.setAnalyzer(analyzer);
        config.setAnalyzerTestName(analyzerTestName);
        config.setTransformType(transformType);
        config.setIsActive(payload.get("isActive") != null ? (Boolean) payload.get("isActive") : true);
        if (payload.get("transformConfig") != null) {
            try {
                config.setTransformConfigJson(objectMapper.writeValueAsString(payload.get("transformConfig")));
            } catch (Exception e) {
                throw new LIMSRuntimeException("Invalid transformConfig JSON", e);
            }
        }
        configDAO.insert(config);
        return configDAO.get(config.getId()).orElse(config);
    }

    @Override
    @Transactional
    public AstmTestMappingConfig update(String configId, Map<String, Object> payload) {
        AstmTestMappingConfig config = configDAO.get(configId)
                .orElseThrow(() -> new LIMSRuntimeException("Test mapping config not found"));
        String effectiveType = config.getTransformType();
        if (payload.containsKey("transformType")) {
            effectiveType = (String) payload.get("transformType");
            validateTransformType(effectiveType);
            config.setTransformType(effectiveType);
        }
        if (payload.containsKey("transformConfig")) {
            validateTransformConfig(effectiveType, payload.get("transformConfig"));
            try {
                config.setTransformConfigJson(objectMapper.writeValueAsString(payload.get("transformConfig")));
            } catch (Exception e) {
                throw new LIMSRuntimeException("Invalid transformConfig JSON", e);
            }
        }
        if (payload.containsKey("isActive")) {
            config.setIsActive((Boolean) payload.get("isActive"));
        }
        return configDAO.update(config);
    }

    @Override
    @Transactional
    public void delete(String configId) {
        AstmTestMappingConfig config = configDAO.get(configId)
                .orElseThrow(() -> new LIMSRuntimeException("Test mapping config not found"));
        configDAO.delete(config);
    }

    @Override
    @Transactional(readOnly = true)
    public String applyTransform(String analyzerTestName, String rawValue, List<AstmTestMappingConfig> configs) {
        for (AstmTestMappingConfig c : configs) {
            if (!Boolean.TRUE.equals(c.getIsActive())) {
                continue;
            }
            if (analyzerTestName.equals(c.getAnalyzerTestName())) {
                return applyTransform(c.getTransformType(), c.getTransformConfigJson(), rawValue);
            }
        }
        return rawValue;
    }

    private void validateTransformType(String type) {
        if (type == null || type.isEmpty()) {
            throw new LIMSRuntimeException("transformType is required");
        }
        for (String t : VALID_TRANSFORM_TYPES) {
            if (t.equals(type)) {
                return;
            }
        }
        throw new LIMSRuntimeException("Invalid transformType: " + type);
    }

    private void validateTransformConfig(String transformType, Object transformConfig) {
        switch (transformType) {
        case "PASS_THROUGH":
            return;
        case "GREATER_LESS_FLAG":
            requireConfigMap(transformConfig, "GREATER_LESS_FLAG requires transformConfig with operators");
            List<?> operators = asMap(transformConfig).containsKey("operators")
                    ? (List<?>) asMap(transformConfig).get("operators")
                    : null;
            if (operators == null || operators.isEmpty()) {
                throw new LIMSRuntimeException("GREATER_LESS_FLAG requires operators");
            }
            for (Object operator : operators) {
                String op = String.valueOf(operator);
                if (!op.equals(">") && !op.equals("<") && !op.equals(">=") && !op.equals("<=")) {
                    throw new LIMSRuntimeException("GREATER_LESS_FLAG has unsupported operator: " + op);
                }
            }
            return;
        case "VALUE_MAP":
            requireConfigMap(transformConfig, "VALUE_MAP requires transformConfig.mappings");
            Object mappings = asMap(transformConfig).get("mappings");
            if (!(mappings instanceof Map<?, ?>) || ((Map<?, ?>) mappings).isEmpty()) {
                throw new LIMSRuntimeException("VALUE_MAP requires non-empty mappings");
            }
            return;
        case "THRESHOLD_CLASSIFY":
            requireConfigMap(transformConfig, "THRESHOLD_CLASSIFY requires transformConfig.thresholds");
            Object thresholdsObj = asMap(transformConfig).get("thresholds");
            if (!(thresholdsObj instanceof List<?>) || ((List<?>) thresholdsObj).isEmpty()) {
                throw new LIMSRuntimeException("THRESHOLD_CLASSIFY requires non-empty thresholds");
            }
            for (Object threshold : (List<?>) thresholdsObj) {
                if (!(threshold instanceof Map<?, ?>)) {
                    throw new LIMSRuntimeException("THRESHOLD_CLASSIFY threshold entries must be objects");
                }
                Map<?, ?> rule = (Map<?, ?>) threshold;
                String op = String.valueOf(rule.get("op"));
                if (!op.equals(">") && !op.equals("<") && !op.equals(">=") && !op.equals("<=") && !op.equals("==")
                        && !op.equals("!=")) {
                    throw new LIMSRuntimeException("Unsupported threshold operator: " + op);
                }
                if (!(rule.get("value") instanceof Number)) {
                    throw new LIMSRuntimeException("THRESHOLD_CLASSIFY threshold value must be numeric");
                }
                Object label = rule.get("label");
                if (label == null || String.valueOf(label).trim().isEmpty()) {
                    throw new LIMSRuntimeException("THRESHOLD_CLASSIFY threshold label is required");
                }
            }
            return;
        case "CODED_LOOKUP":
            requireConfigMap(transformConfig, "CODED_LOOKUP requires transformConfig.lookup");
            Object lookup = asMap(transformConfig).get("lookup");
            if (!(lookup instanceof Map<?, ?>) || ((Map<?, ?>) lookup).isEmpty()) {
                throw new LIMSRuntimeException("CODED_LOOKUP requires non-empty lookup");
            }
            return;
        default:
            throw new LIMSRuntimeException("Invalid transformType: " + transformType);
        }
    }

    private String applyTransform(String type, String configJson, String rawValue) {
        if ("PASS_THROUGH".equals(type)) {
            return rawValue;
        }
        Map<String, Object> config = parseConfigJson(configJson);
        switch (type) {
        case "GREATER_LESS_FLAG":
            return applyGreaterLess(rawValue, config);
        case "VALUE_MAP":
            return applyValueMap(rawValue, config);
        case "THRESHOLD_CLASSIFY":
            return applyThreshold(rawValue, config);
        case "CODED_LOOKUP":
            return applyCodedLookup(rawValue, config);
        default:
            return rawValue;
        }
    }

    private String applyGreaterLess(String rawValue, Map<String, Object> config) {
        Object operatorsObj = config.get("operators");
        if (!(operatorsObj instanceof List<?>)) {
            return rawValue;
        }
        for (Object opObj : (List<?>) operatorsObj) {
            String op = String.valueOf(opObj);
            if (rawValue != null && rawValue.startsWith(op)) {
                return rawValue.substring(op.length()).trim();
            }
        }
        return rawValue;
    }

    private String applyValueMap(String rawValue, Map<String, Object> config) {
        Object mappingsObj = config.get("mappings");
        if (!(mappingsObj instanceof Map<?, ?>)) {
            return rawValue;
        }
        Map<?, ?> mappings = (Map<?, ?>) mappingsObj;
        Object mapped = mappings.get(rawValue);
        return mapped != null ? String.valueOf(mapped) : rawValue;
    }

    private String applyThreshold(String rawValue, Map<String, Object> config) {
        Object thresholdsObj = config.get("thresholds");
        if (!(thresholdsObj instanceof List<?>)) {
            return rawValue;
        }
        BigDecimal value;
        try {
            value = new BigDecimal(rawValue);
        } catch (Exception e) {
            return rawValue;
        }
        for (Object thresholdObj : (List<?>) thresholdsObj) {
            if (!(thresholdObj instanceof Map<?, ?>)) {
                continue;
            }
            Map<?, ?> threshold = (Map<?, ?>) thresholdObj;
            String op = String.valueOf(threshold.get("op"));
            Number compareValue = (Number) threshold.get("value");
            String label = String.valueOf(threshold.get("label"));
            if (compareValue == null) {
                continue;
            }
            BigDecimal thresholdValue = BigDecimal.valueOf(compareValue.doubleValue());
            if (matchesThreshold(value, op, thresholdValue)) {
                return label;
            }
        }
        return rawValue;
    }

    private String applyCodedLookup(String rawValue, Map<String, Object> config) {
        Object lookupObj = config.get("lookup");
        if (!(lookupObj instanceof Map<?, ?>)) {
            return rawValue;
        }
        Map<?, ?> lookup = (Map<?, ?>) lookupObj;
        Object mapped = lookup.get(rawValue);
        return mapped != null ? String.valueOf(mapped) : rawValue;
    }

    private boolean matchesThreshold(BigDecimal value, String operator, BigDecimal thresholdValue) {
        int compare = value.compareTo(thresholdValue);
        switch (operator) {
        case ">":
            return compare > 0;
        case "<":
            return compare < 0;
        case ">=":
            return compare >= 0;
        case "<=":
            return compare <= 0;
        case "==":
            return compare == 0;
        case "!=":
            return compare != 0;
        default:
            return false;
        }
    }

    private Map<String, Object> parseConfigJson(String configJson) {
        if (configJson == null || configJson.trim().isEmpty()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(configJson, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new LIMSRuntimeException("Invalid transformConfig JSON", e);
        }
    }

    private void requireConfigMap(Object transformConfig, String message) {
        if (!(transformConfig instanceof Map<?, ?>)) {
            throw new LIMSRuntimeException(message);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object transformConfig) {
        return (Map<String, Object>) transformConfig;
    }
}
