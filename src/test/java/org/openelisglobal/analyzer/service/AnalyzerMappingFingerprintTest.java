package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingState;

public class AnalyzerMappingFingerprintTest {

    @Test
    public void canonicalFingerprintIgnoresInputOrderingButPreservesIndependentRows() {
        AnalyzerMappingDraft first = new AnalyzerMappingDraft(
                List.of(test("wbc-alias", AnalyzerMappingState.UNRESOLVED, null),
                        test("wbc-primary", AnalyzerMappingState.BOUND, "9701")),
                List.of(result("hiv-interpretation", "POS", AnalyzerMappingState.BOUND, "811"),
                        result("hiv-interpretation", "NEG", AnalyzerMappingState.BOUND, "812")));
        AnalyzerMappingDraft reordered = new AnalyzerMappingDraft(
                List.of(test("wbc-primary", AnalyzerMappingState.BOUND, "9701"),
                        test("wbc-alias", AnalyzerMappingState.UNRESOLVED, null)),
                List.of(result("hiv-interpretation", "NEG", AnalyzerMappingState.BOUND, "812"),
                        result("hiv-interpretation", "POS", AnalyzerMappingState.BOUND, "811")));
        AnalyzerMappingDraft collapsedAlias = new AnalyzerMappingDraft(
                List.of(test("wbc-primary", AnalyzerMappingState.BOUND, "9701")), first.results());

        assertEquals(AnalyzerMappingFingerprint.calculate(first), AnalyzerMappingFingerprint.calculate(reordered));
        assertNotEquals(AnalyzerMappingFingerprint.calculate(first),
                AnalyzerMappingFingerprint.calculate(collapsedAlias));
    }

    @Test
    public void canonicalFingerprintChangesWhenALocalDecisionChanges() {
        AnalyzerMappingDraft first = new AnalyzerMappingDraft(
                List.of(test("wbc-primary", AnalyzerMappingState.BOUND, "9701")),
                List.of(result("hiv-interpretation", "POS", AnalyzerMappingState.BOUND, "811")));
        AnalyzerMappingDraft changedTest = new AnalyzerMappingDraft(
                List.of(test("wbc-primary", AnalyzerMappingState.BOUND, "9702")), first.results());
        AnalyzerMappingDraft changedResult = new AnalyzerMappingDraft(first.tests(),
                List.of(result("hiv-interpretation", "POS", AnalyzerMappingState.BOUND, "812")));

        assertNotEquals(AnalyzerMappingFingerprint.calculate(first), AnalyzerMappingFingerprint.calculate(changedTest));
        assertNotEquals(AnalyzerMappingFingerprint.calculate(first),
                AnalyzerMappingFingerprint.calculate(changedResult));
    }

    @Test
    public void aRecordSubIdentityAndACallTargetEachChangeTheFingerprint() {
        AnalyzerMappingTestDraft main = test("HIVVL", AnalyzerMappingState.BOUND, "9701");
        AnalyzerMappingDraft plain = new AnalyzerMappingDraft(List.of(main), List.of());
        AnalyzerMappingDraft withCallTarget = new AnalyzerMappingDraft(List.of(new AnalyzerMappingTestDraft("HIVVL",
                AnalyzerMappingState.BOUND, "9701", null, null, null, "", "comp-call")), List.of());
        AnalyzerMappingDraft logRecord = new AnalyzerMappingDraft(List.of(new AnalyzerMappingTestDraft("HIVVL",
                AnalyzerMappingState.BOUND, "9701", null, null, null, "&LOG", null)), List.of());

        assertNotEquals(AnalyzerMappingFingerprint.calculate(plain),
                AnalyzerMappingFingerprint.calculate(withCallTarget));
        assertNotEquals(AnalyzerMappingFingerprint.calculate(plain), AnalyzerMappingFingerprint.calculate(logRecord));
    }

    @Test
    public void aMappingSavedBeforeAssaysCouldBeTurnedOffKeepsItsFingerprint() {
        AnalyzerMappingDraft saved = new AnalyzerMappingDraft(
                List.of(test("MTB-RIF", AnalyzerMappingState.BOUND, "9701"),
                        test("HIV-VL", AnalyzerMappingState.UNRESOLVED, null)),
                List.of(result("MTB-RIF", "DETECTED", AnalyzerMappingState.BOUND, "811")));

        assertEquals("sha256:4e6dc804327db0cbfa3b724e2ff68481022e7d1b1e6eb50d5defe6a52ebe00e6",
                AnalyzerMappingFingerprint.calculate(saved));
    }

    @Test
    public void turningAnAssayOffOrChangingItsInstrumentCodeChangesTheFingerprint() {
        AnalyzerMappingTestDraft main = test("MTB-RIF", AnalyzerMappingState.BOUND, "9701");
        String on = AnalyzerMappingFingerprint.calculate(new AnalyzerMappingDraft(List.of(main), List.of()));

        assertNotEquals(on, AnalyzerMappingFingerprint
                .calculate(new AnalyzerMappingDraft(List.of(main.withAssay(false, null)), List.of())));
        assertNotEquals(on, AnalyzerMappingFingerprint
                .calculate(new AnalyzerMappingDraft(List.of(main.withAssay(true, "MTB")), List.of())));
        assertEquals("an assay on under its profile code is the default", on, AnalyzerMappingFingerprint
                .calculate(new AnalyzerMappingDraft(List.of(main.withAssay(true, null)), List.of())));
    }

    private static AnalyzerMappingTestDraft test(String sourceRowKey, AnalyzerMappingState state, String testId) {
        return new AnalyzerMappingTestDraft(sourceRowKey, state, testId);
    }

    private static AnalyzerMappingResultDraft result(String sourceRowKey, String rawValue, AnalyzerMappingState state,
            String testResultId) {
        return new AnalyzerMappingResultDraft(sourceRowKey, rawValue, state, testResultId);
    }
}
