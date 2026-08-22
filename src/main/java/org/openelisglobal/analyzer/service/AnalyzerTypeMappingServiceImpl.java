package org.openelisglobal.analyzer.service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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

    public AnalyzerTypeMappingServiceImpl(BridgeProfileCatalogService bridgeProfileCatalogService,
            AnalyzerProfileBindingDAO profileBindingDAO, AnalyzerSiteBindingService siteBindingService,
            AnalyzerMappingCatalogService mappingCatalogService, AnalyzerProfileBindingService profileBindingService) {
        this.bridgeProfileCatalogService = bridgeProfileCatalogService;
        this.profileBindingDAO = profileBindingDAO;
        this.siteBindingService = siteBindingService;
        this.mappingCatalogService = mappingCatalogService;
        this.profileBindingService = profileBindingService;
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
        AnalyzerSiteBindingDraft draft = validateUpdate(profile, update);
        Optional<AnalyzerSiteBindingSnapshot> current = findBinding(profile.profileId(), profile.revision());
        validateLoadedFingerprint(current.orElse(null), update.baseBindingFingerprint());

        AnalyzerProfileBinding profileBinding = profileBindingService.resolveActiveRevision(profile.profileId(),
                profile.revision(), actor);
        AnalyzerSiteBindingSnapshot basis = current
                .orElseGet(() -> siteBindingService.resolveInitialRevision(profileBinding, profile.document(), actor));
        AnalyzerSiteBindingSnapshot saved = siteBindingService.appendRevision(basis.binding(), draft, actor);
        return compose(revision, profile, saved);
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

        List<AnalyzerTypeMappingView.TestRow> rows = profile
                .testDefinitions().stream().map(definition -> composeTestRow(definition,
                        currentTests.get(definition.analyzerCode()), currentResults, activeTests, activeTestsById))
                .toList();
        return new AnalyzerTypeMappingView(profile.profileId(), profile.revision(), profile.revisionFingerprint(),
                profile.displayName(), profile.protocol(), binding == null ? null : binding.binding().getId(),
                binding == null ? 0 : binding.revision().getRevisionNumber(),
                binding == null ? null : binding.revision().getBindingFingerprint(), rows,
                revision.controlRecognitionSummary());
    }

    private static AnalyzerSiteBindingDraft validateUpdate(BridgeAnalyzerProfile profile,
            AnalyzerTypeMappingUpdate update) {
        if (update == null) {
            throw new IllegalArgumentException("Mapping update is required");
        }
        AnalyzerSiteBindingDraft draft = update.toDraft();
        Set<String> expectedTests = profile.testDefinitions().stream()
                .map(BridgeAnalyzerProfile.TestDefinition::analyzerCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> actualTests = draft.tests().stream().filter(row -> row != null && row.sourceRowKey() != null)
                .map(AnalyzerSiteBindingTestDraft::sourceRowKey).collect(Collectors.toCollection(LinkedHashSet::new));
        if (draft.tests().size() != expectedTests.size() || !actualTests.equals(expectedTests)) {
            throw new IllegalArgumentException("Mapping update test rows must exactly match profile revision");
        }

        Set<ResultSourceKey> expectedResults = profile.testDefinitions().stream()
                .flatMap(definition -> definition.resultValues().stream()
                        .map(value -> new ResultSourceKey(definition.analyzerCode(), value)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<ResultSourceKey> actualResults = draft.results().stream()
                .filter(row -> row != null && row.sourceRowKey() != null && row.rawValue() != null)
                .map(row -> new ResultSourceKey(row.sourceRowKey(), row.rawValue()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (draft.results().size() != expectedResults.size() || !actualResults.equals(expectedResults)) {
            throw new IllegalArgumentException("Mapping update result rows must exactly match profile revision");
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
            List<AnalyzerMappingCatalogService.TestOption> activeTests,
            Map<String, AnalyzerMappingCatalogService.TestOption> activeTestsById) {
        AnalyzerSiteBindingMappingState state = current == null ? AnalyzerSiteBindingMappingState.UNRESOLVED
                : current.getMappingState();
        String testId = current == null ? null : current.getTestId();
        AnalyzerMappingCatalogService.TestOption selected = testId == null ? null : activeTestsById.get(testId);
        AnalyzerMappingCatalogService.TestOption suggested = state == AnalyzerSiteBindingMappingState.UNRESOLVED
                ? uniqueSuggestion(definition, activeTests)
                : null;
        Map<String, AnalyzerMappingCatalogService.ResultOption> activeResults = selected == null ? Map.of()
                : mappingCatalogService.getActiveResultOptions(selected.id()).stream()
                        .collect(Collectors.toMap(AnalyzerMappingCatalogService.ResultOption::id, Function.identity()));
        List<AnalyzerTypeMappingView.ResultRow> results = definition.resultValues().stream().map(rawValue -> {
            AnalyzerSiteBindingResult result = currentResults
                    .get(new ResultSourceKey(definition.analyzerCode(), rawValue));
            AnalyzerSiteBindingMappingState resultState = result == null ? AnalyzerSiteBindingMappingState.UNRESOLVED
                    : result.getMappingState();
            String optionId = result == null ? null : result.getTestResultId();
            return new AnalyzerTypeMappingView.ResultRow(rawValue, resultState, optionId,
                    optionId == null ? null : activeResults.get(optionId));
        }).toList();
        return new AnalyzerTypeMappingView.TestRow(definition.analyzerCode(), definition.analyzerCode(),
                definition.aliases(), definition.testNameHint(), definition.loinc(), definition.unit(),
                definition.resultType(), definition.normalizedCoding(), state, testId, selected, suggested, results);
    }

    private static AnalyzerMappingCatalogService.TestOption uniqueSuggestion(
            BridgeAnalyzerProfile.TestDefinition definition,
            List<AnalyzerMappingCatalogService.TestOption> activeTests) {
        Map<String, AnalyzerMappingCatalogService.TestOption> candidates = new LinkedHashMap<>();
        for (AnalyzerMappingCatalogService.TestOption option : activeTests) {
            if (matches(definition, option)) {
                candidates.put(option.id(), option);
            }
        }
        return candidates.size() == 1 ? candidates.values().iterator().next() : null;
    }

    private static boolean matches(BridgeAnalyzerProfile.TestDefinition definition,
            AnalyzerMappingCatalogService.TestOption option) {
        if (option.loincCodes().contains(definition.loinc()) || equalText(option.code(), definition.analyzerCode())
                || equalText(option.name(), definition.testNameHint())) {
            return true;
        }
        return definition.aliases().stream().anyMatch(alias -> equalText(option.code(), alias));
    }

    private static boolean equalText(String left, String right) {
        return left != null && right != null
                && left.trim().toLowerCase(Locale.ROOT).equals(right.trim().toLowerCase(Locale.ROOT));
    }

    private record ResultSourceKey(String sourceRowKey, String rawValue) {
    }
}
