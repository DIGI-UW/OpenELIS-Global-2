package org.openelisglobal.notification.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.notification.dao.AnalysisNotificationConfigDAO;
import org.openelisglobal.notification.valueholder.AnalysisNotificationConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AnalysisNotificationConfigServiceImpl
        extends AuditableBaseObjectServiceImpl<AnalysisNotificationConfig, Integer>
        implements AnalysisNotificationConfigService {

    @Autowired
    private AnalysisNotificationConfigDAO baseDAO;

    public AnalysisNotificationConfigServiceImpl() {
        super(AnalysisNotificationConfig.class);
        this.auditTrailLog = false;
    }

    @Override
    protected BaseDAO<AnalysisNotificationConfig, Integer> getBaseObjectDAO() {
        return baseDAO;
    }

    @Override
    public Optional<AnalysisNotificationConfig> getAnalysisNotificationConfigForAnalysisId(String analysisId) {
        return baseDAO.getAnalysisNotificationConfigForAnalysisId(analysisId);
    }

    @Override
    public List<AnalysisNotificationConfig> getAnalysisNotificationConfigForAnalysisId(List<String> analysisIds) {
        return baseDAO.getAnalysisNotificationConfigsForAnalysisIds(analysisIds);
    }

    @Override
    public AnalysisNotificationConfig getForConfigOption(Integer configOptionId) {
        return baseDAO.getForConfigOption(configOptionId);
    }
}
