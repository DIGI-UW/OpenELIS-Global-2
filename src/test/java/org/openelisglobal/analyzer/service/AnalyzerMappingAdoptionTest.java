package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.openelisglobal.analyzer.service.AnalyzerMappingAdoption.BlockReason;
import org.openelisglobal.analyzer.service.AnalyzerMappingAdoption.Bucket;
import org.openelisglobal.analyzer.service.AnalyzerMappingAdoption.Row;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingOrigin;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingState;

/**
 * Rule 7: adopting revision N+1 buckets every record of the analyzer's mapping.
 */
public class AnalyzerMappingAdoptionTest {

    private static final String POS = "LA6576-8";
    private static final String NEG = "LA6577-6";

    @Test
    public void anIdenticalRowIsUnchangedWhateverItsOrigin() throws Exception {
        var profile = profile(test("RAW-A", "94500-6", "POS", "NEG"), test("RAW-B", "94309-2", "POS", "NEG"));
        var current = draft(bound("RAW-A", "t1", AnalyzerMappingOrigin.DEFAULT),
                bound("RAW-B", "t9", AnalyzerMappingOrigin.OVERRIDE));
        var defaults = draft(bound("RAW-A", "t1", null), bound("RAW-B", "t2", null));

        var rows = plan(profile, profile, current, defaults);

        assertEquals(Bucket.UNCHANGED, row(rows, "RAW-A").bucket());
        assertEquals("t1", row(rows, "RAW-A").proposed().test().testId());
        assertEquals(Bucket.UNCHANGED, row(rows, "RAW-B").bucket());
        assertEquals("the operator's override stands", "t9", row(rows, "RAW-B").proposed().test().testId());
    }

    @Test
    public void aDefaultRowWhoseNewDefaultDiffersIsChangedAndProposesTheNewDefault() throws Exception {
        var from = profile(test("RAW-A", "94500-6", "POS", "NEG"));
        var to = profile(test("RAW-A", "94309-2", "POS", "NEG"));

        var row = row(plan(from, to, draft(bound("RAW-A", "t1", AnalyzerMappingOrigin.DEFAULT)),
                draft(bound("RAW-A", "t3", null))), "RAW-A");

        assertEquals(Bucket.CHANGED, row.bucket());
        assertEquals("t1", row.current().test().testId());
        assertEquals("t3", row.proposed().test().testId());
    }

    @Test
    public void aChangedRowKeepsTheOverrideBesideTheNewDefault() throws Exception {
        var from = profile(test("RAW-A", "94500-6", "POS", "NEG"));
        var to = profile(test("RAW-A", "94500-6", "POS", "NEG", "IND"));

        var row = row(plan(from, to, draft(bound("RAW-A", "t2", AnalyzerMappingOrigin.OVERRIDE)),
                draft(bound("RAW-A", "t1", null))), "RAW-A");

        assertEquals(Bucket.CHANGED, row.bucket());
        assertEquals("t2", row.proposed().test().testId());
        assertEquals("t1", row.newDefault().test().testId());
    }

    @Test
    public void aCodeTheNewRevisionAddsNeedsMappingWithItsDefault() throws Exception {
        var from = profile(test("RAW-A", "94500-6", "POS", "NEG"));
        var to = profile(test("RAW-A", "94500-6", "POS", "NEG"), test("RAW-B", "94309-2", "POS", "NEG"));

        var row = row(plan(from, to, draft(bound("RAW-A", "t1", AnalyzerMappingOrigin.DEFAULT)),
                draft(bound("RAW-A", "t1", null), bound("RAW-B", "t4", null))), "RAW-B");

        assertEquals(Bucket.NEEDS_MAPPING, row.bucket());
        assertEquals("t4", row.proposed().test().testId());
    }

    @Test
    public void aRenamedCodeRetiresTheOldRowAndResolvesTheNewOneFresh() throws Exception {
        var from = profile(test("RAW-A", "94500-6", "POS", "NEG"));
        var to = profile(test("RAW-A2", "94500-6", "POS", "NEG"));

        var rows = plan(from, to, draft(bound("RAW-A", "t2", AnalyzerMappingOrigin.OVERRIDE)),
                draft(bound("RAW-A2", "t1", null)));

        assertEquals(Bucket.RETIRED, row(rows, "RAW-A").bucket());
        assertEquals(Bucket.NEEDS_MAPPING, row(rows, "RAW-A2").bucket());
        assertEquals("nothing carries over from the old code", "t1", row(rows, "RAW-A2").proposed().test().testId());
    }

    @Test
    public void anOverrideEqualToTheNewDefaultIsUnchangedAndStaysAnOverride() throws Exception {
        var from = profile(test("RAW-A", "94500-6", "POS", "NEG"));
        var to = profile(test("RAW-A", "94309-2", "POS", "NEG"));

        var row = row(plan(from, to, draft(bound("RAW-A", "t3", AnalyzerMappingOrigin.OVERRIDE)),
                draft(bound("RAW-A", "t3", null))), "RAW-A");

        assertEquals(Bucket.UNCHANGED, row.bucket());
        assertEquals(AnalyzerMappingOrigin.OVERRIDE, row.proposed().test().origin());
        assertTrue(row.alsoDefault());
    }

    @Test
    public void anOverrideOnAnInactiveTestBlocks() throws Exception {
        var profile = profile(test("RAW-A", "94500-6", "POS", "NEG"));

        var row = row(AnalyzerMappingAdoption.plan(profile, profile,
                draft(bound("RAW-A", "t2", AnalyzerMappingOrigin.OVERRIDE)), draft(bound("RAW-A", "t1", null)),
                Set.of("t2"), Set.of()), "RAW-A");

        assertEquals(Bucket.BLOCKED, row.bucket());
        assertEquals(BlockReason.INACTIVE_TEST, row.blockReason());
    }

    @Test
    public void aRemovedCodeWithHeldResultsBlocksAndOneWithoutIsRetired() throws Exception {
        var from = profile(test("RAW-A", "94500-6", "POS", "NEG"), test("RAW-B", "94309-2", "POS", "NEG"));
        var to = profile(test("RAW-C", "94500-6", "POS", "NEG"));

        var rows = AnalyzerMappingAdoption.plan(from, to,
                draft(bound("RAW-A", "t1", AnalyzerMappingOrigin.DEFAULT),
                        bound("RAW-B", "t2", AnalyzerMappingOrigin.DEFAULT)),
                draft(bound("RAW-C", "t1", null)), Set.of(), Set.of(AnalyzerMappingRowKey.main("RAW-A")));

        assertEquals(Bucket.BLOCKED, row(rows, "RAW-A").bucket());
        assertEquals(BlockReason.HELD_RESULTS, row(rows, "RAW-A").blockReason());
        assertEquals(Bucket.RETIRED, row(rows, "RAW-B").bucket());
        assertFalse(row(rows, "RAW-B").alsoDefault());
    }

    @Test
    public void aRecordTheOperatorAddedCarriesOverUnchanged() throws Exception {
        var from = profile(test("RAW-A", "94500-6", "POS", "NEG"));
        var to = profile(test("RAW-A", "94309-2", "POS", "NEG"));

        var row = row(plan(from, to,
                draft(bound("RAW-A", "t1", AnalyzerMappingOrigin.DEFAULT),
                        bound("VENDOR-NEW", "t7", AnalyzerMappingOrigin.OVERRIDE)),
                draft(bound("RAW-A", "t3", null))), "VENDOR-NEW");

        assertEquals(Bucket.UNCHANGED, row.bucket());
        assertEquals("t7", row.proposed().test().testId());
    }

    private static List<Row> plan(BridgeAnalyzerProfile from, BridgeAnalyzerProfile to, AnalyzerMappingDraft current,
            AnalyzerMappingDraft defaults) {
        return AnalyzerMappingAdoption.plan(from, to, current, defaults, Set.of(), Set.of());
    }

    private static Row row(List<Row> rows, String code) {
        return rows.stream().filter(row -> row.key().equals(AnalyzerMappingRowKey.main(code))).findFirst()
                .orElseThrow(() -> new AssertionError("no adoption row for " + code));
    }

    /** A record bound to a test, its two answers bound to the test's options. */
    private static AnalyzerMappingDraft bound(String code, String testId, AnalyzerMappingOrigin origin) {
        return new AnalyzerMappingDraft(
                List.of(new AnalyzerMappingTestDraft(code, AnalyzerMappingState.BOUND, testId, null, null, origin)),
                List.of(new AnalyzerMappingResultDraft(code, "POS", AnalyzerMappingState.BOUND, testId + "-pos", null,
                        origin),
                        new AnalyzerMappingResultDraft(code, "NEG", AnalyzerMappingState.BOUND, testId + "-neg", null,
                                origin)));
    }

    private static AnalyzerMappingDraft draft(AnalyzerMappingDraft... records) {
        List<AnalyzerMappingTestDraft> tests = new ArrayList<>();
        List<AnalyzerMappingResultDraft> results = new ArrayList<>();
        for (AnalyzerMappingDraft record : records) {
            tests.addAll(record.tests());
            results.addAll(record.results());
        }
        return new AnalyzerMappingDraft(tests, results);
    }

    private static String test(String code, String loinc, String... values) {
        StringBuilder codes = new StringBuilder();
        for (String value : values) {
            String answer = "POS".equals(value) ? POS : "NEG".equals(value) ? NEG : "LA9663-1";
            codes.append(codes.length() == 0 ? "" : ",").append("\"").append(value)
                    .append("\":[{\"system\":\"http://loinc.org\",\"code\":\"").append(answer).append("\"}]");
        }
        return "{\"test_code\":\"" + code + "\",\"loinc\":\"" + loinc + "\",\"result_type\":\"qualitative\","
                + "\"values\":[\"" + String.join("\",\"", values) + "\"],\"value_codes\":{" + codes + "}}";
    }

    private static BridgeAnalyzerProfile profile(String... tests) throws Exception {
        return BridgeAnalyzerProfile.from(new ObjectMapper().readTree("{\"profileMeta\":{\"id\":\"fixture.adoption\","
                + "\"displayName\":\"Adoption\"},\"catalog\":{\"revision\":1,\"revisionFingerprint\":\"sha256:"
                + "a".repeat(64) + "\",\"source\":\"SHIPPED\",\"status\":\"ACTIVE\"},\"protocol\":{\"name\":\"ASTM\"},"
                + "\"default_test_mappings\":[" + String.join(",", tests) + "]}"));
    }
}
