package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Integration test for profile import/apply lifecycle.
 *
 * <p>
 * Verifies: import profile, apply to analyzer, provenance, re-import with new
 * version (accepted), duplicate version (rejected).
 */
public class AnalyzerProfileIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AnalyzerProfileService analyzerProfileService;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate = new JdbcTemplate(dataSource);
        executeDataSetWithStateManagement("testdata/analyzer.xml");
        cleanProfileData();
    }

    private void cleanProfileData() {
        jdbcTemplate.execute("DELETE FROM analyzer_profile_application");
        jdbcTemplate.execute("DELETE FROM analyzer_profile");
    }

    @Test
    public void testImportApplyLifecycle() {
        Map<String, Object> payload = minimalProfilePayload("lifecycle-test", "1.0.0", "Lifecycle Test");

        String profileId = analyzerProfileService.importProfile(payload, "SITE", "1");
        assertNotNull("Import should return profile ID", profileId);

        String analyzerId = "1";
        analyzerProfileService.applyProfileToAnalyzer(analyzerId, profileId, "1");

        Integer provenanceCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM analyzer_profile_application WHERE analyzer_id = ? AND source_profile_id = ?",
                Integer.class, Integer.valueOf(analyzerId), profileId);
        assertEquals("Provenance record should exist", Integer.valueOf(1), provenanceCount);
    }

    @Test
    public void testReimportWithNewVersion_Accepted() {
        Map<String, Object> v1 = minimalProfilePayload("version-test", "1.0.0", "Version Test v1");
        String id1 = analyzerProfileService.importProfile(v1, "SITE", "1");
        assertNotNull(id1);

        Map<String, Object> v2 = minimalProfilePayload("version-test", "1.1.0", "Version Test v2");
        String id2 = analyzerProfileService.importProfile(v2, "SITE", "1");
        assertNotNull("Re-import with new version should succeed", id2);
        assertTrue("New version should have different ID", !id1.equals(id2));
    }

    @Test
    public void testReimportDuplicateVersion_Rejected() {
        Map<String, Object> payload = minimalProfilePayload("dup-test", "1.0.0", "Dup Test");
        analyzerProfileService.importProfile(payload, "SITE", "1");

        try {
            analyzerProfileService.importProfile(payload, "SITE", "1");
            fail("Duplicate version should throw");
        } catch (org.openelisglobal.common.exception.LIMSRuntimeException e) {
            assertTrue("Error should mention duplicate", e.getMessage().contains("Duplicate"));
        }
    }

    private static Map<String, Object> minimalProfilePayload(String id, String version, String displayName) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("id", id);
        meta.put("version", version);
        meta.put("displayName", displayName);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("profileMeta", meta);
        payload.put("analyzer_name", displayName);
        return payload;
    }
}
