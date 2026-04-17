package org.openelisglobal.analyzer.service;

import java.util.Map;
import org.openelisglobal.analyzer.valueholder.AnalyzerPluginConfig;
import org.openelisglobal.common.service.BaseObjectService;
import org.springframework.security.access.prepost.PreAuthorize;

public interface AnalyzerPluginConfigService extends BaseObjectService<AnalyzerPluginConfig, String> {
    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    AnalyzerPluginConfig getOrCreate(String analyzerId, String sysUserId);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    AnalyzerPluginConfig upsert(String analyzerId, Map<String, Object> config, String sysUserId);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    Map<String, Object> getConfigAsMap(String analyzerId);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    void applyConfigDefaults(String analyzerId, Object configDefaults, String sysUserId);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    boolean hasAtLeastOneActiveQcRule(String analyzerId);
}
