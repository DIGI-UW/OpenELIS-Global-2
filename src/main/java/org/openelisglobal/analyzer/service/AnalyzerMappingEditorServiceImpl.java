package org.openelisglobal.analyzer.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingOrigin;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingState;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnalyzerMappingEditorServiceImpl implements AnalyzerMappingEditorService {

    private final AnalyzerService analyzerService;
    private final BridgeProfileCatalogService bridgeProfileCatalogService;
    private final AnalyzerMappingService mappingService;
    private final AnalyzerMappingCatalogService mappingCatalogService;
    private final AnalyzerMappingConfirmationService confirmationService;
    private final AnalyzerResultsService analyzerResultsService;
    private final AnalyzerMappingDefaults mappingDefaults;

    public AnalyzerMappingEditorServiceImpl(AnalyzerService analyzerService,
            BridgeProfileCatalogService bridgeProfileCatalogService, AnalyzerMappingService mappingService,
            AnalyzerMappingCatalogService mappingCatalogService, AnalyzerMappingConfirmationService confirmationService,
            AnalyzerResultsService analyzerResultsService, AnalyzerMappingDefaults mappingDefaults) {
        this.analyzerService = analyzerService;
        this.bridgeProfileCatalogService = bridgeProfileCatalogService;
        this.mappingService = mappingService;
        this.mappingCatalogService = mappingCatalogService;
        this.confirmationService = confirmationService;
        this.analyzerResultsService = analyzerResultsService;
        this.mappingDefaults = mappingDefaults;
    }

    @Override
    public AnalyzerMappingView getMapping(String analyzerId) {
        Analyzer analyzer = find(analyzerId);
        AnalyzerMappingSnapshot latest = latest(analyzer);
        BridgeProfileCatalog.ProfileRevision revision = bridgeProfileCatalogService
                .getProfile(latest.mapping().getProfileId(), latest.mapping().getProfileRevision());
        BridgeAnalyzerProfile profile = BridgeAnalyzerProfile.from(revision.profile());
        return compose(analyzer, revision, profile, CurrentRows.of(latest),
                analyzerResultsService.findHeldMappingResultsByAnalyzer(analyzer.getId()), latest);
    }

    @Override
    @Transactional
    public AnalyzerMappingView saveMapping(String analyzerId, AnalyzerMappingUpdate update, String actor) {
        if (update == null) {
            throw new IllegalArgumentException("Mapping update is required");
        }
        // Saves of one analyzer's mapping run one after another, so a save made
        // from a revision that another save replaced is refused as stale.
        analyzerService.findByIdForUpdate(analyzerId)
                .orElseThrow(() -> new IllegalArgumentException("Analyzer not found: " + analyzerId));
        Analyzer analyzer = find(analyzerId);
        AnalyzerMappingSnapshot current = latest(analyzer);
        BridgeProfileCatalog.ProfileRevision revision = bridgeProfileCatalogService
                .getProfile(current.mapping().getProfileId(), current.mapping().getProfileRevision());
        BridgeAnalyzerProfile profile = BridgeAnalyzerProfile.from(revision.profile());
        validateLoadedFingerprint(current, update.baseMappingFingerprint());
        List<AnalyzerResults> observed = analyzerResultsService.findHeldMappingResultsByAnalyzer(analyzer.getId());
        AnalyzerMappingDraft draft = withOrigins(current, validateUpdate(profile, current, observed, update));
        AnalyzerMappingSnapshot saved = mappingService.appendRevision(analyzer, draft, actor);
        return compose(analyzer, revision, profile, CurrentRows.of(saved), observed, saved);
    }

    @Override
    @Transactional
    public AnalyzerMappingConfirmationView confirmMapping(String analyzerId, AnalyzerMappingConfirmationRequest request,
            String actor) {
        Analyzer analyzer = find(analyzerId);
        AnalyzerMappingSnapshot candidate = latest(analyzer);
        BridgeProfileCatalog.ProfileRevision revision = bridgeProfileCatalogService
                .getProfile(candidate.mapping().getProfileId(), candidate.mapping().getProfileRevision());
        BridgeAnalyzerProfile profile = BridgeAnalyzerProfile.from(revision.profile());
        validateConfirmable(compose(analyzer, revision, profile, CurrentRows.of(candidate),
                analyzerResultsService.findHeldMappingResultsByAnalyzer(analyzer.getId()), candidate));
        return confirmationService.confirm(candidate, revision.controlRecognitionSummary().recognitionFingerprint(),
                request, actor);
    }

    @Override
    public AnalyzerMappingView getDefaults(String profileId, int profileRevision) {
        BridgeProfileCatalog.ProfileRevision revision = bridgeProfileCatalogService.getProfile(profileId,
                profileRevision);
        BridgeAnalyzerProfile profile = BridgeAnalyzerProfile.from(revision.profile());
        return compose(null, revision, profile, CurrentRows.of(mappingDefaults.resolve(profile)), List.of(), null);
    }

    private Analyzer find(String analyzerId) {
        return analyzerService.getWithMapping(analyzerId)
                .orElseThrow(() -> new IllegalArgumentException("Analyzer not found: " + analyzerId));
    }

    private AnalyzerMappingSnapshot latest(Analyzer analyzer) {
        return mappingService.findLatestByAnalyzerId(analyzer.getId())
                .orElseThrow(() -> new IllegalArgumentException("Analyzer has no mapping: " + analyzer.getId()));
    }

    private AnalyzerMappingView compose(Analyzer analyzer, BridgeProfileCatalog.ProfileRevision revision,
            BridgeAnalyzerProfile profile, CurrentRows current, List<AnalyzerResults> observed,
            AnalyzerMappingSnapshot snapshot) {
        List<AnalyzerMappingCatalogService.TestOption> activeTests = mappingCatalogService.searchActiveTests(null);
        Map<String, AnalyzerMappingCatalogService.TestOption> activeTestsById = activeTests.stream()
                .collect(Collectors.toMap(AnalyzerMappingCatalogService.TestOption::id, Function.identity()));
        Set<ResultSourceKey> observedHeldValues = observed.stream()
                .filter(row -> !"N".equals(row.getResultType())
                        || AnalyzerResults.IMPORT_ISSUE_UNKNOWN_RESULT_VALUE.equals(row.getImportIssueReason())
                        || AnalyzerResults.IMPORT_ISSUE_RESULT_MAPPING_NOT_READY.equals(row.getImportIssueReason())
                        || AnalyzerResults.IMPORT_ISSUE_INVALID_RESULT_MAPPING.equals(row.getImportIssueReason()))
                .filter(row -> row.getRawTestCode() != null && row.getRawResultValue() != null)
                .map(row -> new ResultSourceKey(row.getRawTestCode(), row.getRawResultValue()))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, BridgeAnalyzerProfile.TestDefinition> definitions = new LinkedHashMap<>();
        profile.testDefinitions().forEach(definition -> definitions.put(definition.analyzerCode(), definition));
        for (AnalyzerResults row : observed) {
            if (row.getRawTestCode() != null && !row.getRawTestCode().isBlank()) {
                definitions.putIfAbsent(row.getRawTestCode(),
                        new BridgeAnalyzerProfile.TestDefinition(row.getRawTestCode(), List.of(), null, null,
                                row.getUnits(), row.getResultType(), List.of(), null, Map.of()));
            }
        }
        current.tests().keySet()
                .forEach(source -> definitions.putIfAbsent(source, new BridgeAnalyzerProfile.TestDefinition(source,
                        List.of(), null, null, null, null, List.of(), null, Map.of())));
        List<AnalyzerMappingView.TestRow> rows = definitions.values().stream()
                .map(definition -> composeTestRow(definition, current.tests().get(definition.analyzerCode()),
                        current.results(), observedHeldValues, activeTests, activeTestsById))
                .toList();
        AnalyzerMappingConfirmationView confirmation = snapshot == null ? AnalyzerMappingConfirmationView.unconfirmed()
                : confirmationService.getStatus(snapshot,
                        revision.controlRecognitionSummary().recognitionFingerprint());
        return new AnalyzerMappingView(analyzer == null ? null : analyzer.getId(), profile.profileId(),
                profile.revision(), profile.revisionFingerprint(), profile.displayName(), profile.protocol(),
                snapshot == null ? null : snapshot.mapping().getId(),
                snapshot == null ? 0 : snapshot.mapping().getRevisionNumber(),
                snapshot == null ? null : snapshot.mapping().getMappingFingerprint(), rows,
                revision.controlRecognitionSummary(), confirmation);
    }

    private static void validateConfirmable(AnalyzerMappingView view) {
        for (AnalyzerMappingView.TestRow test : view.tests()) {
            if (test.mappingState() == AnalyzerMappingState.BOUND && test.selectedTest() == null) {
                throw new IllegalArgumentException("Bound test rows must reference a current catalog Test");
            }
            for (AnalyzerMappingView.ResultRow result : test.results()) {
                if (result.mappingState() == AnalyzerMappingState.BOUND && result.selectedOption() == null) {
                    throw new IllegalArgumentException("Bound result rows must reference a current Result Option");
                }
            }
        }
    }

    private static AnalyzerMappingDraft validateUpdate(BridgeAnalyzerProfile profile, AnalyzerMappingSnapshot current,
            List<AnalyzerResults> observed, AnalyzerMappingUpdate update) {
        AnalyzerMappingDraft draft = update.toDraft();
        Set<String> expectedTests = profile.testDefinitions().stream()
                .map(BridgeAnalyzerProfile.TestDefinition::analyzerCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        current.tests().forEach(row -> expectedTests.add(row.getId().getSourceRowKey()));
        Set<String> allowedTests = new LinkedHashSet<>(expectedTests);
        observed.stream().map(AnalyzerResults::getRawTestCode).filter(Objects::nonNull).forEach(allowedTests::add);
        Set<String> actualTests = draft.tests().stream().filter(row -> row != null && row.sourceRowKey() != null)
                .map(AnalyzerMappingTestDraft::sourceRowKey).collect(Collectors.toCollection(LinkedHashSet::new));
        if (draft.tests().size() != actualTests.size() || !actualTests.containsAll(expectedTests)
                || !allowedTests.containsAll(actualTests)) {
            throw new IllegalArgumentException(
                    "Mapping update must retain declared and saved tests and may add only received test codes");
        }

        Set<ResultSourceKey> expectedResults = profile.testDefinitions().stream()
                .flatMap(definition -> definition.resultValues().stream()
                        .map(value -> new ResultSourceKey(definition.analyzerCode(), value)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        current.results().forEach(row -> expectedResults
                .add(new ResultSourceKey(row.getId().getSourceRowKey(), row.getId().getRawValue())));
        Set<ResultSourceKey> actualResults = draft.results().stream()
                .filter(row -> row != null && row.sourceRowKey() != null && row.rawValue() != null)
                .map(row -> new ResultSourceKey(row.sourceRowKey(), row.rawValue()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        boolean hasDuplicateResults = draft.results().size() != actualResults.size();
        boolean omitsProfileDefault = !actualResults.containsAll(expectedResults);
        boolean hasUnknownTest = actualResults.stream().anyMatch(row -> !actualTests.contains(row.sourceRowKey()));
        if (hasDuplicateResults || omitsProfileDefault || hasUnknownTest) {
            throw new IllegalArgumentException(
                    "Mapping update must retain declared and saved result rows and may add values only to included tests");
        }
        return draft;
    }

    /**
     * A row the operator changed is an override whatever the profile's default
     * says; a row left as it was keeps its origin.
     */
    private static AnalyzerMappingDraft withOrigins(AnalyzerMappingSnapshot previous, AnalyzerMappingDraft draft) {
        Map<String, org.openelisglobal.analyzer.valueholder.AnalyzerMappingTest> before = previous.tests().stream()
                .collect(Collectors.toMap(row -> row.getId().getSourceRowKey(), Function.identity()));
        Map<ResultSourceKey, org.openelisglobal.analyzer.valueholder.AnalyzerMappingResult> beforeResults = previous
                .results().stream()
                .collect(Collectors.toMap(
                        row -> new ResultSourceKey(row.getId().getSourceRowKey(), row.getId().getRawValue()),
                        Function.identity()));
        List<AnalyzerMappingTestDraft> tests = draft.tests().stream().map(row -> {
            var old = before.get(row.sourceRowKey());
            boolean same = old != null && old.getMappingState() == row.mappingState()
                    && Objects.equals(old.getTestId(), row.testId())
                    && Objects.equals(old.getComponentId(), row.componentId());
            return new AnalyzerMappingTestDraft(row.sourceRowKey(), row.mappingState(), row.testId(), row.componentId(),
                    row.unresolvedReason(), same ? old.getOrigin() : AnalyzerMappingOrigin.OVERRIDE);
        }).toList();
        List<AnalyzerMappingResultDraft> results = draft.results().stream().map(row -> {
            var old = beforeResults.get(new ResultSourceKey(row.sourceRowKey(), row.rawValue()));
            boolean same = old != null && old.getMappingState() == row.mappingState()
                    && Objects.equals(old.getTestResultId(), row.testResultId());
            return new AnalyzerMappingResultDraft(row.sourceRowKey(), row.rawValue(), row.mappingState(),
                    row.testResultId(), row.unresolvedReason(),
                    same ? old.getOrigin() : AnalyzerMappingOrigin.OVERRIDE);
        }).toList();
        return new AnalyzerMappingDraft(tests, results);
    }

    private static void validateLoadedFingerprint(AnalyzerMappingSnapshot current, String loadedFingerprint) {
        String normalized = loadedFingerprint == null || loadedFingerprint.isBlank() ? null : loadedFingerprint.trim();
        if (!Objects.equals(normalized, current.mapping().getMappingFingerprint())) {
            throw new IllegalArgumentException("The analyzer's mapping changed after this editor was loaded");
        }
    }

    private AnalyzerMappingView.TestRow composeTestRow(BridgeAnalyzerProfile.TestDefinition definition,
            CurrentTest current, Map<ResultSourceKey, CurrentResult> currentResults,
            Set<ResultSourceKey> observedHeldValues, List<AnalyzerMappingCatalogService.TestOption> activeTests,
            Map<String, AnalyzerMappingCatalogService.TestOption> activeTestsById) {
        AnalyzerMappingState state = current == null ? AnalyzerMappingState.UNRESOLVED : current.state();
        AnalyzerMappingOrigin origin = current == null ? AnalyzerMappingOrigin.DEFAULT : current.origin();
        String testId = current == null ? null : current.testId();
        String componentId = current == null ? null : current.componentId();
        AnalyzerMappingCatalogService.TestOption selected = testId == null ? null : activeTestsById.get(testId);
        AnalyzerMappingTestDraft resolved = state == AnalyzerMappingState.UNRESOLVED
                ? mappingDefaults.resolveTest(definition, activeTests)
                : null;
        AnalyzerMappingCatalogService.TestOption suggested = resolved == null || resolved.testId() == null ? null
                : activeTestsById.get(resolved.testId());
        AnalyzerUnresolvedReason testReason = resolved == null ? null : resolved.unresolvedReason();
        AnalyzerMappingCatalogService.TestOption answerTest = selected != null ? selected : suggested;
        List<AnalyzerMappingCatalogService.ResultOption> answerOptions = answerTest == null ? List.of()
                : mappingCatalogService.getActiveResultOptions(answerTest.id());
        Map<String, AnalyzerMappingCatalogService.ResultOption> activeResults = selected == null ? Map.of()
                : answerOptions.stream()
                        .collect(Collectors.toMap(AnalyzerMappingCatalogService.ResultOption::id, Function.identity()));
        LinkedHashSet<String> rawValues = new LinkedHashSet<>(definition.resultValues());
        currentResults.keySet().stream().filter(key -> definition.analyzerCode().equals(key.sourceRowKey()))
                .map(ResultSourceKey::rawValue).forEach(rawValues::add);
        observedHeldValues.stream().filter(key -> definition.analyzerCode().equals(key.sourceRowKey()))
                .map(ResultSourceKey::rawValue).forEach(rawValues::add);
        List<AnalyzerMappingView.ResultRow> results = new ArrayList<>();
        for (String rawValue : rawValues) {
            ResultSourceKey key = new ResultSourceKey(definition.analyzerCode(), rawValue);
            CurrentResult result = currentResults.get(key);
            AnalyzerMappingState resultState = result == null ? AnalyzerMappingState.UNRESOLVED : result.state();
            AnalyzerMappingOrigin resultOrigin = result == null ? AnalyzerMappingOrigin.DEFAULT : result.origin();
            String optionId = result == null ? null : result.testResultId();
            AnalyzerMappingCatalogService.ResultOption suggestedOption = null;
            AnalyzerUnresolvedReason reason = null;
            if (resultState == AnalyzerMappingState.UNRESOLVED) {
                if (answerTest == null) {
                    reason = testReason == null ? AnalyzerUnresolvedReason.NO_MATCH : testReason;
                } else {
                    AnalyzerMappingResultDraft answer = mappingDefaults.resolveAnswer(definition, rawValue,
                            answerOptions);
                    reason = answer.unresolvedReason();
                    suggestedOption = answer.testResultId() == null ? null
                            : answerOptions.stream().filter(option -> option.id().equals(answer.testResultId()))
                                    .findFirst().orElse(null);
                }
            }
            results.add(new AnalyzerMappingView.ResultRow(rawValue, resultState, resultOrigin, optionId,
                    optionId == null ? null : activeResults.get(optionId), suggestedOption, reason,
                    observedHeldValues.contains(key)));
        }
        return new AnalyzerMappingView.TestRow(definition.analyzerCode(), definition.analyzerCode(),
                definition.aliases(), definition.testNameHint(), definition.loinc(), definition.unit(),
                definition.resultType(), definition.normalizedCoding(), state, origin, testId, componentId, selected,
                suggested, testReason, results);
    }

    private record ResultSourceKey(String sourceRowKey, String rawValue) {
    }

    private record CurrentTest(AnalyzerMappingState state, AnalyzerMappingOrigin origin, String testId,
            String componentId) {
    }

    private record CurrentResult(AnalyzerMappingState state, AnalyzerMappingOrigin origin, String testResultId) {
    }

    /**
     * The decisions in a saved revision, or in a freshly resolved set of defaults.
     */
    private record CurrentRows(Map<String, CurrentTest> tests, Map<ResultSourceKey, CurrentResult> results) {

        static CurrentRows of(AnalyzerMappingSnapshot snapshot) {
            Map<String, CurrentTest> tests = snapshot.tests().stream().collect(
                    Collectors.toMap(row -> row.getId().getSourceRowKey(), row -> new CurrentTest(row.getMappingState(),
                            row.getOrigin(), row.getTestId(), row.getComponentId())));
            Map<ResultSourceKey, CurrentResult> results = snapshot.results().stream()
                    .collect(Collectors.toMap(
                            row -> new ResultSourceKey(row.getId().getSourceRowKey(), row.getId().getRawValue()),
                            row -> new CurrentResult(row.getMappingState(), row.getOrigin(), row.getTestResultId())));
            return new CurrentRows(tests, results);
        }

        static CurrentRows of(AnalyzerMappingDraft draft) {
            Map<String, CurrentTest> tests = draft.tests().stream()
                    .collect(Collectors.toMap(AnalyzerMappingTestDraft::sourceRowKey,
                            row -> new CurrentTest(row.mappingState(), row.origin(), row.testId(), row.componentId())));
            Map<ResultSourceKey, CurrentResult> results = draft.results().stream()
                    .collect(Collectors.toMap(row -> new ResultSourceKey(row.sourceRowKey(), row.rawValue()),
                            row -> new CurrentResult(row.mappingState(), row.origin(), row.testResultId())));
            return new CurrentRows(tests, results);
        }
    }
}
