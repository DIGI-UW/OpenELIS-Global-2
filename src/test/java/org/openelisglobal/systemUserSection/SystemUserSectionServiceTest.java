package org.openelisglobal.systemUserSection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.systemusersection.service.SystemUserSectionService;
import org.openelisglobal.systemusersection.valueholder.SystemUserSection;
import org.springframework.beans.factory.annotation.Autowired;

public class SystemUserSectionServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SystemUserSectionService systemUserSectionService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/system-user-section.xml");

    }

    @Test
    public void testDataBase() {
        List<SystemUserSection> systemUserSections = systemUserSectionService.getAll();
        systemUserSections.forEach(systemUserSection -> {
            System.out.print(systemUserSection.getId() + " ");
        });
    }

    @Test
    public void testGetAll() {
        List<SystemUserSection> systemUserSections = systemUserSectionService.getAllSystemUserSections();
        assertEquals(2, systemUserSections.size());
        assertEquals("1", systemUserSections.get(0).getId());
        assertEquals("2", systemUserSections.get(1).getId());
    }

    @Test
    public void getPageOfSystemUserSections() {
        List<SystemUserSection> systemUserSections = systemUserSectionService.getPageOfSystemUserSections(1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(systemUserSections.size() <= expectedPages);
    }

    @Test
    public void getTotalSystemUserSectionCount() {
        List<SystemUserSection> systemUserSections = systemUserSectionService.getAll();
        int systemCount = systemUserSectionService.getTotalSystemUserSectionCount();
        assertEquals(systemUserSections.size(), systemCount);
    }

    @Test
    public void getAllSystemUserSectionsBySystemUserId() {
        List<SystemUserSection> systemUserSections = systemUserSectionService.getAllSystemUserSectionsBySystemUserId(3);
        assertEquals(2, systemUserSections.size());
        assertEquals("1", systemUserSections.get(0).getId());
        assertEquals("2", systemUserSections.get(1).getId());

    }

}
