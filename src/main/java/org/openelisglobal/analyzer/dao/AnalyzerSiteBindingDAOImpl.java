package org.openelisglobal.analyzer.dao;

import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBinding;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AnalyzerSiteBindingDAOImpl extends BaseDAOImpl<AnalyzerSiteBinding, String>
        implements AnalyzerSiteBindingDAO {

    public AnalyzerSiteBindingDAOImpl() {
        super(AnalyzerSiteBinding.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnalyzerSiteBinding> findByProfileBindingId(String profileBindingId) {
        if (profileBindingId == null || profileBindingId.trim().isEmpty()) {
            return Optional.empty();
        }
        String hql = "FROM AnalyzerSiteBinding b JOIN FETCH b.profileBinding "
                + "WHERE b.profileBinding.id = :profileBindingId";
        Query<AnalyzerSiteBinding> query = entityManager.unwrap(Session.class).createQuery(hql,
                AnalyzerSiteBinding.class);
        query.setParameter("profileBindingId", profileBindingId.trim());
        return query.uniqueResultOptional();
    }
}
