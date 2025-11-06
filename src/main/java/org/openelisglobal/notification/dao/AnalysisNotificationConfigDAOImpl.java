package org.openelisglobal.notification.dao;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.notification.valueholder.AnalysisNotificationConfig;
import org.springframework.stereotype.Component;

@Component
public class AnalysisNotificationConfigDAOImpl extends BaseDAOImpl<AnalysisNotificationConfig, Integer>
        implements AnalysisNotificationConfigDAO {

    public AnalysisNotificationConfigDAOImpl() {
        super(AnalysisNotificationConfig.class);
    }

    @Override
    public Optional<AnalysisNotificationConfig> getAnalysisNotificationConfigForAnalysisId(String analysisId) {
        AnalysisNotificationConfig data;
        try {
            String sql = "From AnalysisNotificationConfig as anc where anc.analysis.id = :analysisId";
            Query<AnalysisNotificationConfig> query = entityManager.unwrap(Session.class).createQuery(sql,
                    AnalysisNotificationConfig.class);
            query.setParameter("analysisId", Integer.parseInt(analysisId));
            data = query.uniqueResult();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException(
                    "Error in AnalysisNotificationConfigDAOImpl getAnalysisNotificationConfigForAnalysisId()", e);
        }

        return Optional.ofNullable(data);
    }

    @Override
    public List<AnalysisNotificationConfig> getAnalysisNotificationConfigsForAnalysisIds(List<String> analysisIds) {
        List<AnalysisNotificationConfig> data;
        try {
            String sql = "From AnalysisNotificationConfig as anc where anc.analysis.id IN (:analysisIds)";
            Query<AnalysisNotificationConfig> query = entityManager.unwrap(Session.class).createQuery(sql,
                    AnalysisNotificationConfig.class);
            query.setParameterList("analysisIds",
                    analysisIds.stream().map(i -> Integer.parseInt(i)).collect(Collectors.toList()));
            data = query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException(
                    "Error in AnalysisNotificationConfigDAOImpl" + " getAnalysisNotificationConfigForAnalysisIds()", e);
        }

        return data;
    }

    @Override
    public AnalysisNotificationConfig getForConfigOption(Integer configOptionId) {
        AnalysisNotificationConfig data;
        try {
            String sql = "SELECT anc From AnalysisNotificationConfig as anc join anc.options as anco where anco.id"
                    + " = :configOptionId";
            Query<AnalysisNotificationConfig> query = entityManager.unwrap(Session.class).createQuery(sql,
                    AnalysisNotificationConfig.class);
            query.setParameter("configOptionId", configOptionId);
            data = query.uniqueResult();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in AnalysisNotificationConfigDAOImpl getForConfigOption()", e);
        }

        return data;
    }
}
