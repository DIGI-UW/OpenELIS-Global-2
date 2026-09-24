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

    /**
     * system_role.name is character(30). Before this guard an over-long name
     * reached Postgres, which rejected the insert ("value too long for type
     * character(30)"); Hibernate wrapped that as a DataException and the caller got
     * a bare 500 saying only that the role could not be created. A 31-character
     * name — one over — was enough to trigger it, with nothing pointing at the
     * name.
     */
    @Test
    public void createAssignableRole_shouldRejectNameLongerThanColumn() {
        String tooLong = "X".repeat(31);

        IllegalArgumentException thrown = Assert.assertThrows(IllegalArgumentException.class,
                () -> roleService.createAssignableRole(tooLong, "desc", null, "Global Roles", null, "1"));

        Assert.assertTrue("message should name the limit and the actual length, got: " + thrown.getMessage(),
                thrown.getMessage().contains("30") && thrown.getMessage().contains("31"));
    }

    @Test
    public void createAssignableRole_shouldRejectDescriptionLongerThanColumn() {
        IllegalArgumentException thrown = Assert.assertThrows(IllegalArgumentException.class, () -> roleService
                .createAssignableRole("ZZ Len Probe", "Y".repeat(81), null, "Global Roles", null, "1"));

        Assert.assertTrue("message should name the description limit, got: " + thrown.getMessage(),
                thrown.getMessage().contains("80"));
    }

    /**
     * Inversion test: the guard must reject only what the column cannot hold, so a
     * name of exactly 30 characters has to get PAST the length check. It still
     * fails afterwards on the grouping parent, because this fixture seeds no
     * grouping roles — that is the point. Asserting the message is about the parent
     * and not about length proves the boundary is {@code > 30} rather than
     * {@code >= 30}; a bare assertThrows here would pass even if the guard were off
     * by one.
     */
    @Test
    public void createAssignableRole_shouldNotRejectNameExactlyAtColumnLimitForLength() {
        String exact = "Z".repeat(30);

        IllegalArgumentException thrown = Assert.assertThrows(IllegalArgumentException.class,
                () -> roleService.createAssignableRole(exact, "at the limit", null, "Global Roles", null, "1"));

        Assert.assertTrue("a 30-character name must clear the length guard, got: " + thrown.getMessage(),
                thrown.getMessage().contains("grouping parent"));
    }

    @Test
    public void getData_shouldReturncopiedPropertiesFromDatabase() {
        Role role = new Role();
        role.setId(1);
        roleService.getData(role);

        Assert.assertEquals("Global Administrator", role.getName().trim());
    }

    @Test
    public void getAllActiveRoles_shouldReturnAllActiveRoles() {
        Assert.assertEquals(6, roleService.getAllActiveRoles().size());
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
        Assert.assertEquals(6, roleService.getAllRoles().size());
    }

    @Test
    public void getRoleById_shouldReturnRoleById() {
        Role role = roleService.getRoleById(4);
        Assert.assertEquals("enter and review results.", role.getDescription());
    }
}
