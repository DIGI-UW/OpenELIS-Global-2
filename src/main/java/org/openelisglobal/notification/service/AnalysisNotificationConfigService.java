package org.openelisglobal.notification.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.notification.valueholder.AnalysisNotificationConfig;

public interface AnalysisNotificationConfigService extends BaseObjectService<AnalysisNotificationConfig, Integer> {

    Optional<AnalysisNotificationConfig> getAnalysisNotificationConfigForAnalysisId(String analysisId);

    List<AnalysisNotificationConfig> getAnalysisNotificationConfigForAnalysisId(List<String> analysisIds);

    AnalysisNotificationConfig getForConfigOption(Integer configOptionId);
}
