package org.openelisglobal.analyzer.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.openelisglobal.analyzer.valueholder.AnalyzerUpgradeConfig;
import org.openelisglobal.analyzer.valueholder.AnalyzerUpgradeMapping;
import org.openelisglobal.analyzer.valueholder.AnalyzerUpgradeSource;
import org.openelisglobal.analyzer.valueholder.AnalyzerUpgradeType;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads preserved settings only; all writes use the current analyzer services.
 */
@Repository
@Transactional(readOnly = true)
public class AnalyzerUpgradeDAO {
    /**
     * Retained storage the read-only entities map. A database that ran the removed
     * changeset 098-remove-superseded-analyzer-schema no longer has it, and the
     * entity queries fail there.
     */
    private static final Set<String> RETAINED_STORAGE = Set.of("analyzer.analyzer_type_id", "analyzer.ip_address",
            "analyzer.port", "analyzer.communication_mode", "analyzer.import_directory", "analyzer.file_pattern",
            "analyzer.file_format", "analyzer.column_mappings_json", "analyzer_type.id",
            "analyzer_plugin_config.config", "analyzer_test_map.analyzer_test_name",
            "serial_port_configuration.active");
    private final AtomicBoolean reportedAbsent = new AtomicBoolean();
    @PersistenceContext
    private EntityManager entityManager;

    public List<String> pendingIds() {
        if (!retainedStorageExists())
            return List.of();
        return entityManager.createQuery("""
                SELECT s.id FROM AnalyzerUpgradeSource s, Analyzer a
                WHERE a.id = s.id AND a.bridgeConnectionId IS NULL
                  AND (s.typeId IS NOT NULL
                    OR s.host IS NOT NULL OR s.directory IS NOT NULL
                    OR EXISTS (FROM AnalyzerUpgradeConfig c WHERE c.id = s.id)
                    OR EXISTS (FROM AnalyzerUpgradeMapping m WHERE m.analyzerId = s.id)
                    OR EXISTS (FROM AnalyzerUpgradeSerial p WHERE p.analyzerId = s.id AND p.active = true))
                """, String.class).getResultList();
    }

    public boolean retainedStorageExists() {
        List<?> rows = entityManager.createNativeQuery("""
                SELECT table_name || '.' || column_name FROM information_schema.columns
                WHERE table_schema = 'clinlims' AND table_name IN ('analyzer', 'analyzer_type',
                  'analyzer_plugin_config', 'analyzer_test_map', 'serial_port_configuration')
                """).getResultList();
        Set<String> present = rows.stream().map(Object::toString).collect(Collectors.toSet());
        boolean exists = present.containsAll(RETAINED_STORAGE);
        if (!exists && reportedAbsent.compareAndSet(false, true))
            LogEvent.logInfo(getClass().getSimpleName(), "retainedStorageExists",
                    "Retained analyzer settings storage is absent; no analyzer upgrade is pending");
        return exists;
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
