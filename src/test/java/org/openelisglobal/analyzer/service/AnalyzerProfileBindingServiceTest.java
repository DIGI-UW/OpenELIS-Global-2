package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.dao.AnalyzerProfileBindingDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingRevision;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerProfileBindingServiceTest {

    private static final String PROFILE_ID = "site.mock-hematology";
    private static final int REVISION = 3;
    private static final String FINGERPRINT = "sha256:1111111111111111111111111111111111111111111111111111111111111111";
    private static final String CHANGED_FINGERPRINT = "sha256:2222222222222222222222222222222222222222222222222222222222222222";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AnalyzerProfileBindingDAO bindingDAO;

    @Mock
    private BridgeProfileCatalogService catalogService;

    @Mock
    private AnalyzerSiteBindingService siteBindingService;

    private AnalyzerProfileBindingService service;

    @Before
    public void setUp() {
        service = new AnalyzerProfileBindingServiceImpl(bindingDAO, catalogService, siteBindingService);
    }

    @Test
    public void resolveActiveRevisionCreatesRevisionScopedBinding() {
        when(catalogService.getCatalog()).thenReturn(catalog("ACTIVE", FINGERPRINT));
        when(bindingDAO.findByProfileIdAndRevision(PROFILE_ID, REVISION)).thenReturn(Optional.empty());
        when(bindingDAO.insert(any(AnalyzerProfileBinding.class))).thenReturn("41");

        AnalyzerProfileBinding result = service.resolveActiveRevision(PROFILE_ID, REVISION, "oe-user-17");

        ArgumentCaptor<AnalyzerProfileBinding> captor = ArgumentCaptor.forClass(AnalyzerProfileBinding.class);
        verify(bindingDAO).insert(captor.capture());
        assertSame(captor.getValue(), result);
        assertEquals(PROFILE_ID, result.getProfileId());
        assertEquals(REVISION, result.getProfileRevision());
        assertEquals(FINGERPRINT, result.getProfileFingerprint());
        assertEquals("oe-user-17", result.getSysUserId());
    }

    @Test
    public void resolveActiveRevisionReusesExistingBinding() {
        AnalyzerProfileBinding existing = binding(FINGERPRINT);
        when(catalogService.getCatalog()).thenReturn(catalog("ACTIVE", FINGERPRINT));
        when(bindingDAO.findByProfileIdAndRevision(PROFILE_ID, REVISION)).thenReturn(Optional.of(existing));

        AnalyzerProfileBinding result = service.resolveActiveRevision(PROFILE_ID, REVISION, "oe-user-17");

        assertSame(existing, result);
        verify(bindingDAO, never()).insert(any(AnalyzerProfileBinding.class));
    }

    @Test
    public void resolveActiveRevisionRejectsFingerprintDrift() {
        when(catalogService.getCatalog()).thenReturn(catalog("ACTIVE", CHANGED_FINGERPRINT));
        when(bindingDAO.findByProfileIdAndRevision(PROFILE_ID, REVISION))
                .thenReturn(Optional.of(binding(FINGERPRINT)));

        AnalyzerProfileBindingException exception = assertThrows(AnalyzerProfileBindingException.class,
                () -> service.resolveActiveRevision(PROFILE_ID, REVISION, "oe-user-17"));

        assertEquals("Bridge profile site.mock-hematology revision 3 changed fingerprint", exception.getMessage());
        verify(bindingDAO, never()).insert(any(AnalyzerProfileBinding.class));
    }

    @Test
    public void resolveActiveRevisionRejectsInactiveProfile() {
        when(catalogService.getCatalog()).thenReturn(catalog("INACTIVE", FINGERPRINT));

        AnalyzerProfileBindingException exception = assertThrows(AnalyzerProfileBindingException.class,
                () -> service.resolveActiveRevision(PROFILE_ID, REVISION, "oe-user-17"));

        assertEquals("Bridge profile site.mock-hematology revision 3 is not active", exception.getMessage());
        verify(bindingDAO, never()).insert(any(AnalyzerProfileBinding.class));
    }

    @Test
    public void resolveActiveRevisionRejectsMissingProfile() {
        when(catalogService.getCatalog()).thenReturn(new BridgeProfileCatalog("1.0",
                "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", List.of()));

        AnalyzerProfileBindingException exception = assertThrows(AnalyzerProfileBindingException.class,
                () -> service.resolveActiveRevision(PROFILE_ID, REVISION, "oe-user-17"));

        assertEquals("Bridge profile site.mock-hematology revision 3 was not found", exception.getMessage());
        verify(bindingDAO, never()).insert(any(AnalyzerProfileBinding.class));
    }

    @Test
    public void assignProfilePreservesAnUnchangedPinnedRevisionWithoutConsultingLatestCatalog() {
        Analyzer analyzer = new Analyzer();
        AnalyzerProfileBinding existing = binding(FINGERPRINT);
        AnalyzerSiteBindingRevision existingRevision = siteBindingRevision(existing);
        analyzer.setSiteBindingRevision(existingRevision);

        AnalyzerProfileBinding result = service.assignProfile(analyzer, PROFILE_ID, REVISION, "oe-user-17");

        assertSame(existing, result);
        assertSame(existingRevision, analyzer.getSiteBindingRevision());
        assertSame(existing, analyzer.getPinnedProfileBinding());
        verify(catalogService, never()).getCatalog();
        verify(bindingDAO, never()).insert(any(AnalyzerProfileBinding.class));
        verify(siteBindingService, never()).resolveInitialRevision(any(), any(), any());
    }

    @Test
    public void assignProfilePinsTheSharedLocalBindingRevision() {
        Analyzer analyzer = new Analyzer();
        AnalyzerProfileBinding selected = binding(FINGERPRINT);
        JsonNode portableProfile = catalog("ACTIVE", FINGERPRINT).profiles().get(0).profile();
        AnalyzerSiteBindingRevision siteBindingRevision = siteBindingRevision(selected);
        when(catalogService.getCatalog()).thenReturn(catalog("ACTIVE", FINGERPRINT));
        when(bindingDAO.findByProfileIdAndRevision(PROFILE_ID, REVISION)).thenReturn(Optional.of(selected));
        when(siteBindingService.resolveInitialRevision(eq(selected), eq(portableProfile), eq("oe-user-17")))
                .thenReturn(new AnalyzerSiteBindingSnapshot(siteBindingRevision.getSiteBinding(), siteBindingRevision,
                        List.of(), List.of()));

        AnalyzerProfileBinding result = service.assignProfile(analyzer, PROFILE_ID, REVISION, "oe-user-17");

        assertSame(selected, result);
        assertSame(siteBindingRevision, analyzer.getSiteBindingRevision());
        assertSame(selected, analyzer.getPinnedProfileBinding());
    }

    @Test
    public void assignProfileChangesOnlyTheLocalBindingReference() {
        Analyzer analyzer = new Analyzer();
        analyzer.setName("Hematology bench 1");
        AnalyzerProfileBinding selected = binding(FINGERPRINT);
        JsonNode profile = catalog("ACTIVE", FINGERPRINT).profiles().get(0).profile();
        AnalyzerSiteBindingRevision siteBindingRevision = siteBindingRevision(selected);
        when(catalogService.getCatalog()).thenReturn(catalog("ACTIVE", FINGERPRINT));
        when(bindingDAO.findByProfileIdAndRevision(PROFILE_ID, REVISION)).thenReturn(Optional.of(selected));
        when(siteBindingService.resolveInitialRevision(eq(selected), eq(profile), eq("oe-user-17")))
                .thenReturn(new AnalyzerSiteBindingSnapshot(siteBindingRevision.getSiteBinding(), siteBindingRevision,
                        List.of(), List.of()));

        service.assignProfile(analyzer, PROFILE_ID, REVISION, "oe-user-17");

        assertEquals("Hematology bench 1", analyzer.getName());
        assertNull(analyzer.getType());
        assertNull(analyzer.getIdentifierPattern());
        assertSame(siteBindingRevision, analyzer.getSiteBindingRevision());
        assertSame(selected, analyzer.getPinnedProfileBinding());
    }

    @Test
    public void assignProfileLetsTwoAnalyzersShareOneExactProfileAndSiteBindingRevision() {
        Analyzer first = new Analyzer();
        Analyzer second = new Analyzer();
        AnalyzerProfileBinding selected = binding(FINGERPRINT);
        JsonNode profile = catalog("ACTIVE", FINGERPRINT).profiles().get(0).profile();
        AnalyzerSiteBindingRevision sharedRevision = siteBindingRevision(selected);
        AnalyzerSiteBindingSnapshot sharedSnapshot = new AnalyzerSiteBindingSnapshot(sharedRevision.getSiteBinding(),
                sharedRevision, List.of(), List.of());
        when(catalogService.getCatalog()).thenReturn(catalog("ACTIVE", FINGERPRINT));
        when(bindingDAO.findByProfileIdAndRevision(PROFILE_ID, REVISION)).thenReturn(Optional.of(selected));
        when(siteBindingService.resolveInitialRevision(eq(selected), eq(profile), eq("oe-user-17")))
                .thenReturn(sharedSnapshot);

        AnalyzerProfileBinding firstBinding = service.assignProfile(first, PROFILE_ID, REVISION, "oe-user-17");
        AnalyzerProfileBinding secondBinding = service.assignProfile(second, PROFILE_ID, REVISION, "oe-user-17");

        assertSame(selected, firstBinding);
        assertSame(selected, secondBinding);
        assertSame(sharedRevision, first.getSiteBindingRevision());
        assertSame(sharedRevision, second.getSiteBindingRevision());
        assertSame(selected, first.getPinnedProfileBinding());
        assertSame(selected, second.getPinnedProfileBinding());
        verify(siteBindingService, org.mockito.Mockito.times(2)).resolveInitialRevision(selected, profile,
                "oe-user-17");
    }

    @Test
    public void getAnalyzerUsageCountUsesProfileBindingReferences() {
        when(bindingDAO.countAnalyzersByBindingId("41")).thenReturn(2L);

        assertEquals(2L, service.getAnalyzerUsageCount("41"));
    }

    private BridgeProfileCatalog catalog(String status, String fingerprint) {
        JsonNode profile;
        try {
            profile = objectMapper.readTree(
                    """
                            {
                              "schemaVersion":"1.0",
                              "profileMeta":{"id":"%s","version":"1.0.0","displayName":"Mock Hematology","confidence":"VALIDATED"},
                              "protocol":{"name":"ASTM","version":"LIS2-A2"},
                              "identifier_pattern":"MOCK-H|ACME",
                              "communication":{"mode":"BOTH","supports_lis_initiated":true},
                              "default_test_mappings":[{"test_code":"WBC","loinc":"6690-2","result_type":"quantitative"}],
                              "configDefaults":{"connectionRole":"SERVER","transport":"TCP/IP","port":9100,"aggregationMode":"PER_MESSAGE"},
                              "catalog":{"revision":%d,"revisionFingerprint":"%s","source":"SITE","status":"%s"}
                            }
                            """
                            .formatted(PROFILE_ID, REVISION, fingerprint, status));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        JsonNode publication = objectMapper.createObjectNode().put("action", "CREATED");
        return new BridgeProfileCatalog("1.0",
                "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                List.of(new BridgeProfileCatalog.ProfileRevision(profile, publication)));
    }

    private static AnalyzerProfileBinding binding(String fingerprint) {
        AnalyzerProfileBinding binding = new AnalyzerProfileBinding();
        binding.setId("41");
        binding.setProfileId(PROFILE_ID);
        binding.setProfileRevision(REVISION);
        binding.setProfileFingerprint(fingerprint);
        return binding;
    }

    private static AnalyzerSiteBindingRevision siteBindingRevision(AnalyzerProfileBinding profileBinding) {
        AnalyzerSiteBinding siteBinding = new AnalyzerSiteBinding();
        siteBinding.setId("51");
        siteBinding.setProfileBinding(profileBinding);
        AnalyzerSiteBindingRevision revision = new AnalyzerSiteBindingRevision();
        revision.setId("61");
        revision.setSiteBinding(siteBinding);
        revision.setRevisionNumber(1);
        return revision;
    }
}
