package org.openelisglobal.analyzer.dao;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingConfirmation;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AnalyzerSiteBindingConfirmationDAOImpl extends BaseDAOImpl<AnalyzerSiteBindingConfirmation, String>
        implements AnalyzerSiteBindingConfirmationDAO {

    public AnalyzerSiteBindingConfirmationDAOImpl() {
        super(AnalyzerSiteBindingConfirmation.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnalyzerSiteBindingConfirmation> findByRevisionId(String revisionId) {
        if (revisionId == null || revisionId.trim().isEmpty()) {
            return Optional.empty();
        }
        String hql = "FROM AnalyzerSiteBindingConfirmation c JOIN FETCH c.siteBindingRevision r "
                + "JOIN FETCH r.siteBinding b JOIN FETCH b.profileBinding WHERE r.id = :revisionId";
        Query<AnalyzerSiteBindingConfirmation> query = entityManager.unwrap(Session.class).createQuery(hql,
                AnalyzerSiteBindingConfirmation.class);
        query.setParameter("revisionId", revisionId.trim());
        return query.uniqueResultOptional();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnalyzerSiteBindingConfirmation> findLatestByBindingId(String bindingId) {
        if (bindingId == null || bindingId.trim().isEmpty()) {
            return Optional.empty();
        }
        String hql = "FROM AnalyzerSiteBindingConfirmation c JOIN FETCH c.siteBindingRevision r "
                + "JOIN FETCH r.siteBinding b JOIN FETCH b.profileBinding "
                + "WHERE b.id = :bindingId ORDER BY c.confirmedAt DESC, c.id DESC";
        Query<AnalyzerSiteBindingConfirmation> query = entityManager.unwrap(Session.class).createQuery(hql,
                AnalyzerSiteBindingConfirmation.class);
        query.setParameter("bindingId", bindingId.trim());
        query.setMaxResults(1);
        List<AnalyzerSiteBindingConfirmation> confirmations = query.getResultList();
        return confirmations.stream().findFirst();
    }
}
