package org.openelisglobal.analyzer.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzer.valueholder.AnalyzerPluginConfig;
import org.openelisglobal.common.service.BaseObjectService;

public interface AnalyzerPluginConfigService extends BaseObjectService<AnalyzerPluginConfig, String> {
    AnalyzerPluginConfig getOrCreate(String analyzerId, String sysUserId);

    AnalyzerPluginConfig upsert(String analyzerId, Map<String, Object> config, String sysUserId);

    Map<String, Object> getConfigAsMap(String analyzerId);

    void applyConfigDefaults(String analyzerId, Object configDefaults, String sysUserId);

    void applyProfileDefaults(String analyzerId, Map<String, Object> profileConfig, String sysUserId);

    List<Map<String, Object>> getResultValueMappings(String analyzerId);

    Map<String, Object> updateResultValueMappings(String analyzerId, List<Map<String, Object>> mappings,
            String sysUserId);

    List<Map<String, Object>> getPendingResultValues(String analyzerId);

    Map<String, Object> resolvePendingResultValue(String analyzerId, String pendingResultValueId,
            Map<String, Object> request, String sysUserId);

    boolean hasAtLeastOneActiveQcRule(String analyzerId);
}
