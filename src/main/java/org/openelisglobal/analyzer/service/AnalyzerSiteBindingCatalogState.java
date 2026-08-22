package org.openelisglobal.analyzer.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingMappingState;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingResult;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingTest;

final class AnalyzerSiteBindingCatalogState {

    private final AnalyzerMappingCatalogService mappingCatalogService;
    private final Map<String, AnalyzerMappingCatalogService.TestOption> activeTests;
    private final Map<String, Set<String>> activeResultOptions = new HashMap<>();

    private AnalyzerSiteBindingCatalogState(AnalyzerMappingCatalogService mappingCatalogService) {
        this.mappingCatalogService = mappingCatalogService;
        this.activeTests = Optional.ofNullable(mappingCatalogService.searchActiveTests(null)).orElse(List.of()).stream()
                .collect(Collectors.toMap(AnalyzerMappingCatalogService.TestOption::id, Function.identity()));
    }

    static AnalyzerSiteBindingCatalogState load(AnalyzerMappingCatalogService mappingCatalogService) {
        return new AnalyzerSiteBindingCatalogState(mappingCatalogService);
    }

    Validation validate(AnalyzerSiteBindingSnapshot binding) {
        if (binding == null) {
            return Validation.empty();
        }

        Map<String, AnalyzerSiteBindingTest> testsBySource = binding.tests().stream()
                .collect(Collectors.toMap(row -> row.getId().getSourceRowKey(), Function.identity()));
        Set<String> currentTests = binding.tests().stream().filter(this::isCurrentTest)
                .map(row -> row.getId().getSourceRowKey()).collect(Collectors.toCollection(HashSet::new));
        Set<ResultSourceKey> currentResults = binding.results().stream()
                .filter(row -> isCurrentResult(row, testsBySource.get(row.getId().getSourceRowKey())))
                .map(row -> new ResultSourceKey(row.getId().getSourceRowKey(), row.getId().getRawValue()))
                .collect(Collectors.toCollection(HashSet::new));
        return new Validation(currentTests, currentResults, binding.tests().size(), binding.results().size());
    }

    private boolean isCurrentTest(AnalyzerSiteBindingTest row) {
        if (row.getMappingState() == AnalyzerSiteBindingMappingState.EXCLUDED) {
            return true;
        }
        return row.getMappingState() == AnalyzerSiteBindingMappingState.BOUND && row.getTestId() != null
                && activeTests.containsKey(row.getTestId());
    }

    private boolean isCurrentResult(AnalyzerSiteBindingResult result, AnalyzerSiteBindingTest test) {
        if (result.getMappingState() == AnalyzerSiteBindingMappingState.EXCLUDED) {
            return true;
        }
        if (result.getMappingState() != AnalyzerSiteBindingMappingState.BOUND || result.getTestResultId() == null
                || test == null || test.getMappingState() != AnalyzerSiteBindingMappingState.BOUND
                || !isCurrentTest(test)) {
            return false;
        }
        Set<String> optionIds = activeResultOptions.computeIfAbsent(test.getTestId(),
                testId -> Optional.ofNullable(mappingCatalogService.getActiveResultOptions(testId)).orElse(List.of())
                        .stream().map(AnalyzerMappingCatalogService.ResultOption::id).collect(Collectors.toSet()));
        return optionIds.contains(result.getTestResultId());
    }

    record ResultSourceKey(String sourceRowKey, String rawValue) {
    }

    record Validation(Set<String> currentTestRows, Set<ResultSourceKey> currentResultRows, int testRows,
            int resultRows) {

        Validation {
            currentTestRows = currentTestRows == null ? Set.of() : Set.copyOf(currentTestRows);
            currentResultRows = currentResultRows == null ? Set.of() : Set.copyOf(currentResultRows);
        }

        static Validation empty() {
            return new Validation(Set.of(), Set.of(), 0, 0);
        }

        boolean allRowsCurrent() {
            return currentTestRows.size() == testRows && currentResultRows.size() == resultRows;
        }

        boolean isCurrentTest(String sourceRowKey) {
            return currentTestRows.contains(sourceRowKey);
        }

        boolean isCurrentResult(String sourceRowKey, String rawValue) {
            return currentResultRows.contains(new ResultSourceKey(sourceRowKey, rawValue));
        }
    }
}
