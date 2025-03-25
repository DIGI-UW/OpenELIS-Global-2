package org.openelisglobal.systemModule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.systemmodule.service.SystemModuleService;
import org.openelisglobal.systemmodule.valueholder.SystemModule;
import org.springframework.beans.factory.annotation.Autowired;

public class SystemModuleServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SystemModuleService systemModuleService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/system-module.xml");
    }

    @Test
    public void testDatabaseData() {
        List<SystemModule> systemModules = systemModuleService.getAll();
        assertFalse("SystemModules should not be empty", systemModules.isEmpty());
        systemModules.forEach(systemModule -> {
            assertNotNull("SystemModule name should not be null", systemModule.getSystemModuleName());
            assertFalse("SystemModule name should not be empty", systemModule.getSystemModuleName().isEmpty());
        });
    }

    @Test
    public void getData_shouldReturnDataGivenSystemModule() {
        SystemModule systemModule = systemModuleService.get("1");
        systemModuleService.getData(systemModule);
        assertEquals("Module 1", systemModule.getSystemModuleName());
        assertEquals("Description for Module 1", systemModule.getDescription());

    }

    @Test
    public void getTotalSystemModuleCount() {
        int totalCount = systemModuleService.getTotalSystemModuleCount();
        assertEquals(4, totalCount);
    }

    @Test
    public void getPageOfSystemModules() {
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        List<SystemModule> pages = systemModuleService.getPageOfSystemModules(1);
        assertTrue(pages.size() <= expectedPages);
    }

    @Test
    public void getAllSystemModules() {
        List<SystemModule> systemModules = systemModuleService.getAllSystemModules();
        assertEquals(4, systemModules.size());
    }

    @Test
    public void getSystemModuleByName_shouldReturnSystemModuleGivenName() {
        SystemModule systemModule1 = systemModuleService.getSystemModuleByName("Module 1");
        assertEquals("Description for Module 1", systemModule1.getDescription());

        SystemModule systemModule2 = systemModuleService.getSystemModuleByName("Module 2");
        assertEquals("Description for Module 2", systemModule2.getDescription());

    }

}
