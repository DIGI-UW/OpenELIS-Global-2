package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

    private AnalyzerProfileBindingService service;

    @Before
    public void setUp() {
        service = new AnalyzerProfileBindingServiceImpl(bindingDAO, catalogService);
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
        analyzer.setProfileBinding(existing);

        AnalyzerProfileBinding result = service.assignProfile(analyzer, PROFILE_ID, REVISION, "oe-user-17");

        assertSame(existing, result);
        assertSame(existing, analyzer.getProfileBinding());
        verify(catalogService, never()).getCatalog();
        verify(bindingDAO, never()).insert(any(AnalyzerProfileBinding.class));
    }

    @Test
    public void getAnalyzerUsageCountUsesProfileBindingReferences() {
        when(bindingDAO.countAnalyzersByBindingId("41")).thenReturn(2L);

        assertEquals(2L, service.getAnalyzerUsageCount("41"));
    }

    private BridgeProfileCatalog catalog(String status, String fingerprint) {
        JsonNode profile = objectMapper.createObjectNode().put("profileId", PROFILE_ID).put("revision", REVISION)
                .put("revisionFingerprint", fingerprint).put("status", status);
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
}
