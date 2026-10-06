package org.openelisglobal.analyzer.service;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
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

    public AnalyzerAdoptionServiceImpl(AnalyzerMappingService mappingService,
            BridgeProfileCatalogService profileCatalogService, AnalyzerMappingDefaults mappingDefaults,
            AnalyzerMappingCatalogService catalogService, AnalyzerResultsService analyzerResultsService) {
        this.mappingService = mappingService;
        this.profileCatalogService = profileCatalogService;
        this.mappingDefaults = mappingDefaults;
        this.catalogService = catalogService;
        this.analyzerResultsService = analyzerResultsService;
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

    private BridgeAnalyzerProfile profile(String profileId, int revision) {
        BridgeProfileCatalog.ProfileRevision found = profileCatalogService.getProfile(profileId, revision);
        if (found == null) {
            throw new IllegalArgumentException(profileId + " revision " + revision + " was not found");
        }
        return BridgeAnalyzerProfile.from(found.profile());
    }
}
