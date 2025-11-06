package org.openelisglobal.systemModule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.systemmodule.service.SystemModuleUrlService;
import org.openelisglobal.systemmodule.valueholder.SystemModuleUrl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

public class SystemModuleUrlServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SystemModuleUrlService systemModuleUrlService;

    private List<SystemModuleUrl> systemModuleUrls;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/system-module-url.xml");
    }

    @Test
    public void getByUrlPath_ShouldReturnSystemModuleUrlsWithTheSameUrlPathPassedAsParameter() {
        systemModuleUrls = systemModuleUrlService.getByUrlPath("/settings/profile");
        assertNotNull(systemModuleUrls);
        assertEquals(2, systemModuleUrls.size());
        assertEquals("4", systemModuleUrls.get(1).getId());
    }

    @Test
    public void getByRequest_ShouldReturnAllSystemModuleUrls_UsingAnHttpServletRequest() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setRequestURI("/settings/profile");
        systemModuleUrls = systemModuleUrlService.getByRequest(mockRequest);
        assertNotNull(systemModuleUrls);
        assertEquals(2, systemModuleUrls.size());
        assertEquals("4", systemModuleUrls.get(1).getId());
    }

    @Test
    public void getByModuleAndUrl_ShouldReturnASystemModuleUrl_UsingAModuleIdAndAUrlPath() {
        SystemModuleUrl systemModuleUrl = systemModuleUrlService.getByModuleAndUrl("7002", "/reports/generate");
        assertNotNull(systemModuleUrl);
        assertEquals("302", systemModuleUrl.getParam().getId());
    }

    @Test
    public void delete_ShouldDeleteASystemModuleUrl() {
        systemModuleUrls = systemModuleUrlService.getAll();
        assertEquals(4, systemModuleUrls.size());
        SystemModuleUrl systemModuleUrls = systemModuleUrlService.get("2");
        systemModuleUrlService.delete(systemModuleUrls);
        List<SystemModuleUrl> newSystemModuleUrls = systemModuleUrlService.getAll();
        assertEquals(3, newSystemModuleUrls.size());
    }

    @Test
    public void deleteAll_ShouldDeleteAllSystemModuleUrls() {
        systemModuleUrls = systemModuleUrlService.getAll();
        assertEquals(4, systemModuleUrls.size());
        systemModuleUrlService.deleteAll(systemModuleUrls);
        List<SystemModuleUrl> updatedSystemModuleUrls = systemModuleUrlService.getAll();
        assertTrue(updatedSystemModuleUrls.isEmpty());
    }

}
