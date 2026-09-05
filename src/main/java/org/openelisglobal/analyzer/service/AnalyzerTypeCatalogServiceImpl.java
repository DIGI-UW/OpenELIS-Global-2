package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnalyzerTypeCatalogServiceImpl implements AnalyzerTypeCatalogService {

    private static final String SCHEMA_VERSION = "1.0";

    private final BridgeProfileCatalogService bridgeCatalogService;

    @Autowired
    public AnalyzerTypeCatalogServiceImpl(BridgeProfileCatalogService bridgeCatalogService) {
        this.bridgeCatalogService = bridgeCatalogService;
    }

    @Override
    public AnalyzerTypeCatalogView getCatalog() {
        BridgeProfileCatalog bridgeCatalog = bridgeCatalogService.getCatalog();
        List<AnalyzerTypeCatalogView.TypeSummary> types = bridgeCatalog.profiles().stream().map(this::summarize)
                .sorted(Comparator.comparing(summary -> summary.displayName().toLowerCase(Locale.ROOT))).toList();

        int deactivated = (int) types.stream().filter(type -> "INACTIVE".equals(type.status())).count();
        int needsAttention = (int) types.stream().filter(type -> "ACTIVE".equals(type.status()))
                .filter(type -> !"READY".equals(type.readiness())).count();
        AnalyzerTypeCatalogView.CatalogSummary summary = new AnalyzerTypeCatalogView.CatalogSummary(types.size(), 0,
                needsAttention, deactivated);
        return new AnalyzerTypeCatalogView(SCHEMA_VERSION, bridgeCatalog.catalogFingerprint(), summary, types);
    }

    @Override
    public AnalyzerTypeCatalogView.TypeSummary getType(String profileId, int revision) {
        return summarize(bridgeCatalogService.getProfile(profileId, revision));
    }

    private AnalyzerTypeCatalogView.TypeSummary summarize(BridgeProfileCatalog.ProfileRevision revision) {
        BridgeAnalyzerProfile profile = BridgeAnalyzerProfile.from(revision.profile());
        int testTotal = profile.testDefinitions().size();
        int resultTotal = profile.testDefinitions().stream().mapToInt(test -> test.resultValues().size()).sum();
        AnalyzerTypeCatalogView.MappingSummary testMappings = emptyMappingSummary(testTotal);
        AnalyzerTypeCatalogView.MappingSummary resultMappings = emptyMappingSummary(resultTotal);
        String readiness = readiness(profile.status(), testTotal);
        return new AnalyzerTypeCatalogView.TypeSummary(profile.profileId(), profile.revision(),
                profile.revisionFingerprint(), profile.displayName(), profile.manufacturer(), profile.model(),
                profile.source(), profile.status(), profile.protocol(), instanceDefaults(profile),
                profile.parentProfileId(), profile.parentRevision(), null, testMappings, resultMappings, 0L, readiness,
                nullableText(revision.publication(), "action"), nullableText(revision.publication(), "actor"),
                nullableText(revision.publication(), "markedAt"));
    }

    private static AnalyzerTypeCatalogView.InstanceDefaults instanceDefaults(BridgeAnalyzerProfile profile) {
        return new AnalyzerTypeCatalogView.InstanceDefaults(profile.protocolVersion(), profile.communicationMode(),
                profile.instanceDefaults().port());
    }

    private static AnalyzerTypeCatalogView.MappingSummary emptyMappingSummary(int total) {
        return new AnalyzerTypeCatalogView.MappingSummary(0, total, total == 0 ? "NOT_APPLICABLE" : "NOT_STARTED");
    }

    private static String readiness(String status, int testTotal) {
        if (!"ACTIVE".equals(status)) {
            return "DEACTIVATED";
        }
        return testTotal == 0 ? "NEEDS_PROFILE_TESTS" : "NEEDS_LOCAL_MAPPING";
    }

    private static String nullableText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() || value.asText().isBlank() ? null : value.asText();
    }
}
