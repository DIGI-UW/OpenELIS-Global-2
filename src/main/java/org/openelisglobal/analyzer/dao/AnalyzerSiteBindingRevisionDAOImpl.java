package org.openelisglobal.analyzer.dao;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingRevision;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AnalyzerSiteBindingRevisionDAOImpl extends BaseDAOImpl<AnalyzerSiteBindingRevision, String>
        implements AnalyzerSiteBindingRevisionDAO {

    public AnalyzerSiteBindingRevisionDAOImpl() {
        super(AnalyzerSiteBindingRevision.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnalyzerSiteBindingRevision> findLatestByBindingId(String bindingId) {
        if (bindingId == null || bindingId.trim().isEmpty()) {
            return Optional.empty();
        }
        String hql = "FROM AnalyzerSiteBindingRevision r JOIN FETCH r.siteBinding b "
                + "JOIN FETCH b.profileBinding WHERE b.id = :bindingId ORDER BY r.revisionNumber DESC";
        Query<AnalyzerSiteBindingRevision> query = entityManager.unwrap(Session.class).createQuery(hql,
                AnalyzerSiteBindingRevision.class);
        query.setParameter("bindingId", bindingId.trim());
        query.setMaxResults(1);
        List<AnalyzerSiteBindingRevision> revisions = query.getResultList();
        return revisions.stream().findFirst();
    }
}
