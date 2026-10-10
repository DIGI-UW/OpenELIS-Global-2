package org.openelisglobal.analyzer.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingState;

public record AnalyzerMappingDraft(List<AnalyzerMappingTestDraft> tests, List<AnalyzerMappingResultDraft> results) {

    public AnalyzerMappingDraft {
        tests = tests == null ? List.of() : List.copyOf(tests);
        results = results == null ? List.of() : List.copyOf(results);
    }

    /** The decisions a saved revision holds, as a draft. */
    /**
     * Refuses a bound record left without the component it lands on: a part's own
     * component, or the call component the profile declares for a main record
     * (checked only when the profile is given). Import stages a record with no
     * target on the test's main result.
     */
    public void requireComponentTargets(BridgeAnalyzerProfile profile) {
        Set<String> callCodes = profile == null ? Set.of()
                : profile.testDefinitions().stream().filter(definition -> definition.callComponent() != null)
                        .map(BridgeAnalyzerProfile.TestDefinition::analyzerCode).collect(Collectors.toSet());
        for (AnalyzerMappingTestDraft row : tests) {
            if (row.mappingState() != AnalyzerMappingState.BOUND) {
                continue;
            }
            boolean missing = row.subIdentity().isEmpty()
                    ? callCodes.contains(row.sourceRowKey()) && isBlank(row.callComponentId())
                    : isBlank(row.componentId());
            if (missing) {
                String record = row.rowKey().label();
                throw new AnalyzerRequestException("analyzer.mapping.error.componentRequired",
                        Map.<String, Object>of("record", record),
                        "BOUND test row " + record + " must name the component it lands on");
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static AnalyzerMappingDraft of(AnalyzerMappingSnapshot snapshot) {
        return new AnalyzerMappingDraft(
                snapshot.tests().stream()
                        .map(row -> new AnalyzerMappingTestDraft(row.getId().getSourceRowKey(), row.getMappingState(),
                                row.getTestId(), row.getComponentId(), row.getUnresolvedReason(), row.getOrigin(),
                                row.getId().getSubIdentity(), row.getCallComponentId(), row.isEnabled(),
                                row.getInstrumentCode()))
                        .toList(),
                snapshot.results().stream()
                        .map(row -> new AnalyzerMappingResultDraft(row.getId().getSourceRowKey(),
                                row.getId().getRawValue(), row.getMappingState(), row.getTestResultId(),
                                row.getUnresolvedReason(), row.getOrigin(), row.getId().getSubIdentity()))
                        .toList());
    }
}
