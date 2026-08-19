package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.openelisglobal.analyzer.dao.AnalyzerProfileBindingDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnalyzerTypeCatalogServiceImpl implements AnalyzerTypeCatalogService {

    private static final String SCHEMA_VERSION = "1.0";

    private final BridgeProfileCatalogService bridgeCatalogService;
    private final AnalyzerProfileBindingDAO bindingDAO;

    @Autowired
    public AnalyzerTypeCatalogServiceImpl(BridgeProfileCatalogService bridgeCatalogService,
            AnalyzerProfileBindingDAO bindingDAO) {
        this.bridgeCatalogService = bridgeCatalogService;
        this.bindingDAO = bindingDAO;
    }

    @Override
    public AnalyzerTypeCatalogView getCatalog() {
        BridgeProfileCatalog bridgeCatalog = bridgeCatalogService.getCatalog();
        Map<ProfileRevisionKey, AnalyzerProfileBinding> bindings = bindingDAO.getAll().stream()
                .collect(Collectors.toMap(
                        binding -> new ProfileRevisionKey(binding.getProfileId(), binding.getProfileRevision()),
                        Function.identity()));
        Map<String, Long> usageByBindingId = new HashMap<>();
        bindings.values().forEach(binding -> usageByBindingId.put(binding.getId(),
                bindingDAO.countAnalyzersByBindingId(binding.getId())));

        List<AnalyzerTypeCatalogView.TypeSummary> types = bridgeCatalog.profiles().stream()
                .map(revision -> summarize(revision, bindings, usageByBindingId))
                .sorted(Comparator.comparing(summary -> summary.displayName().toLowerCase(Locale.ROOT))).toList();

        int inUse = (int) types.stream().filter(type -> type.usedBy() > 0).count();
        int deactivated = (int) types.stream().filter(type -> "INACTIVE".equals(type.status())).count();
        int needsAttention = (int) types.stream().filter(type -> "ACTIVE".equals(type.status()))
                .filter(type -> !"READY".equals(type.readiness())).count();
        AnalyzerTypeCatalogView.CatalogSummary summary = new AnalyzerTypeCatalogView.CatalogSummary(types.size(), inUse,
                needsAttention, deactivated);
        return new AnalyzerTypeCatalogView(SCHEMA_VERSION, bridgeCatalog.catalogFingerprint(), summary, types);
    }

    private AnalyzerTypeCatalogView.TypeSummary summarize(BridgeProfileCatalog.ProfileRevision revision,
            Map<ProfileRevisionKey, AnalyzerProfileBinding> bindings, Map<String, Long> usageByBindingId) {
        JsonNode profile = revision.profile();
        String profileId = profile.path("profileId").asText();
        int profileRevision = profile.path("revision").asInt();
        AnalyzerProfileBinding binding = bindings.get(new ProfileRevisionKey(profileId, profileRevision));
        int testTotal = profile.path("tests").size();
        int resultTotal = 0;
        for (JsonNode test : profile.path("tests")) {
            resultTotal += test.path("resultValues").size();
        }
        String status = profile.path("status").asText();
        String readiness = readiness(status, testTotal);
        JsonNode identity = profile.path("identity");
        JsonNode lineage = profile.path("lineage");
        JsonNode publication = revision.publication();
        return new AnalyzerTypeCatalogView.TypeSummary(profileId, profileRevision,
                profile.path("revisionFingerprint").asText(), profile.path("displayName").asText(),
                nullableText(identity, "manufacturer"), nullableText(identity, "model"),
                profile.path("source").asText(), status, profile.path("protocol").asText(),
                nullableText(lineage, "parentProfileId"), nullableInteger(lineage, "parentRevision"),
                binding == null ? null : binding.getId(), mappingSummary(testTotal), mappingSummary(resultTotal),
                binding == null ? 0 : usageByBindingId.getOrDefault(binding.getId(), 0L), readiness,
                nullableText(publication, "action"), nullableText(publication, "actor"),
                nullableText(publication, "markedAt"));
    }

    private static AnalyzerTypeCatalogView.MappingSummary mappingSummary(int total) {
        return new AnalyzerTypeCatalogView.MappingSummary(0, total, total == 0 ? "NOT_APPLICABLE" : "NOT_STARTED");
    }

    private static String readiness(String status, int testTotal) {
        if (!"ACTIVE".equals(status)) {
            return "DEACTIVATED";
        }
        if (testTotal == 0) {
            return "NEEDS_PROFILE_TESTS";
        }
        return "NEEDS_LOCAL_MAPPING";
    }

    private static String nullableText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() || value.asText().isBlank() ? null : value.asText();
    }

    private static Integer nullableInteger(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.canConvertToInt() ? value.asInt() : null;
    }

    private record ProfileRevisionKey(String profileId, int revision) {
    }
}
