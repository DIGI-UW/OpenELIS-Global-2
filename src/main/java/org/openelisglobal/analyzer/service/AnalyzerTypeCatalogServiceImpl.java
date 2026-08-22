package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.openelisglobal.analyzer.dao.AnalyzerProfileBindingDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingRevision;
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
    private final AnalyzerMappingCatalogService mappingCatalogService;

    @Autowired
    public AnalyzerTypeCatalogServiceImpl(BridgeProfileCatalogService bridgeCatalogService,
            AnalyzerProfileBindingDAO bindingDAO, AnalyzerSiteBindingService siteBindingService,
            AnalyzerMappingCatalogService mappingCatalogService) {
        this.bridgeCatalogService = bridgeCatalogService;
        this.bindingDAO = bindingDAO;
        this.siteBindingService = siteBindingService;
        this.mappingCatalogService = mappingCatalogService;
    }

    @Override
    public AnalyzerTypeCatalogView getCatalog() {
        BridgeProfileCatalog bridgeCatalog = bridgeCatalogService.getCatalog();
        Map<ProfileRevisionKey, AnalyzerProfileBinding> bindings = bindingDAO.getAll().stream()
                .collect(Collectors.toMap(
                        binding -> new ProfileRevisionKey(binding.getProfileId(), binding.getProfileRevision()),
                        Function.identity()));
        Map<String, List<Analyzer>> analyzersByProfileId = bridgeCatalog.profiles().stream()
                .map(revision -> BridgeAnalyzerProfile.from(revision.profile()).profileId()).distinct()
                .collect(Collectors.toMap(Function.identity(), this::affectedAnalyzers));
        AnalyzerSiteBindingCatalogState catalogState = AnalyzerSiteBindingCatalogState.load(mappingCatalogService);

        List<AnalyzerTypeCatalogView.TypeSummary> types = bridgeCatalog.profiles().stream().map(revision -> {
            BridgeAnalyzerProfile profile = BridgeAnalyzerProfile.from(revision.profile());
            AnalyzerProfileBinding binding = bindings
                    .get(new ProfileRevisionKey(profile.profileId(), profile.revision()));
            return summarize(revision, binding, analyzersByProfileId.getOrDefault(profile.profileId(), List.of()),
                    catalogState);
        }).sorted(Comparator.comparing(summary -> summary.displayName().toLowerCase(Locale.ROOT))).toList();

        int inUse = (int) types.stream().filter(type -> type.usedBy() > 0).count();
        int deactivated = (int) types.stream().filter(type -> "INACTIVE".equals(type.status())).count();
        int needsAttention = (int) types.stream().filter(type -> "ACTIVE".equals(type.status()))
                .filter(type -> !"READY".equals(type.readiness())).count();
        AnalyzerTypeCatalogView.CatalogSummary summary = new AnalyzerTypeCatalogView.CatalogSummary(types.size(), inUse,
                needsAttention, deactivated);
        return new AnalyzerTypeCatalogView(SCHEMA_VERSION, bridgeCatalog.catalogFingerprint(), summary, types);
    }

    @Override
    public AnalyzerTypeCatalogView.TypeSummary getType(String profileId, int revision) {
        BridgeProfileCatalog.ProfileRevision profileRevision = bridgeCatalogService.getProfile(profileId, revision);
        BridgeAnalyzerProfile profile = BridgeAnalyzerProfile.from(profileRevision.profile());
        AnalyzerProfileBinding binding = bindingDAO.findByProfileIdAndRevision(profile.profileId(), profile.revision())
                .orElse(null);
        return summarize(profileRevision, binding, affectedAnalyzers(profile.profileId()),
                AnalyzerSiteBindingCatalogState.load(mappingCatalogService));
    }

    private AnalyzerTypeCatalogView.TypeSummary summarize(BridgeProfileCatalog.ProfileRevision revision,
            AnalyzerProfileBinding binding, List<Analyzer> analyzers, AnalyzerSiteBindingCatalogState catalogState) {
        BridgeAnalyzerProfile profile = BridgeAnalyzerProfile.from(revision.profile());
        AnalyzerSiteBindingSnapshot siteBinding = binding == null ? null
                : siteBindingService.findCurrentByProfileBindingId(binding.getId()).orElse(null);
        List<AnalyzerTypeCatalogView.AffectedAnalyzer> affectedAnalyzers = analyzers.stream()
                .map(analyzer -> affectedAnalyzer(analyzer, profile, siteBinding)).toList();
        int testTotal = profile.testDefinitions().size();
        int resultTotal = profile.testDefinitions().stream().mapToInt(test -> test.resultValues().size()).sum();
        String status = profile.status();
        AnalyzerSiteBindingCatalogState.Validation catalogValidation = catalogState.validate(siteBinding);
        AnalyzerTypeCatalogView.MappingSummary testMappings = testMappingSummary(profile, catalogValidation);
        AnalyzerTypeCatalogView.MappingSummary resultMappings = resultMappingSummary(profile, catalogValidation);
        String readiness = readiness(status, testMappings, resultMappings);
        return new AnalyzerTypeCatalogView.TypeSummary(profile.profileId(), profile.revision(),
                profile.revisionFingerprint(), profile.displayName(), profile.manufacturer(), profile.model(),
                profile.source(), status, profile.protocol(), instanceDefaults(profile), profile.parentProfileId(),
                profile.parentRevision(), siteBinding == null ? null : siteBinding.binding().getId(), testMappings,
                resultMappings, affectedAnalyzers.size(), readiness, nullableText(revision.publication(), "action"),
                nullableText(revision.publication(), "actor"), nullableText(revision.publication(), "markedAt"),
                affectedAnalyzers);
    }

    private List<Analyzer> affectedAnalyzers(String profileId) {
        return bindingDAO.findAnalyzersByProfileId(profileId);
    }

    private AnalyzerTypeCatalogView.AffectedAnalyzer affectedAnalyzer(Analyzer analyzer, BridgeAnalyzerProfile profile,
            AnalyzerSiteBindingSnapshot currentSiteBinding) {
        AnalyzerSiteBindingRevision pinnedMapping = analyzer.getSiteBindingRevision();
        int pinnedProfileRevision = pinnedMapping.getSiteBinding().getProfileBinding().getProfileRevision();
        int pinnedMappingRevision = pinnedMapping.getRevisionNumber();
        boolean newerProfileRevision = pinnedProfileRevision < profile.revision();
        boolean newerMappingRevision = pinnedProfileRevision == profile.revision() && currentSiteBinding != null
                && pinnedMappingRevision < currentSiteBinding.revision().getRevisionNumber();
        return new AnalyzerTypeCatalogView.AffectedAnalyzer(analyzer.getId(), analyzer.getName(), analyzer.isActive(),
                pinnedProfileRevision, pinnedMappingRevision, newerProfileRevision || newerMappingRevision);
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
            AnalyzerSiteBindingCatalogState.Validation catalogValidation) {
        long mapped = profile.testDefinitions().stream().map(BridgeAnalyzerProfile.TestDefinition::analyzerCode)
                .filter(catalogValidation::isCurrentBoundTest).count();
        long excluded = profile.testDefinitions().stream().map(BridgeAnalyzerProfile.TestDefinition::analyzerCode)
                .filter(catalogValidation::isCurrentExcludedTest).count();
        return mappingSummary(profile.testDefinitions().size(), mapped, excluded);
    }

    private static AnalyzerTypeCatalogView.MappingSummary resultMappingSummary(BridgeAnalyzerProfile profile,
            AnalyzerSiteBindingCatalogState.Validation catalogValidation) {
        int total = profile.testDefinitions().stream().mapToInt(test -> test.resultValues().size()).sum();
        long mapped = profile.testDefinitions().stream()
                .flatMap(test -> test.resultValues().stream()
                        .map(value -> catalogValidation.isCurrentBoundResult(test.analyzerCode(), value)))
                .filter(Boolean::booleanValue).count();
        long excluded = profile.testDefinitions().stream()
                .flatMap(test -> test.resultValues().stream()
                        .map(value -> catalogValidation.isCurrentExcludedResult(test.analyzerCode(), value)))
                .filter(Boolean::booleanValue).count();
        return mappingSummary(total, mapped, excluded);
    }

    private static AnalyzerTypeCatalogView.MappingSummary mappingSummary(int total, long mapped, long excluded) {
        if (total == 0) {
            return new AnalyzerTypeCatalogView.MappingSummary(0, 0, 0, "NOT_APPLICABLE");
        }
        long resolved = mapped + excluded;
        String state = resolved == 0 ? "NOT_STARTED" : resolved == total ? "COMPLETE" : "INCOMPLETE";
        return new AnalyzerTypeCatalogView.MappingSummary(Math.toIntExact(mapped), Math.toIntExact(excluded), total,
                state);
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

}
