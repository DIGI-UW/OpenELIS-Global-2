package org.openelisglobal.analyzer.dao;

import java.util.List;
import java.util.Map;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfile;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AnalyzerProfileDAOImpl extends BaseDAOImpl<AnalyzerProfile, String> implements AnalyzerProfileDAO {

    public AnalyzerProfileDAOImpl() {
        super(AnalyzerProfile.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerProfile> findBySource(String source) {
        return getAllMatching("source", source);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerProfile> findByMetaId(String profileMetaId) {
        return getAllMatching("profileMetaId", profileMetaId);
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyzerProfile findLatestByMetaId(String profileMetaId) {
        try {
            Session session = entityManager.unwrap(Session.class);
            String hql = "FROM AnalyzerProfile p WHERE p.profileMetaId = :metaId AND p.isLatest = true";
            Query<AnalyzerProfile> query = session.createQuery(hql, AnalyzerProfile.class);
            query.setParameter("metaId", profileMetaId);
            return query.uniqueResult();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding latest profile by meta id", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyzerProfile findByMetaIdAndVersion(String profileMetaId, String profileMetaVersion) {
        List<AnalyzerProfile> matches = getAllMatching(
                Map.of("profileMetaId", profileMetaId, "profileMetaVersion", profileMetaVersion));
        return matches.isEmpty() ? null : matches.get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByMetaIdAndVersion(String profileMetaId, String profileMetaVersion) {
        return findByMetaIdAndVersion(profileMetaId, profileMetaVersion) != null;
    }

    @Override
    public void clearLatestForMetaId(String profileMetaId) {
        List<AnalyzerProfile> current = getAllMatching(
                Map.of("profileMetaId", profileMetaId, "isLatest", Boolean.TRUE));
        for (AnalyzerProfile p : current) {
            p.setIsLatest(false);
            update(p);
        }
    }
}
