package org.openelisglobal.analyzer.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingOrigin;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyzerAdoptionServiceImpl implements AnalyzerAdoptionService {

    private final AnalyzerMappingService mappingService;
    private final BridgeProfileCatalogService profileCatalogService;
    private final AnalyzerMappingDefaults mappingDefaults;
    private final AnalyzerMappingCatalogService catalogService;
    private final AnalyzerResultsService analyzerResultsService;
    private final AnalyzerService analyzerService;

    public AnalyzerAdoptionServiceImpl(AnalyzerMappingService mappingService,
            BridgeProfileCatalogService profileCatalogService, AnalyzerMappingDefaults mappingDefaults,
            AnalyzerMappingCatalogService catalogService, AnalyzerResultsService analyzerResultsService,
            AnalyzerService analyzerService) {
        this.mappingService = mappingService;
        this.profileCatalogService = profileCatalogService;
        this.mappingDefaults = mappingDefaults;
        this.catalogService = catalogService;
        this.analyzerResultsService = analyzerResultsService;
        this.analyzerService = analyzerService;
    }

    @Override
    @Transactional
    public AnalyzerMappingSnapshot adopt(String analyzerId, int toRevision, AnalyzerMappingDraft decisions,
            String actor) {
        Analyzer analyzer = analyzerService.findByIdForUpdate(analyzerId)
                .orElseThrow(() -> new IllegalArgumentException("Analyzer not found: " + analyzerId));
        AdoptionPlan plan = prepareAdoption(analyzerId, toRevision);
        Map<AnalyzerMappingRowKey, AnalyzerMappingAdoption.Decision> decided = AnalyzerMappingAdoption
                .decisions(decisions);
        List<AnalyzerMappingAdoption.Row> kept = plan.rows().stream()
                .filter(row -> row.bucket() != AnalyzerMappingAdoption.Bucket.RETIRED).toList();
        for (AnalyzerMappingAdoption.Row row : kept) {
            if (row.blockReason() == AnalyzerMappingAdoption.BlockReason.HELD_RESULTS) {
                throw new IllegalArgumentException(row.key().label() + " still has held results from revision "
                        + plan.fromRevision() + "; resolve them before adopting revision " + toRevision);
            }
        }
        Set<AnalyzerMappingRowKey> keptKeys = kept.stream().map(AnalyzerMappingAdoption.Row::key)
                .collect(Collectors.toSet());
        if (!decided.keySet().equals(keptKeys)) {
            String missing = kept.stream().map(AnalyzerMappingAdoption.Row::key)
                    .filter(key -> !decided.containsKey(key)).map(AnalyzerMappingRowKey::label)
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException("Adoption needs one decision for each record the revision keeps"
                    + (missing.isEmpty() ? "" : "; missing " + missing));
        }
        List<AnalyzerMappingTestDraft> tests = new ArrayList<>();
        List<AnalyzerMappingResultDraft> results = new ArrayList<>();
        for (AnalyzerMappingAdoption.Row row : kept) {
            AnalyzerMappingAdoption.Decision decision = decided.get(row.key());
            if (row.blockReason() == AnalyzerMappingAdoption.BlockReason.INACTIVE_TEST
                    && AnalyzerMappingAdoption.sameDecision(decision, row.current())) {
                throw new IllegalArgumentException(row.key().label()
                        + " is mapped to a test that is no longer active; choose another test before adopting");
            }
            tests.add(withOrigin(decision.test(), originFor(decision.test(), row.proposed())));
            for (AnalyzerMappingResultDraft result : decision.results()) {
                results.add(withOrigin(result, originFor(result, row.proposed())));
            }
        }
        return mappingService.adoptRevision(analyzer, toRevision, new AnalyzerMappingDraft(tests, results),
                requireActor(actor));
    }

    @Override
    @Transactional(readOnly = true)
    public AdoptionPlan prepareAdoption(String analyzerId, int toRevision) {
        AnalyzerMappingSnapshot current = mappingService.findLatestByAnalyzerId(analyzerId)
                .orElseThrow(() -> new IllegalArgumentException("Analyzer has no mapping: " + analyzerId));
        String profileId = current.mapping().getProfileId();
        int fromRevision = current.mapping().getProfileRevision();
        if (toRevision <= fromRevision) {
            throw new IllegalArgumentException(
                    "Adoption moves to a newer revision of " + profileId + " than " + fromRevision);
        }
        BridgeAnalyzerProfile from = profile(profileId, fromRevision);
        BridgeAnalyzerProfile to = profile(profileId, toRevision);
        if (!"ACTIVE".equals(to.status())) {
            throw new IllegalArgumentException(profileId + " revision " + toRevision + " is not active");
        }
        Set<String> activeTestIds = catalogService.searchActiveTests(null).stream()
                .map(AnalyzerMappingCatalogService.TestOption::id).collect(Collectors.toSet());
        Set<String> inactiveTargets = current.tests().stream().map(row -> row.getTestId()).filter(Objects::nonNull)
                .filter(testId -> !activeTestIds.contains(testId)).collect(Collectors.toSet());
        Set<AnalyzerMappingRowKey> heldRecords = analyzerResultsService.findHeldMappingResultsByAnalyzer(analyzerId)
                .stream().filter(held -> Objects.equals(held.getSourceProfileRevision(), fromRevision))
                .filter(held -> held.getRawTestCode() != null)
                .map(held -> new AnalyzerMappingRowKey(held.getRawTestCode(), held.getRawSubIdentity()))
                .collect(Collectors.toSet());
        return new AdoptionPlan(analyzerId, profileId, fromRevision, toRevision, AnalyzerMappingAdoption.plan(from, to,
                AnalyzerMappingDraft.of(current), mappingDefaults.resolve(to), inactiveTargets, heldRecords));
    }

    /**
     * The proposal's origin when the operator kept its decision, otherwise an
     * override.
     */
    private static AnalyzerMappingOrigin originFor(AnalyzerMappingTestDraft decision,
            AnalyzerMappingAdoption.Decision proposed) {
        AnalyzerMappingTestDraft proposal = proposed == null ? null : proposed.test();
        boolean kept = proposal != null && decision.mappingState() == proposal.mappingState()
                && Objects.equals(decision.testId(), proposal.testId())
                && Objects.equals(decision.componentId(), proposal.componentId())
                && Objects.equals(decision.callComponentId(), proposal.callComponentId());
        return kept ? originOf(proposal.origin()) : AnalyzerMappingOrigin.OVERRIDE;
    }

    private static AnalyzerMappingOrigin originFor(AnalyzerMappingResultDraft decision,
            AnalyzerMappingAdoption.Decision proposed) {
        AnalyzerMappingResultDraft proposal = proposed == null ? null
                : proposed.results().stream().filter(result -> result.rawValue().equals(decision.rawValue()))
                        .findFirst().orElse(null);
        boolean kept = proposal != null && decision.mappingState() == proposal.mappingState()
                && Objects.equals(decision.testResultId(), proposal.testResultId());
        return kept ? originOf(proposal.origin()) : AnalyzerMappingOrigin.OVERRIDE;
    }

    private static AnalyzerMappingOrigin originOf(AnalyzerMappingOrigin origin) {
        return origin == null ? AnalyzerMappingOrigin.DEFAULT : origin;
    }

    private static AnalyzerMappingTestDraft withOrigin(AnalyzerMappingTestDraft row, AnalyzerMappingOrigin origin) {
        return new AnalyzerMappingTestDraft(row.sourceRowKey(), row.mappingState(), row.testId(), row.componentId(),
                row.unresolvedReason(), origin, row.subIdentity(), row.callComponentId());
    }

    private static AnalyzerMappingResultDraft withOrigin(AnalyzerMappingResultDraft row, AnalyzerMappingOrigin origin) {
        return new AnalyzerMappingResultDraft(row.sourceRowKey(), row.rawValue(), row.mappingState(),
                row.testResultId(), row.unresolvedReason(), origin, row.subIdentity());
    }

    private static String requireActor(String actor) {
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("actor is required");
        }
        return actor.trim();
    }

    private BridgeAnalyzerProfile profile(String profileId, int revision) {
        BridgeProfileCatalog.ProfileRevision found = profileCatalogService.getProfile(profileId, revision);
        if (found == null) {
            throw new IllegalArgumentException(profileId + " revision " + revision + " was not found");
        }
        return BridgeAnalyzerProfile.from(found.profile());
    }
}
