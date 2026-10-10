package org.openelisglobal.analyzer.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingResult;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingState;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingTest;

public final class AnalyzerMappingCatalogState {

    private final AnalyzerMappingCatalogService mappingCatalogService;
    private final Map<String, AnalyzerMappingCatalogService.TestOption> activeTests;
    private final Map<String, Set<String>> activeResultOptions = new HashMap<>();

    private AnalyzerMappingCatalogState(AnalyzerMappingCatalogService mappingCatalogService) {
        this.mappingCatalogService = mappingCatalogService;
        this.activeTests = Optional.ofNullable(mappingCatalogService.searchActiveTests(null)).orElse(List.of()).stream()
                .collect(Collectors.toMap(AnalyzerMappingCatalogService.TestOption::id, Function.identity()));
    }

    public static AnalyzerMappingCatalogState load(AnalyzerMappingCatalogService mappingCatalogService) {
        return new AnalyzerMappingCatalogState(mappingCatalogService);
    }

    public Validation validate(AnalyzerMappingSnapshot binding) {
        if (binding == null) {
            return Validation.empty();
        }

        Map<AnalyzerMappingRowKey, AnalyzerMappingTest> testsBySource = binding.tests().stream()
                .collect(Collectors.toMap(AnalyzerMappingRowKey::of, Function.identity()));
        Set<AnalyzerMappingRowKey> currentBoundTests = binding.tests().stream()
                .filter(row -> row.getMappingState() == AnalyzerMappingState.BOUND).filter(this::isCurrentTest)
                .map(AnalyzerMappingRowKey::of).collect(Collectors.toCollection(HashSet::new));
        Set<AnalyzerMappingRowKey> currentExcludedTests = binding.tests().stream()
                .filter(row -> row.getMappingState() == AnalyzerMappingState.EXCLUDED).map(AnalyzerMappingRowKey::of)
                .collect(Collectors.toCollection(HashSet::new));
        Set<ResultSourceKey> currentBoundResults = binding.results().stream()
                .filter(row -> row.getMappingState() == AnalyzerMappingState.BOUND)
                .filter(row -> isCurrentResult(row, testsBySource.get(AnalyzerMappingRowKey.of(row))))
                .map(ResultSourceKey::of).collect(Collectors.toCollection(HashSet::new));
        Set<ResultSourceKey> currentExcludedResults = binding.results().stream()
                .filter(row -> row.getMappingState() == AnalyzerMappingState.EXCLUDED).map(ResultSourceKey::of)
                .collect(Collectors.toCollection(HashSet::new));
        return new Validation(currentBoundTests, currentExcludedTests, currentBoundResults, currentExcludedResults,
                binding.tests().size(), binding.results().size());
    }

    private boolean isCurrentTest(AnalyzerMappingTest row) {
        if (row.getMappingState() == AnalyzerMappingState.EXCLUDED) {
            return true;
        }
        return row.getMappingState() == AnalyzerMappingState.BOUND && row.getTestId() != null
                && activeTests.containsKey(row.getTestId());
    }

    private boolean isCurrentResult(AnalyzerMappingResult result, AnalyzerMappingTest test) {
        if (result.getMappingState() == AnalyzerMappingState.EXCLUDED) {
            return true;
        }
        if (result.getMappingState() != AnalyzerMappingState.BOUND || result.getTestResultId() == null || test == null
                || test.getMappingState() != AnalyzerMappingState.BOUND || !isCurrentTest(test)) {
            return false;
        }
        Set<String> optionIds = activeResultOptions.computeIfAbsent(test.getTestId(),
                testId -> Optional.ofNullable(mappingCatalogService.getActiveResultOptions(testId)).orElse(List.of())
                        .stream().map(AnalyzerMappingCatalogService.ResultOption::id).collect(Collectors.toSet()));
        return optionIds.contains(result.getTestResultId());
    }

    /** One answer of one record: code, raw value and the record's sub-identity. */
    public record ResultSourceKey(String sourceRowKey, String rawValue, String subIdentity) {

        public ResultSourceKey {
            subIdentity = subIdentity == null ? "" : subIdentity;
        }

        public ResultSourceKey(String sourceRowKey, String rawValue) {
            this(sourceRowKey, rawValue, "");
        }

        public ResultSourceKey(AnalyzerMappingRowKey record, String rawValue) {
            this(record.sourceRowKey(), rawValue, record.subIdentity());
        }

        static ResultSourceKey of(AnalyzerMappingResult row) {
            return new ResultSourceKey(row.getId().getSourceRowKey(), row.getId().getRawValue(),
                    row.getId().getSubIdentity());
        }
    }

    public record Validation(Set<AnalyzerMappingRowKey> currentBoundTestRows,
            Set<AnalyzerMappingRowKey> currentExcludedTestRows, Set<ResultSourceKey> currentBoundResultRows,
            Set<ResultSourceKey> currentExcludedResultRows, int testRows, int resultRows) {

        public Validation {
            currentBoundTestRows = currentBoundTestRows == null ? Set.of() : Set.copyOf(currentBoundTestRows);
            currentExcludedTestRows = currentExcludedTestRows == null ? Set.of() : Set.copyOf(currentExcludedTestRows);
            currentBoundResultRows = currentBoundResultRows == null ? Set.of() : Set.copyOf(currentBoundResultRows);
            currentExcludedResultRows = currentExcludedResultRows == null ? Set.of()
                    : Set.copyOf(currentExcludedResultRows);
        }

        static Validation empty() {
            return new Validation(Set.of(), Set.of(), Set.of(), Set.of(), 0, 0);
        }

        boolean isCurrentTest(AnalyzerMappingRowKey record) {
            return isCurrentBoundTest(record) || currentExcludedTestRows.contains(record);
        }

        public boolean isCurrentBoundTest(AnalyzerMappingRowKey record) {
            return currentBoundTestRows.contains(record);
        }

        /** The main result of a code. */
        public boolean isCurrentBoundTest(String sourceRowKey) {
            return isCurrentBoundTest(AnalyzerMappingRowKey.main(sourceRowKey));
        }

        boolean isCurrentResult(AnalyzerMappingRowKey record, String rawValue) {
            ResultSourceKey key = new ResultSourceKey(record, rawValue);
            return currentBoundResultRows.contains(key) || currentExcludedResultRows.contains(key);
        }

        public boolean isCurrentBoundResult(AnalyzerMappingRowKey record, String rawValue) {
            return currentBoundResultRows.contains(new ResultSourceKey(record, rawValue));
        }

        /** An answer of the main result of a code. */
        public boolean isCurrentBoundResult(String sourceRowKey, String rawValue) {
            return isCurrentBoundResult(AnalyzerMappingRowKey.main(sourceRowKey), rawValue);
        }
    }
}
