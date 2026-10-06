package org.openelisglobal.analyzer.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingOrigin;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingState;

/**
 * Rule 7: what adopting a newer revision of an analyzer's profile does to each
 * record of its mapping. A row keeps its decision when nothing about it
 * changed; a default row whose new default differs takes the new default; an
 * override whose record the new revision changed stays beside the new default
 * for the operator to decide. A code the new revision adds needs mapping, a
 * code it drops is retired, and a record the operator added that no revision
 * declares carries over.
 */
public final class AnalyzerMappingAdoption {

    public enum Bucket {
        UNCHANGED, CHANGED, NEEDS_MAPPING, BLOCKED, RETIRED
    }

    public enum BlockReason {
        /** An override targets a test that is no longer active. */
        INACTIVE_TEST,
        /** A code the new revision drops still has held results. */
        HELD_RESULTS
    }

    /** One record's decision: its test row and its answer rows. */
    public record Decision(AnalyzerMappingTestDraft test, List<AnalyzerMappingResultDraft> results) {
    }

    /**
     * One record's fate. {@code proposed} is what adoption saves unless the
     * operator changes it; it is null for a retired or blocked removed record.
     * {@code alsoDefault} marks an override that the new revision now also
     * proposes.
     */
    public record Row(AnalyzerMappingRowKey key, Bucket bucket, Decision current, Decision newDefault,
            Decision proposed, boolean alsoDefault, BlockReason blockReason) {
    }

    private AnalyzerMappingAdoption() {
    }

    public static List<Row> plan(BridgeAnalyzerProfile from, BridgeAnalyzerProfile to, AnalyzerMappingDraft current,
            AnalyzerMappingDraft newDefaults, Set<String> inactiveTestIds,
            Set<AnalyzerMappingRowKey> recordsWithHeldResults) {
        Map<AnalyzerMappingRowKey, List<Object>> before = definitions(from);
        Map<AnalyzerMappingRowKey, List<Object>> after = definitions(to);
        Map<AnalyzerMappingRowKey, Decision> decisions = decisions(current);
        Map<AnalyzerMappingRowKey, Decision> defaults = decisions(newDefaults);

        Set<AnalyzerMappingRowKey> keys = new LinkedHashSet<>(after.keySet());
        keys.addAll(decisions.keySet());
        List<Row> rows = new ArrayList<>();
        for (AnalyzerMappingRowKey key : keys) {
            Decision decision = decisions.get(key);
            Decision newDefault = defaults.get(key);
            if (before.containsKey(key) && !after.containsKey(key)) {
                boolean held = recordsWithHeldResults.contains(key);
                rows.add(new Row(key, held ? Bucket.BLOCKED : Bucket.RETIRED, decision, null, null, false,
                        held ? BlockReason.HELD_RESULTS : null));
            } else if (decision == null) {
                rows.add(new Row(key, Bucket.NEEDS_MAPPING, null, newDefault, newDefault, false, null));
            } else if (isOverride(decision) && decision.test().mappingState() == AnalyzerMappingState.BOUND
                    && inactiveTestIds.contains(decision.test().testId())) {
                rows.add(
                        new Row(key, Bucket.BLOCKED, decision, newDefault, decision, false, BlockReason.INACTIVE_TEST));
            } else if (newDefault != null && sameDecision(decision, newDefault)) {
                rows.add(new Row(key, Bucket.UNCHANGED, decision, newDefault, decision, isOverride(decision), null));
            } else if (!isOverride(decision) && after.containsKey(key)) {
                rows.add(new Row(key, Bucket.CHANGED, decision, newDefault, newDefault, false, null));
            } else if (isOverride(decision) && !Objects.equals(before.get(key), after.get(key))) {
                rows.add(new Row(key, Bucket.CHANGED, decision, newDefault, decision, false, null));
            } else {
                rows.add(new Row(key, Bucket.UNCHANGED, decision, newDefault, decision, false, null));
            }
        }
        return rows;
    }

    private static boolean isOverride(Decision decision) {
        return decision.test().origin() == AnalyzerMappingOrigin.OVERRIDE;
    }

    /** Two decisions agree on the record's target and on every answer. */
    private static boolean sameDecision(Decision a, Decision b) {
        AnalyzerMappingTestDraft x = a.test();
        AnalyzerMappingTestDraft y = b.test();
        return x.mappingState() == y.mappingState() && Objects.equals(x.testId(), y.testId())
                && Objects.equals(x.componentId(), y.componentId())
                && Objects.equals(x.callComponentId(), y.callComponentId()) && answers(a).equals(answers(b));
    }

    private static Map<String, List<Object>> answers(Decision decision) {
        Map<String, List<Object>> answers = new LinkedHashMap<>();
        decision.results().forEach(result -> answers.put(result.rawValue(),
                List.of(result.mappingState(), Objects.toString(result.testResultId(), ""))));
        return answers;
    }

    private static Map<AnalyzerMappingRowKey, Decision> decisions(AnalyzerMappingDraft draft) {
        Map<AnalyzerMappingRowKey, Decision> decisions = new LinkedHashMap<>();
        for (AnalyzerMappingTestDraft test : draft.tests()) {
            List<AnalyzerMappingResultDraft> results = draft.results().stream()
                    .filter(result -> result.rowKey().equals(test.rowKey())).toList();
            decisions.put(test.rowKey(), new Decision(test, results));
        }
        return decisions;
    }

    /**
     * What the profile says about each record, compared field by field across
     * revisions.
     */
    private static Map<AnalyzerMappingRowKey, List<Object>> definitions(BridgeAnalyzerProfile profile) {
        Map<AnalyzerMappingRowKey, List<Object>> definitions = new LinkedHashMap<>();
        for (var test : profile.testDefinitions()) {
            definitions.put(AnalyzerMappingRowKey.main(test.analyzerCode()),
                    List.of(Objects.toString(test.loinc(), ""), Objects.toString(test.unit(), ""),
                            Objects.toString(test.resultType(), ""), test.resultValues(), test.valueCodes(),
                            test.translations(), Objects.toString(test.callComponent(), "")));
            for (var component : test.recordComponents()) {
                definitions.put(new AnalyzerMappingRowKey(test.analyzerCode(), component.subIdentity()),
                        List.of(component.code(), Objects.toString(component.unit(), ""),
                                Objects.toString(component.resultType(), ""), component.resultValues(),
                                component.valueCodes(), component.translations()));
            }
        }
        return definitions;
    }
}
