package org.openelisglobal.analyzer.dao;

import jakarta.persistence.LockModeType;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileMigrationAnomaly;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AnalyzerProfileMigrationAnomalyDAOImpl extends BaseDAOImpl<AnalyzerProfileMigrationAnomaly, String>
        implements AnalyzerProfileMigrationAnomalyDAO {

    public AnalyzerProfileMigrationAnomalyDAOImpl() {
        super(AnalyzerProfileMigrationAnomaly.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerProfileMigrationAnomaly> findOpenByAnalyzerId(String analyzerId) {
        return queryOpen(analyzerId, null);
    }

    @Override
    public List<AnalyzerProfileMigrationAnomaly> findByAnalyzerIdForUpdate(String analyzerId) {
        return queryOpen(analyzerId, LockModeType.PESSIMISTIC_WRITE);
    }

    private List<AnalyzerProfileMigrationAnomaly> queryOpen(String analyzerId, LockModeType lockMode) {
        if (analyzerId == null || analyzerId.isBlank()) {
            return List.of();
        }
        Query<AnalyzerProfileMigrationAnomaly> query = entityManager.unwrap(Session.class)
                .createQuery("SELECT anomaly FROM AnalyzerProfileMigrationAnomaly anomaly JOIN FETCH anomaly.analyzer"
                        + " WHERE anomaly.analyzer.id = :analyzerId AND anomaly.status = :status"
                        + " ORDER BY anomaly.evidenceKey, anomaly.id", AnalyzerProfileMigrationAnomaly.class);
        query.setParameter("analyzerId", analyzerId.trim());
        query.setParameter("status", AnalyzerProfileMigrationAnomaly.Status.OPEN);
        if (lockMode != null) {
            query.setLockMode(lockMode);
        }
        return query.getResultList();
    }
}
