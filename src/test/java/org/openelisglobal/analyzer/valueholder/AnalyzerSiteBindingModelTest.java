package org.openelisglobal.analyzer.valueholder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;

import java.util.Arrays;
import org.junit.Test;

public class AnalyzerSiteBindingModelTest {

    @Test
    public void independentSourceRowsRemainDistinctWhenTheyShareOneLocalTest() {
        AnalyzerSiteBindingRevision revision = revision();
        AnalyzerSiteBindingTest first = testDecision(revision, "wbc-primary", "9701");
        AnalyzerSiteBindingTest alias = testDecision(revision, "wbc-alias", "9701");

        assertNotEquals(first.getId(), alias.getId());
        assertEquals("9701", first.getTestId());
        assertEquals("9701", alias.getTestId());
        assertSame(revision, first.getSiteBindingRevision());
        assertSame(revision, alias.getSiteBindingRevision());
    }

    @Test
    public void resultRowsUsePortableRowIdentityWithoutCopyingPortableProfileContent() {
        AnalyzerSiteBindingRevision revision = revision();
        AnalyzerSiteBindingResult positive = resultDecision(revision, "hiv-interpretation", "POS", "811");
        AnalyzerSiteBindingResult negative = resultDecision(revision, "hiv-interpretation", "NEG", "812");

        assertNotEquals(positive.getId(), negative.getId());
        assertEquals("811", positive.getTestResultId());
        assertEquals("812", negative.getTestResultId());

        for (Class<?> type : Arrays.asList(AnalyzerSiteBinding.class, AnalyzerSiteBindingRevision.class,
                AnalyzerSiteBindingTest.class, AnalyzerSiteBindingResult.class)) {
            assertFalse(Arrays.stream(type.getDeclaredFields()).map(field -> field.getName().toLowerCase())
                    .anyMatch(name -> name.contains("json") || name.contains("snapshot") || name.contains("payload")
                            || name.contains("analyzercode") || name.contains("displayname")
                            || name.contains("normalizedcoding")));
        }
    }

    @Test
    public void analyzerPinsTheExactLocalBindingRevision() {
        AnalyzerSiteBindingRevision revision = revision();
        Analyzer analyzer = new Analyzer();

        analyzer.setSiteBindingRevision(revision);

        assertSame(revision, analyzer.getSiteBindingRevision());
        assertEquals("site.mock-hematology", analyzer.getPinnedProfileBinding().getProfileId());
        assertEquals(3, analyzer.getPinnedProfileBinding().getProfileRevision());
    }

    private static AnalyzerSiteBindingRevision revision() {
        AnalyzerProfileBinding profile = new AnalyzerProfileBinding();
        profile.setId("41");
        profile.setProfileId("site.mock-hematology");
        profile.setProfileRevision(3);

        AnalyzerSiteBinding binding = new AnalyzerSiteBinding();
        binding.setId("51");
        binding.setProfileBinding(profile);

        AnalyzerSiteBindingRevision revision = new AnalyzerSiteBindingRevision();
        revision.setId("61");
        revision.setSiteBinding(binding);
        revision.setRevisionNumber(1);
        return revision;
    }

    private static AnalyzerSiteBindingTest testDecision(AnalyzerSiteBindingRevision revision, String sourceRowKey,
            String testId) {
        AnalyzerSiteBindingTest row = new AnalyzerSiteBindingTest();
        row.setId(new AnalyzerSiteBindingTestPK(revision.getId(), sourceRowKey));
        row.setSiteBindingRevision(revision);
        row.setMappingState(AnalyzerSiteBindingMappingState.BOUND);
        row.setTestId(testId);
        return row;
    }

    private static AnalyzerSiteBindingResult resultDecision(AnalyzerSiteBindingRevision revision, String sourceRowKey,
            String rawValue, String testResultId) {
        AnalyzerSiteBindingResult row = new AnalyzerSiteBindingResult();
        row.setId(new AnalyzerSiteBindingResultPK(revision.getId(), sourceRowKey, rawValue));
        row.setSiteBindingRevision(revision);
        row.setMappingState(AnalyzerSiteBindingMappingState.BOUND);
        row.setTestResultId(testResultId);
        return row;
    }
}
