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
import org.openelisglobal.analyzer.valueholder.CommunicationMode;
import org.openelisglobal.analyzer.valueholder.ProtocolVersion;
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
        BridgeAnalyzerProfile profile = BridgeAnalyzerProfile.from(revision.profile());
        AnalyzerProfileBinding binding = bindings.get(new ProfileRevisionKey(profile.profileId(), profile.revision()));
        int testTotal = profile.testDefinitions().size();
        int resultTotal = profile.testDefinitions().stream().mapToInt(test -> test.resultValues().size()).sum();
        String status = profile.status();
        String readiness = readiness(status, testTotal);
        return new AnalyzerTypeCatalogView.TypeSummary(profile.profileId(), profile.revision(),
                profile.revisionFingerprint(), profile.displayName(), profile.manufacturer(), profile.model(),
                profile.source(), status, profile.protocol(), instanceDefaults(profile), profile.parentProfileId(),
                profile.parentRevision(), binding == null ? null : binding.getId(), mappingSummary(testTotal),
                mappingSummary(resultTotal), binding == null ? 0 : usageByBindingId.getOrDefault(binding.getId(), 0L),
                readiness, nullableText(revision.publication(), "action"),
                nullableText(revision.publication(), "actor"), nullableText(revision.publication(), "markedAt"));
    }

    private static AnalyzerTypeCatalogView.InstanceDefaults instanceDefaults(BridgeAnalyzerProfile profile) {
        ProtocolVersion protocolVersion = profile.resolvedProtocolVersion();
        if (!"FILE".equals(profile.protocol()) && protocolVersion == null) {
            throw new IllegalArgumentException(
                    "Bridge profile " + profile.profileId() + " has an unsupported protocol version");
        }
        CommunicationMode communicationMode = profile.resolvedCommunicationMode();
        if (!"FILE".equals(profile.protocol()) && communicationMode == null) {
            throw new IllegalArgumentException(
                    "Bridge profile " + profile.profileId() + " has an unsupported communication mode");
        }
        return new AnalyzerTypeCatalogView.InstanceDefaults(protocolVersion == null ? null : protocolVersion.name(),
                communicationMode == null ? null : communicationMode.name(), profile.instanceDefaults().port());
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

    private record ProfileRevisionKey(String profileId, int revision) {
    }
}
