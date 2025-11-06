package org.openelisglobal.notification.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.notification.valueholder.AnalysisNotificationConfig;

public interface AnalysisNotificationConfigDAO extends BaseDAO<AnalysisNotificationConfig, Integer> {

    Optional<AnalysisNotificationConfig> getAnalysisNotificationConfigForAnalysisId(String analysisId);

    List<AnalysisNotificationConfig> getAnalysisNotificationConfigsForAnalysisIds(List<String> analysisIds);

    AnalysisNotificationConfig getForConfigOption(Integer configOptionId);
}
