package org.openelisglobal.rolemodule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.rolemodule.service.RoleModuleService;
import org.openelisglobal.systemusermodule.valueholder.RoleModule;
import org.springframework.beans.factory.annotation.Autowired;

public class RoleModuleServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private RoleModuleService roleModuleService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/role-module.xml");
    }

    @Test
    public void getAllPermissionModules_shouldReturnAll() {
        List<RoleModule> modules = roleModuleService.getAllPermissionModules();
        assertNotNull(modules);
        assertEquals(3, modules.size());
    }

    // Exercises RoleModuleDAOImpl.getAllPermissionModulesByAgentId which passes
    // an int role ID to an HQL query on Role.id (String via
    // LIMSStringNumberUserType). Before the fix, this threw:
    // Parameter value [3001] did not match expected type [java.lang.String]
    @Test
    public void getAllPermissionModulesByAgentId_shouldReturnModulesForRole() {
        List<RoleModule> modules = roleModuleService.getAllPermissionModulesByAgentId(3001);
        assertNotNull(modules);
        assertEquals(2, modules.size());
        assertTrue(modules.stream().allMatch(m -> m.getRole().getId().equals("3001")));
    }

    @Test
    public void getAllPermissionModulesByAgentId_noModules_shouldReturnEmpty() {
        List<RoleModule> modules = roleModuleService.getAllPermissionModulesByAgentId(9999);
        assertNotNull(modules);
        assertEquals(0, modules.size());
    }

    @Test
    public void getRoleModuleByRoleAndModuleId_shouldReturnMatch() {
        RoleModule module = roleModuleService.getRoleModuleByRoleAndModuleId("3001", "2001");
        assertNotNull(module);
        assertEquals("Y", module.getHasSelect());
        assertEquals("Y", module.getHasAdd());
    }

    @Test
    public void getRoleModuleByRoleAndModuleId_noMatch_shouldReturnEmptyRoleModule() {
        RoleModule module = roleModuleService.getRoleModuleByRoleAndModuleId("3002", "2002");
        assertNotNull(module);
        // No matching row - returns a new empty RoleModule
        assertTrue(module.getId() == null);
    }

    @Test
    public void doesUserHaveAnyModules_shouldReturnTrue_whenUserHasRoles() {
        boolean hasModules = roleModuleService.doesUserHaveAnyModules(1001);
        assertTrue(hasModules);
    }

    @Test
    public void doesUserHaveAnyModules_shouldReturnFalse_whenUserHasNoRoles() {
        boolean hasModules = roleModuleService.doesUserHaveAnyModules(9999);
        assertEquals(false, hasModules);
    }
}
