package org.openelisglobal.analyzer.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.openelisglobal.analyzer.dao.AnalyzerProfileBindingDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingMappingState;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingResult;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingTest;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnalyzerTypeMappingServiceImpl implements AnalyzerTypeMappingService {

    private final BridgeProfileCatalogService bridgeProfileCatalogService;
    private final AnalyzerProfileBindingDAO profileBindingDAO;
    private final AnalyzerSiteBindingService siteBindingService;
    private final AnalyzerMappingCatalogService mappingCatalogService;
    private final AnalyzerProfileBindingService profileBindingService;
    private final AnalyzerSiteBindingConfirmationService confirmationService;
    private final AnalyzerResultsService analyzerResultsService;
    private final AnalyzerMappingDefaults mappingDefaults;

    public AnalyzerTypeMappingServiceImpl(BridgeProfileCatalogService bridgeProfileCatalogService,
            AnalyzerProfileBindingDAO profileBindingDAO, AnalyzerSiteBindingService siteBindingService,
            AnalyzerMappingCatalogService mappingCatalogService, AnalyzerProfileBindingService profileBindingService,
            AnalyzerSiteBindingConfirmationService confirmationService, AnalyzerResultsService analyzerResultsService,
            AnalyzerMappingDefaults mappingDefaults) {
        this.bridgeProfileCatalogService = bridgeProfileCatalogService;
        this.profileBindingDAO = profileBindingDAO;
        this.siteBindingService = siteBindingService;
        this.mappingCatalogService = mappingCatalogService;
        this.profileBindingService = profileBindingService;
        this.confirmationService = confirmationService;
        this.analyzerResultsService = analyzerResultsService;
        this.mappingDefaults = mappingDefaults;
    }

    @Override
    public AnalyzerTypeMappingView getMapping(String profileId, int profileRevision) {
        BridgeProfileCatalog.ProfileRevision revision = bridgeProfileCatalogService.getProfile(profileId,
                profileRevision);
        BridgeAnalyzerProfile profile = BridgeAnalyzerProfile.from(revision.profile());
        AnalyzerSiteBindingSnapshot binding = findBinding(profile.profileId(), profile.revision()).orElse(null);
        return compose(revision, profile, binding);
    }

    @Override
    @Transactional
    public AnalyzerTypeMappingView saveMapping(String profileId, int profileRevision, AnalyzerTypeMappingUpdate update,
            String actor) {
        BridgeProfileCatalog.ProfileRevision revision = bridgeProfileCatalogService.getProfile(profileId,
                profileRevision);
        BridgeAnalyzerProfile profile = BridgeAnalyzerProfile.from(revision.profile());
        if (update == null) {
            throw new IllegalArgumentException("Mapping update is required");
        }
        Optional<AnalyzerSiteBindingSnapshot> current = findBinding(profile.profileId(), profile.revision());
        validateLoadedFingerprint(current.orElse(null), update.baseBindingFingerprint());
        AnalyzerSiteBindingDraft draft = validateUpdate(profile, current.orElse(null),
                analyzerResultsService.findHeldMappingResultsByProfile(profile.profileId(), profile.revision()),
                update);

        AnalyzerProfileBinding profileBinding = profileBindingService.resolveActiveRevision(profile.profileId(),
                profile.revision(), actor);
        AnalyzerSiteBindingSnapshot basis = current
                .orElseGet(() -> siteBindingService.resolveInitialRevision(profileBinding, profile.document(), actor));
        AnalyzerSiteBindingSnapshot saved = siteBindingService.appendRevision(basis.binding(), draft, actor);
        return compose(revision, profile, saved);
    }

    @Override
    @Transactional
    public AnalyzerSiteBindingConfirmationView confirmMapping(String profileId, int profileRevision,
            AnalyzerSiteBindingConfirmationRequest request, String actor) {
        BridgeProfileCatalog.ProfileRevision revision = bridgeProfileCatalogService.getProfile(profileId,
                profileRevision);
        BridgeAnalyzerProfile profile = BridgeAnalyzerProfile.from(revision.profile());
        AnalyzerSiteBindingSnapshot candidate = findBinding(profile.profileId(), profile.revision()).orElseThrow(
                () -> new IllegalArgumentException("Analyzer Type mappings must be saved before confirmation"));
        validateConfirmable(compose(revision, profile, candidate));
        return confirmationService.confirm(candidate, revision.controlRecognitionSummary().recognitionFingerprint(),
                request, actor);
    }

    private AnalyzerTypeMappingView compose(BridgeProfileCatalog.ProfileRevision revision,
            BridgeAnalyzerProfile profile, AnalyzerSiteBindingSnapshot binding) {
        List<AnalyzerMappingCatalogService.TestOption> activeTests = mappingCatalogService.searchActiveTests(null);
        Map<String, AnalyzerMappingCatalogService.TestOption> activeTestsById = activeTests.stream()
                .collect(Collectors.toMap(AnalyzerMappingCatalogService.TestOption::id, Function.identity()));
        Map<String, AnalyzerSiteBindingTest> currentTests = Optional.ofNullable(binding)
                .map(AnalyzerSiteBindingSnapshot::tests).orElse(List.of()).stream()
                .collect(Collectors.toMap(row -> row.getId().getSourceRowKey(), Function.identity()));
        Map<ResultSourceKey, AnalyzerSiteBindingResult> currentResults = Optional.ofNullable(binding)
                .map(AnalyzerSiteBindingSnapshot::results).orElse(List.of()).stream()
                .collect(Collectors.toMap(
                        row -> new ResultSourceKey(row.getId().getSourceRowKey(), row.getId().getRawValue()),
                        Function.identity()));
        List<AnalyzerResults> observed = analyzerResultsService.findHeldMappingResultsByProfile(profile.profileId(),
                profile.revision());
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
        currentTests.keySet()
                .forEach(source -> definitions.putIfAbsent(source, new BridgeAnalyzerProfile.TestDefinition(source,
                        List.of(), null, null, null, null, List.of(), null, Map.of())));
        List<AnalyzerTypeMappingView.TestRow> rows = definitions.values().stream()
                .map(definition -> composeTestRow(definition, currentTests.get(definition.analyzerCode()),
                        currentResults, observedHeldValues, activeTests, activeTestsById))
                .toList();
        AnalyzerSiteBindingConfirmationView confirmation = binding == null
                ? AnalyzerSiteBindingConfirmationView.unconfirmed()
                : confirmationService.getStatus(binding, revision.controlRecognitionSummary().recognitionFingerprint());
        return new AnalyzerTypeMappingView(profile.profileId(), profile.revision(), profile.revisionFingerprint(),
                profile.displayName(), profile.protocol(), binding == null ? null : binding.binding().getId(),
                binding == null ? 0 : binding.revision().getRevisionNumber(),
                binding == null ? null : binding.revision().getBindingFingerprint(), rows,
                revision.controlRecognitionSummary(), confirmation);
    }

    private static void validateConfirmable(AnalyzerTypeMappingView view) {
        for (AnalyzerTypeMappingView.TestRow test : view.tests()) {
            if (test.mappingState() == AnalyzerSiteBindingMappingState.BOUND && test.selectedTest() == null) {
                throw new IllegalArgumentException("Bound test rows must reference a current catalog Test");
            }
            for (AnalyzerTypeMappingView.ResultRow result : test.results()) {
                if (result.mappingState() == AnalyzerSiteBindingMappingState.BOUND && result.selectedOption() == null) {
                    throw new IllegalArgumentException("Bound result rows must reference a current Result Option");
                }
            }
        }
    }

    private static AnalyzerSiteBindingDraft validateUpdate(BridgeAnalyzerProfile profile,
            AnalyzerSiteBindingSnapshot current, List<AnalyzerResults> observed, AnalyzerTypeMappingUpdate update) {
        if (update == null) {
            throw new IllegalArgumentException("Mapping update is required");
        }
        AnalyzerSiteBindingDraft draft = update.toDraft();
        Set<String> expectedTests = profile.testDefinitions().stream()
                .map(BridgeAnalyzerProfile.TestDefinition::analyzerCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (current != null) {
            current.tests().forEach(row -> expectedTests.add(row.getId().getSourceRowKey()));
        }
        Set<String> allowedTests = new LinkedHashSet<>(expectedTests);
        observed.stream().map(AnalyzerResults::getRawTestCode).filter(java.util.Objects::nonNull)
                .forEach(allowedTests::add);
        Set<String> actualTests = draft.tests().stream().filter(row -> row != null && row.sourceRowKey() != null)
                .map(AnalyzerSiteBindingTestDraft::sourceRowKey).collect(Collectors.toCollection(LinkedHashSet::new));
        if (draft.tests().size() != actualTests.size() || !actualTests.containsAll(expectedTests)
                || !allowedTests.containsAll(actualTests)) {
            throw new IllegalArgumentException(
                    "Mapping update must retain declared and saved tests and may add only received test codes");
        }

        Set<ResultSourceKey> expectedResults = profile.testDefinitions().stream()
                .flatMap(definition -> definition.resultValues().stream()
                        .map(value -> new ResultSourceKey(definition.analyzerCode(), value)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (current != null) {
            current.results().forEach(row -> expectedResults
                    .add(new ResultSourceKey(row.getId().getSourceRowKey(), row.getId().getRawValue())));
        }
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

    private static void validateLoadedFingerprint(AnalyzerSiteBindingSnapshot current, String loadedFingerprint) {
        String normalized = loadedFingerprint == null || loadedFingerprint.isBlank() ? null : loadedFingerprint.trim();
        String currentFingerprint = current == null ? null : current.revision().getBindingFingerprint();
        if (!java.util.Objects.equals(normalized, currentFingerprint)) {
            throw new IllegalArgumentException("Analyzer Type mappings changed after this editor was loaded");
        }
    }

    private Optional<AnalyzerSiteBindingSnapshot> findBinding(String profileId, int profileRevision) {
        return profileBindingDAO.findByProfileIdAndRevision(profileId, profileRevision)
                .map(AnalyzerProfileBinding::getId).flatMap(siteBindingService::findCurrentByProfileBindingId);
    }

    private AnalyzerTypeMappingView.TestRow composeTestRow(BridgeAnalyzerProfile.TestDefinition definition,
            AnalyzerSiteBindingTest current, Map<ResultSourceKey, AnalyzerSiteBindingResult> currentResults,
            Set<ResultSourceKey> observedHeldValues, List<AnalyzerMappingCatalogService.TestOption> activeTests,
            Map<String, AnalyzerMappingCatalogService.TestOption> activeTestsById) {
        AnalyzerSiteBindingMappingState state = current == null ? AnalyzerSiteBindingMappingState.UNRESOLVED
                : current.getMappingState();
        String testId = current == null ? null : current.getTestId();
        AnalyzerMappingCatalogService.TestOption selected = testId == null ? null : activeTestsById.get(testId);
        AnalyzerSiteBindingTestDraft resolved = state == AnalyzerSiteBindingMappingState.UNRESOLVED
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
        List<AnalyzerTypeMappingView.ResultRow> results = new ArrayList<>();
        for (String rawValue : rawValues) {
            ResultSourceKey key = new ResultSourceKey(definition.analyzerCode(), rawValue);
            AnalyzerSiteBindingResult result = currentResults.get(key);
            AnalyzerSiteBindingMappingState resultState = result == null ? AnalyzerSiteBindingMappingState.UNRESOLVED
                    : result.getMappingState();
            String optionId = result == null ? null : result.getTestResultId();
            AnalyzerMappingCatalogService.ResultOption suggestedOption = null;
            AnalyzerUnresolvedReason reason = null;
            if (resultState == AnalyzerSiteBindingMappingState.UNRESOLVED) {
                if (answerTest == null) {
                    reason = testReason == null ? AnalyzerUnresolvedReason.NO_MATCH : testReason;
                } else {
                    AnalyzerSiteBindingResultDraft answer = mappingDefaults.resolveAnswer(definition, rawValue,
                            answerOptions);
                    reason = answer.unresolvedReason();
                    suggestedOption = answer.testResultId() == null ? null
                            : answerOptions.stream().filter(option -> option.id().equals(answer.testResultId()))
                                    .findFirst().orElse(null);
                }
            }
            results.add(new AnalyzerTypeMappingView.ResultRow(rawValue, resultState, optionId,
                    optionId == null ? null : activeResults.get(optionId), suggestedOption, reason,
                    observedHeldValues.contains(key)));
        }
        return new AnalyzerTypeMappingView.TestRow(definition.analyzerCode(), definition.analyzerCode(),
                definition.aliases(), definition.testNameHint(), definition.loinc(), definition.unit(),
                definition.resultType(), definition.normalizedCoding(), state, testId, selected, suggested, testReason,
                results);
    }

    private record ResultSourceKey(String sourceRowKey, String rawValue) {
    }
}
