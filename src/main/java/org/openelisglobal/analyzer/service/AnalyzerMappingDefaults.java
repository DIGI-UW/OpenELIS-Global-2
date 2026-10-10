package org.openelisglobal.analyzer.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingMappingState;
import org.openelisglobal.testresult.service.TestResultService;
import org.springframework.stereotype.Service;

/**
 * Resolves a profile's defaults against the local catalog by exact
 * standard-code match, and says why when a row cannot be resolved. Test LOINC
 * selects the test; the answer code selects the answer. Never chooses between
 * two usable candidates.
 */
@Service
public class AnalyzerMappingDefaults {
    private final AnalyzerMappingCatalogService catalog;
    private final TestResultService testResults;

    public AnalyzerMappingDefaults(AnalyzerMappingCatalogService catalog, TestResultService testResults) {
        this.catalog = catalog;
        this.testResults = testResults;
    }

    public AnalyzerSiteBindingDraft resolve(BridgeAnalyzerProfile profile) {
        List<AnalyzerMappingCatalogService.TestOption> active = catalog.searchActiveTests(null);
        List<AnalyzerSiteBindingTestDraft> tests = new ArrayList<>();
        List<AnalyzerSiteBindingResultDraft> results = new ArrayList<>();
        for (var definition : profile.testDefinitions()) {
            TestResolution resolution = resolve(definition, active);
            tests.add(resolution.draft());
            for (String raw : definition.resultValues()) {
                results.add(resolution.options() == null
                        ? unresolvedAnswer(definition, raw, resolution.draft().unresolvedReason())
                        : resolveAnswer(definition, raw, resolution.options()));
            }
        }
        return new AnalyzerSiteBindingDraft(tests, results);
    }

    /** Resolves one profile test against the active catalog. */
    public AnalyzerSiteBindingTestDraft resolveTest(BridgeAnalyzerProfile.TestDefinition definition,
            List<AnalyzerMappingCatalogService.TestOption> activeTests) {
        return resolve(definition, activeTests).draft();
    }

    /**
     * Resolves one declared answer of a profile test against the active answers of
     * a test, which may be one the operator chose rather than a default.
     */
    public AnalyzerSiteBindingResultDraft resolveAnswer(BridgeAnalyzerProfile.TestDefinition definition,
            String rawValue, List<AnalyzerMappingCatalogService.ResultOption> options) {
        BridgeAnalyzerProfile.NormalizedCoding coding = definition.valueCodes().get(rawValue);
        if (coding == null) {
            return unresolvedAnswer(definition, rawValue, AnalyzerUnresolvedReason.NO_MATCH);
        }
        var matches = options.stream().filter(option -> sameCode(option.answerCode(), coding.code())).toList();
        if (matches.size() > 1) {
            return unresolvedAnswer(definition, rawValue, AnalyzerUnresolvedReason.AMBIGUOUS);
        }
        if (matches.isEmpty()) {
            return unresolvedAnswer(definition, rawValue, AnalyzerUnresolvedReason.NO_MATCH);
        }
        return new AnalyzerSiteBindingResultDraft(definition.analyzerCode(), rawValue,
                AnalyzerSiteBindingMappingState.BOUND, matches.get(0).id());
    }

    private TestResolution resolve(BridgeAnalyzerProfile.TestDefinition definition,
            List<AnalyzerMappingCatalogService.TestOption> activeTests) {
        String loinc = definition.loinc();
        var candidates = loinc == null ? List.<AnalyzerMappingCatalogService.TestOption>of()
                : activeTests.stream().filter(test -> test.loincCodes().contains(loinc)).toList();
        if (candidates.isEmpty()) {
            return unresolvedTest(definition, AnalyzerUnresolvedReason.NO_MATCH);
        }
        Map<AnalyzerMappingCatalogService.TestOption, List<AnalyzerMappingCatalogService.ResultOption>> usable = new LinkedHashMap<>();
        for (var candidate : candidates) {
            var options = catalog.getActiveResultOptions(candidate.id());
            if (canHold(definition, candidate, options)) {
                usable.put(candidate, options);
            }
        }
        if (usable.isEmpty()) {
            return unresolvedTest(definition, AnalyzerUnresolvedReason.INCOMPATIBLE);
        }
        if (usable.size() > 1) {
            return unresolvedTest(definition, AnalyzerUnresolvedReason.AMBIGUOUS);
        }
        var selected = usable.entrySet().iterator().next();
        return new TestResolution(new AnalyzerSiteBindingTestDraft(definition.analyzerCode(),
                AnalyzerSiteBindingMappingState.BOUND, selected.getKey().id()), selected.getValue());
    }

    private boolean canHold(BridgeAnalyzerProfile.TestDefinition definition,
            AnalyzerMappingCatalogService.TestOption test, List<AnalyzerMappingCatalogService.ResultOption> options) {
        boolean categorical = !definition.resultValues().isEmpty()
                || "qualitative".equalsIgnoreCase(definition.resultType());
        boolean numeric = "quantitative".equalsIgnoreCase(definition.resultType());
        if (categorical && options.isEmpty()) {
            return false;
        }
        return !numeric || testResults.getActiveTestResultsByTest(test.id()).stream()
                .anyMatch(option -> "N".equals(option.getTestResultType()));
    }

    private static TestResolution unresolvedTest(BridgeAnalyzerProfile.TestDefinition definition,
            AnalyzerUnresolvedReason reason) {
        return new TestResolution(new AnalyzerSiteBindingTestDraft(definition.analyzerCode(),
                AnalyzerSiteBindingMappingState.UNRESOLVED, null, reason), null);
    }

    private static AnalyzerSiteBindingResultDraft unresolvedAnswer(BridgeAnalyzerProfile.TestDefinition definition,
            String rawValue, AnalyzerUnresolvedReason reason) {
        return new AnalyzerSiteBindingResultDraft(definition.analyzerCode(), rawValue,
                AnalyzerSiteBindingMappingState.UNRESOLVED, null, reason);
    }

    /**
     * The profile names an answer by system and code; the dictionary stores the
     * code alone.
     */
    private static boolean sameCode(String local, String profile) {
        return local != null && profile != null && !local.isBlank() && local.trim().equalsIgnoreCase(profile.trim());
    }

    private record TestResolution(AnalyzerSiteBindingTestDraft draft,
            List<AnalyzerMappingCatalogService.ResultOption> options) {
    }
}
