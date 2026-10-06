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
    public Optional<AnalyzerMappingConfirmation> findByRevisionId(String revisionId) {
        if (revisionId == null || revisionId.trim().isEmpty()) {
            return Optional.empty();
        }
        String hql = "FROM AnalyzerMappingConfirmation c JOIN FETCH c.siteBindingRevision r "
                + "JOIN FETCH r.siteBinding b JOIN FETCH b.profileBinding WHERE r.id = :revisionId";
        Query<AnalyzerMappingConfirmation> query = entityManager.unwrap(Session.class).createQuery(hql,
                AnalyzerMappingConfirmation.class);
        query.setParameter("revisionId", revisionId.trim());
        return query.uniqueResultOptional();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnalyzerMappingConfirmation> findLatestByBindingId(String bindingId) {
        if (bindingId == null || bindingId.trim().isEmpty()) {
            return Optional.empty();
        }
        String hql = "FROM AnalyzerMappingConfirmation c JOIN FETCH c.siteBindingRevision r "
                + "JOIN FETCH r.siteBinding b JOIN FETCH b.profileBinding "
                + "WHERE b.id = :bindingId ORDER BY c.confirmedAt DESC, c.id DESC";
        Query<AnalyzerMappingConfirmation> query = entityManager.unwrap(Session.class).createQuery(hql,
                AnalyzerMappingConfirmation.class);
        query.setParameter("bindingId", bindingId.trim());
        query.setMaxResults(1);
        List<AnalyzerMappingConfirmation> confirmations = query.getResultList();
        return confirmations.stream().findFirst();
    }
}
