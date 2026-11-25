package org.openelisglobal.analyzer.dao;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzer.valueholder.Analyzer;
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
    public Optional<AnalyzerMapping> get(String id) {
        String hql = "FROM AnalyzerMapping m JOIN FETCH m.analyzer WHERE m.id = :id";
        return entityManager.unwrap(Session.class).createQuery(hql, AnalyzerMapping.class).setParameter("id", id)
                .getResultList().stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnalyzerMapping> findLatestByAnalyzerId(String analyzerId) {
        if (analyzerId == null || analyzerId.trim().isEmpty()) {
            return Optional.empty();
        }
        String hql = "FROM AnalyzerMapping m JOIN FETCH m.analyzer a WHERE a.id = :analyzerId "
                + "ORDER BY m.revisionNumber DESC";
        Query<AnalyzerMapping> query = entityManager.unwrap(Session.class).createQuery(hql, AnalyzerMapping.class);
        query.setParameter("analyzerId", analyzerId.trim());
        query.setMaxResults(1);
        return query.getResultList().stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analyzer> findAnalyzersInForceOnProfile(String profileId) {
        if (profileId == null || profileId.trim().isEmpty()) {
            return List.of();
        }
        String hql = "SELECT a FROM Analyzer a JOIN FETCH a.mapping m WHERE m.profileId = :profileId "
                + "ORDER BY lower(a.name), a.id";
        Query<Analyzer> query = entityManager.unwrap(Session.class).createQuery(hql, Analyzer.class);
        query.setParameter("profileId", profileId.trim());
        return query.getResultList();
    }

    @Override
    public void deleteAnalyzerWithItsMappings(String analyzerId) {
        Session session = entityManager.unwrap(Session.class);
        String id = analyzerId.trim();
        String mappings = "SELECT m.id FROM AnalyzerMapping m WHERE m.analyzer.id = :id";
        for (String hql : List.of("UPDATE Analyzer SET mapping = NULL WHERE id = :id",
                "DELETE FROM AnalyzerMappingConfirmation WHERE mapping.id IN (" + mappings + ")",
                "DELETE FROM AnalyzerMappingResult WHERE mapping.id IN (" + mappings + ")",
                "DELETE FROM AnalyzerMappingTest WHERE mapping.id IN (" + mappings + ")",
                "DELETE FROM AnalyzerMapping WHERE analyzer.id = :id", "DELETE FROM Analyzer WHERE id = :id")) {
            session.createQuery(hql).setParameter("id", id).executeUpdate();
        }
    }
}
