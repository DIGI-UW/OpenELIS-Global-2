package org.openelisglobal.analyzer.dao;

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
public class AnalyzerPendingCodeDAOImpl extends BaseDAOImpl<AnalyzerPendingCode, String> implements AnalyzerPendingCodeDAO {

    public AnalyzerPendingCodeDAOImpl() {
        super(AnalyzerPendingCode.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerPendingCode> findByAnalyzerId(String analyzerId) {
        String hql = "FROM AnalyzerPendingCode p WHERE p.analyzerId = :analyzerId ORDER BY p.lastSeenAt DESC";
        Query<AnalyzerPendingCode> query = entityManager.unwrap(Session.class).createQuery(hql, AnalyzerPendingCode.class);
        query.setParameter("analyzerId", analyzerId);
        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnalyzerPendingCode> findByAnalyzerAndCode(String analyzerId, String analyzerTestName) {
        String hql = "FROM AnalyzerPendingCode p WHERE p.analyzerId = :analyzerId AND p.analyzerTestName = :analyzerTestName";
        Query<AnalyzerPendingCode> query = entityManager.unwrap(Session.class).createQuery(hql, AnalyzerPendingCode.class);
        query.setParameter("analyzerId", analyzerId);
        query.setParameter("analyzerTestName", analyzerTestName);
        return Optional.ofNullable(query.uniqueResult());
    }
}
