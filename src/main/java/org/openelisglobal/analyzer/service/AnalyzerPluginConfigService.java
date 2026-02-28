package org.openelisglobal.analyzer.service;

import java.util.Map;
import org.openelisglobal.analyzer.valueholder.AnalyzerPluginConfig;
import org.openelisglobal.common.service.BaseObjectService;

public interface AnalyzerPluginConfigService extends BaseObjectService<AnalyzerPluginConfig, String> {
    AnalyzerPluginConfig getOrCreate(String analyzerId, String sysUserId);

    AnalyzerPluginConfig upsert(String analyzerId, Map<String, Object> config, String sysUserId);

    Map<String, Object> getConfigAsMap(String analyzerId);

    void applyConfigDefaults(String analyzerId, Object configDefaults, String sysUserId);

    boolean hasAtLeastOneActiveQcRule(String analyzerId);
}
