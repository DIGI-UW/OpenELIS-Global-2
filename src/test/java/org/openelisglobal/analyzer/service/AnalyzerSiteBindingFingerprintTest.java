package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingMappingState;

public class AnalyzerSiteBindingFingerprintTest {

    @Test
    public void canonicalFingerprintIgnoresInputOrderingButPreservesIndependentRows() {
        AnalyzerSiteBindingDraft first = new AnalyzerSiteBindingDraft(
                List.of(test("wbc-alias", AnalyzerSiteBindingMappingState.UNRESOLVED, null),
                        test("wbc-primary", AnalyzerSiteBindingMappingState.BOUND, "9701")),
                List.of(result("hiv-interpretation", "POS", AnalyzerSiteBindingMappingState.BOUND, "811"),
                        result("hiv-interpretation", "NEG", AnalyzerSiteBindingMappingState.BOUND, "812")));
        AnalyzerSiteBindingDraft reordered = new AnalyzerSiteBindingDraft(
                List.of(test("wbc-primary", AnalyzerSiteBindingMappingState.BOUND, "9701"),
                        test("wbc-alias", AnalyzerSiteBindingMappingState.UNRESOLVED, null)),
                List.of(result("hiv-interpretation", "NEG", AnalyzerSiteBindingMappingState.BOUND, "812"),
                        result("hiv-interpretation", "POS", AnalyzerSiteBindingMappingState.BOUND, "811")));
        AnalyzerSiteBindingDraft collapsedAlias = new AnalyzerSiteBindingDraft(
                List.of(test("wbc-primary", AnalyzerSiteBindingMappingState.BOUND, "9701")), first.results());

        assertEquals(AnalyzerSiteBindingFingerprint.calculate(first),
                AnalyzerSiteBindingFingerprint.calculate(reordered));
        assertNotEquals(AnalyzerSiteBindingFingerprint.calculate(first),
                AnalyzerSiteBindingFingerprint.calculate(collapsedAlias));
    }

    @Test
    public void canonicalFingerprintChangesWhenALocalDecisionChanges() {
        AnalyzerSiteBindingDraft first = new AnalyzerSiteBindingDraft(
                List.of(test("wbc-primary", AnalyzerSiteBindingMappingState.BOUND, "9701")),
                List.of(result("hiv-interpretation", "POS", AnalyzerSiteBindingMappingState.BOUND, "811")));
        AnalyzerSiteBindingDraft changedTest = new AnalyzerSiteBindingDraft(
                List.of(test("wbc-primary", AnalyzerSiteBindingMappingState.BOUND, "9702")), first.results());
        AnalyzerSiteBindingDraft changedResult = new AnalyzerSiteBindingDraft(first.tests(),
                List.of(result("hiv-interpretation", "POS", AnalyzerSiteBindingMappingState.BOUND, "812")));

        assertNotEquals(AnalyzerSiteBindingFingerprint.calculate(first),
                AnalyzerSiteBindingFingerprint.calculate(changedTest));
        assertNotEquals(AnalyzerSiteBindingFingerprint.calculate(first),
                AnalyzerSiteBindingFingerprint.calculate(changedResult));
    }

    private static AnalyzerSiteBindingTestDraft test(String sourceRowKey, AnalyzerSiteBindingMappingState state,
            String testId) {
        return new AnalyzerSiteBindingTestDraft(sourceRowKey, state, testId);
    }

    private static AnalyzerSiteBindingResultDraft result(String sourceRowKey, String rawValue,
            AnalyzerSiteBindingMappingState state, String testResultId) {
        return new AnalyzerSiteBindingResultDraft(sourceRowKey, rawValue, state, testResultId);
    }
}
