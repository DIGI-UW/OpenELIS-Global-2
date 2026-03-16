package org.openelisglobal.analyzer.dao;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzer.valueholder.AnalyzerPendingCode;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
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
        String hql = "FROM AnalyzerPendingCode a WHERE a.analyzerId = :analyzerId ORDER BY a.lastSeenAt DESC";
        Query<AnalyzerPendingCode> query = entityManager.unwrap(Session.class).createQuery(hql,
                AnalyzerPendingCode.class);
        query.setParameter("analyzerId", parseAnalyzerId(analyzerId));
        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnalyzerPendingCode> findByAnalyzerAndCode(String analyzerId, String analyzerTestName) {
        if (analyzerId == null || analyzerId.trim().isEmpty()) {
            return Optional.empty();
        }
        String hql = "FROM AnalyzerPendingCode a WHERE a.analyzerId = :analyzerId"
                + " AND a.analyzerTestName = :analyzerTestName";
        Query<AnalyzerPendingCode> query = entityManager.unwrap(Session.class).createQuery(hql,
                AnalyzerPendingCode.class);
        query.setParameter("analyzerId", parseAnalyzerId(analyzerId));
        query.setParameter("analyzerTestName", analyzerTestName);
        return Optional.ofNullable(query.uniqueResultOptional().orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public long countByAnalyzerIdAndStatus(String analyzerId, AnalyzerPendingCode.Status status) {
        if (analyzerId == null || analyzerId.trim().isEmpty() || status == null) {
            return 0;
        }
        String hql = "SELECT COUNT(a) FROM AnalyzerPendingCode a WHERE a.analyzerId = :analyzerId"
                + " AND a.status = :status";
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
        query.setParameter("analyzerId", parseAnalyzerId(analyzerId));
        query.setParameter("status", status);
        Long count = query.uniqueResult();
        return count == null ? 0 : count;
    }

    @Override
    public int deletePendingOlderThan(String analyzerId, Timestamp cutoff) {
        if (analyzerId == null || analyzerId.trim().isEmpty() || cutoff == null) {
            return 0;
        }
        try {
            // Hibernate 5.6-jakarta rejects DELETE HQL — load + delete entities
            String hql = "FROM AnalyzerPendingCode a WHERE a.analyzerId = :analyzerId"
                    + " AND a.status = :status AND a.lastSeenAt < :cutoff";
            Query<AnalyzerPendingCode> query = entityManager.unwrap(Session.class).createQuery(hql,
                    AnalyzerPendingCode.class);
            query.setParameter("analyzerId", parseAnalyzerId(analyzerId));
            query.setParameter("status", AnalyzerPendingCode.Status.PENDING);
            query.setParameter("cutoff", cutoff);
            List<AnalyzerPendingCode> toDelete = query.getResultList();
            for (AnalyzerPendingCode code : toDelete) {
                entityManager.remove(code);
            }
            return toDelete.size();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error deleting old pending codes for analyzer " + analyzerId, e);
        }
    }

    private Integer parseAnalyzerId(String analyzerId) {
        try {
            return Integer.parseInt(analyzerId.trim());
        } catch (NumberFormatException e) {
            throw new LIMSRuntimeException("Invalid analyzer ID format: " + analyzerId, e);
        }
    }
}
