package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.dao.AnalyzerDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileMigrationAnomaly;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingRevision;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingTest;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.openelisglobal.audittrail.dao.AuditTrailService;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.test.service.TestService;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerProfileMigrationExecutorTest {

    @Mock
    private AnalyzerDAO analyzerDAO;

    @Mock
    private AnalyzerTestMappingService legacyMappingService;

    @Mock
    private AnalyzerSiteBindingService siteBindingService;

    @Mock
    private AnalyzerProfileMigrationAnomalyService anomalyService;

    @Mock
    private TestService testService;

    @Mock
    private AuditTrailService auditTrailService;

    private AnalyzerProfileMigrationExecutor executor;
    private Analyzer analyzer;

    @Before
    public void setUp() {
        executor = new AnalyzerProfileMigrationExecutor(analyzerDAO, legacyMappingService, siteBindingService,
                anomalyService, testService, auditTrailService);
        analyzer = new Analyzer();
        analyzer.setId("71");
        analyzer.setName("Legacy analyzer");
        analyzer.setStatus(Analyzer.AnalyzerStatus.SETUP);
        when(analyzerDAO.findByIdForUpdate("71")).thenReturn(Optional.of(analyzer));
        when(anomalyService.replaceOpen(eq(analyzer), any(), eq("42"))).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<AnalyzerProfileMigrationAnomalyDraft> findings = invocation.getArgument(1);
            return findings.stream().map(this::anomaly).toList();
        });
    }

    @Test
    public void executeMigratesEveryExactSourceRowAndAtomicallySwitchesTheAnalyzer() throws Exception {
        BridgeProfileCatalogEntry profile = profile(
                """
                        [
                          {"sourceRowKey":"wbc","analyzerCode":"WBC","aliases":["WBC#"],"displayName":"White cells","resultType":"NUMERIC","normalizedCoding":{"system":"http://loinc.org","code":"6690-2"}},
                          {"sourceRowKey":"hiv","analyzerCode":"HIV-INTERP","aliases":["HIV"],"displayName":"HIV interpretation","resultType":"QUALITATIVE","normalizedCoding":{"system":"http://loinc.org","code":"7918-6"}}
                        ]
                        """);
        AnalyzerTestMapping wbc = mapping("WBC#", "101", null);
        AnalyzerTestMapping hiv = mapping("HIV-INTERP", "102", "component-1");
        when(legacyMappingService.getAllForAnalyzer("71")).thenReturn(List.of(hiv, wbc));
        when(testService.getActiveTestById(101)).thenReturn(activeTest("101"));
        when(testService.getActiveTestById(102)).thenReturn(activeTest("102"));
        AnalyzerSiteBindingSnapshot snapshot = snapshot("revision-1");
        when(siteBindingService.findOrCreate(any(AnalyzerSiteBindingDraft.class), eq("42"))).thenReturn(snapshot);

        AnalyzerProfileMigrationResult result = executor.execute("71", profile, "42");

        assertEquals(AnalyzerProfileMigrationResult.Status.MIGRATED, result.status());
        assertEquals("revision-1", result.siteBindingRevisionId());
        assertEquals("site.mock", analyzer.getBridgeProfileId());
        assertEquals(Integer.valueOf(3), analyzer.getBridgeProfileRevision());
        assertSame(snapshot.revision(), analyzer.getSiteBindingRevision());

        ArgumentCaptor<AnalyzerSiteBindingDraft> draft = ArgumentCaptor.forClass(AnalyzerSiteBindingDraft.class);
        verify(siteBindingService).findOrCreate(draft.capture(), eq("42"));
        assertEquals(List.of("hiv", "wbc"),
                draft.getValue().tests().stream().map(AnalyzerSiteBindingTestDraft::sourceRowKey).toList());
        assertEquals(List.of("102", "101"),
                draft.getValue().tests().stream().map(AnalyzerSiteBindingTestDraft::testId).toList());
        assertEquals("component-1", draft.getValue().tests().get(0).componentId());
        verify(anomalyService).replaceOpen(analyzer, List.of(), "42");
        verify(analyzerDAO).update(analyzer);
        verify(auditTrailService).saveHistory(eq(analyzer), any(Analyzer.class), eq("42"),
                eq(IActionConstants.AUDIT_TRAIL_UPDATE), eq("analyzer"));
    }

    @Test
    public void executeBlocksWithoutPartialBindingOrAnalyzerSwitchWhenAnyRowIsUnmatched() throws Exception {
        BridgeProfileCatalogEntry profile = profile("""
                [
                  {"sourceRowKey":"wbc","analyzerCode":"WBC","aliases":[],"resultType":"NUMERIC"},
                  {"sourceRowKey":"hgb","analyzerCode":"HGB","aliases":[],"resultType":"NUMERIC"}
                ]
                """);
        when(legacyMappingService.getAllForAnalyzer("71"))
                .thenReturn(List.of(mapping("WBC", "101", null), mapping("EXTRA", "103", null)));
        when(testService.getActiveTestById(101)).thenReturn(activeTest("101"));

        AnalyzerProfileMigrationResult result = executor.execute("71", profile, "42");

        assertEquals(AnalyzerProfileMigrationResult.Status.BLOCKED, result.status());
        assertEquals(List.of("PROFILE_SOURCE_ROW_MISSING|EXTRA|103", "PROFILE_SOURCE_ROW_MISSING|hgb|-"),
                result.anomalies().stream().map(AnalyzerProfileMigrationAnomaly::getEvidenceKey).toList());
        assertNull(analyzer.getBridgeProfileId());
        assertNull(analyzer.getSiteBindingRevision());
        verifyZeroInteractions(siteBindingService);
        verify(analyzerDAO, never()).update(any());
        verifyZeroInteractions(auditTrailService);
    }

    @Test
    public void executeBlocksAmbiguousAliasesAndPreservesEachProfileSourceRow() throws Exception {
        BridgeProfileCatalogEntry profile = profile("""
                [
                  {"sourceRowKey":"first","analyzerCode":"A","aliases":["SHARED"],"resultType":"NUMERIC"},
                  {"sourceRowKey":"second","analyzerCode":"B","aliases":["SHARED"],"resultType":"NUMERIC"}
                ]
                """);
        when(legacyMappingService.getAllForAnalyzer("71")).thenReturn(List.of(mapping("SHARED", "101", null)));
        AnalyzerProfileMigrationResult result = executor.execute("71", profile, "42");

        assertEquals(AnalyzerProfileMigrationResult.Status.BLOCKED, result.status());
        assertEquals(List.of("DISTINCT_SOURCE_ROWS_SHARE_NORMALIZED_IDENTITY|SHARED|101"),
                result.anomalies().stream().map(AnalyzerProfileMigrationAnomaly::getEvidenceKey).toList());
        verify(testService, never()).getActiveTestById(anyInt());
        verifyZeroInteractions(siteBindingService);
    }

    @Test
    public void executeBlocksAnInactiveLegacyTargetWithoutReplacingItsEvidence() throws Exception {
        BridgeProfileCatalogEntry profile = profile("""
                [{"sourceRowKey":"wbc","analyzerCode":"WBC","aliases":[],"resultType":"NUMERIC"}]
                """);
        when(legacyMappingService.getAllForAnalyzer("71")).thenReturn(List.of(mapping("WBC", "101", null)));
        when(testService.getActiveTestById(101)).thenReturn(null);
        AnalyzerProfileMigrationResult result = executor.execute("71", profile, "42");

        assertEquals(List.of("LOCAL_TEST_INACTIVE_OR_MISSING|WBC|101"),
                result.anomalies().stream().map(AnalyzerProfileMigrationAnomaly::getEvidenceKey).toList());
        verifyZeroInteractions(siteBindingService);
        verify(analyzerDAO, never()).update(any());
    }

    @Test
    public void executePreservesDistinctExplicitBindingsThatShareANormalizedIdentity() throws Exception {
        BridgeProfileCatalogEntry profile = profile(
                """
                        [
                          {"sourceRowKey":"first","analyzerCode":"A","aliases":[],"resultType":"NUMERIC","normalizedCoding":{"system":"http://loinc.org","code":"shared"}},
                          {"sourceRowKey":"second","analyzerCode":"B","aliases":[],"resultType":"NUMERIC","normalizedCoding":{"system":"http://loinc.org","code":"shared"}}
                        ]
                        """);
        when(legacyMappingService.getAllForAnalyzer("71"))
                .thenReturn(List.of(mapping("A", "101", null), mapping("B", "102", null)));
        when(testService.getActiveTestById(101)).thenReturn(activeTest("101"));
        when(testService.getActiveTestById(102)).thenReturn(activeTest("102"));
        when(siteBindingService.findOrCreate(any(AnalyzerSiteBindingDraft.class), eq("42")))
                .thenReturn(snapshot("revision-1"));

        AnalyzerProfileMigrationResult result = executor.execute("71", profile, "42");

        assertEquals(AnalyzerProfileMigrationResult.Status.MIGRATED, result.status());
        ArgumentCaptor<AnalyzerSiteBindingDraft> draft = ArgumentCaptor.forClass(AnalyzerSiteBindingDraft.class);
        verify(siteBindingService).findOrCreate(draft.capture(), eq("42"));
        assertEquals(List.of("first", "second"),
                draft.getValue().tests().stream().map(AnalyzerSiteBindingTestDraft::sourceRowKey).toList());
        assertEquals(List.of("101", "102"),
                draft.getValue().tests().stream().map(AnalyzerSiteBindingTestDraft::testId).toList());
        verify(anomalyService).replaceOpen(analyzer, List.of(), "42");
    }

    @Test
    public void executePreservesDistinctExplicitBindingsThatShareTheSameLocalTestTarget() throws Exception {
        BridgeProfileCatalogEntry profile = profile(
                """
                        [
                          {"sourceRowKey":"first","analyzerCode":"A","aliases":[],"resultType":"NUMERIC","normalizedCoding":{"system":"http://loinc.org","code":"one"}},
                          {"sourceRowKey":"second","analyzerCode":"B","aliases":[],"resultType":"NUMERIC","normalizedCoding":{"system":"http://loinc.org","code":"two"}}
                        ]
                        """);
        when(legacyMappingService.getAllForAnalyzer("71"))
                .thenReturn(List.of(mapping("A", "101", null), mapping("B", "101", null)));
        when(testService.getActiveTestById(101)).thenReturn(activeTest("101"));
        when(siteBindingService.findOrCreate(any(AnalyzerSiteBindingDraft.class), eq("42")))
                .thenReturn(snapshot("revision-1"));

        AnalyzerProfileMigrationResult result = executor.execute("71", profile, "42");

        assertEquals(AnalyzerProfileMigrationResult.Status.MIGRATED, result.status());
        ArgumentCaptor<AnalyzerSiteBindingDraft> draft = ArgumentCaptor.forClass(AnalyzerSiteBindingDraft.class);
        verify(siteBindingService).findOrCreate(draft.capture(), eq("42"));
        assertEquals(List.of("first", "second"),
                draft.getValue().tests().stream().map(AnalyzerSiteBindingTestDraft::sourceRowKey).toList());
        assertEquals(List.of("101", "101"),
                draft.getValue().tests().stream().map(AnalyzerSiteBindingTestDraft::testId).toList());
        verify(anomalyService).replaceOpen(analyzer, List.of(), "42");
    }

    @Test
    public void executeIsIdempotentWhenTheAnalyzerAlreadyReferencesTheExactBinding() throws Exception {
        BridgeProfileCatalogEntry profile = profile("""
                [{"sourceRowKey":"wbc","analyzerCode":"WBC","aliases":[],"resultType":"NUMERIC"}]
                """);
        when(legacyMappingService.getAllForAnalyzer("71")).thenReturn(List.of(mapping("WBC", "101", null)));
        when(testService.getActiveTestById(101)).thenReturn(activeTest("101"));
        AnalyzerSiteBindingSnapshot snapshot = snapshot("revision-1");
        analyzer.setBridgeProfileId("site.mock");
        analyzer.setBridgeProfileRevision(3);
        analyzer.setSiteBindingRevision(snapshot.revision());
        when(siteBindingService.findOrCreate(any(AnalyzerSiteBindingDraft.class), eq("42"))).thenReturn(snapshot);

        AnalyzerProfileMigrationResult result = executor.execute("71", profile, "42");

        assertEquals(AnalyzerProfileMigrationResult.Status.UNCHANGED, result.status());
        verify(anomalyService).replaceOpen(analyzer, List.of(), "42");
        verify(analyzerDAO, never()).update(any());
        verifyZeroInteractions(auditTrailService);
    }

    private static AnalyzerTestMapping mapping(String analyzerCode, String testId, String componentId) {
        AnalyzerTestMapping mapping = new AnalyzerTestMapping();
        mapping.setAnalyzerId("71");
        mapping.setAnalyzerTestName(analyzerCode);
        mapping.setTestId(testId);
        mapping.setComponentId(componentId);
        return mapping;
    }

    private static org.openelisglobal.test.valueholder.Test activeTest(String id) {
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId(id);
        test.setIsActive("Y");
        return test;
    }

    private static AnalyzerSiteBindingSnapshot snapshot(String revisionId) {
        AnalyzerSiteBinding binding = new AnalyzerSiteBinding();
        binding.setId("binding-1");
        AnalyzerSiteBindingRevision revision = new AnalyzerSiteBindingRevision();
        revision.setId(revisionId);
        revision.setSiteBinding(binding);
        revision.setBridgeProfileId("site.mock");
        revision.setBridgeProfileRevision(3);
        return new AnalyzerSiteBindingSnapshot(binding, revision, List.<AnalyzerSiteBindingTest>of());
    }

    private static BridgeProfileCatalogEntry profile(String tests) throws Exception {
        return new BridgeProfileCatalogEntry(new ObjectMapper().readTree("""
                {
                  "profileId":"site.mock",
                  "revision":3,
                  "status":"ACTIVE",
                  "tests":%s
                }
                """.formatted(tests)),
                new BridgeProfileAudit("CREATED", "bridge", Instant.parse("2026-08-14T08:00:00Z")),
                "sha256:" + "a".repeat(64));
    }

    private AnalyzerProfileMigrationAnomaly anomaly(AnalyzerProfileMigrationAnomalyDraft finding) {
        AnalyzerProfileMigrationAnomaly anomaly = new AnalyzerProfileMigrationAnomaly();
        anomaly.setAnalyzer(analyzer);
        anomaly.setCode(finding.code());
        anomaly.setEvidenceKey(finding.evidenceKey());
        anomaly.setLegacySourceKey(finding.legacySourceKey());
        anomaly.setLegacyTestId(finding.legacyTestId());
        anomaly.setDetail(finding.detail());
        return anomaly;
    }
}
