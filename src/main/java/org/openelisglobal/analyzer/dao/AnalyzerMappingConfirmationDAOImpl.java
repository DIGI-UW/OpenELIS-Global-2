package org.openelisglobal.analyzer.dao;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingConfirmation;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AnalyzerMappingConfirmationDAOImpl extends BaseDAOImpl<AnalyzerMappingConfirmation, String>
        implements AnalyzerMappingConfirmationDAO {

    public AnalyzerMappingConfirmationDAOImpl() {
        super(AnalyzerMappingConfirmation.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnalyzerMappingConfirmation> findByMappingId(String mappingId) {
        if (mappingId == null || mappingId.trim().isEmpty()) {
            return Optional.empty();
        }
        String hql = "FROM AnalyzerMappingConfirmation c JOIN FETCH c.mapping m JOIN FETCH m.analyzer "
                + "WHERE m.id = :mappingId";
        Query<AnalyzerMappingConfirmation> query = entityManager.unwrap(Session.class).createQuery(hql,
                AnalyzerMappingConfirmation.class);
        query.setParameter("mappingId", mappingId.trim());
        return query.uniqueResultOptional();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnalyzerMappingConfirmation> findLatestByAnalyzerId(String analyzerId) {
        if (analyzerId == null || analyzerId.trim().isEmpty()) {
            return Optional.empty();
        }
        String hql = "FROM AnalyzerMappingConfirmation c JOIN FETCH c.mapping m JOIN FETCH m.analyzer a "
                + "WHERE a.id = :analyzerId ORDER BY c.confirmedAt DESC, c.id DESC";
        Query<AnalyzerMappingConfirmation> query = entityManager.unwrap(Session.class).createQuery(hql,
                AnalyzerMappingConfirmation.class);
        query.setParameter("analyzerId", analyzerId.trim());
        query.setMaxResults(1);
        List<AnalyzerMappingConfirmation> confirmations = query.getResultList();
        return confirmations.stream().findFirst();
    }
}
