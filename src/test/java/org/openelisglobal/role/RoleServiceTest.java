package org.openelisglobal.role;

import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.springframework.beans.factory.annotation.Autowired;

public class RoleServiceTest extends BaseWebContextSensitiveTest {
    @Autowired
    RoleService roleService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/role.xml");
    }

    @Test
    public void getData_shouldReturncopiedPropertiesFromDatabase() {
        Role role = new Role();
        role.setId("1");
        roleService.getData(role);

        Assert.assertEquals("Global Administrator", role.getName().trim());
    }

    @Test
    public void getAllActiveRoles_shouldReturnAllActiveRoles() {
        Assert.assertEquals(4, roleService.getAllActiveRoles().size());
    }

    @Test
    public void getPageOfRoles_shouldReturnPageOfRoles() {
        List<Role> rolesPage = roleService.getPageOfRoles(1);

        int expectedPageSize = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        Assert.assertTrue(rolesPage.size() <= expectedPageSize);

        if (expectedPageSize >= 5) {
            Assert.assertTrue(rolesPage.stream().anyMatch(r -> r.getName().trim().equals("Global Administrator")));
            Assert.assertTrue(rolesPage.stream().anyMatch(r -> r.getName().trim().equals("Reception")));
        }
    }

    @Test
    public void getRoleByName_shouldReturnRoleByName() {
        Role role = roleService.getRoleByName("Inventory mgr");
        Assert.assertEquals("tracks inventory.", role.getDescription());
    }

    @Test
    public void getAllRoles_shouldReturnAllRoles() {
        Assert.assertEquals(5, roleService.getAllRoles().size());
    }

    @Test
    public void getRoleById_shouldReturnRoleById() {
        Role role = roleService.getRoleById("4");
        Assert.assertEquals("enter and review results.", role.getDescription());
    }
}
