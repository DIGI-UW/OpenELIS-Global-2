package org.openelisglobal.analyzer.dao;

import static org.junit.Assert.assertEquals;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileMigrationAnomaly;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class AnalyzerProfileMigrationAnomalyDAOIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AnalyzerDAO analyzerDAO;

    @Autowired
    private AnalyzerProfileMigrationAnomalyDAO anomalyDAO;

    @Test
    public void openQueriesExcludeResolvedRowsAndReturnDeterministicEvidenceOrder() {
        Analyzer analyzer = new Analyzer();
        analyzer.setName("OGC-1054 migration anomaly " + UUID.randomUUID());
        analyzer.setStatus(Analyzer.AnalyzerStatus.SETUP);
        analyzerDAO.insert(analyzer);

        anomalyDAO.insert(
                anomaly(analyzer, "PROFILE_SOURCE_ROW_MISSING|Z|2", AnalyzerProfileMigrationAnomaly.Status.OPEN));
        anomalyDAO.insert(anomaly(analyzer, "PROFILE_REF_MISSING|-|-", AnalyzerProfileMigrationAnomaly.Status.OPEN));
        anomalyDAO.insert(
                anomaly(analyzer, "LOCAL_TEST_NOT_UNIQUE|A|1", AnalyzerProfileMigrationAnomaly.Status.RESOLVED));

        List<String> readOnly = anomalyDAO.findOpenByAnalyzerId(analyzer.getId()).stream()
                .map(AnalyzerProfileMigrationAnomaly::getEvidenceKey).toList();
        List<String> locked = anomalyDAO.findByAnalyzerIdForUpdate(analyzer.getId()).stream()
                .map(AnalyzerProfileMigrationAnomaly::getEvidenceKey).toList();

        assertEquals(List.of("PROFILE_REF_MISSING|-|-", "PROFILE_SOURCE_ROW_MISSING|Z|2"), readOnly);
        assertEquals(readOnly, locked);
    }

    private static AnalyzerProfileMigrationAnomaly anomaly(Analyzer analyzer, String evidenceKey,
            AnalyzerProfileMigrationAnomaly.Status status) {
        AnalyzerProfileMigrationAnomaly anomaly = new AnalyzerProfileMigrationAnomaly();
        anomaly.setAnalyzer(analyzer);
        anomaly.setCode(evidenceKey.startsWith("PROFILE_REF") ? AnalyzerProfileMigrationAnomaly.Code.PROFILE_REF_MISSING
                : evidenceKey.startsWith("LOCAL_TEST") ? AnalyzerProfileMigrationAnomaly.Code.LOCAL_TEST_NOT_UNIQUE
                        : AnalyzerProfileMigrationAnomaly.Code.PROFILE_SOURCE_ROW_MISSING);
        anomaly.setEvidenceKey(evidenceKey);
        anomaly.setLegacySourceKey("source");
        anomaly.setLegacyTestId("1");
        anomaly.setDetail("Migration evidence");
        anomaly.setStatus(status);
        anomaly.setDetectedBy("1");
        anomaly.setDetectedAt(Timestamp.from(Instant.parse("2026-08-14T08:00:00Z")));
        if (status == AnalyzerProfileMigrationAnomaly.Status.RESOLVED) {
            anomaly.setResolvedBy("1");
            anomaly.setResolvedAt(Timestamp.from(Instant.parse("2026-08-14T09:00:00Z")));
        }
        return anomaly;
    }
}
