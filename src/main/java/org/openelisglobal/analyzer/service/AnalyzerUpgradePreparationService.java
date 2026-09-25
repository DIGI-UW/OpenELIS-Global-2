package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.openelisglobal.analyzer.dao.AnalyzerUpgradeDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingMappingState;
import org.openelisglobal.analyzer.valueholder.AnalyzerUpgradeSource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transfers retained upgrade input into current local bindings, without
 * activating anything.
 */
@Service
public class AnalyzerUpgradePreparationService {
    private final AnalyzerService analyzers;
    private final AnalyzerUpgradeDAO source;
    private final AnalyzerProfileBindingService profiles;
    private final BridgeProfileCatalogService catalog;
    private final AnalyzerSiteBindingService bindings;
    private final ObjectMapper json = new ObjectMapper();

    public AnalyzerUpgradePreparationService(AnalyzerService analyzers, AnalyzerUpgradeDAO source,
            AnalyzerProfileBindingService profiles, BridgeProfileCatalogService catalog,
            AnalyzerSiteBindingService bindings) {
        this.analyzers = analyzers;
        this.source = source;
        this.profiles = profiles;
        this.catalog = catalog;
        this.bindings = bindings;
    }

    public record ProfileSelection(String profileId, int revision) {
    }

    public record Prepared(String analyzerId, ObjectNode values) {
    }

    public List<String> pendingIds() {
        return source.pendingIds();
    }

    @Transactional
    public Prepared prepare(String id, ProfileSelection selection, String actor) {
        Analyzer persisted = analyzers.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Analyzer not found"));
        if (persisted.getBridgeConnectionId() != null)
            return new Prepared(id, json.createObjectNode());
        if (source.hasSerialSettings(id))
            throw new AnalyzerUpgradePendingException("analyzer.upgrade.reason.serialSettings",
                    "Saved serial settings require review before transfer");
        AnalyzerUpgradeSource old = source.source(id);
        var config = source.config(id);
        JsonNode saved;
        try {
            saved = config == null ? json.createObjectNode() : json.readTree(config.getConfig());
        } catch (Exception exception) {
            throw new AnalyzerUpgradePendingException("analyzer.upgrade.reason.invalidConfiguration",
                    "Saved analyzer configuration is invalid", exception);
        }
        BridgeAnalyzerProfile profile = selectProfile(persisted, old, selection);
        if (old.getColumnMappings() != null && !old.getColumnMappings().isBlank()) {
            try {
                JsonNode columns = json.readTree(old.getColumnMappings());
                if (!columns.isEmpty() && !columns.equals(profile.document().path("column_mapping")))
                    throw new AnalyzerUpgradePendingException("analyzer.upgrade.reason.fileColumnsMismatch",
                            "Saved file column mappings differ from the selected profile");
            } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
                throw new AnalyzerUpgradePendingException("analyzer.upgrade.reason.invalidFileColumns",
                        "Saved file column mappings are invalid", exception);
            }
        }
        ObjectNode values = connectionValues(old, saved, profile.document());
        Analyzer update = new Analyzer();
        BeanUtils.copyProperties(persisted, update);
        update.setTestUnitIds(new ArrayList<>(persisted.getTestUnitIds()));
        boolean hadBinding = persisted.getSiteBindingRevision() != null;
        profiles.assignProfile(update, profile.profileId(), profile.revision(), actor);
        AnalyzerProfileBinding pin = update.getPinnedProfileBinding();
        AnalyzerSiteBindingSnapshot binding = bindings.findByRevisionId(update.getSiteBindingRevision().getId())
                .orElseThrow();
        Map<String, AnalyzerSiteBindingTestDraft> tests = binding.tests().stream()
                .collect(
                        Collectors.toMap(row -> row.getId().getSourceRowKey(),
                                row -> new AnalyzerSiteBindingTestDraft(row.getId().getSourceRowKey(),
                                        row.getMappingState(), row.getTestId()),
                                (left, right) -> left, LinkedHashMap::new));
        List<AnalyzerSiteBindingResultDraft> results = binding
                .results().stream().map(row -> new AnalyzerSiteBindingResultDraft(row.getId().getSourceRowKey(),
                        row.getId().getRawValue(), row.getMappingState(), row.getTestResultId()))
                .collect(Collectors.toCollection(ArrayList::new));
        boolean shared = profiles.getAnalyzerUsageCount(pin.getId()) > (hadBinding ? 1 : 0);
        for (var mapping : source.mappings(id)) {
            if (mapping.getComponentId() != null)
                throw new AnalyzerUpgradePendingException("analyzer.upgrade.reason.componentMapping",
                        "Saved component mapping requires review: " + mapping.getSourceCode());
            var current = tests.get(mapping.getSourceCode());
            boolean same = current != null && current.mappingState() == AnalyzerSiteBindingMappingState.BOUND
                    && Objects.equals(current.testId(), mapping.getTestId());
            if (shared && !same)
                throw new AnalyzerUpgradePendingException("analyzer.upgrade.reason.sharedMapping",
                        "Shared mappings differ for " + mapping.getSourceCode());
            if (!same) {
                tests.put(mapping.getSourceCode(), new AnalyzerSiteBindingTestDraft(mapping.getSourceCode(),
                        AnalyzerSiteBindingMappingState.BOUND, mapping.getTestId()));
                // Answer defaults belong to their original target test; do not carry them to a
                // different test.
                results.replaceAll(
                        row -> row.sourceRowKey().equals(mapping.getSourceCode())
                                ? new AnalyzerSiteBindingResultDraft(row.sourceRowKey(), row.rawValue(),
                                        AnalyzerSiteBindingMappingState.UNRESOLVED, null)
                                : row);
            }
        }
        var transferred = bindings.appendRevision(binding.binding(),
                new AnalyzerSiteBindingDraft(new ArrayList<>(tests.values()), results), actor);
        update.setSiteBindingRevision(transferred.revision());
        update.setActive(false);
        update.setStatus(Analyzer.AnalyzerStatus.SETUP);
        update.setSysUserId(actor);
        analyzers.update(update);
        return new Prepared(id, values);
    }

    private BridgeAnalyzerProfile selectProfile(Analyzer analyzer, AnalyzerUpgradeSource old,
            ProfileSelection selection) {
        AnalyzerProfileBinding pin = analyzer.getPinnedProfileBinding();
        if (pin == null && old.getProfileBindingId() != null)
            pin = profiles.get(old.getProfileBindingId());
        if (pin != null) {
            if (selection != null && (!pin.getProfileId().equals(selection.profileId())
                    || pin.getProfileRevision() != selection.revision()))
                throw new AnalyzerUpgradePendingException("analyzer.upgrade.reason.profileMismatch",
                        "Existing profile reference differs from the requested profile");
            return BridgeAnalyzerProfile
                    .from(catalog.getProfile(pin.getProfileId(), pin.getProfileRevision()).profile());
        }
        if (selection != null)
            return BridgeAnalyzerProfile
                    .from(catalog.getProfile(selection.profileId(), selection.revision()).profile());
        var type = source.type(old.getTypeId());
        if (type == null)
            throw new AnalyzerUpgradePendingException("analyzer.upgrade.reason.selectProfile",
                    "Select a Bridge profile for this existing analyzer");
        List<BridgeAnalyzerProfile> matches = catalog.getCatalog().profiles().stream()
                .map(item -> BridgeAnalyzerProfile.from(item.profile())).filter(item -> "ACTIVE".equals(item.status()))
                .filter(item -> type.getName().equalsIgnoreCase(item.displayName())
                        || type.getName().equals(item.profileId()))
                .toList();
        if (matches.stream().map(BridgeAnalyzerProfile::profileId).distinct().count() != 1)
            throw new AnalyzerUpgradePendingException("analyzer.upgrade.reason.selectProfile",
                    "Select an unambiguous Bridge profile for " + type.getName());
        return matches.stream().max(Comparator.comparingInt(BridgeAnalyzerProfile::revision)).orElseThrow();
    }

    private ObjectNode connectionValues(AnalyzerUpgradeSource old, JsonNode saved, JsonNode profile) {
        ObjectNode values = json.createObjectNode();
        for (JsonNode field : profile.path("connectionFields")) {
            String key = field.path("key").asText();
            if (saved.hasNonNull(key))
                values.set(key, saved.get(key));
        }
        if ("LIS_INITIATED".equals(old.getCommunicationMode())) {
            values.put("connectionRole", "CLIENT");
            values.put("dataFlow", "TWO_WAY");
        } else if ("BOTH".equals(old.getCommunicationMode())) {
            values.put("dataFlow", "TWO_WAY");
        }
        put(values, "host", old.getHost());
        put(values, "directory", old.getDirectory());
        put(values, "filePattern", old.getFilePattern());
        String role = values.path("connectionRole")
                .asText(profile.path("configDefaults").path("connectionRole").asText());
        // Old SERVER ports described the shared listener, not an outbound analyzer
        // endpoint.
        if ("CLIENT".equals(role) && old.getPort() != null && !values.has("port"))
            values.put("port", old.getPort());
        if ("SERVER".equals(role))
            values.remove("port");
        var allowed = new java.util.HashSet<String>();
        profile.path("connectionFields").forEach(field -> allowed.add(field.path("key").asText()));
        values.retain(allowed);
        if (old.getFileFormat() != null && profile.path("configDefaults").hasNonNull("fileFormat")
                && !old.getFileFormat().equalsIgnoreCase(profile.path("configDefaults").path("fileFormat").asText()))
            throw new AnalyzerUpgradePendingException("analyzer.upgrade.reason.fileFormatMismatch",
                    "Saved file format differs from the selected profile");
        return values;
    }

    private static void put(ObjectNode values, String key, String value) {
        if (value != null && !value.isBlank() && !values.hasNonNull(key))
            values.put(key, value);
    }
}
