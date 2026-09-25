package org.openelisglobal.analyzer.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.openelisglobal.analyzer.valueholder.AnalyzerUpgradeConfig;
import org.openelisglobal.analyzer.valueholder.AnalyzerUpgradeMapping;
import org.openelisglobal.analyzer.valueholder.AnalyzerUpgradeSource;
import org.openelisglobal.analyzer.valueholder.AnalyzerUpgradeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads preserved settings only; all writes use the current analyzer services.
 */
@Repository
@Transactional(readOnly = true)
public class AnalyzerUpgradeDAO {
    @PersistenceContext
    private EntityManager entityManager;

    public List<String> pendingIds() {
        return entityManager.createQuery("""
                SELECT s.id FROM AnalyzerUpgradeSource s, Analyzer a
                WHERE a.id = s.id AND a.bridgeConnectionId IS NULL
                  AND (s.typeId IS NOT NULL OR s.profileBindingId IS NOT NULL
                    OR s.host IS NOT NULL OR s.directory IS NOT NULL
                    OR EXISTS (FROM AnalyzerUpgradeConfig c WHERE c.id = s.id)
                    OR EXISTS (FROM AnalyzerUpgradeMapping m WHERE m.analyzerId = s.id)
                    OR EXISTS (FROM AnalyzerUpgradeSerial p WHERE p.analyzerId = s.id AND p.active = true))
                """, String.class).getResultList();
    }

    public boolean hasSerialSettings(String id) {
        return entityManager.createQuery(
                "SELECT count(p) FROM AnalyzerUpgradeSerial p WHERE p.analyzerId = :id AND p.active = true", Long.class)
                .setParameter("id", id).getSingleResult() > 0;
    }

    public AnalyzerUpgradeSource source(String id) {
        return entityManager.find(AnalyzerUpgradeSource.class, id);
    }

    public AnalyzerUpgradeConfig config(String id) {
        return entityManager.find(AnalyzerUpgradeConfig.class, id);
    }

    public AnalyzerUpgradeType type(String id) {
        return id == null ? null : entityManager.find(AnalyzerUpgradeType.class, id);
    }

    public List<AnalyzerUpgradeMapping> mappings(String id) {
        return entityManager
                .createQuery("FROM AnalyzerUpgradeMapping m WHERE m.analyzerId = :id", AnalyzerUpgradeMapping.class)
                .setParameter("id", id).getResultList();
    }
}
