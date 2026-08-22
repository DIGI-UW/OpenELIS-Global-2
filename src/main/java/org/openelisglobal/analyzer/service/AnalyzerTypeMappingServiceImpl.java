package org.openelisglobal.analyzer.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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

    public AnalyzerTypeMappingServiceImpl(BridgeProfileCatalogService bridgeProfileCatalogService,
            AnalyzerProfileBindingDAO profileBindingDAO, AnalyzerSiteBindingService siteBindingService,
            AnalyzerMappingCatalogService mappingCatalogService) {
        this.bridgeProfileCatalogService = bridgeProfileCatalogService;
        this.profileBindingDAO = profileBindingDAO;
        this.siteBindingService = siteBindingService;
        this.mappingCatalogService = mappingCatalogService;
    }

    @Override
    public AnalyzerTypeMappingView getMapping(String profileId, int profileRevision) {
        BridgeProfileCatalog.ProfileRevision revision = bridgeProfileCatalogService.getProfile(profileId,
                profileRevision);
        BridgeAnalyzerProfile profile = BridgeAnalyzerProfile.from(revision.profile());
        AnalyzerSiteBindingSnapshot binding = findBinding(profile.profileId(), profile.revision()).orElse(null);
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
