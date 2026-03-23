package org.openelisglobal.sitebranding.dao;

import static org.junit.Assert.*;

import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.sitebranding.valueholder.SiteBranding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * DAO tests for SiteBrandingDAO
 * 
 * Tests all HQL queries to ensure they compile and execute correctly. This
 * catches HQL property reference errors (e.g., non-existent properties).
 * 
 * Uses BaseWebContextSensitiveTest (legacy pattern) since project doesn't use
 * Spring Boot. Reference: Testing Roadmap > BaseWebContextSensitiveTest (Legacy
 * Integration)
 * 
 * Task Reference: T007
 */
public class SiteBrandingDAOTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private SiteBrandingDAO siteBrandingDAO;

    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate = new JdbcTemplate(dataSource);
        cleanTestData();
    }

    @After
    public void tearDown() throws Exception {
        cleanTestData();
    }

    private void cleanTestData() {
        try {
            // Clean up ALL site branding data to ensure test isolation
            // This is necessary because some tests create records with auto-generated UUIDs
            jdbcTemplate.execute("DELETE FROM site_branding");
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    /**
     * Test: getBranding - returns single record or null Task Reference: T007
     */
    @Test
    public void testGetBranding_WhenNoneExists_ReturnsNull() {
        // Act: Get branding when none exists
        SiteBranding result = siteBrandingDAO.getBranding();

        // Assert: Returns null
        assertNull("Result should be null when no branding exists", result);
    }

    /**
     * Test: getBranding - returns existing branding Task Reference: T007
     */
    @Test
    public void testGetBranding_WhenExists_ReturnsBranding() {
        // Arrange: Insert test branding using the sequence
        jdbcTemplate.update(
                "INSERT INTO site_branding (id, primary_color, secondary_color, header_color, color_mode, use_header_logo_for_login, sys_user_id, last_updated) "
                        + "VALUES (nextval('site_branding_seq'), ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                "#1d4ed8", "#64748b", "#0891b2", "light", false, 1);

        // Act: Get branding
        SiteBranding result = siteBrandingDAO.getBranding();

        // Assert: Returns branding
        assertNotNull("Result should not be null", result);
        assertNotNull("ID should not be null", result.getId());
        assertEquals("Primary color should match", "#1d4ed8", result.getPrimaryColor());
    }

    /**
     * Test: insert - persists new branding Task Reference: T007
     */
    @Test
    public void testInsert_WithValidBranding_PersistsToDatabase() {
        // Arrange: New branding
        SiteBranding branding = new SiteBranding();
        branding.setPrimaryColor("#ff0000");
        branding.setSecondaryColor("#00ff00");
        branding.setHeaderColor("#0000ff");
        branding.setColorMode("light");
        branding.setUseHeaderLogoForLogin(false);
        branding.setSysUserId("1");

        // Act: Insert branding
        Integer id = siteBrandingDAO.insert(branding);

        // Assert: Branding persisted
        assertNotNull("ID should be generated", id);
        SiteBranding retrieved = siteBrandingDAO.get(id)
                .orElseThrow(() -> new AssertionError("Retrieved branding should not be null"));
        assertEquals("Primary color should match", "#ff0000", retrieved.getPrimaryColor());
    }

    /**
     * Test: update - updates existing branding Task Reference: T007
     */
    @Test
    public void testUpdate_WithExistingBranding_UpdatesDatabase() {
        // Arrange: Insert test branding and get the generated ID
        Integer testId = jdbcTemplate.queryForObject(
                "INSERT INTO site_branding (id, primary_color, secondary_color, header_color, color_mode, use_header_logo_for_login, sys_user_id, last_updated) "
                        + "VALUES (nextval('site_branding_seq'), ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP) RETURNING id",
                Integer.class, "#1d4ed8", "#64748b", "#0891b2", "light", false, 1);

        SiteBranding branding = siteBrandingDAO.get(testId)
                .orElseThrow(() -> new AssertionError("Branding should exist"));

        // Modify branding
        branding.setPrimaryColor("#ff0000");
        branding.setSysUserId("1");

        // Act: Update branding
        SiteBranding result = siteBrandingDAO.update(branding);

        // Assert: Branding updated
        assertNotNull("Result should not be null", result);
        assertEquals("Primary color should be updated", "#ff0000", result.getPrimaryColor());

        // Verify in database
        SiteBranding retrieved = siteBrandingDAO.get(testId)
                .orElseThrow(() -> new AssertionError("Retrieved branding should not be null"));
        assertEquals("Primary color should be updated in database", "#ff0000", retrieved.getPrimaryColor());
    }
}
