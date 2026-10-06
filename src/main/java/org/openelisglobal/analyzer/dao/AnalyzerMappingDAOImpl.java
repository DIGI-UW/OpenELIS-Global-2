package org.openelisglobal.analyzer.dao;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzer.valueholder.AnalyzerMapping;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AnalyzerMappingDAOImpl extends BaseDAOImpl<AnalyzerMapping, String> implements AnalyzerMappingDAO {

    public AnalyzerMappingDAOImpl() {
        super(AnalyzerMapping.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnalyzerMapping> findLatestByBindingId(String bindingId) {
        if (bindingId == null || bindingId.trim().isEmpty()) {
            return Optional.empty();
        }
        String hql = "FROM AnalyzerMapping r JOIN FETCH r.siteBinding b "
                + "JOIN FETCH b.profileBinding WHERE b.id = :bindingId ORDER BY r.revisionNumber DESC";
        Query<AnalyzerMapping> query = entityManager.unwrap(Session.class).createQuery(hql, AnalyzerMapping.class);
        query.setParameter("bindingId", bindingId.trim());
        query.setMaxResults(1);
        List<AnalyzerMapping> revisions = query.getResultList();
        return revisions.stream().findFirst();
    }
}
