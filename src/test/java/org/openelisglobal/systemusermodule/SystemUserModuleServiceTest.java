package org.openelisglobal.systemusermodule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.systemusermodule.service.SystemUserModuleService;
import org.openelisglobal.systemusermodule.valueholder.SystemUserModule;
import org.springframework.beans.factory.annotation.Autowired;

public class SystemUserModuleServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SystemUserModuleService systemUserModuleService;

    private List<SystemUserModule> systemUserModules;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/system-user-module.xml");
    }

    @Test
    public void getData_ShouldReturnDataForASystemUserModule() {
        SystemUserModule systemUserModule = systemUserModuleService.get("2");
        systemUserModuleService.getData(systemUserModule);
        assertNotNull(systemUserModule);
        assertEquals("Y", systemUserModule.getHasSelect());
    }

    @Test
    public void getAllPermissionModules_ShouldReturnAllSystemUserModules() {
        systemUserModules = systemUserModuleService.getAllPermissionModules();
        assertNotNull(systemUserModules);
        assertEquals(3, systemUserModules.size());
        assertEquals("Y", systemUserModules.get(0).getHasUpdate());
    }

    @Test
    public void getTotalPermissionModuleCount_ShouldReturnNumberOfSystemUserModules() {
        Integer systemUserModuleCount = systemUserModuleService.getTotalPermissionModuleCount();
        assertNotNull(systemUserModuleCount);
        assertEquals(3, (int) systemUserModuleCount);
    }

    @Test
    public void getPageOfPermissionModules_ShouldReturnAPageOfSystemUserModules() {
        int numberOfPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        systemUserModules = systemUserModuleService.getPageOfPermissionModules(1);
        assertTrue(numberOfPages >= systemUserModules.size());
    }

    @Test
    public void getAllPermissionModulesByAgentId_ShouldReturnAllSystemUserModules_UsingAgentId() {
        systemUserModules = systemUserModuleService.getAllPermissionModulesByAgentId(1002);
        assertNotNull(systemUserModules);
        assertEquals(1, systemUserModules.size());
        assertEquals("Y", systemUserModules.get(0).getHasDelete());
    }

    @Test
    public void doesUserHaveAnyModules_ShouldReturnTrueIfTheUserHasModules() {
        boolean AreModulesPresent = systemUserModuleService.doesUserHaveAnyModules(1001);
        assertTrue(AreModulesPresent);
    }

    @Test
    public void getAllPermittedPagesFromAgentId_ShouldReturnAllPermittedPages_UsingAnAgentId() {
        Set<String> permittedPages = systemUserModuleService.getAllPermittedPagesFromAgentId(1003);
        assertNotNull(permittedPages);
        assertEquals(1, permittedPages.size());
        assertEquals("Module 2", permittedPages.iterator().next());
    }
}
