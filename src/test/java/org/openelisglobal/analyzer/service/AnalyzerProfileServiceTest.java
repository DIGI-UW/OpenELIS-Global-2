package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.dao.AnalyzerProfileApplicationDAO;
import org.openelisglobal.analyzer.dao.AnalyzerProfileDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfile;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileApplication;
import org.openelisglobal.common.exception.LIMSRuntimeException;

/**
 * Unit tests for AnalyzerProfileService.
 *
 * <p>
 * Test cases: import with version policy (duplicate rejected, new version
 * accepted), designated latest, built-in immutability, export.
 */
@RunWith(MockitoJUnitRunner.class)
public class AnalyzerProfileServiceTest {

    @Mock
    private AnalyzerProfileDAO analyzerProfileDAO;

    @Mock
    private AnalyzerProfileApplicationDAO analyzerProfileApplicationDAO;

    @InjectMocks
    private AnalyzerProfileServiceImpl analyzerProfileService;

    private Map<String, Object> validProfilePayload;

    @Before
    public void setUp() {
        validProfilePayload = new HashMap<>();
        Map<String, Object> meta = new HashMap<>();
        meta.put("id", "test-profile");
        meta.put("version", "1.0.0");
        meta.put("displayName", "Test Profile");
        validProfilePayload.put("profileMeta", meta);
    }

    @Test
    public void testImportProfile_ValidPayload_ReturnsId() {
        when(analyzerProfileDAO.existsByMetaIdAndVersion("test-profile", "1.0.0")).thenReturn(false);
        when(analyzerProfileDAO.insert(any(AnalyzerProfile.class))).thenReturn("profile-uuid-123");

        String id = analyzerProfileService.importProfile(validProfilePayload, "SITE", "user1");

        assertNotNull(id);
        assertEquals("profile-uuid-123", id);
        verify(analyzerProfileDAO).existsByMetaIdAndVersion("test-profile", "1.0.0");
        verify(analyzerProfileDAO).insert(any(AnalyzerProfile.class));
    }

    @Test
    public void testImportProfile_DuplicateVersion_ThrowsException() {
        when(analyzerProfileDAO.existsByMetaIdAndVersion("test-profile", "1.0.0")).thenReturn(true);

        LIMSRuntimeException ex = assertThrows(LIMSRuntimeException.class,
                () -> analyzerProfileService.importProfile(validProfilePayload, "SITE", "user1"));

        assertEquals(true, ex.getMessage().contains("Duplicate"));
        verify(analyzerProfileDAO).existsByMetaIdAndVersion("test-profile", "1.0.0");
        verify(analyzerProfileDAO, org.mockito.Mockito.never()).insert(any());
    }

    @Test
    public void testImportProfile_NewVersion_Accepted() {
        when(analyzerProfileDAO.existsByMetaIdAndVersion("test-profile", "2.0.0")).thenReturn(false);
        when(analyzerProfileDAO.insert(any(AnalyzerProfile.class))).thenReturn("profile-uuid-456");

        Map<String, Object> meta = new HashMap<>();
        meta.put("id", "test-profile");
        meta.put("version", "2.0.0");
        meta.put("displayName", "Test Profile v2");
        validProfilePayload.put("profileMeta", meta);

        String id = analyzerProfileService.importProfile(validProfilePayload, "SITE", "user1");

        assertEquals("profile-uuid-456", id);
        verify(analyzerProfileDAO).existsByMetaIdAndVersion("test-profile", "2.0.0");
    }

    @Test
    public void testImportProfile_MissingProfileMeta_ThrowsException() {
        validProfilePayload.remove("profileMeta");

        assertThrows(LIMSRuntimeException.class,
                () -> analyzerProfileService.importProfile(validProfilePayload, "SITE", "user1"));

        verify(analyzerProfileDAO, org.mockito.Mockito.never()).insert(any());
    }

    @Test
    public void testImportProfile_MissingProfileMetaId_ThrowsException() {
        @SuppressWarnings("unchecked")
        Map<String, Object> meta = (Map<String, Object>) validProfilePayload.get("profileMeta");
        meta.remove("id");

        assertThrows(LIMSRuntimeException.class,
                () -> analyzerProfileService.importProfile(validProfilePayload, "SITE", "user1"));
    }

    @Test
    public void testListBySource_ReturnsProfiles() {
        AnalyzerProfile p1 = new AnalyzerProfile();
        p1.setId("1");
        p1.setProfileMetaId("test");
        when(analyzerProfileDAO.findBySource("BUILT_IN")).thenReturn(Arrays.asList(p1));

        List<AnalyzerProfile> result = analyzerProfileService.listBySource("BUILT_IN");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("1", result.get(0).getId());
    }

    @Test
    public void testGetLatestByMetaId_ReturnsProfile() {
        AnalyzerProfile latest = new AnalyzerProfile();
        latest.setId("latest-id");
        latest.setProfileMetaId("genexpert-cepheid-astm");
        when(analyzerProfileDAO.findLatestByMetaId("genexpert-cepheid-astm")).thenReturn(latest);

        AnalyzerProfile result = analyzerProfileService.getLatestByMetaId("genexpert-cepheid-astm");

        assertNotNull(result);
        assertEquals("latest-id", result.getId());
    }

    @Test
    public void testApplyProfileToAnalyzer_CreatesProvenance() {
        AnalyzerProfile profile = new AnalyzerProfile();
        profile.setId("profile-1");
        profile.setProfileMetaId("test-profile");
        profile.setProfileMetaVersion("1.0.0");
        when(analyzerProfileDAO.get("profile-1")).thenReturn(Optional.of(profile));

        analyzerProfileService.applyProfileToAnalyzer("2013", "profile-1", "user1");

        verify(analyzerProfileApplicationDAO).insert(any(AnalyzerProfileApplication.class));
    }

    @Test
    public void testApplyProfileToAnalyzer_ProfileNotFound_ThrowsException() {
        when(analyzerProfileDAO.get("nonexistent")).thenReturn(Optional.empty());

        assertThrows(LIMSRuntimeException.class,
                () -> analyzerProfileService.applyProfileToAnalyzer("2013", "nonexistent", "user1"));

        verify(analyzerProfileApplicationDAO, org.mockito.Mockito.never()).insert(any());
    }

}
