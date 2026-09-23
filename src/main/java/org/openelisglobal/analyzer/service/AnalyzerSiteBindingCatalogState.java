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
        Set<String> currentBoundTests = binding.tests().stream()
                .filter(row -> row.getMappingState() == AnalyzerSiteBindingMappingState.BOUND)
                .filter(this::isCurrentTest).map(row -> row.getId().getSourceRowKey())
                .collect(Collectors.toCollection(HashSet::new));
        Set<String> currentExcludedTests = binding.tests().stream()
                .filter(row -> row.getMappingState() == AnalyzerSiteBindingMappingState.EXCLUDED)
                .map(row -> row.getId().getSourceRowKey()).collect(Collectors.toCollection(HashSet::new));
        Set<ResultSourceKey> currentBoundResults = binding.results().stream()
                .filter(row -> row.getMappingState() == AnalyzerSiteBindingMappingState.BOUND)
                .filter(row -> isCurrentResult(row, testsBySource.get(row.getId().getSourceRowKey())))
                .map(row -> new ResultSourceKey(row.getId().getSourceRowKey(), row.getId().getRawValue()))
                .collect(Collectors.toCollection(HashSet::new));
        Set<ResultSourceKey> currentExcludedResults = binding.results().stream()
                .filter(row -> row.getMappingState() == AnalyzerSiteBindingMappingState.EXCLUDED)
                .map(row -> new ResultSourceKey(row.getId().getSourceRowKey(), row.getId().getRawValue()))
                .collect(Collectors.toCollection(HashSet::new));
        return new Validation(currentBoundTests, currentExcludedTests, currentBoundResults, currentExcludedResults,
                binding.tests().size(), binding.results().size());
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

    record Validation(Set<String> currentBoundTestRows, Set<String> currentExcludedTestRows,
            Set<ResultSourceKey> currentBoundResultRows, Set<ResultSourceKey> currentExcludedResultRows, int testRows,
            int resultRows) {

        Validation {
            currentBoundTestRows = currentBoundTestRows == null ? Set.of() : Set.copyOf(currentBoundTestRows);
            currentExcludedTestRows = currentExcludedTestRows == null ? Set.of() : Set.copyOf(currentExcludedTestRows);
            currentBoundResultRows = currentBoundResultRows == null ? Set.of() : Set.copyOf(currentBoundResultRows);
            currentExcludedResultRows = currentExcludedResultRows == null ? Set.of()
                    : Set.copyOf(currentExcludedResultRows);
        }

        static Validation empty() {
            return new Validation(Set.of(), Set.of(), Set.of(), Set.of(), 0, 0);
        }

        boolean allRowsCurrent() {
            return currentBoundTestRows.size() + currentExcludedTestRows.size() == testRows
                    && currentBoundResultRows.size() + currentExcludedResultRows.size() == resultRows;
        }

        boolean isCurrentTest(String sourceRowKey) {
            return isCurrentBoundTest(sourceRowKey) || isCurrentExcludedTest(sourceRowKey);
        }

        boolean isCurrentBoundTest(String sourceRowKey) {
            return currentBoundTestRows.contains(sourceRowKey);
        }

        boolean isCurrentExcludedTest(String sourceRowKey) {
            return currentExcludedTestRows.contains(sourceRowKey);
        }

        boolean isCurrentResult(String sourceRowKey, String rawValue) {
            return isCurrentBoundResult(sourceRowKey, rawValue) || isCurrentExcludedResult(sourceRowKey, rawValue);
        }

        boolean isCurrentBoundResult(String sourceRowKey, String rawValue) {
            return currentBoundResultRows.contains(new ResultSourceKey(sourceRowKey, rawValue));
        }

        boolean isCurrentExcludedResult(String sourceRowKey, String rawValue) {
            return currentExcludedResultRows.contains(new ResultSourceKey(sourceRowKey, rawValue));
        }
    }
}
