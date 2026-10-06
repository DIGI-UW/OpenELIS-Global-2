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

    @Override
    public AnalyzerMappingView preview(String analyzerId, int profileRevision, AnalyzerMappingDraft decisions) {
        Analyzer analyzer = find(analyzerId);
        BridgeProfileCatalog.ProfileRevision revision = bridgeProfileCatalogService
                .getProfile(latest(analyzer).mapping().getProfileId(), profileRevision);
        return compose(analyzer, revision, BridgeAnalyzerProfile.from(revision.profile()), CurrentRows.of(decisions),
                List.of(), null);
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
                .map(row -> new ResultSourceKey(recordOf(row), row.getRawResultValue()))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<AnalyzerMappingRowKey, ReportedRecord> records = declaredRecords(profile);
        for (AnalyzerResults row : observed) {
            if (row.getRawTestCode() != null && !row.getRawTestCode().isBlank()) {
                records.putIfAbsent(recordOf(row),
                        ReportedRecord.main(new BridgeAnalyzerProfile.TestDefinition(row.getRawTestCode(), List.of(),
                                null, null, row.getUnits(), row.getResultType(), List.of(), null, Map.of())));
            }
        }
        current.tests().keySet().forEach(key -> records.putIfAbsent(key, ReportedRecord.undeclared(key)));
        List<AnalyzerMappingView.TestRow> rows = records.entrySet().stream()
                .map(entry -> composeTestRow(entry.getKey(), entry.getValue(), current.tests().get(entry.getKey()),
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
                throw new AnalyzerRequestException("analyzer.mapping.error.testNotCurrent",
                        "Bound test rows must reference a current catalog Test");
            }
            for (AnalyzerMappingView.ResultRow result : test.results()) {
                if (result.mappingState() == AnalyzerMappingState.BOUND && result.selectedOption() == null) {
                    throw new AnalyzerRequestException("analyzer.mapping.error.resultNotCurrent",
                            "Bound result rows must reference a current Result Option");
                }
            }
        }
    }

    private static AnalyzerMappingDraft validateUpdate(BridgeAnalyzerProfile profile, AnalyzerMappingSnapshot current,
            List<AnalyzerResults> observed, AnalyzerMappingUpdate update) {
        AnalyzerMappingDraft draft = update.toDraft();
        Map<AnalyzerMappingRowKey, ReportedRecord> declared = declaredRecords(profile);
        Set<AnalyzerMappingRowKey> expectedTests = new LinkedHashSet<>(declared.keySet());
        current.tests().forEach(row -> expectedTests.add(AnalyzerMappingRowKey.of(row)));
        Set<AnalyzerMappingRowKey> allowedTests = new LinkedHashSet<>(expectedTests);
        observed.stream().filter(row -> row.getRawTestCode() != null).map(AnalyzerMappingEditorServiceImpl::recordOf)
                .forEach(allowedTests::add);
        Set<AnalyzerMappingRowKey> actualTests = draft.tests().stream()
                .filter(row -> row != null && row.sourceRowKey() != null).map(AnalyzerMappingTestDraft::rowKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (draft.tests().size() != actualTests.size() || !actualTests.containsAll(expectedTests)
                || !allowedTests.containsAll(actualTests)) {
            throw new IllegalArgumentException(
                    "Mapping update must retain declared and saved tests and may add only received test codes");
        }

        Set<ResultSourceKey> expectedResults = declared.entrySet().stream().flatMap(
                entry -> entry.getValue().values().stream().map(value -> new ResultSourceKey(entry.getKey(), value)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        current.results().forEach(row -> expectedResults.add(ResultSourceKey.of(row)));
        Set<ResultSourceKey> actualResults = draft.results().stream()
                .filter(row -> row != null && row.sourceRowKey() != null && row.rawValue() != null)
                .map(row -> new ResultSourceKey(row.rowKey(), row.rawValue()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        boolean hasDuplicateResults = draft.results().size() != actualResults.size();
        boolean omitsProfileDefault = !actualResults.containsAll(expectedResults);
        boolean hasUnknownTest = actualResults.stream().anyMatch(row -> !actualTests.contains(row.record()));
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
        Map<AnalyzerMappingRowKey, org.openelisglobal.analyzer.valueholder.AnalyzerMappingTest> before = previous
                .tests().stream().collect(Collectors.toMap(AnalyzerMappingRowKey::of, Function.identity()));
        Map<ResultSourceKey, org.openelisglobal.analyzer.valueholder.AnalyzerMappingResult> beforeResults = previous
                .results().stream().collect(Collectors.toMap(ResultSourceKey::of, Function.identity()));
        List<AnalyzerMappingTestDraft> tests = draft.tests().stream().map(row -> {
            var old = before.get(row.rowKey());
            boolean same = old != null && old.getMappingState() == row.mappingState()
                    && Objects.equals(old.getTestId(), row.testId())
                    && Objects.equals(old.getComponentId(), row.componentId())
                    && Objects.equals(old.getCallComponentId(), row.callComponentId());
            // An edit that does not state an assay's switch or code keeps what it had.
            Boolean enabled = row.enabled() != null ? row.enabled() : old == null || old.isEnabled();
            String instrumentCode = row.enabled() != null || row.instrumentCode() != null ? row.instrumentCode()
                    : old == null ? null : old.getInstrumentCode();
            return new AnalyzerMappingTestDraft(row.sourceRowKey(), row.mappingState(), row.testId(), row.componentId(),
                    row.unresolvedReason(), same ? old.getOrigin() : AnalyzerMappingOrigin.OVERRIDE, row.subIdentity(),
                    row.callComponentId(), enabled, instrumentCode);
        }).toList();
        List<AnalyzerMappingResultDraft> results = draft.results().stream().map(row -> {
            var old = beforeResults.get(new ResultSourceKey(row.rowKey(), row.rawValue()));
            boolean same = old != null && old.getMappingState() == row.mappingState()
                    && Objects.equals(old.getTestResultId(), row.testResultId());
            return new AnalyzerMappingResultDraft(row.sourceRowKey(), row.rawValue(), row.mappingState(),
                    row.testResultId(), row.unresolvedReason(), same ? old.getOrigin() : AnalyzerMappingOrigin.OVERRIDE,
                    row.subIdentity());
        }).toList();
        return new AnalyzerMappingDraft(tests, results);
    }

    private static void validateLoadedFingerprint(AnalyzerMappingSnapshot current, String loadedFingerprint) {
        String normalized = loadedFingerprint == null || loadedFingerprint.isBlank() ? null : loadedFingerprint.trim();
        if (!Objects.equals(normalized, current.mapping().getMappingFingerprint())) {
            throw new AnalyzerRequestException("analyzer.mapping.error.changedSinceLoaded",
                    "The analyzer's mapping changed after this editor was loaded");
        }
    }

    private AnalyzerMappingView.TestRow composeTestRow(AnalyzerMappingRowKey key, ReportedRecord record,
            CurrentTest current, Map<ResultSourceKey, CurrentResult> currentResults,
            Set<ResultSourceKey> observedHeldValues, List<AnalyzerMappingCatalogService.TestOption> activeTests,
            Map<String, AnalyzerMappingCatalogService.TestOption> activeTestsById) {
        BridgeAnalyzerProfile.TestDefinition definition = record.test();
        AnalyzerMappingState state = current == null ? AnalyzerMappingState.UNRESOLVED : current.state();
        AnalyzerMappingOrigin origin = current == null ? AnalyzerMappingOrigin.DEFAULT : current.origin();
        String testId = current == null ? null : current.testId();
        String componentId = current == null ? null : current.componentId();
        String callComponentId = current == null ? null : current.callComponentId();
        AnalyzerMappingCatalogService.TestOption selected = testId == null ? null : activeTestsById.get(testId);
        AnalyzerMappingTestDraft resolved = state == AnalyzerMappingState.UNRESOLVED
                ? mappingDefaults.resolveTest(definition, activeTests)
                : null;
        AnalyzerMappingCatalogService.TestOption suggested = resolved == null || resolved.testId() == null ? null
                : activeTestsById.get(resolved.testId());
        AnalyzerUnresolvedReason testReason = resolved == null ? null : resolved.unresolvedReason();
        AnalyzerMappingCatalogService.TestOption answerTest = selected != null ? selected : suggested;
        // A record's answers are the options of the component it lands on: the
        // main record's call component, or the record's own component. Until that
        // component is known, no answer is offered.
        boolean landsOnComponent = !key.subIdentity().isEmpty() || definition.callComponent() != null;
        String answerComponent = key.subIdentity().isEmpty() ? callComponentId : componentId;
        List<AnalyzerMappingCatalogService.ResultOption> answerOptions = answerTest == null
                || landsOnComponent && answerComponent == null
                        ? List.of()
                        : mappingCatalogService.getActiveResultOptions(answerTest.id()).stream().filter(
                                option -> answerComponent == null || answerComponent.equals(option.componentId()))
                                .toList();
        Map<String, AnalyzerMappingCatalogService.ResultOption> activeResults = selected == null ? Map.of()
                : answerOptions.stream()
                        .collect(Collectors.toMap(AnalyzerMappingCatalogService.ResultOption::id, Function.identity()));
        LinkedHashSet<String> rawValues = new LinkedHashSet<>(record.values());
        currentResults.keySet().stream().filter(result -> key.equals(result.record())).map(ResultSourceKey::rawValue)
                .forEach(rawValues::add);
        observedHeldValues.stream().filter(result -> key.equals(result.record())).map(ResultSourceKey::rawValue)
                .forEach(rawValues::add);
        List<AnalyzerMappingView.ResultRow> results = new ArrayList<>();
        for (String rawValue : rawValues) {
            ResultSourceKey resultKey = new ResultSourceKey(key, rawValue);
            CurrentResult result = currentResults.get(resultKey);
            AnalyzerMappingState resultState = result == null ? AnalyzerMappingState.UNRESOLVED : result.state();
            AnalyzerMappingOrigin resultOrigin = result == null ? AnalyzerMappingOrigin.DEFAULT : result.origin();
            String optionId = result == null ? null : result.testResultId();
            AnalyzerMappingCatalogService.ResultOption suggestedOption = null;
            AnalyzerUnresolvedReason reason = null;
            if (resultState == AnalyzerMappingState.UNRESOLVED) {
                if (answerTest == null) {
                    reason = testReason == null ? AnalyzerUnresolvedReason.NO_MATCH : testReason;
                } else {
                    AnalyzerMappingResultDraft answer = mappingDefaults.resolveAnswer(definition.analyzerCode(),
                            key.subIdentity(), record.valueCodes(), rawValue, answerOptions);
                    reason = answer.unresolvedReason();
                    suggestedOption = answer.testResultId() == null ? null
                            : answerOptions.stream().filter(option -> option.id().equals(answer.testResultId()))
                                    .findFirst().orElse(null);
                }
            }
            results.add(new AnalyzerMappingView.ResultRow(rawValue, resultState, resultOrigin, optionId,
                    optionId == null ? null : activeResults.get(optionId), suggestedOption, reason,
                    observedHeldValues.contains(resultKey), record.translationOf().get(rawValue)));
        }
        return new AnalyzerMappingView.TestRow(definition.analyzerCode(), definition.analyzerCode(),
                definition.aliases(), definition.testNameHint(), definition.loinc(), record.unit(), record.resultType(),
                definition.normalizedCoding(), state, origin, testId, componentId, selected, suggested, testReason,
                results, key.subIdentity(), callComponentId, record.componentCode(),
                key.subIdentity().isEmpty() ? definition.callComponent() : null, current == null || current.enabled(),
                current == null ? null : current.instrumentCode());
    }

    private static AnalyzerMappingRowKey recordOf(AnalyzerResults staged) {
        return new AnalyzerMappingRowKey(staged.getRawTestCode(), staged.getRawSubIdentity());
    }

    /**
     * Every record the profile declares: each test's main result, then its
     * components' records.
     */
    private static Map<AnalyzerMappingRowKey, ReportedRecord> declaredRecords(BridgeAnalyzerProfile profile) {
        Map<AnalyzerMappingRowKey, ReportedRecord> records = new LinkedHashMap<>();
        for (var definition : profile.testDefinitions()) {
            records.put(AnalyzerMappingRowKey.main(definition.analyzerCode()), ReportedRecord.main(definition));
            for (var component : definition.recordComponents()) {
                records.put(new AnalyzerMappingRowKey(definition.analyzerCode(), component.subIdentity()),
                        ReportedRecord.of(definition, component));
            }
        }
        return records;
    }

    /**
     * One record a test reports. Each declared value is followed by its
     * translations; a translation answers with its value's code.
     */
    private record ReportedRecord(BridgeAnalyzerProfile.TestDefinition test, String componentCode, String unit,
            String resultType, List<String> values,
            Map<String, List<BridgeAnalyzerProfile.NormalizedCoding>> valueCodes, Map<String, String> translationOf) {

        static ReportedRecord main(BridgeAnalyzerProfile.TestDefinition test) {
            return translated(test, null, test.unit(), test.resultType(), test.resultValues(), test.valueCodes(),
                    test.translations());
        }

        static ReportedRecord of(BridgeAnalyzerProfile.TestDefinition test,
                BridgeAnalyzerProfile.ComponentDefinition component) {
            return translated(test, component.code(), component.unit(), component.resultType(),
                    component.resultValues(), component.valueCodes(), component.translations());
        }

        static ReportedRecord undeclared(AnalyzerMappingRowKey key) {
            return new ReportedRecord(new BridgeAnalyzerProfile.TestDefinition(key.sourceRowKey(), List.of(), null,
                    null, null, null, List.of(), null, Map.of()), null, null, null, List.of(), Map.of(), Map.of());
        }

        private static ReportedRecord translated(BridgeAnalyzerProfile.TestDefinition test, String componentCode,
                String unit, String resultType, List<String> declared,
                Map<String, List<BridgeAnalyzerProfile.NormalizedCoding>> codes,
                Map<String, List<String>> translations) {
            List<String> values = new ArrayList<>();
            Map<String, List<BridgeAnalyzerProfile.NormalizedCoding>> valueCodes = new LinkedHashMap<>(codes);
            Map<String, String> translationOf = new LinkedHashMap<>();
            for (String value : declared) {
                values.add(value);
                for (String text : translations.getOrDefault(value, List.of())) {
                    values.add(text);
                    translationOf.put(text, value);
                    if (codes.containsKey(value)) {
                        valueCodes.put(text, codes.get(value));
                    }
                }
            }
            return new ReportedRecord(test, componentCode, unit, resultType, List.copyOf(values), valueCodes,
                    translationOf);
        }
    }

    private record ResultSourceKey(AnalyzerMappingRowKey record, String rawValue) {

        static ResultSourceKey of(org.openelisglobal.analyzer.valueholder.AnalyzerMappingResult row) {
            return new ResultSourceKey(AnalyzerMappingRowKey.of(row), row.getId().getRawValue());
        }
    }

    private record CurrentTest(AnalyzerMappingState state, AnalyzerMappingOrigin origin, String testId,
            String componentId, String callComponentId, boolean enabled, String instrumentCode) {
    }

    private record CurrentResult(AnalyzerMappingState state, AnalyzerMappingOrigin origin, String testResultId) {
    }

    /**
     * The decisions in a saved revision, or in a freshly resolved set of defaults.
     */
    private record CurrentRows(Map<AnalyzerMappingRowKey, CurrentTest> tests,
            Map<ResultSourceKey, CurrentResult> results) {

        static CurrentRows of(AnalyzerMappingSnapshot snapshot) {
            Map<AnalyzerMappingRowKey, CurrentTest> tests = snapshot.tests().stream()
                    .collect(Collectors.toMap(AnalyzerMappingRowKey::of,
                            row -> new CurrentTest(row.getMappingState(), row.getOrigin(), row.getTestId(),
                                    row.getComponentId(), row.getCallComponentId(), row.isEnabled(),
                                    row.getInstrumentCode())));
            Map<ResultSourceKey, CurrentResult> results = snapshot.results().stream()
                    .collect(Collectors.toMap(ResultSourceKey::of,
                            row -> new CurrentResult(row.getMappingState(), row.getOrigin(), row.getTestResultId())));
            return new CurrentRows(tests, results);
        }

        static CurrentRows of(AnalyzerMappingDraft draft) {
            Map<AnalyzerMappingRowKey, CurrentTest> tests = draft.tests().stream()
                    .collect(Collectors.toMap(AnalyzerMappingTestDraft::rowKey,
                            row -> new CurrentTest(row.mappingState(), row.origin(), row.testId(), row.componentId(),
                                    row.callComponentId(), row.isEnabled(), row.instrumentCode())));
            Map<ResultSourceKey, CurrentResult> results = draft.results().stream()
                    .collect(Collectors.toMap(row -> new ResultSourceKey(row.rowKey(), row.rawValue()),
                            row -> new CurrentResult(row.mappingState(), row.origin(), row.testResultId())));
            return new CurrentRows(tests, results);
        }
    }
}
