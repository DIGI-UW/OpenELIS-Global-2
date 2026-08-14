package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.openelisglobal.analyzer.dao.AnalyzerDAO;
import org.openelisglobal.analyzer.dao.AnalyzerSiteBindingRevisionDAO;
import org.openelisglobal.analyzer.dao.AnalyzerSiteBindingTestDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingRevision;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingTest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnalyzerTypeCatalogServiceImpl implements AnalyzerTypeCatalogService {

    private final AnalyzerProfileCatalogClient profileCatalogClient;
    private final AnalyzerSiteBindingRevisionDAO revisionDAO;
    private final AnalyzerSiteBindingTestDAO testDAO;
    private final AnalyzerDAO analyzerDAO;

    public AnalyzerTypeCatalogServiceImpl(AnalyzerProfileCatalogClient profileCatalogClient,
            AnalyzerSiteBindingRevisionDAO revisionDAO, AnalyzerSiteBindingTestDAO testDAO, AnalyzerDAO analyzerDAO) {
        this.profileCatalogClient = profileCatalogClient;
        this.revisionDAO = revisionDAO;
        this.testDAO = testDAO;
        this.analyzerDAO = analyzerDAO;
    }

    @Override
    public List<AnalyzerTypeCatalogSummary> list(AnalyzerProfileCatalogFilter filter) {
        AnalyzerProfileCatalogFilter effectiveFilter = filter == null ? AnalyzerProfileCatalogFilter.empty() : filter;
        return composeAll(profileCatalogClient.list(effectiveFilter));
    }

    @Override
    public AnalyzerTypeCatalogSummary get(String profileId, Integer revision) {
        return composeOne(profileCatalogClient.get(profileId, revision));
    }

    @Override
    public List<BridgeProfileCatalogEntry> history(String profileId) {
        return List.copyOf(profileCatalogClient.history(profileId));
    }

    @Override
    public JsonNode exportProfile(String profileId, Integer revision) {
        return profileCatalogClient.get(profileId, revision).profile();
    }

    @Override
    public AnalyzerTypeCatalogSummary fork(String profileId, AnalyzerProfileForkRequest request, String actor) {
        return composeOne(profileCatalogClient.fork(profileId, request, actor));
    }

    @Override
    public AnalyzerTypeCatalogSummary deactivate(String profileId, String actor) {
        return composeOne(profileCatalogClient.deactivate(profileId, actor));
    }

    @Override
    public AnalyzerTypeCatalogSummary reactivate(String profileId, String actor) {
        return composeOne(profileCatalogClient.reactivate(profileId, actor));
    }

    private AnalyzerTypeCatalogSummary composeOne(BridgeProfileCatalogEntry profile) {
        return composeAll(List.of(profile)).getFirst();
    }

    private List<AnalyzerTypeCatalogSummary> composeAll(List<BridgeProfileCatalogEntry> profiles) {
        if (profiles.isEmpty()) {
            return List.of();
        }

        List<String> profileIds = profiles.stream().map(entry -> text(entry.profile(), "profileId")).distinct()
                .toList();
        Map<String, List<AnalyzerSiteBindingRevision>> revisionsByProfile = revisionDAO
                .findLatestByProfileIds(profileIds).stream()
                .collect(Collectors.groupingBy(AnalyzerSiteBindingRevision::getBridgeProfileId));
        List<AnalyzerSiteBindingRevision> selectedRevisions = revisionsByProfile.values().stream()
                .filter(revisions -> revisions.size() == 1).map(List::getFirst).toList();
        List<String> selectedRevisionIds = selectedRevisions.stream().map(AnalyzerSiteBindingRevision::getId).toList();
        Map<String, List<AnalyzerSiteBindingTest>> rowsByRevision = selectedRevisionIds.isEmpty() ? Map.of()
                : testDAO.findByRevisionIds(selectedRevisionIds).stream()
                        .collect(Collectors.groupingBy(row -> row.getId().getSiteBindingRevisionId()));
        Map<String, Long> analyzerCounts = analyzerDAO.countByBridgeProfileIds(profileIds);

        return profiles.stream()
                .map(entry -> compose(entry,
                        revisionsByProfile.getOrDefault(text(entry.profile(), "profileId"), List.of()), rowsByRevision,
                        analyzerCounts))
                .sorted(Comparator
                        .comparing(AnalyzerTypeCatalogSummary::displayName,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(AnalyzerTypeCatalogSummary::profileId))
                .toList();
    }

    private static AnalyzerTypeCatalogSummary compose(BridgeProfileCatalogEntry entry,
            List<AnalyzerSiteBindingRevision> revisions, Map<String, List<AnalyzerSiteBindingTest>> rowsByRevision,
            Map<String, Long> analyzerCounts) {
        JsonNode profile = entry.profile();
        String profileId = text(profile, "profileId");
        int profileRevision = profile.path("revision").asInt();
        JsonNode tests = profile.path("tests");
        int testTotal = tests.isArray() ? tests.size() : 0;
        int resultValueTotal = resultValueTotal(tests);
        List<AnalyzerTypeAttentionCode> attention = new ArrayList<>();
        AnalyzerTypeMappingProgress testProgress;
        AnalyzerTypeSiteBindingSummary bindingSummary = null;

        if (revisions.isEmpty()) {
            testProgress = new AnalyzerTypeMappingProgress(testTotal, 0, 0, 0, testTotal, 0);
            attention.add(AnalyzerTypeAttentionCode.SITE_BINDING_REQUIRED);
        } else if (revisions.size() > 1) {
            testProgress = new AnalyzerTypeMappingProgress(testTotal, 0, 0, 0, testTotal, 0);
            attention.add(AnalyzerTypeAttentionCode.MULTIPLE_SITE_BINDINGS);
        } else {
            AnalyzerSiteBindingRevision revision = revisions.getFirst();
            List<AnalyzerSiteBindingTest> rows = rowsByRevision.getOrDefault(revision.getId(), List.of());
            testProgress = testProgress(tests, rows);
            bindingSummary = bindingSummary(revision);
            if (revision.getBridgeProfileRevision() == null
                    || revision.getBridgeProfileRevision().intValue() != profileRevision) {
                attention.add(AnalyzerTypeAttentionCode.PROFILE_REVISION_MISMATCH);
            }
            if (testProgress.missing() > 0) {
                attention.add(AnalyzerTypeAttentionCode.MISSING_TEST_ROWS);
            }
            if (testProgress.extra() > 0) {
                attention.add(AnalyzerTypeAttentionCode.EXTRA_TEST_ROWS);
            }
            if (testProgress.unresolved() > 0) {
                attention.add(AnalyzerTypeAttentionCode.UNRESOLVED_TEST_MAPPINGS);
            }
        }

        AnalyzerTypeMappingProgress resultProgress = new AnalyzerTypeMappingProgress(resultValueTotal, 0,
                resultValueTotal, 0, 0, 0);
        if (resultValueTotal > 0) {
            attention.add(AnalyzerTypeAttentionCode.RESULT_VALUE_BINDING_REQUIRED);
        }

        JsonNode identity = profile.path("identity");
        JsonNode lineage = profile.path("lineage");
        return new AnalyzerTypeCatalogSummary(profileId, profileRevision, text(profile, "displayName"),
                text(profile, "category"), text(profile, "protocol"), text(profile, "source"), text(profile, "status"),
                text(identity, "manufacturer"), text(identity, "model"), text(profile, "legacyVersion"),
                text(lineage, "parentProfileId"), integer(lineage, "parentRevision"),
                profile.path("capabilities").path("connectionTest").asBoolean(false), entry.fingerprint(),
                entry.audit(), testProgress, resultProgress, arraySize(profile.path("qcIdentification")),
                analyzerCounts.getOrDefault(profileId, 0L), bindingSummary, attention);
    }

    private static AnalyzerTypeMappingProgress testProgress(JsonNode profileTests, List<AnalyzerSiteBindingTest> rows) {
        Set<String> sourceRowKeys = iterable(profileTests).stream().map(test -> text(test, "sourceRowKey"))
                .collect(Collectors.toSet());
        Map<String, AnalyzerSiteBindingTest> rowsByKey = rows.stream()
                .collect(Collectors.toMap(row -> row.getId().getSourceRowKey(), Function.identity(),
                        (first, duplicate) -> first, LinkedHashMap::new));
        int bound = 0;
        int unresolved = 0;
        int ignored = 0;
        for (String sourceRowKey : sourceRowKeys) {
            AnalyzerSiteBindingTest row = rowsByKey.get(sourceRowKey);
            if (row == null) {
                continue;
            }
            if (row.getMappingState() == AnalyzerSiteBindingTest.MappingState.BOUND) {
                bound++;
            } else if (row.getMappingState() == AnalyzerSiteBindingTest.MappingState.IGNORED) {
                ignored++;
            } else {
                unresolved++;
            }
        }
        int missing = (int) sourceRowKeys.stream().filter(key -> !rowsByKey.containsKey(key)).count();
        int extra = (int) rowsByKey.keySet().stream().filter(key -> !sourceRowKeys.contains(key)).count();
        return new AnalyzerTypeMappingProgress(sourceRowKeys.size(), bound, unresolved, ignored, missing, extra);
    }

    private static AnalyzerTypeSiteBindingSummary bindingSummary(AnalyzerSiteBindingRevision revision) {
        Timestamp createdAt = revision.getCreatedAt();
        return new AnalyzerTypeSiteBindingSummary(revision.getSiteBinding().getId(), revision.getId(),
                revision.getRevisionNumber(), revision.getBridgeProfileRevision(), revision.getFingerprint(),
                revision.getCreatedBy(), createdAt == null ? null : createdAt.toInstant());
    }

    private static int resultValueTotal(JsonNode tests) {
        return iterable(tests).stream().mapToInt(test -> arraySize(test.path("resultValues"))).sum();
    }

    private static int arraySize(JsonNode node) {
        return node != null && node.isArray() ? node.size() : 0;
    }

    private static List<JsonNode> iterable(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<JsonNode> values = new ArrayList<>();
        node.forEach(values::add);
        return values;
    }

    private static String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode()) {
            return null;
        }
        JsonNode value = node.path(field);
        return value.isTextual() && !value.asText().isBlank() ? value.asText() : null;
    }

    private static Integer integer(JsonNode node, String field) {
        if (node == null || node.isMissingNode()) {
            return null;
        }
        JsonNode value = node.path(field);
        return value.isIntegralNumber() ? value.asInt() : null;
    }
}
