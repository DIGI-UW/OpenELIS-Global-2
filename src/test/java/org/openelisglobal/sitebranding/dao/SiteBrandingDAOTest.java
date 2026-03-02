package org.openelisglobal.sitebranding.dao;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.sitebranding.valueholder.SiteBranding;
import org.springframework.beans.factory.annotation.Autowired;

public class SiteBrandingDAOTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SiteBrandingDAO siteBrandingDAO;

    @Before
public void setUp() throws Exception {
    super.setUp();
    executeDataSetWithStateManagement("testdata/site-branding.xml");
}

    private SiteBranding createTestBranding(String primaryColor) {
        SiteBranding branding = new SiteBranding();
        branding.setPrimaryColor(primaryColor);
        branding.setSecondaryColor("#64748b");
        branding.setHeaderColor("#0891b2");
        branding.setColorMode("light");
        branding.setUseHeaderLogoForLogin(false);
        branding.setSysUserId("1");
        return branding;
    }

    @Test
    public void testGetBranding_WhenNoneExists_ReturnsNull() {
        SiteBranding result = siteBrandingDAO.getBranding();
        assertNull("Result should be null when no branding exists", result);
    }

    @Test
    public void testGetBranding_WhenExists_ReturnsBranding() {
        SiteBranding branding = createTestBranding("#1d4ed8");
        siteBrandingDAO.insert(branding);
        SiteBranding result = siteBrandingDAO.getBranding();
        assertNotNull("Result should not be null", result);
        assertNotNull("ID should not be null", result.getId());
        assertEquals("Primary color should match", "#1d4ed8", result.getPrimaryColor());
    }

    @Test
    public void testInsert_WithValidBranding_PersistsToDatabase() {
        SiteBranding branding = createTestBranding("#ff0000");
        branding.setSecondaryColor("#00ff00");
        branding.setHeaderColor("#0000ff");
        Integer id = siteBrandingDAO.insert(branding);
        assertNotNull("ID should be generated", id);
        SiteBranding retrieved = siteBrandingDAO.get(id)
                .orElseThrow(() -> new AssertionError("Retrieved branding should not be null"));
        assertEquals("Primary color should match", "#ff0000", retrieved.getPrimaryColor());
    }

    @Test
    public void testUpdate_WithExistingBranding_UpdatesDatabase() {
        SiteBranding branding = createTestBranding("#1d4ed8");
        Integer testId = siteBrandingDAO.insert(branding);
        SiteBranding existing = siteBrandingDAO.get(testId)
                .orElseThrow(() -> new AssertionError("Branding should exist"));
        existing.setPrimaryColor("#ff0000");
        existing.setSysUserId("1");
        SiteBranding result = siteBrandingDAO.update(existing);
        assertNotNull("Result should not be null", result);
        assertEquals("Primary color should be updated", "#ff0000", result.getPrimaryColor());
        SiteBranding retrieved = siteBrandingDAO.get(testId)
                .orElseThrow(() -> new AssertionError("Retrieved branding should not be null"));
        assertEquals("Primary color should be updated in database", "#ff0000", retrieved.getPrimaryColor());
    }
}
