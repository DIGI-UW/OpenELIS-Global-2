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
    private final Map<String, Map<String, AnalyzerMappingCatalogService.ResultOption>> activeResultOptions = new HashMap<>();
    private final Map<String, List<AnalyzerMappingCatalogService.ComponentOption>> activeComponents = new HashMap<>();

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
                binding.tests().size(), binding.results().size(), activeTests, activeComponents, activeResultOptions);
    }

    private boolean isCurrentTest(AnalyzerMappingTest row) {
        if (row.getMappingState() == AnalyzerMappingState.EXCLUDED) {
            return true;
        }
        return row.getMappingState() == AnalyzerMappingState.BOUND && row.getTestId() != null
                && activeTests.containsKey(row.getTestId()) && hasComponent(row.getTestId(), row.getComponentId())
                && hasComponent(row.getTestId(), row.getCallComponentId());
    }

    private List<AnalyzerMappingCatalogService.ComponentOption> componentsOf(String testId) {
        return activeComponents.computeIfAbsent(testId, id -> List
                .copyOf(Optional.ofNullable(mappingCatalogService.getActiveComponents(id)).orElse(List.of())));
    }

    private boolean hasComponent(String testId, String componentId) {
        var components = componentsOf(testId);
        return componentId == null || components.stream().anyMatch(component -> componentId.equals(component.id()));
    }

    private boolean isCurrentResult(AnalyzerMappingResult result, AnalyzerMappingTest test) {
        if (result.getMappingState() == AnalyzerMappingState.EXCLUDED) {
            return true;
        }
        if (result.getMappingState() != AnalyzerMappingState.BOUND || result.getTestResultId() == null || test == null
                || test.getMappingState() != AnalyzerMappingState.BOUND || !isCurrentTest(test)) {
            return false;
        }
        AnalyzerMappingCatalogService.ResultOption option = activeResultOptions.computeIfAbsent(test.getTestId(),
                testId -> Optional.ofNullable(mappingCatalogService.getActiveResultOptions(testId)).orElse(List.of())
                        .stream().collect(Collectors.toMap(AnalyzerMappingCatalogService.ResultOption::id,
                                Function.identity(), (first, ignored) -> first)))
                .get(result.getTestResultId());
        if (option == null) {
            return false;
        }
        // The answer must belong to the component the record lands on.
        String target = AnalyzerMappingTestDraft.answerComponentOf(test.getId().getSubIdentity(), test.getComponentId(),
                test.getCallComponentId());
        if (target != null) {
            return target.equals(option.componentId());
        }
        return option.componentId() == null || componentsOf(test.getTestId()).stream()
                .anyMatch(component -> component.primary() && component.id().equals(option.componentId()));
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
            Set<ResultSourceKey> currentExcludedResultRows, int testRows, int resultRows,
            Map<String, AnalyzerMappingCatalogService.TestOption> activeTests,
            Map<String, List<AnalyzerMappingCatalogService.ComponentOption>> components,
            Map<String, Map<String, AnalyzerMappingCatalogService.ResultOption>> answers) {

        public Validation {
            activeTests = Map.copyOf(activeTests);
            components = Map.copyOf(components);
            answers = Map.copyOf(answers);
            currentBoundTestRows = currentBoundTestRows == null ? Set.of() : Set.copyOf(currentBoundTestRows);
            currentExcludedTestRows = currentExcludedTestRows == null ? Set.of() : Set.copyOf(currentExcludedTestRows);
            currentBoundResultRows = currentBoundResultRows == null ? Set.of() : Set.copyOf(currentBoundResultRows);
            currentExcludedResultRows = currentExcludedResultRows == null ? Set.of()
                    : Set.copyOf(currentExcludedResultRows);
        }

        static Validation empty() {
            return new Validation(Set.of(), Set.of(), Set.of(), Set.of(), 0, 0, Map.of(), Map.of(), Map.of());
        }

        boolean isCurrentTest(AnalyzerMappingRowKey record) {
            return isCurrentBoundTest(record) || currentExcludedTestRows.contains(record);
        }

        public boolean isCurrentBoundTest(AnalyzerMappingRowKey record) {
            return currentBoundTestRows.contains(record);
        }

        /**
         * Known delivered and local clinical codings must agree; uncoded vendor
         * identities use the approved mapping.
         */
        public boolean acceptsTestMeaning(String testId, List<org.openelisglobal.terminology.Coding> delivered) {
            var target = activeTests.get(testId);
            if (target == null)
                return false;
            return delivered.isEmpty() || target.codings().isEmpty()
                    || delivered.stream().anyMatch(code -> target.codings().stream().anyMatch(code::sameAs));
        }

        /**
         * Validate the delivered part against its local destination, without a Bridge
         * profile.
         */
        public boolean acceptsValue(String testId, String componentId, boolean numeric) {
            var active = components.getOrDefault(testId, List.of());
            var target = active.stream()
                    .filter(component -> componentId == null ? component.primary() : componentId.equals(component.id()))
                    .findFirst().orElse(null);
            if (target == null)
                return componentId == null;
            if (target.resultType() != null && !target.resultType().isBlank()
                    && numeric != "N".equals(target.resultType()))
                return false;
            return true;
        }

        public boolean acceptsAnswerMeaning(String testId, String answerId,
                List<org.openelisglobal.terminology.Coding> delivered) {
            var target = answers.getOrDefault(testId, Map.of()).get(answerId);
            return target != null && (delivered.isEmpty() || target.codings().isEmpty()
                    || delivered.stream().anyMatch(code -> target.codings().stream().anyMatch(code::sameAs)));
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
