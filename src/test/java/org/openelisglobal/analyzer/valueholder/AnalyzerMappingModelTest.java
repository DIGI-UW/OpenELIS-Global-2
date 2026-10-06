package org.openelisglobal.analyzer.valueholder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.Arrays;
import org.junit.Test;

public class AnalyzerMappingModelTest {

    @Test
    public void independentSourceRowsRemainDistinctWhenTheyShareOneLocalTest() {
        AnalyzerMapping revision = revision();
        AnalyzerMappingTest first = testDecision(revision, "wbc-primary", "9701");
        AnalyzerMappingTest alias = testDecision(revision, "wbc-alias", "9701");

        assertNotEquals(first.getId(), alias.getId());
        assertEquals("9701", first.getTestId());
        assertEquals("9701", alias.getTestId());
        assertSame(revision, first.getMapping());
        assertSame(revision, alias.getMapping());
    }

    @Test
    public void aTestRowCanTargetOneComponentOfALocalTest() {
        AnalyzerMappingTest row = testDecision(revision(), "bp", "9701");

        row.setComponentId("comp-systolic");

        assertEquals("9701", row.getTestId());
        assertEquals("comp-systolic", row.getComponentId());
    }

    @Test
    public void rowsAreDefaultsUntilTheOperatorOverridesThem() {
        AnalyzerMapping revision = revision();
        AnalyzerMappingTest test = testDecision(revision, "wbc", "9701");
        AnalyzerMappingResult result = resultDecision(revision, "hiv", "POS", "811");

        assertEquals(AnalyzerMappingOrigin.DEFAULT, test.getOrigin());
        assertEquals(AnalyzerMappingOrigin.DEFAULT, result.getOrigin());
        test.setOrigin(AnalyzerMappingOrigin.OVERRIDE);
        assertEquals(AnalyzerMappingOrigin.OVERRIDE, test.getOrigin());
    }

    @Test
    public void resultRowsUsePortableRowIdentityWithoutCopyingPortableProfileContent() {
        AnalyzerMapping revision = revision();
        AnalyzerMappingResult positive = resultDecision(revision, "hiv-interpretation", "POS", "811");
        AnalyzerMappingResult negative = resultDecision(revision, "hiv-interpretation", "NEG", "812");

        assertNotEquals(positive.getId(), negative.getId());
        assertEquals("811", positive.getTestResultId());
        assertEquals("812", negative.getTestResultId());

        for (Class<?> type : Arrays.asList(AnalyzerMapping.class, AnalyzerMappingTest.class,
                AnalyzerMappingResult.class)) {
            assertFalse(Arrays.stream(type.getDeclaredFields()).map(field -> field.getName().toLowerCase())
                    .anyMatch(name -> name.contains("json") || name.contains("snapshot") || name.contains("payload")
                            || name.contains("analyzercode") || name.contains("displayname")
                            || name.contains("normalizedcoding")));
        }
    }

    @Test
    public void analyzerPinsTheProfileRevisionOfTheMappingInForce() {
        AnalyzerMapping revision = revision();
        Analyzer analyzer = new Analyzer();

        analyzer.setMapping(revision);

        assertSame(revision, analyzer.getMapping());
        assertEquals("site.mock-hematology", analyzer.getPinnedProfile().getProfileId());
        assertEquals(3, analyzer.getPinnedProfile().getProfileRevision());
        assertEquals("sha256:" + "a".repeat(64), analyzer.getPinnedProfile().getProfileFingerprint());
    }

    @Test
    public void anAnalyzerWithNoMappingHasNoPinnedProfile() {
        assertNull(new Analyzer().getPinnedProfile());
    }

    private static AnalyzerMapping revision() {
        AnalyzerMapping revision = new AnalyzerMapping();
        revision.setId("61");
        revision.setRevisionNumber(1);
        revision.setProfileId("site.mock-hematology");
        revision.setProfileRevision(3);
        revision.setProfileFingerprint("sha256:" + "a".repeat(64));
        return revision;
    }

    private static AnalyzerMappingTest testDecision(AnalyzerMapping revision, String sourceRowKey, String testId) {
        AnalyzerMappingTest row = new AnalyzerMappingTest();
        row.setId(new AnalyzerMappingTestPK(revision.getId(), sourceRowKey));
        row.setMapping(revision);
        row.setMappingState(AnalyzerMappingState.BOUND);
        row.setTestId(testId);
        return row;
    }

    private static AnalyzerMappingResult resultDecision(AnalyzerMapping revision, String sourceRowKey, String rawValue,
            String testResultId) {
        AnalyzerMappingResult row = new AnalyzerMappingResult();
        row.setId(new AnalyzerMappingResultPK(revision.getId(), sourceRowKey, rawValue));
        row.setMapping(revision);
        row.setMappingState(AnalyzerMappingState.BOUND);
        row.setTestResultId(testResultId);
        return row;
    }
}
