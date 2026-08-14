package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.analyzer.dao.AnalyzerDAO;
import org.openelisglobal.analyzer.dao.AnalyzerSiteBindingRevisionDAO;
import org.openelisglobal.analyzer.dao.AnalyzerSiteBindingTestDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingRevision;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingTest;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingTestPK;

public class AnalyzerTypeCatalogServiceImplTest {

    private static final Path PROFILE_FIXTURE = Path.of("tools", "openelis-analyzer-bridge", "contracts", "analyzer",
            "v1", "fixtures", "profile-catalog-entry.json");
    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();

    private AnalyzerProfileCatalogClient profileCatalogClient;
    private AnalyzerSiteBindingRevisionDAO revisionDAO;
    private AnalyzerSiteBindingTestDAO testDAO;
    private AnalyzerDAO analyzerDAO;
    private AnalyzerTypeCatalogService service;
    private BridgeProfileCatalogEntry profileEntry;

    @Before
    public void setUp() throws Exception {
        profileCatalogClient = mock(AnalyzerProfileCatalogClient.class);
        revisionDAO = mock(AnalyzerSiteBindingRevisionDAO.class);
        testDAO = mock(AnalyzerSiteBindingTestDAO.class);
        analyzerDAO = mock(AnalyzerDAO.class);
        service = new AnalyzerTypeCatalogServiceImpl(profileCatalogClient, revisionDAO, testDAO, analyzerDAO);
        profileEntry = JSON.readValue(Files.readString(PROFILE_FIXTURE), BridgeProfileCatalogEntry.class);
    }

    @Test
    public void composesBridgeProfileWithCurrentSiteBindingAndUsage() {
        AnalyzerProfileCatalogFilter filter = new AnalyzerProfileCatalogFilter("mock", "SITE", "ACTIVE", "ASTM");
        AnalyzerSiteBindingRevision revision = revision("binding-1", "revision-1", 4, 1);
        List<AnalyzerSiteBindingTest> rows = List.of(row(revision, "wbc", AnalyzerSiteBindingTest.MappingState.BOUND),
                row(revision, "hiv-interpretation", AnalyzerSiteBindingTest.MappingState.UNRESOLVED),
                row(revision, "legacy-extra", AnalyzerSiteBindingTest.MappingState.BOUND));
        when(profileCatalogClient.list(filter)).thenReturn(List.of(profileEntry));
        when(revisionDAO.findLatestByProfileIds(List.of("site.mock-hematology"))).thenReturn(List.of(revision));
        when(testDAO.findByRevisionIds(List.of("revision-1"))).thenReturn(rows);
        when(analyzerDAO.countByBridgeProfileIds(List.of("site.mock-hematology")))
                .thenReturn(Map.of("site.mock-hematology", 2L));

        AnalyzerTypeCatalogSummary summary = service.list(filter).get(0);

        assertEquals("site.mock-hematology", summary.profileId());
        assertEquals(1, summary.revision());
        assertEquals("Mock Hematology Analyzer", summary.displayName());
        assertEquals("ASTM", summary.protocol());
        assertEquals("SITE", summary.source());
        assertEquals("ACTIVE", summary.status());
        assertEquals("OpenELIS", summary.manufacturer());
        assertEquals("Mock Hematology", summary.model());
        assertEquals(2, summary.testMappings().total());
        assertEquals(1, summary.testMappings().bound());
        assertEquals(1, summary.testMappings().unresolved());
        assertEquals(0, summary.testMappings().missing());
        assertEquals(1, summary.testMappings().extra());
        assertEquals(2, summary.resultValueMappings().total());
        assertEquals(0, summary.resultValueMappings().bound());
        assertEquals(1, summary.qcIdentificationRuleCount());
        assertEquals(2L, summary.analyzerCount());
        assertEquals("binding-1", summary.siteBinding().bindingId());
        assertEquals("revision-1", summary.siteBinding().revisionId());
        assertEquals(4, summary.siteBinding().revisionNumber());
        assertEquals("sha256:binding", summary.siteBinding().fingerprint());
        assertEquals(Instant.parse("2026-08-14T03:00:00Z"), summary.siteBinding().createdAt());
        assertEquals(
                List.of(AnalyzerTypeAttentionCode.EXTRA_TEST_ROWS, AnalyzerTypeAttentionCode.UNRESOLVED_TEST_MAPPINGS,
                        AnalyzerTypeAttentionCode.RESULT_VALUE_BINDING_REQUIRED),
                summary.attentionCodes());
    }

    @Test
    public void reportsUnusedShippedProfileAsRequiringSiteBinding() {
        when(profileCatalogClient.list(AnalyzerProfileCatalogFilter.empty())).thenReturn(List.of(profileEntry));
        when(revisionDAO.findLatestByProfileIds(List.of("site.mock-hematology"))).thenReturn(List.of());
        when(analyzerDAO.countByBridgeProfileIds(List.of("site.mock-hematology"))).thenReturn(Map.of());

        AnalyzerTypeCatalogSummary summary = service.list(AnalyzerProfileCatalogFilter.empty()).get(0);

        assertNull(summary.siteBinding());
        assertEquals(2, summary.testMappings().missing());
        assertEquals(0L, summary.analyzerCount());
        assertEquals(List.of(AnalyzerTypeAttentionCode.SITE_BINDING_REQUIRED,
                AnalyzerTypeAttentionCode.RESULT_VALUE_BINDING_REQUIRED), summary.attentionCodes());
    }

    @Test
    public void failsClosedWhenProfileHasMultipleCurrentBindingAggregates() {
        AnalyzerSiteBindingRevision first = revision("binding-1", "revision-1", 1, 1);
        AnalyzerSiteBindingRevision second = revision("binding-2", "revision-2", 1, 1);
        when(profileCatalogClient.list(AnalyzerProfileCatalogFilter.empty())).thenReturn(List.of(profileEntry));
        when(revisionDAO.findLatestByProfileIds(List.of("site.mock-hematology"))).thenReturn(List.of(first, second));
        when(analyzerDAO.countByBridgeProfileIds(List.of("site.mock-hematology"))).thenReturn(Map.of());

        AnalyzerTypeCatalogSummary summary = service.list(AnalyzerProfileCatalogFilter.empty()).get(0);

        assertNull(summary.siteBinding());
        assertEquals(List.of(AnalyzerTypeAttentionCode.MULTIPLE_SITE_BINDINGS,
                AnalyzerTypeAttentionCode.RESULT_VALUE_BINDING_REQUIRED), summary.attentionCodes());
    }

    @Test
    public void marksBindingStaleWhenBridgeProfileRevisionHasAdvanced() {
        AnalyzerSiteBindingRevision revision = revision("binding-1", "revision-1", 4, 0);
        when(profileCatalogClient.list(AnalyzerProfileCatalogFilter.empty())).thenReturn(List.of(profileEntry));
        when(revisionDAO.findLatestByProfileIds(List.of("site.mock-hematology"))).thenReturn(List.of(revision));
        when(testDAO.findByRevisionIds(List.of("revision-1"))).thenReturn(List.of());
        when(analyzerDAO.countByBridgeProfileIds(List.of("site.mock-hematology"))).thenReturn(Map.of());

        AnalyzerTypeCatalogSummary summary = service.list(AnalyzerProfileCatalogFilter.empty()).get(0);

        assertEquals(List.of(AnalyzerTypeAttentionCode.PROFILE_REVISION_MISMATCH,
                AnalyzerTypeAttentionCode.MISSING_TEST_ROWS, AnalyzerTypeAttentionCode.RESULT_VALUE_BINDING_REQUIRED),
                summary.attentionCodes());
    }

    @Test
    public void getsExplicitProfileRevisionWithCurrentSiteState() {
        AnalyzerSiteBindingRevision revision = revision("binding-1", "revision-1", 4, 1);
        when(profileCatalogClient.get("site.mock-hematology", 1)).thenReturn(profileEntry);
        when(revisionDAO.findLatestByProfileIds(List.of("site.mock-hematology"))).thenReturn(List.of(revision));
        when(testDAO.findByRevisionIds(List.of("revision-1")))
                .thenReturn(List.of(row(revision, "wbc", AnalyzerSiteBindingTest.MappingState.BOUND),
                        row(revision, "hiv-interpretation", AnalyzerSiteBindingTest.MappingState.IGNORED)));
        when(analyzerDAO.countByBridgeProfileIds(List.of("site.mock-hematology")))
                .thenReturn(Map.of("site.mock-hematology", 2L));

        AnalyzerTypeCatalogSummary summary = service.get("site.mock-hematology", 1);

        assertEquals("site.mock-hematology", summary.profileId());
        assertEquals(1, summary.testMappings().bound());
        assertEquals(1, summary.testMappings().ignored());
        assertEquals(2L, summary.analyzerCount());
    }

    @Test
    public void delegatesHistoryToBridgeAuthority() {
        when(profileCatalogClient.history("site.mock-hematology")).thenReturn(List.of(profileEntry));

        List<BridgeProfileCatalogEntry> history = service.history("site.mock-hematology");

        assertEquals(List.of(profileEntry), history);
    }

    @Test
    public void recomposesLifecycleChangesReturnedByBridge() {
        BridgeProfileCatalogEntry inactive = withProfile("site.mock-hematology", "INACTIVE", null, null);
        when(profileCatalogClient.deactivate("site.mock-hematology", "42")).thenReturn(inactive);
        when(profileCatalogClient.reactivate("site.mock-hematology", "42")).thenReturn(profileEntry);
        when(revisionDAO.findLatestByProfileIds(List.of("site.mock-hematology"))).thenReturn(List.of());
        when(analyzerDAO.countByBridgeProfileIds(List.of("site.mock-hematology"))).thenReturn(Map.of());

        AnalyzerTypeCatalogSummary deactivated = service.deactivate("site.mock-hematology", "42");
        AnalyzerTypeCatalogSummary reactivated = service.reactivate("site.mock-hematology", "42");

        assertEquals("INACTIVE", deactivated.status());
        assertEquals("ACTIVE", reactivated.status());
    }

    @Test
    public void forksBridgeProfileBeforeCreatingAnyLocalSiteBinding() {
        AnalyzerProfileForkRequest request = new AnalyzerProfileForkRequest(1, "site.mock-hematology-1",
                "Mock Hematology -1");
        BridgeProfileCatalogEntry forked = withProfile("site.mock-hematology-1", "ACTIVE", "site.mock-hematology", 1);
        when(profileCatalogClient.fork("site.mock-hematology", request, "42")).thenReturn(forked);
        when(revisionDAO.findLatestByProfileIds(List.of("site.mock-hematology-1"))).thenReturn(List.of());
        when(analyzerDAO.countByBridgeProfileIds(List.of("site.mock-hematology-1"))).thenReturn(Map.of());

        AnalyzerTypeCatalogSummary summary = service.fork("site.mock-hematology", request, "42");

        assertEquals("site.mock-hematology-1", summary.profileId());
        assertEquals("site.mock-hematology", summary.parentProfileId());
        assertEquals(Integer.valueOf(1), summary.parentRevision());
        assertNull(summary.siteBinding());
        assertEquals(AnalyzerTypeAttentionCode.SITE_BINDING_REQUIRED, summary.attentionCodes().get(0));
        verify(profileCatalogClient).fork("site.mock-hematology", request, "42");
    }

    private BridgeProfileCatalogEntry withProfile(String profileId, String status, String parentProfileId,
            Integer parentRevision) {
        ObjectNode profile = profileEntry.profile().deepCopy();
        profile.put("profileId", profileId);
        profile.put("status", status);
        if (parentProfileId == null) {
            profile.remove("lineage");
        } else {
            ObjectNode lineage = profile.putObject("lineage");
            lineage.put("parentProfileId", parentProfileId);
            lineage.put("parentRevision", parentRevision);
        }
        return new BridgeProfileCatalogEntry(profile, profileEntry.audit(), profileEntry.fingerprint());
    }

    private static AnalyzerSiteBindingRevision revision(String bindingId, String revisionId, int revisionNumber,
            int bridgeProfileRevision) {
        AnalyzerSiteBinding binding = new AnalyzerSiteBinding();
        binding.setId(bindingId);
        AnalyzerSiteBindingRevision revision = new AnalyzerSiteBindingRevision();
        revision.setId(revisionId);
        revision.setSiteBinding(binding);
        revision.setRevisionNumber(revisionNumber);
        revision.setBridgeProfileId("site.mock-hematology");
        revision.setBridgeProfileRevision(bridgeProfileRevision);
        revision.setFingerprint("sha256:binding");
        revision.setCreatedBy("oe-user");
        revision.setCreatedAt(Timestamp.from(Instant.parse("2026-08-14T03:00:00Z")));
        return revision;
    }

    private static AnalyzerSiteBindingTest row(AnalyzerSiteBindingRevision revision, String sourceRowKey,
            AnalyzerSiteBindingTest.MappingState state) {
        AnalyzerSiteBindingTest row = new AnalyzerSiteBindingTest();
        row.setId(new AnalyzerSiteBindingTestPK(revision.getId(), sourceRowKey));
        row.setSiteBindingRevision(revision);
        row.setRawAnalyzerCode(sourceRowKey.toUpperCase());
        row.setMappingState(state);
        if (state == AnalyzerSiteBindingTest.MappingState.BOUND) {
            row.setTestId("9701");
        }
        return row;
    }
}
