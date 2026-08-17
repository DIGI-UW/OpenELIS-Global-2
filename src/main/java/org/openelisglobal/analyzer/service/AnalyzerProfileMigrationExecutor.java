package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.openelisglobal.analyzer.dao.AnalyzerDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileMigrationAnomaly;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingRevision;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingTest;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.openelisglobal.audittrail.dao.AuditTrailService;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.test.service.TestService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyzerProfileMigrationExecutor {

    private static final String AUDIT_TABLE = "analyzer";

    private final AnalyzerDAO analyzerDAO;
    private final AnalyzerTestMappingService legacyMappingService;
    private final AnalyzerSiteBindingService siteBindingService;
    private final AnalyzerProfileMigrationAnomalyService anomalyService;
    private final TestService testService;
    private final AuditTrailService auditTrailService;

    public AnalyzerProfileMigrationExecutor(AnalyzerDAO analyzerDAO, AnalyzerTestMappingService legacyMappingService,
            AnalyzerSiteBindingService siteBindingService, AnalyzerProfileMigrationAnomalyService anomalyService,
            TestService testService, AuditTrailService auditTrailService) {
        this.analyzerDAO = analyzerDAO;
        this.legacyMappingService = legacyMappingService;
        this.siteBindingService = siteBindingService;
        this.anomalyService = anomalyService;
        this.testService = testService;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public AnalyzerProfileMigrationResult execute(String analyzerId, BridgeProfileCatalogEntry entry, String actor) {
        String effectiveAnalyzerId = requireText(analyzerId, "analyzerId is required");
        String effectiveActor = requireText(actor, "actor is required");
        JsonNode profile = requireProfile(entry);
        String profileId = requireText(text(profile, "profileId"), "profile profileId is required");
        int profileRevision = profile.path("revision").asInt();
        if (profileRevision < 1) {
            throw new IllegalArgumentException("profile revision must be positive");
        }

        Analyzer analyzer = analyzerDAO.findByIdForUpdate(effectiveAnalyzerId)
                .orElseThrow(() -> new IllegalArgumentException("Analyzer not found: " + effectiveAnalyzerId));
        MigrationPlan plan = plan(profile, legacyMappingService.getAllForAnalyzer(effectiveAnalyzerId));
        List<AnalyzerProfileMigrationAnomaly> persistedAnomalies = anomalyService.replaceOpen(analyzer, plan.findings(),
                effectiveActor);
        if (!persistedAnomalies.isEmpty()) {
            return AnalyzerProfileMigrationResult.blocked(effectiveAnalyzerId, profileId, profileRevision,
                    persistedAnomalies);
        }

        AnalyzerSiteBindingSnapshot binding = siteBindingService.findOrCreate(plan.binding(), effectiveActor);
        AnalyzerSiteBindingRevision revision = binding.revision();
        String bindingRevisionId = requireText(revision.getId(), "site binding revision id is required");
        if (sameAssociation(analyzer, profileId, profileRevision, bindingRevisionId)) {
            return AnalyzerProfileMigrationResult.unchanged(effectiveAnalyzerId, profileId, profileRevision,
                    bindingRevisionId);
        }

        Analyzer previous = copy(analyzer);
        analyzer.setBridgeProfileId(profileId);
        analyzer.setBridgeProfileRevision(profileRevision);
        analyzer.setSiteBindingRevision(revision);
        analyzer.setSysUserId(effectiveActor);
        analyzerDAO.update(analyzer);
        auditTrailService.saveHistory(analyzer, previous, effectiveActor, IActionConstants.AUDIT_TRAIL_UPDATE,
                AUDIT_TABLE);
        return AnalyzerProfileMigrationResult.migrated(effectiveAnalyzerId, profileId, profileRevision,
                bindingRevisionId);
    }

    private MigrationPlan plan(JsonNode profile, List<AnalyzerTestMapping> legacyMappings) {
        List<ProfileRow> profileRows = parseRows(profile.path("tests"));
        Map<String, List<ProfileRow>> rowsByIdentifier = indexIdentifiers(profileRows);
        Map<String, List<AnalyzerTestMapping>> legacyBySourceRow = new HashMap<>();
        Set<String> ambiguousSourceRows = new HashSet<>();
        List<AnalyzerProfileMigrationAnomalyDraft> findings = new ArrayList<>();

        List<AnalyzerTestMapping> orderedLegacy = legacyMappings == null ? List.of()
                : legacyMappings.stream()
                        .sorted(Comparator
                                .comparing(AnalyzerTestMapping::getAnalyzerTestName,
                                        Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER))
                                .thenComparing(AnalyzerTestMapping::getTestId,
                                        Comparator.nullsFirst(String::compareTo)))
                        .toList();
        for (AnalyzerTestMapping legacy : orderedLegacy) {
            String rawCode = trimToNull(legacy.getAnalyzerTestName());
            List<ProfileRow> matches = rawCode == null ? List.of() : rowsByIdentifier.getOrDefault(rawCode, List.of());
            if (matches.isEmpty()) {
                findings.add(finding(AnalyzerProfileMigrationAnomaly.Code.PROFILE_SOURCE_ROW_MISSING, rawCode,
                        legacy.getTestId(), "Legacy analyzer code has no exact source row in the selected profile"));
            } else if (matches.size() > 1) {
                findings.add(finding(
                        AnalyzerProfileMigrationAnomaly.Code.DISTINCT_SOURCE_ROWS_SHARE_NORMALIZED_IDENTITY, rawCode,
                        legacy.getTestId(), "Legacy analyzer code matches more than one profile source row"));
                matches.forEach(row -> ambiguousSourceRows.add(row.sourceRowKey()));
            } else {
                legacyBySourceRow.computeIfAbsent(matches.getFirst().sourceRowKey(), ignored -> new ArrayList<>())
                        .add(legacy);
            }
        }

        List<AnalyzerSiteBindingTestDraft> bindingRows = new ArrayList<>();
        for (ProfileRow row : profileRows) {
            List<AnalyzerTestMapping> matches = legacyBySourceRow.getOrDefault(row.sourceRowKey(), List.of());
            if (ambiguousSourceRows.contains(row.sourceRowKey())) {
                bindingRows.add(row.unresolved());
                continue;
            }
            if (matches.isEmpty()) {
                findings.add(finding(AnalyzerProfileMigrationAnomaly.Code.PROFILE_SOURCE_ROW_MISSING,
                        row.sourceRowKey(), null, "Profile source row has no exact legacy analyzer mapping"));
                bindingRows.add(row.unresolved());
                continue;
            }
            if (matches.size() > 1) {
                for (AnalyzerTestMapping match : matches) {
                    findings.add(
                            finding(AnalyzerProfileMigrationAnomaly.Code.DISTINCT_SOURCE_ROWS_SHARE_NORMALIZED_IDENTITY,
                                    match.getAnalyzerTestName(), match.getTestId(),
                                    "More than one legacy analyzer mapping matches the same profile source row"));
                }
                bindingRows.add(row.unresolved());
                continue;
            }

            AnalyzerTestMapping match = matches.getFirst();
            org.openelisglobal.test.valueholder.Test activeTest = activeTest(match.getTestId());
            if (activeTest == null) {
                findings.add(finding(AnalyzerProfileMigrationAnomaly.Code.LOCAL_TEST_INACTIVE_OR_MISSING,
                        match.getAnalyzerTestName(), match.getTestId(),
                        "Legacy analyzer mapping target is missing or inactive"));
                bindingRows.add(row.unresolved());
                continue;
            }
            bindingRows.add(row.bound(activeTest.getId(), match.getComponentId()));
        }

        List<AnalyzerProfileMigrationAnomalyDraft> normalizedFindings = normalizeFindings(findings);
        AnalyzerSiteBindingDraft binding = new AnalyzerSiteBindingDraft(text(profile, "profileId"),
                profile.path("revision").asInt(), bindingRows);
        return new MigrationPlan(binding, normalizedFindings);
    }

    private static List<AnalyzerProfileMigrationAnomalyDraft> normalizeFindings(
            List<AnalyzerProfileMigrationAnomalyDraft> findings) {
        Map<String, List<AnalyzerProfileMigrationAnomalyDraft>> byEvidence = findings.stream()
                .collect(java.util.stream.Collectors.groupingBy(AnalyzerProfileMigrationAnomalyDraft::evidenceKey,
                        java.util.TreeMap::new, java.util.stream.Collectors.toList()));
        List<AnalyzerProfileMigrationAnomalyDraft> normalized = new ArrayList<>();
        byEvidence.values().forEach(group -> {
            AnalyzerProfileMigrationAnomalyDraft first = group.getFirst();
            String detail = group.stream().map(AnalyzerProfileMigrationAnomalyDraft::detail).distinct().sorted()
                    .collect(java.util.stream.Collectors.joining("; "));
            normalized.add(new AnalyzerProfileMigrationAnomalyDraft(first.code(), first.legacySourceKey(),
                    first.legacyTestId(), detail));
        });
        return List.copyOf(normalized);
    }

    private org.openelisglobal.test.valueholder.Test activeTest(String testId) {
        try {
            return testId == null ? null : testService.getActiveTestById(Integer.valueOf(testId.trim()));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static List<ProfileRow> parseRows(JsonNode tests) {
        if (tests == null || !tests.isArray()) {
            throw new IllegalArgumentException("profile tests must be an array");
        }
        Map<String, ProfileRow> rows = new LinkedHashMap<>();
        tests.forEach(node -> {
            String sourceRowKey = requireText(text(node, "sourceRowKey"), "profile sourceRowKey is required");
            String analyzerCode = requireText(text(node, "analyzerCode"),
                    "profile analyzerCode is required for " + sourceRowKey);
            List<String> aliases = stringArray(node.path("aliases"));
            JsonNode coding = node.path("normalizedCoding");
            ProfileRow row = new ProfileRow(sourceRowKey, analyzerCode, aliases, text(node, "displayName"),
                    text(node, "resultType"), text(coding, "system"), text(coding, "code"));
            if (rows.putIfAbsent(sourceRowKey, row) != null) {
                throw new IllegalArgumentException("Duplicate profile sourceRowKey: " + sourceRowKey);
            }
        });
        return rows.values().stream().sorted(Comparator.comparing(ProfileRow::sourceRowKey)).toList();
    }

    private static Map<String, List<ProfileRow>> indexIdentifiers(List<ProfileRow> rows) {
        Map<String, LinkedHashSet<ProfileRow>> indexed = new LinkedHashMap<>();
        for (ProfileRow row : rows) {
            LinkedHashSet<String> identifiers = new LinkedHashSet<>();
            identifiers.add(row.analyzerCode());
            identifiers.addAll(row.aliases());
            identifiers.forEach(
                    identifier -> indexed.computeIfAbsent(identifier, ignored -> new LinkedHashSet<>()).add(row));
        }
        Map<String, List<ProfileRow>> result = new LinkedHashMap<>();
        indexed.forEach((identifier, matches) -> result.put(identifier, List.copyOf(matches)));
        return result;
    }

    private static AnalyzerProfileMigrationAnomalyDraft finding(AnalyzerProfileMigrationAnomaly.Code code,
            String legacySourceKey, String legacyTestId, String detail) {
        return new AnalyzerProfileMigrationAnomalyDraft(code, legacySourceKey, legacyTestId, detail);
    }

    private static boolean sameAssociation(Analyzer analyzer, String profileId, int profileRevision,
            String bindingRevisionId) {
        return Objects.equals(profileId, analyzer.getBridgeProfileId())
                && Objects.equals(profileRevision, analyzer.getBridgeProfileRevision())
                && analyzer.getSiteBindingRevision() != null
                && Objects.equals(bindingRevisionId, analyzer.getSiteBindingRevision().getId());
    }

    private static Analyzer copy(Analyzer analyzer) {
        try {
            return (Analyzer) analyzer.clone();
        } catch (CloneNotSupportedException exception) {
            throw new IllegalStateException("Analyzer could not be copied for audit", exception);
        }
    }

    private static JsonNode requireProfile(BridgeProfileCatalogEntry entry) {
        if (entry == null || entry.profile() == null || !entry.profile().isObject()) {
            throw new IllegalArgumentException("Bridge profile entry is required");
        }
        return entry.profile();
    }

    private static List<String> stringArray(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return List.of();
        }
        if (!node.isArray()) {
            throw new IllegalArgumentException("profile aliases must be an array");
        }
        List<String> values = new ArrayList<>();
        node.forEach(value -> values.add(requireText(value.asText(null), "profile alias cannot be blank")));
        return List.copyOf(values);
    }

    private static String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        JsonNode value = node.path(field);
        return value.isTextual() ? trimToNull(value.asText()) : null;
    }

    private static String requireText(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record MigrationPlan(AnalyzerSiteBindingDraft binding,
            List<AnalyzerProfileMigrationAnomalyDraft> findings) {
    }

    private record ProfileRow(String sourceRowKey, String analyzerCode, List<String> aliases, String displayName,
            String resultType, String normalizedSystem, String normalizedCode) {

        private AnalyzerSiteBindingTestDraft unresolved() {
            return new AnalyzerSiteBindingTestDraft(sourceRowKey, analyzerCode, aliases, displayName, resultType,
                    normalizedSystem, normalizedCode, AnalyzerSiteBindingTest.MappingState.UNRESOLVED, null, null);
        }

        private AnalyzerSiteBindingTestDraft bound(String testId, String componentId) {
            return new AnalyzerSiteBindingTestDraft(sourceRowKey, analyzerCode, aliases, displayName, resultType,
                    normalizedSystem, normalizedCode, AnalyzerSiteBindingTest.MappingState.BOUND, testId, componentId);
        }
    }
}
