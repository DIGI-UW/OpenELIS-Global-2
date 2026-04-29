package org.openelisglobal.notification.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.notification.valueholder.AnalysisNotificationConfig;
import org.springframework.security.access.prepost.PreAuthorize;

public interface AnalysisNotificationConfigService extends BaseObjectService<AnalysisNotificationConfig, Integer> {

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    Optional<AnalysisNotificationConfig> getAnalysisNotificationConfigForAnalysisId(String analysisId);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<AnalysisNotificationConfig> getAnalysisNotificationConfigForAnalysisId(List<String> analysisIds);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    AnalysisNotificationConfig getForConfigOption(Integer configOptionId);
}
