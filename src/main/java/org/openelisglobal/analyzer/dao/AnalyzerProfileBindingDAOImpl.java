package org.openelisglobal.analyzer.dao;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AnalyzerProfileBindingDAOImpl extends BaseDAOImpl<AnalyzerProfileBinding, String>
        implements AnalyzerProfileBindingDAO {

    public AnalyzerProfileBindingDAOImpl() {
        super(AnalyzerProfileBinding.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnalyzerProfileBinding> findByProfileIdAndRevision(String profileId, int profileRevision) {
        if (profileId == null || profileId.trim().isEmpty() || profileRevision < 1) {
            return Optional.empty();
        }

        String hql = "FROM AnalyzerProfileBinding b WHERE b.profileId = :profileId "
                + "AND b.profileRevision = :profileRevision";
        Query<AnalyzerProfileBinding> query = entityManager.unwrap(Session.class).createQuery(hql,
                AnalyzerProfileBinding.class);
        query.setParameter("profileId", profileId.trim());
        query.setParameter("profileRevision", profileRevision);
        List<AnalyzerProfileBinding> bindings = query.getResultList();
        return bindings.stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public long countAnalyzersByBindingId(String bindingId) {
        if (bindingId == null || bindingId.trim().isEmpty()) {
            return 0;
        }

        String hql = "SELECT COUNT(a) FROM Analyzer a WHERE a.profileBinding.id = :bindingId";
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
        query.setParameter("bindingId", bindingId.trim());
        Long count = query.uniqueResult();
        return count == null ? 0 : count;
    }
}
