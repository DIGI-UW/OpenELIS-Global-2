package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.analyzer.dao.AnalyzerPluginConfigDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerPluginConfig;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AnalyzerPluginConfigServiceImpl extends BaseObjectServiceImpl<AnalyzerPluginConfig, String>
        implements AnalyzerPluginConfigService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {
    };
    private static final String RESULT_VALUE_MAPPINGS = "resultValueMappings";
    private static final String PENDING_RESULT_VALUES = "pendingResultValues";
    private static final Set<String> ALLOWED_TRANSFORM_TYPES = Set.of("PASS_THROUGH", "GREATER_LESS_FLAG", "VALUE_MAP",
            "THRESHOLD_CLASSIFY", "CODED_LOOKUP");
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AnalyzerPluginConfigDAO analyzerPluginConfigDAO;

    @Autowired
    private AnalyzerService analyzerService;

    public AnalyzerPluginConfigServiceImpl() {
        super(AnalyzerPluginConfig.class);
    }

    @Override
    protected AnalyzerPluginConfigDAO getBaseObjectDAO() {
        return analyzerPluginConfigDAO;
    }

    @Override
    public AnalyzerPluginConfig getOrCreate(String analyzerId, String sysUserId) {
        return analyzerPluginConfigDAO.findByAnalyzerId(analyzerId).orElseGet(() -> {
            AnalyzerPluginConfig config = new AnalyzerPluginConfig();
            config.setAnalyzerId(analyzerId);
            config.setConfig("{}");
            config.setSysUserId(sysUserId);
            insert(config);
            return config;
        });
    }

    @Override
    public AnalyzerPluginConfig upsert(String analyzerId, Map<String, Object> config, String sysUserId) {
        validateConfig(analyzerId, config);
        AnalyzerPluginConfig entity = getOrCreate(analyzerId, sysUserId);
        entity.setConfig(toJson(config));
        entity.setSysUserId(sysUserId);
        return update(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getConfigAsMap(String analyzerId) {
        return analyzerPluginConfigDAO.findByAnalyzerId(analyzerId).map(AnalyzerPluginConfig::getConfig)
                .map(this::parseConfigMap).orElseGet(LinkedHashMap::new);
    }

    @Override
    public void applyConfigDefaults(String analyzerId, Object configDefaults, String sysUserId) {
        if (!(configDefaults instanceof Map)) {
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> defaultsMap = (Map<String, Object>) configDefaults;
        if (defaultsMap.isEmpty()) {
            return;
        }
        AnalyzerPluginConfig entity = getOrCreate(analyzerId, sysUserId);
        Map<String, Object> existing = parseConfigMap(entity.getConfig());
        Map<String, Object> merged = new LinkedHashMap<>(defaultsMap);
        merged.putAll(existing);
        entity.setConfig(toJson(merged));
        entity.setSysUserId(sysUserId);
        update(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getResultValueMappings(String analyzerId) {
        return mapList(getConfigAsMap(analyzerId).get(RESULT_VALUE_MAPPINGS));
    }

    @Override
    public Map<String, Object> updateResultValueMappings(String analyzerId, List<Map<String, Object>> mappings,
            String sysUserId) {
        AnalyzerPluginConfig entity = getOrCreate(analyzerId, sysUserId);
        Map<String, Object> config = parseConfigMap(entity.getConfig());
        List<Map<String, Object>> normalized = mapList(mappings);
        config.put(RESULT_VALUE_MAPPINGS, normalized);
        entity.setConfig(toJson(config));
        entity.setSysUserId(sysUserId);
        update(entity);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put(RESULT_VALUE_MAPPINGS, normalized);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPendingResultValues(String analyzerId) {
        return mapList(getConfigAsMap(analyzerId).get(PENDING_RESULT_VALUES));
    }

    @Override
    public Map<String, Object> resolvePendingResultValue(String analyzerId, String pendingResultValueId,
            Map<String, Object> request, String sysUserId) {
        AnalyzerPluginConfig entity = getOrCreate(analyzerId, sysUserId);
        Map<String, Object> config = parseConfigMap(entity.getConfig());
        List<Map<String, Object>> pendingValues = mapList(config.get(PENDING_RESULT_VALUES));
        List<Map<String, Object>> resultMappings = mapList(config.get(RESULT_VALUE_MAPPINGS));

        Map<String, Object> pending = pendingValues.stream()
                .filter(value -> pendingResultValueId.equals(String.valueOf(value.get("id")))).findFirst().orElseThrow(
                        () -> new IllegalArgumentException("Pending result value not found: " + pendingResultValueId));

        String requestedStatus = normalizedString(request != null ? request.get("status") : null);
        String openelisValue = request != null && request.get("openelisValue") != null
                ? String.valueOf(request.get("openelisValue"))
                : null;
        String status = requestedStatus != null ? requestedStatus : (openelisValue != null ? "MAPPED" : "IGNORED");
        if ("MAPPED".equals(status) && (openelisValue == null || openelisValue.isBlank())) {
            throw new IllegalArgumentException("openelisValue is required when resolving a pending value as MAPPED");
        }
        if (!"MAPPED".equals(status) && !"IGNORED".equals(status)) {
            throw new IllegalArgumentException("status must be MAPPED or IGNORED");
        }

        pending.put("status", status);
        if (openelisValue != null) {
            pending.put("openelisValue", openelisValue);
        }
        if ("MAPPED".equals(status)) {
            upsertResultValueMapping(resultMappings, pending, openelisValue);
        }

        config.put(PENDING_RESULT_VALUES, pendingValues);
        config.put(RESULT_VALUE_MAPPINGS, resultMappings);
        entity.setConfig(toJson(config));
        entity.setSysUserId(sysUserId);
        update(entity);
        return pending;
    }

    @Autowired
    private AnalyzerQcRuleService analyzerQcRuleService;

    @Override
    @Transactional(readOnly = true)
    public boolean hasAtLeastOneActiveQcRule(String analyzerId) {
        return analyzerQcRuleService.hasAtLeastOneActiveRule(analyzerId);
    }

    private Map<String, Object> parseConfigMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private List<Map<String, Object>> mapList(Object value) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (!(value instanceof List<?> list)) {
            return result;
        }
        for (Object item : list) {
            if (item instanceof Map<?, ?> itemMap) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : itemMap.entrySet()) {
                    row.put(String.valueOf(entry.getKey()), entry.getValue());
                }
                result.add(row);
            }
        }
        return result;
    }

    private void upsertResultValueMapping(List<Map<String, Object>> resultMappings, Map<String, Object> pending,
            String openelisValue) {
        String analyzerValue = String.valueOf(pending.get("analyzerValue"));
        String testCode = pending.get("testCode") != null ? String.valueOf(pending.get("testCode")) : null;
        for (Map<String, Object> mapping : resultMappings) {
            boolean sameAnalyzerValue = analyzerValue.equals(String.valueOf(mapping.get("analyzerValue")));
            Object mappingTestCode = mapping.get("testCode");
            boolean sameTestCode = testCode == null ? mappingTestCode == null
                    : testCode.equals(String.valueOf(mappingTestCode));
            if (sameAnalyzerValue && sameTestCode) {
                mapping.put("openelisValue", openelisValue);
                mapping.put("active", true);
                return;
            }
        }

        Map<String, Object> mapping = new LinkedHashMap<>();
        mapping.put("analyzerValue", analyzerValue);
        if (testCode != null) {
            mapping.put("testCode", testCode);
        }
        mapping.put("openelisValue", openelisValue);
        mapping.put("active", true);
        resultMappings.add(mapping);
    }

    private String toJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data == null ? Map.of() : data);
        } catch (Exception e) {
            return "{}";
        }
    }

    private void validateConfig(String analyzerId, Map<String, Object> config) {
        if (config == null) {
            return;
        }
        validateAggregationWindow(config);
        validateTransforms(config);
        validateConnectionRole(analyzerId, config);
    }

    private void validateAggregationWindow(Map<String, Object> config) {
        String aggregationMode = normalizedString(config.get("aggregationMode"));
        if (!"BY_SESSION".equals(aggregationMode)) {
            return;
        }
        Integer aggregationWindow = toPositiveInteger(config.get("aggregationWindowSeconds"), false, null);
        if (aggregationWindow == null || aggregationWindow < 5 || aggregationWindow > 300) {
            throw new IllegalArgumentException(
                    "aggregationWindowSeconds must be an integer between 5 and 300 when aggregationMode is BY_SESSION");
        }
    }

    private void validateTransforms(Map<String, Object> config) {
        Object transformsObject = config.get("transforms");
        if (transformsObject == null) {
            return;
        }
        if (!(transformsObject instanceof Map<?, ?> transformsMap)) {
            throw new IllegalArgumentException("transforms must be an object map");
        }
        for (Map.Entry<?, ?> entry : transformsMap.entrySet()) {
            String transformKey = String.valueOf(entry.getKey());
            if (!(entry.getValue() instanceof Map<?, ?> transformDef)) {
                throw new IllegalArgumentException("Transform '" + transformKey + "' must be an object");
            }

            String transformType = normalizedString(transformDef.get("type"));
            if (transformType == null || !ALLOWED_TRANSFORM_TYPES.contains(transformType)) {
                throw new IllegalArgumentException("Transform '" + transformKey
                        + "' has invalid type. Allowed: PASS_THROUGH, GREATER_LESS_FLAG, VALUE_MAP, THRESHOLD_CLASSIFY, CODED_LOOKUP");
            }

            if ("VALUE_MAP".equals(transformType)) {
                Object valueMap = transformDef.get("valueMap");
                if (!(valueMap instanceof Map<?, ?> valueMapObj) || valueMapObj.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Transform '" + transformKey + "' of type VALUE_MAP requires a non-empty valueMap");
                }
            }
            if ("THRESHOLD_CLASSIFY".equals(transformType) && !isNumeric(transformDef.get("threshold"))) {
                throw new IllegalArgumentException(
                        "Transform '" + transformKey + "' of type THRESHOLD_CLASSIFY requires numeric threshold");
            }
            if ("CODED_LOOKUP".equals(transformType)) {
                Object lookupTable = transformDef.get("lookupTable");
                if (!(lookupTable instanceof Map<?, ?> lookupTableObj) || lookupTableObj.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Transform '" + transformKey + "' of type CODED_LOOKUP requires a non-empty lookupTable");
                }
            }
        }
    }

    private void validateConnectionRole(String analyzerId, Map<String, Object> config) {
        String connectionRole = normalizedString(config.get("connectionRole"));
        if (connectionRole == null) {
            return;
        }
        if ("SERVER".equals(connectionRole)) {
            Integer listenPort = toPositiveInteger(config.get("serverListenPort"), true,
                    "serverListenPort is required and must be > 0 when connectionRole is SERVER");
            Analyzer conflictingAnalyzer = analyzerService.findActiveByListenPort(listenPort).orElse(null);
            if (conflictingAnalyzer != null
                    && !String.valueOf(conflictingAnalyzer.getId()).equals(String.valueOf(analyzerId))) {
                throw new IllegalArgumentException(
                        "serverListenPort " + listenPort + " is already used by active analyzer '"
                                + conflictingAnalyzer.getName() + "' (id " + conflictingAnalyzer.getId() + ")");
            }
            return;
        }
        if ("CLIENT".equals(connectionRole)) {
            String targetIp = normalizedString(config.get("clientTargetIp"));
            Integer targetPort = toPositiveInteger(config.get("clientTargetPort"), true,
                    "clientTargetPort is required and must be > 0 when connectionRole is CLIENT");
            if (targetIp == null) {
                throw new IllegalArgumentException("clientTargetIp is required when connectionRole is CLIENT");
            }
            if (targetPort == null) {
                throw new IllegalArgumentException("clientTargetPort is required when connectionRole is CLIENT");
            }
            return;
        }
        throw new IllegalArgumentException("connectionRole must be SERVER or CLIENT");
    }

    private String normalizedString(Object value) {
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized.toUpperCase();
    }

    private Integer toPositiveInteger(Object value, boolean required, String requiredMessage) {
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            if (required) {
                throw new IllegalArgumentException(requiredMessage);
            }
            return null;
        }
        Integer parsed = toInteger(value);
        if (parsed == null || parsed < 1) {
            if (required) {
                throw new IllegalArgumentException(requiredMessage);
            }
            return null;
        }
        return parsed;
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.valueOf(String.valueOf(value).trim());
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isNumeric(Object value) {
        if (value instanceof Number) {
            return true;
        }
        if (value == null) {
            return false;
        }
        try {
            Double.parseDouble(String.valueOf(value).trim());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
