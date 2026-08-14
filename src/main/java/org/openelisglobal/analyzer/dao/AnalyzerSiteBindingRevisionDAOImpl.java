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
        if (bindingId == null || bindingId.isBlank()) {
            return Optional.empty();
        }
        Query<AnalyzerSiteBindingRevision> query = entityManager.unwrap(Session.class).createQuery(
                "SELECT r FROM AnalyzerSiteBindingRevision r JOIN FETCH r.siteBinding"
                        + " WHERE r.siteBinding.id = :bindingId ORDER BY r.revisionNumber DESC",
                AnalyzerSiteBindingRevision.class);
        query.setParameter("bindingId", bindingId.trim());
        query.setMaxResults(1);
        return query.uniqueResultOptional();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerSiteBindingRevision> findLatestByProfileIds(List<String> profileIds) {
        if (profileIds == null || profileIds.isEmpty()) {
            return List.of();
        }
        Query<AnalyzerSiteBindingRevision> query = entityManager.unwrap(Session.class)
                .createQuery("SELECT r FROM AnalyzerSiteBindingRevision r JOIN FETCH r.siteBinding"
                        + " WHERE r.bridgeProfileId IN (:profileIds)"
                        + " AND r.revisionNumber = (SELECT MAX(candidate.revisionNumber)"
                        + " FROM AnalyzerSiteBindingRevision candidate"
                        + " WHERE candidate.siteBinding = r.siteBinding)", AnalyzerSiteBindingRevision.class);
        query.setParameter("profileIds", profileIds);
        return query.getResultList();
    }
}
