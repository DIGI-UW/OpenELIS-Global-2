package org.openelisglobal.analyzer.dao;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzer.valueholder.AnalyzerPendingCode;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AnalyzerPendingCodeDAOImpl extends BaseDAOImpl<AnalyzerPendingCode, String>
        implements AnalyzerPendingCodeDAO {

    public AnalyzerPendingCodeDAOImpl() {
        super(AnalyzerPendingCode.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerPendingCode> findByAnalyzerId(String analyzerId) {
        if (analyzerId == null || analyzerId.trim().isEmpty()) {
            return List.of();
        }
        String sql = "SELECT * FROM clinlims.analyzer_pending_code WHERE analyzer_id = CAST(:analyzerId AS NUMERIC) "
                + "ORDER BY last_seen_at DESC";
        Query<AnalyzerPendingCode> query = entityManager.unwrap(Session.class).createNativeQuery(sql,
                AnalyzerPendingCode.class);
        query.setParameter("analyzerId", analyzerId.trim());
        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnalyzerPendingCode> findByAnalyzerAndCode(String analyzerId, String analyzerTestName) {
        if (analyzerId == null || analyzerId.trim().isEmpty()) {
            return Optional.empty();
        }
        String sql = "SELECT * FROM clinlims.analyzer_pending_code WHERE analyzer_id = CAST(:analyzerId AS NUMERIC) "
                + "AND analyzer_test_name = :analyzerTestName";
        Query<AnalyzerPendingCode> query = entityManager.unwrap(Session.class).createNativeQuery(sql,
                AnalyzerPendingCode.class);
        query.setParameter("analyzerId", analyzerId.trim());
        query.setParameter("analyzerTestName", analyzerTestName);
        return Optional.ofNullable(query.uniqueResultOptional().orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public long countByAnalyzerIdAndStatus(String analyzerId, AnalyzerPendingCode.Status status) {
        if (analyzerId == null || analyzerId.trim().isEmpty() || status == null) {
            return 0;
        }
        String sql = "SELECT COUNT(*) FROM clinlims.analyzer_pending_code WHERE analyzer_id = CAST(:analyzerId AS NUMERIC) "
                + "AND status = :status";
        Query<Number> query = entityManager.unwrap(Session.class).createNativeQuery(sql);
        query.setParameter("analyzerId", analyzerId.trim());
        query.setParameter("status", status.name());
        Number count = query.uniqueResult();
        return count == null ? 0 : count.longValue();
    }

    @Override
    public int deletePendingOlderThan(String analyzerId, Timestamp cutoff) {
        if (analyzerId == null || analyzerId.trim().isEmpty() || cutoff == null) {
            return 0;
        }
        String sql = "DELETE FROM clinlims.analyzer_pending_code WHERE analyzer_id = CAST(:analyzerId AS NUMERIC) "
                + "AND status = :status AND last_seen_at < :cutoff";
        Query<?> query = entityManager.unwrap(Session.class).createNativeQuery(sql);
        query.setParameter("analyzerId", analyzerId.trim());
        query.setParameter("status", AnalyzerPendingCode.Status.PENDING.name());
        query.setParameter("cutoff", cutoff);
        return query.executeUpdate();
    }
}
