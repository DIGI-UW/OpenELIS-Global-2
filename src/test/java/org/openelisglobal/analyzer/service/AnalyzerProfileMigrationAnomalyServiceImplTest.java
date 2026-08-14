package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.dao.AnalyzerProfileMigrationAnomalyDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileMigrationAnomaly;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileMigrationAnomaly.Code;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileMigrationAnomaly.Status;
import org.openelisglobal.audittrail.dao.AuditTrailService;
import org.openelisglobal.common.action.IActionConstants;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerProfileMigrationAnomalyServiceImplTest {

    @Mock
    private AnalyzerProfileMigrationAnomalyDAO anomalyDAO;

    @Mock
    private AuditTrailService auditTrailService;

    private AnalyzerProfileMigrationAnomalyServiceImpl service;
    private Analyzer analyzer;

    @Before
    public void setUp() {
        service = new AnalyzerProfileMigrationAnomalyServiceImpl(anomalyDAO, auditTrailService);
        analyzer = new Analyzer();
        analyzer.setId("71");
    }

    @Test
    public void replaceOpenPreservesMatchingFindingResolvesRemovedFindingAndInsertsNewFinding() {
        AnalyzerProfileMigrationAnomaly unchanged = anomaly("same-id", Code.LOCAL_TEST_INACTIVE_OR_MISSING, "WBC",
                "9001", "Existing detail");
        Timestamp originalDetectedAt = Timestamp.from(Instant.parse("2026-08-13T18:00:00Z"));
        unchanged.setDetectedAt(originalDetectedAt);
        AnalyzerProfileMigrationAnomaly removed = anomaly("removed-id", Code.PROFILE_SOURCE_ROW_MISSING, "RBC", "9002",
                "Removed detail");
        when(anomalyDAO.findByAnalyzerIdForUpdate("71")).thenReturn(List.of(removed, unchanged));

        List<AnalyzerProfileMigrationAnomaly> result = service.replaceOpen(analyzer,
                List.of(new AnalyzerProfileMigrationAnomalyDraft(Code.LOCAL_TEST_INACTIVE_OR_MISSING, "WBC", "9001",
                        "Current detail"),
                        new AnalyzerProfileMigrationAnomalyDraft(Code.LOCAL_TEST_NOT_UNIQUE, "HGB", "9003",
                                "Two active local tests match")),
                "42");

        assertEquals(2, result.size());
        assertSame(unchanged, result.get(0));
        assertEquals(originalDetectedAt, unchanged.getDetectedAt());
        assertEquals("Existing detail", unchanged.getDetail());
        assertEquals(Status.RESOLVED, removed.getStatus());
        assertEquals("42", removed.getResolvedBy());
        assertNotNull(removed.getResolvedAt());

        ArgumentCaptor<AnalyzerProfileMigrationAnomaly> inserted = ArgumentCaptor
                .forClass(AnalyzerProfileMigrationAnomaly.class);
        verify(anomalyDAO).insert(inserted.capture());
        AnalyzerProfileMigrationAnomaly created = inserted.getValue();
        assertSame(created, result.get(1));
        assertSame(analyzer, created.getAnalyzer());
        assertEquals(Code.LOCAL_TEST_NOT_UNIQUE, created.getCode());
        assertEquals("LOCAL_TEST_NOT_UNIQUE|HGB|9003", created.getEvidenceKey());
        assertEquals("HGB", created.getLegacySourceKey());
        assertEquals("9003", created.getLegacyTestId());
        assertEquals("Two active local tests match", created.getDetail());
        assertEquals(Status.OPEN, created.getStatus());
        assertEquals("42", created.getDetectedBy());
        assertNotNull(created.getDetectedAt());

        verify(anomalyDAO).update(removed);
        verify(anomalyDAO, never()).update(unchanged);
        verify(auditTrailService).saveHistory(eq(removed), any(AnalyzerProfileMigrationAnomaly.class), eq("42"),
                eq(IActionConstants.AUDIT_TRAIL_UPDATE), eq("analyzer_profile_migration_anomaly"));
        verify(auditTrailService).saveNewHistory(created, "42", "analyzer_profile_migration_anomaly");
    }

    @Test
    public void replaceOpenReturnsDeterministicEvidenceOrderIndependentOfDraftOrder() {
        when(anomalyDAO.findByAnalyzerIdForUpdate("71")).thenReturn(List.of());

        List<AnalyzerProfileMigrationAnomaly> result = service.replaceOpen(analyzer,
                List.of(new AnalyzerProfileMigrationAnomalyDraft(Code.PROFILE_SOURCE_ROW_MISSING, "Z", "2", "z"),
                        new AnalyzerProfileMigrationAnomalyDraft(Code.PROFILE_REF_MISSING, null, null, "profile"),
                        new AnalyzerProfileMigrationAnomalyDraft(Code.PROFILE_SOURCE_ROW_MISSING, "A", "1", "a")),
                "42");

        assertEquals(List.of("PROFILE_REF_MISSING|-|-", "PROFILE_SOURCE_ROW_MISSING|A|1",
                "PROFILE_SOURCE_ROW_MISSING|Z|2"),
                result.stream().map(AnalyzerProfileMigrationAnomaly::getEvidenceKey).toList());
    }

    @Test(expected = IllegalArgumentException.class)
    public void replaceOpenRejectsDuplicateEvidenceInsteadOfSilentlyCollapsingFindings() {
        AnalyzerProfileMigrationAnomalyDraft duplicate = new AnalyzerProfileMigrationAnomalyDraft(
                Code.PROFILE_SOURCE_ROW_MISSING, "WBC", "9001", "Missing source row");

        service.replaceOpen(analyzer, List.of(duplicate, duplicate), "42");
    }

    @Test
    public void resolveAllAuditsEveryOpenFindingAndLeavesNoneOpen() {
        AnalyzerProfileMigrationAnomaly first = anomaly("first", Code.PROFILE_REF_MISSING, null, null, "Profile");
        AnalyzerProfileMigrationAnomaly second = anomaly("second", Code.PROFILE_SOURCE_ROW_MISSING, "WBC", "9001",
                "Row");
        when(anomalyDAO.findByAnalyzerIdForUpdate("71")).thenReturn(List.of(second, first));

        service.resolveAll("71", "42");

        for (AnalyzerProfileMigrationAnomaly anomaly : List.of(first, second)) {
            assertEquals(Status.RESOLVED, anomaly.getStatus());
            assertEquals("42", anomaly.getResolvedBy());
            assertNotNull(anomaly.getResolvedAt());
            verify(anomalyDAO).update(anomaly);
            verify(auditTrailService).saveHistory(eq(anomaly), any(AnalyzerProfileMigrationAnomaly.class), eq("42"),
                    eq(IActionConstants.AUDIT_TRAIL_UPDATE), eq("analyzer_profile_migration_anomaly"));
        }
    }

    @Test
    public void findOpenDelegatesToReadOnlyDeterministicQuery() {
        AnalyzerProfileMigrationAnomaly finding = anomaly("finding", Code.PROFILE_REF_MISSING, null, null,
                "Profile required");
        when(anomalyDAO.findOpenByAnalyzerId("71")).thenReturn(List.of(finding));

        assertEquals(List.of(finding), service.findOpen("71"));
    }

    private AnalyzerProfileMigrationAnomaly anomaly(String id, Code code, String legacySourceKey, String legacyTestId,
            String detail) {
        AnalyzerProfileMigrationAnomaly anomaly = new AnalyzerProfileMigrationAnomaly();
        anomaly.setId(id);
        anomaly.setAnalyzer(analyzer);
        anomaly.setCode(code);
        anomaly.setEvidenceKey(AnalyzerProfileMigrationAnomalyDraft.evidenceKey(code, legacySourceKey, legacyTestId));
        anomaly.setLegacySourceKey(legacySourceKey);
        anomaly.setLegacyTestId(legacyTestId);
        anomaly.setDetail(detail);
        anomaly.setStatus(Status.OPEN);
        anomaly.setDetectedBy("41");
        anomaly.setDetectedAt(Timestamp.from(Instant.parse("2026-08-13T17:00:00Z")));
        return anomaly;
    }
}
