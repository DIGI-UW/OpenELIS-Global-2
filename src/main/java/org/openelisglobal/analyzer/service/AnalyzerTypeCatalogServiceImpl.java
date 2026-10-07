package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.openelisglobal.analyzer.dao.AnalyzerMappingDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerMapping;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnalyzerTypeCatalogServiceImpl implements AnalyzerTypeCatalogService {

    private static final String SCHEMA_VERSION = "1.0";

    private final BridgeProfileCatalogService bridgeCatalogService;
    private final AnalyzerMappingDAO mappingDAO;
    private final AnalyzerMappingDefaults mappingDefaults;
    private final AnalyzerMappingCatalogService mappingCatalogService;

    @Autowired
    public AnalyzerTypeCatalogServiceImpl(BridgeProfileCatalogService bridgeCatalogService,
            AnalyzerMappingDAO mappingDAO, AnalyzerMappingDefaults mappingDefaults,
            AnalyzerMappingCatalogService mappingCatalogService) {
        this.bridgeCatalogService = bridgeCatalogService;
        this.mappingDAO = mappingDAO;
        this.mappingDefaults = mappingDefaults;
        this.mappingCatalogService = mappingCatalogService;
    }

    @Override
    public AnalyzerTypeCatalogView getCatalog() {
        BridgeProfileCatalog bridgeCatalog = bridgeCatalogService.getCatalog();
        Map<String, List<Analyzer>> analyzersByProfileId = bridgeCatalog.profiles().stream()
                .map(revision -> BridgeAnalyzerProfile.from(revision.profile()).profileId()).distinct()
                .collect(Collectors.toMap(Function.identity(), this::affectedAnalyzers));
        List<AnalyzerMappingCatalogService.TestOption> activeTests = mappingCatalogService.searchActiveTests(null);

        List<AnalyzerTypeCatalogView.TypeSummary> types = bridgeCatalog.profiles().stream().map(revision -> {
            BridgeAnalyzerProfile profile = BridgeAnalyzerProfile.from(revision.profile());
            return summarize(revision, analyzersByProfileId.getOrDefault(profile.profileId(), List.of()), activeTests);
        }).sorted(Comparator.comparing(summary -> summary.displayName().toLowerCase(Locale.ROOT))).toList();

        int inUse = (int) types.stream().filter(type -> type.usedBy() > 0).count();
        int deactivated = (int) types.stream().filter(type -> "INACTIVE".equals(type.status())).count();
        int needsAttention = (int) types.stream().filter(type -> "ACTIVE".equals(type.status()))
                .filter(type -> !"READY".equals(type.readiness())).count();
        AnalyzerTypeCatalogView.CatalogSummary summary = new AnalyzerTypeCatalogView.CatalogSummary(types.size(), inUse,
                needsAttention, deactivated);
        return new AnalyzerTypeCatalogView(SCHEMA_VERSION, bridgeCatalog.catalogFingerprint(), summary, types,
                bridgeCatalog.issues());
    }

    @Override
    public AnalyzerTypeCatalogView.TypeSummary getType(String profileId, int revision) {
        BridgeProfileCatalog.ProfileRevision profileRevision = bridgeCatalogService.getProfile(profileId, revision);
        BridgeAnalyzerProfile profile = BridgeAnalyzerProfile.from(profileRevision.profile());
        return summarize(profileRevision, affectedAnalyzers(profile.profileId()),
                mappingCatalogService.searchActiveTests(null));
    }

    /**
     * What a new analyzer on this profile revision would bind by default against
     * the catalog as it is now. It is a preview of defaults; nothing is saved, and
     * each analyzer keeps its own mapping.
     */
    private AnalyzerTypeCatalogView.TypeSummary summarize(BridgeProfileCatalog.ProfileRevision revision,
            List<Analyzer> analyzers, List<AnalyzerMappingCatalogService.TestOption> activeTests) {
        BridgeAnalyzerProfile profile = BridgeAnalyzerProfile.from(revision.profile());
        List<AnalyzerTypeCatalogView.AffectedAnalyzer> affectedAnalyzers = analyzers.stream()
                .map(analyzer -> affectedAnalyzer(analyzer, profile)).toList();
        String status = profile.status();
        AnalyzerMappingDraft defaults = mappingDefaults.resolve(profile, activeTests);
        AnalyzerTypeCatalogView.MappingSummary testMappings = mappingSummary(profile.testDefinitions().size(),
                defaults.tests().stream().filter(row -> row.mappingState() == AnalyzerMappingState.BOUND).count());
        int resultTotal = profile.testDefinitions().stream().mapToInt(test -> test.resultValues().size()).sum();
        AnalyzerTypeCatalogView.MappingSummary resultMappings = mappingSummary(resultTotal,
                defaults.results().stream().filter(row -> row.mappingState() == AnalyzerMappingState.BOUND).count());
        String readiness = readiness(status, testMappings, resultMappings);
        return new AnalyzerTypeCatalogView.TypeSummary(profile.profileId(), profile.revision(),
                profile.revisionFingerprint(), profile.displayName(), profile.manufacturer(), profile.model(),
                profile.source(), status, profile.protocol(), profile.protocolVersion(), profile.communicationMode(),
                profile.parentProfileId(), profile.parentRevision(), testMappings, resultMappings,
                affectedAnalyzers.size(), readiness, nullableText(revision.publication(), "action"),
                nullableText(revision.publication(), "actor"), nullableText(revision.publication(), "markedAt"),
                affectedAnalyzers);
    }

    private List<Analyzer> affectedAnalyzers(String profileId) {
        return mappingDAO.findAnalyzersInForceOnProfile(profileId);
    }

    private AnalyzerTypeCatalogView.AffectedAnalyzer affectedAnalyzer(Analyzer analyzer,
            BridgeAnalyzerProfile profile) {
        AnalyzerMapping inForce = analyzer.getMapping();
        boolean newerProfileRevision = inForce.getProfileRevision() < profile.revision();
        boolean newerMappingRevision = mappingDAO.findLatestByAnalyzerId(analyzer.getId())
                .map(latest -> latest.getRevisionNumber() > inForce.getRevisionNumber()).orElse(false);
        return new AnalyzerTypeCatalogView.AffectedAnalyzer(analyzer.getId(), analyzer.getName(), analyzer.isActive(),
                inForce.getProfileRevision(), inForce.getRevisionNumber(), newerProfileRevision, newerMappingRevision);
    }

    private static AnalyzerTypeCatalogView.MappingSummary mappingSummary(int total, long bound) {
        if (total == 0) {
            return new AnalyzerTypeCatalogView.MappingSummary(0, 0, 0, "NOT_APPLICABLE");
        }
        String state = bound == 0 ? "NOT_STARTED" : bound == total ? "COMPLETE" : "INCOMPLETE";
        return new AnalyzerTypeCatalogView.MappingSummary(Math.toIntExact(bound), 0, total, state);
    }

    private static String readiness(String status, AnalyzerTypeCatalogView.MappingSummary testMappings,
            AnalyzerTypeCatalogView.MappingSummary resultMappings) {
        if (!"ACTIVE".equals(status)) {
            return "DEACTIVATED";
        }
        if (testMappings.total() == 0) {
            return "NEEDS_PROFILE_TESTS";
        }
        if ("COMPLETE".equals(testMappings.state())
                && ("COMPLETE".equals(resultMappings.state()) || "NOT_APPLICABLE".equals(resultMappings.state()))) {
            return "READY";
        }
        return "NEEDS_LOCAL_MAPPING";
    }

    private static String nullableText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() || value.asText().isBlank() ? null : value.asText();
    }

}
