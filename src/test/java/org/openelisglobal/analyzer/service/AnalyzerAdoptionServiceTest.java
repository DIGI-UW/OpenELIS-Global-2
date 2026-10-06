package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.analyzer.service.AnalyzerMappingAdoption.BlockReason;
import org.openelisglobal.analyzer.service.AnalyzerMappingAdoption.Bucket;
import org.openelisglobal.analyzer.service.AnalyzerMappingAdoption.Row;
import org.openelisglobal.analyzer.valueholder.AnalyzerMapping;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingOrigin;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingResult;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingState;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingTest;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingTestPK;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;

public class AnalyzerAdoptionServiceTest {

    private final AnalyzerMappingService mappingService = mock(AnalyzerMappingService.class);
    private final BridgeProfileCatalogService profiles = mock(BridgeProfileCatalogService.class);
    private final AnalyzerMappingDefaults defaults = mock(AnalyzerMappingDefaults.class);
    private final AnalyzerMappingCatalogService catalog = mock(AnalyzerMappingCatalogService.class);
    private final AnalyzerResultsService results = mock(AnalyzerResultsService.class);
    private final AnalyzerAdoptionService service = new AnalyzerAdoptionServiceImpl(mappingService, profiles, defaults,
            catalog, results);

    private AnalyzerMapping mapping;

    @Before
    public void analyzerOnRevisionOne() throws Exception {
        mapping = new AnalyzerMapping();
        mapping.setId("71");
        mapping.setProfileId("site.adoption");
        mapping.setProfileRevision(1);
        when(profiles.getProfile("site.adoption", 1)).thenReturn(revision(1, "RAW-A", "RAW-B"));
        when(profiles.getProfile("site.adoption", 2)).thenReturn(revision(2, "RAW-A"));
        when(catalog.searchActiveTests(null))
                .thenReturn(List.of(new AnalyzerMappingCatalogService.TestOption("t1", "One", null, List.of())));
        when(results.findHeldMappingResultsByAnalyzer("42")).thenReturn(List.of());
    }

    @Test
    public void bucketsTheAnalyzersOwnMappingAgainstTheNewerRevision() {
        current(row("RAW-A", "t1", AnalyzerMappingOrigin.DEFAULT), row("RAW-B", "t1", AnalyzerMappingOrigin.DEFAULT));
        newDefaults(new AnalyzerMappingTestDraft("RAW-A", AnalyzerMappingState.BOUND, "t1"));

        AnalyzerAdoptionService.AdoptionPlan plan = service.prepareAdoption("42", 2);

        assertEquals(1, plan.fromRevision());
        assertEquals(2, plan.toRevision());
        assertEquals(Bucket.UNCHANGED, row(plan, "RAW-A").bucket());
        assertEquals(Bucket.RETIRED, row(plan, "RAW-B").bucket());
    }

    @Test
    public void onlyHeldResultsFromTheAdoptedFromRevisionBlockADroppedCode() {
        current(row("RAW-A", "t1", AnalyzerMappingOrigin.DEFAULT), row("RAW-B", "t1", AnalyzerMappingOrigin.DEFAULT));
        newDefaults(new AnalyzerMappingTestDraft("RAW-A", AnalyzerMappingState.BOUND, "t1"));
        when(results.findHeldMappingResultsByAnalyzer("42")).thenReturn(List.of(held("RAW-B", 1)));

        assertEquals(Bucket.BLOCKED, row(service.prepareAdoption("42", 2), "RAW-B").bucket());

        when(results.findHeldMappingResultsByAnalyzer("42")).thenReturn(List.of(held("RAW-B", 3)));
        assertEquals(Bucket.RETIRED, row(service.prepareAdoption("42", 2), "RAW-B").bucket());
    }

    @Test
    public void anOverrideOnATestThatIsNoLongerActiveBlocks() {
        current(row("RAW-A", "t9", AnalyzerMappingOrigin.OVERRIDE));
        newDefaults(new AnalyzerMappingTestDraft("RAW-A", AnalyzerMappingState.BOUND, "t1"));

        Row row = row(service.prepareAdoption("42", 2), "RAW-A");

        assertEquals(Bucket.BLOCKED, row.bucket());
        assertEquals(BlockReason.INACTIVE_TEST, row.blockReason());
    }

    @Test
    public void adoptsOnlyANewerRevisionOfTheSameProfile() {
        current(row("RAW-A", "t1", AnalyzerMappingOrigin.DEFAULT));

        assertThrows(IllegalArgumentException.class, () -> service.prepareAdoption("42", 1));
    }

    private void current(AnalyzerMappingTest... rows) {
        when(mappingService.findLatestByAnalyzerId("42")).thenReturn(Optional.of(new AnalyzerMappingSnapshot(mapping,
                List.of(rows), List.<AnalyzerMappingResult>of())));
    }

    private void newDefaults(AnalyzerMappingTestDraft... tests) {
        when(defaults.resolve(org.mockito.ArgumentMatchers.argThat(profile -> profile.revision() == 2)))
                .thenReturn(new AnalyzerMappingDraft(List.of(tests), List.of()));
    }

    private AnalyzerMappingTest row(String code, String testId, AnalyzerMappingOrigin origin) {
        AnalyzerMappingTest row = new AnalyzerMappingTest();
        row.setId(new AnalyzerMappingTestPK(mapping.getId(), code, ""));
        row.setMapping(mapping);
        row.setMappingState(AnalyzerMappingState.BOUND);
        row.setTestId(testId);
        row.setOrigin(origin);
        return row;
    }

    private static AnalyzerResults held(String code, int revision) {
        AnalyzerResults held = new AnalyzerResults();
        held.setRawTestCode(code);
        held.setSourceProfileRevision(revision);
        held.setImportIssueReason(AnalyzerResults.IMPORT_ISSUE_UNKNOWN_RESULT_VALUE);
        return held;
    }

    private static Row row(AnalyzerAdoptionService.AdoptionPlan plan, String code) {
        return plan.rows().stream().filter(row -> row.key().equals(AnalyzerMappingRowKey.main(code))).findFirst()
                .orElseThrow(() -> new AssertionError("no adoption row for " + code));
    }

    private static BridgeProfileCatalog.ProfileRevision revision(int revision, String... codes) throws Exception {
        StringBuilder tests = new StringBuilder();
        for (String code : codes) {
            tests.append(tests.length() == 0 ? "" : ",").append("{\"test_code\":\"").append(code)
                    .append("\",\"loinc\":\"94500-6\",\"result_type\":\"quantitative\"}");
        }
        var mapper = new ObjectMapper();
        return new BridgeProfileCatalog.ProfileRevision(mapper.readTree("{\"profileMeta\":{\"id\":\"site.adoption\","
                + "\"displayName\":\"Adoption\"},\"catalog\":{\"revision\":" + revision
                + ",\"revisionFingerprint\":\"sha256:" + "a".repeat(64)
                + "\",\"source\":\"SHIPPED\",\"status\":\"ACTIVE\"},\"protocol\":{\"name\":\"ASTM\"},"
                + "\"default_test_mappings\":[" + tests + "]}"), mapper.createObjectNode());
    }
}
