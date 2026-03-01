package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzer.dao.AnalyzerPluginConfigDAO;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AnalyzerPluginConfigDAO analyzerPluginConfigDAO;

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
    public boolean hasAtLeastOneActiveQcRule(String analyzerId) {
        Map<String, Object> config = getConfigAsMap(analyzerId);
        Object qcRulesObj = config.get("qcRules");
        if (!(qcRulesObj instanceof List<?>)) {
            return false;
        }
        List<?> qcRules = (List<?>) qcRulesObj;
        for (Object entry : qcRules) {
            if (entry instanceof Map<?, ?> ruleMap) {
                Object isActive = ruleMap.get("isActive");
                if (Boolean.TRUE.equals(isActive)) {
                    return true;
                }
            }
        }
        return false;
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

    private String toJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data == null ? Map.of() : data);
        } catch (Exception e) {
            return "{}";
        }
    }
}
