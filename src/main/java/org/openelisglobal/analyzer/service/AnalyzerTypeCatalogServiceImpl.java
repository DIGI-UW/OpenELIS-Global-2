package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Comparator;
import java.util.HashMap;
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
    private final AnalyzerSiteBindingService siteBindingService;

    @Autowired
    public AnalyzerTypeCatalogServiceImpl(BridgeProfileCatalogService bridgeCatalogService,
            AnalyzerProfileBindingDAO bindingDAO, AnalyzerSiteBindingService siteBindingService) {
        this.bridgeCatalogService = bridgeCatalogService;
        this.bindingDAO = bindingDAO;
        this.siteBindingService = siteBindingService;
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
        AnalyzerSiteBindingSnapshot siteBinding = binding == null ? null
                : siteBindingService.findCurrentByProfileBindingId(binding.getId()).orElse(null);
        int testTotal = profile.testDefinitions().size();
        int resultTotal = profile.testDefinitions().stream().mapToInt(test -> test.resultValues().size()).sum();
        String status = profile.status();
        AnalyzerTypeCatalogView.MappingSummary testMappings = testMappingSummary(profile, siteBinding);
        AnalyzerTypeCatalogView.MappingSummary resultMappings = resultMappingSummary(profile, siteBinding);
        String readiness = readiness(status, testMappings, resultMappings);
        return new AnalyzerTypeCatalogView.TypeSummary(profile.profileId(), profile.revision(),
                profile.revisionFingerprint(), profile.displayName(), profile.manufacturer(), profile.model(),
                profile.source(), status, profile.protocol(), instanceDefaults(profile), profile.parentProfileId(),
                profile.parentRevision(), siteBinding == null ? null : siteBinding.binding().getId(), testMappings,
                resultMappings, binding == null ? 0 : usageByBindingId.getOrDefault(binding.getId(), 0L), readiness,
                nullableText(revision.publication(), "action"), nullableText(revision.publication(), "actor"),
                nullableText(revision.publication(), "markedAt"));
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

    private static AnalyzerTypeCatalogView.MappingSummary testMappingSummary(BridgeAnalyzerProfile profile,
            AnalyzerSiteBindingSnapshot siteBinding) {
        Map<String, AnalyzerSiteBindingMappingState> states = Optional.ofNullable(siteBinding)
                .map(AnalyzerSiteBindingSnapshot::tests).orElse(List.of()).stream().collect(Collectors
                        .toMap(row -> row.getId().getSourceRowKey(), AnalyzerSiteBindingTest::getMappingState));
        long resolved = profile.testDefinitions().stream().map(BridgeAnalyzerProfile.TestDefinition::analyzerCode)
                .map(states::get).filter(AnalyzerTypeCatalogServiceImpl::isResolved).count();
        return mappingSummary(profile.testDefinitions().size(), resolved);
    }

    private static AnalyzerTypeCatalogView.MappingSummary resultMappingSummary(BridgeAnalyzerProfile profile,
            AnalyzerSiteBindingSnapshot siteBinding) {
        Map<ResultSourceKey, AnalyzerSiteBindingMappingState> states = Optional.ofNullable(siteBinding)
                .map(AnalyzerSiteBindingSnapshot::results).orElse(List.of()).stream()
                .collect(Collectors.toMap(
                        row -> new ResultSourceKey(row.getId().getSourceRowKey(), row.getId().getRawValue()),
                        AnalyzerSiteBindingResult::getMappingState));
        int total = profile.testDefinitions().stream().mapToInt(test -> test.resultValues().size()).sum();
        long resolved = profile.testDefinitions().stream()
                .flatMap(test -> test.resultValues().stream()
                        .map(value -> new ResultSourceKey(test.analyzerCode(), value)))
                .map(states::get).filter(AnalyzerTypeCatalogServiceImpl::isResolved).count();
        return mappingSummary(total, resolved);
    }

    private static AnalyzerTypeCatalogView.MappingSummary mappingSummary(int total, long resolved) {
        if (total == 0) {
            return new AnalyzerTypeCatalogView.MappingSummary(0, 0, "NOT_APPLICABLE");
        }
        String state = resolved == 0 ? "NOT_STARTED" : resolved == total ? "COMPLETE" : "INCOMPLETE";
        return new AnalyzerTypeCatalogView.MappingSummary(Math.toIntExact(resolved), total, state);
    }

    private static boolean isResolved(AnalyzerSiteBindingMappingState state) {
        return state != null && state != AnalyzerSiteBindingMappingState.UNRESOLVED;
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

    private record ProfileRevisionKey(String profileId, int revision) {
    }

    private record ResultSourceKey(String sourceRowKey, String rawValue) {
    }
}
