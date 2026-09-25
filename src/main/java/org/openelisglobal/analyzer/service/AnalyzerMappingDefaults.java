package org.openelisglobal.analyzer.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingMappingState;
import org.openelisglobal.testresult.service.TestResultService;
import org.springframework.stereotype.Service;

/**
 * Resolves portable defaults without choosing between ambiguous local concepts.
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
            var matches = active.stream().filter(test -> test.loincCodes().contains(definition.loinc())).toList();
            // A matching display name alone is not enough to override conflicting LOINC.
            if (matches.size() > 1) {
                matches = matches.stream()
                        .filter(test -> same(test.code(), definition.analyzerCode())
                                || definition.aliases().stream().anyMatch(alias -> same(test.code(), alias))
                                || same(test.name(), definition.testNameHint()))
                        .toList();
            }
            var selected = matches.size() == 1 ? matches.get(0) : null;
            List<AnalyzerMappingCatalogService.ResultOption> options = selected == null ? List.of()
                    : catalog.getActiveResultOptions(selected.id());
            boolean categorical = !definition.resultValues().isEmpty()
                    || "qualitative".equalsIgnoreCase(definition.resultType());
            boolean numeric = "quantitative".equalsIgnoreCase(definition.resultType());
            if (selected != null && (categorical && options.isEmpty()
                    || numeric && testResults.getActiveTestResultsByTest(selected.id()).stream()
                            .noneMatch(option -> "N".equals(option.getTestResultType())))) {
                selected = null;
                options = List.of();
            }
            tests.add(new AnalyzerSiteBindingTestDraft(definition.analyzerCode(),
                    selected == null ? AnalyzerSiteBindingMappingState.UNRESOLVED
                            : AnalyzerSiteBindingMappingState.BOUND,
                    selected == null ? null : selected.id()));
            for (String raw : definition.resultValues()) {
                var answers = options.stream().filter(option -> same(raw, option.label())).toList();
                var answer = answers.size() == 1 ? answers.get(0) : null;
                results.add(new AnalyzerSiteBindingResultDraft(definition.analyzerCode(), raw,
                        answer == null ? AnalyzerSiteBindingMappingState.UNRESOLVED
                                : AnalyzerSiteBindingMappingState.BOUND,
                        answer == null ? null : answer.id()));
            }
        }
        return new AnalyzerSiteBindingDraft(tests, results);
    }

    private static boolean same(String left, String right) {
        return left != null && right != null && !left.isBlank() && !right.isBlank()
                && normalize(left).equals(normalize(right));
    }

    private static String normalize(String value) {
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
