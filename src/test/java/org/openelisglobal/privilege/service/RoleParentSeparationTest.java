package org.openelisglobal.privilege.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.privilege.dao.PrivilegeDAO;
import org.openelisglobal.privilege.valueholder.Privilege;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.userrole.service.UserRoleService;

/**
 * Privilege inheritance follows {@code parent_role_id}; UI grouping follows
 * {@code grouping_parent}. These are separate columns with separate meanings,
 * and this test pins that they do not influence each other.
 *
 * <p>
 * They used to be one column, which made the two meanings mutually exclusive
 * per role. Pointing at a base role to inherit its privileges meant the role no
 * longer sat under a UI container, so
 * {@code UnifiedSystemUserRestController#setupRoles} dropped it and it became
 * unassignable; pointing at a container to stay visible meant inheriting
 * whatever that container held. The second half was latent rather than live
 * only because the three shipped containers happen to hold zero privileges —
 * granting one privilege to "Lab Unit Roles" would have silently widened every
 * bench role.
 */
@RunWith(MockitoJUnitRunner.class)
public class RoleParentSeparationTest {

    @Mock
    private PrivilegeDAO privilegeDAO;

    @Mock
    private RoleService roleService;

    @Mock
    private UserRoleService userRoleService;

    @InjectMocks
    private PrivilegeServiceImpl privilegeService;

    /** A container row: what grouping_parent points at. Holds no privileges. */
    private Role labUnitRolesGroup;
    /** A real, assignable role. */
    private Role reception;
    /** Inherits from Reception, and renders under the container. */
    private Role receptionPlus;

    private Privilege orderCreate;
    private Privilege resultView;

    @Before
    public void setup() {
        labUnitRolesGroup = role(70, "Lab Unit Roles");
        labUnitRolesGroup.setGroupingRole(true);

        reception = role(4, "Reception");
        reception.setGroupingParent(70);

        receptionPlus = role(90, "Reception Plus Results View");
        receptionPlus.setGroupingParent(70); // renders under the same UI container
        receptionPlus.setParentRoleId(4); // inherits Reception's privileges

        orderCreate = privilege("order:create");
        resultView = privilege("result:view");
    }

    private static Role role(int id, String name) {
        Role r = new Role();
        r.setId(id);
        r.setName(name);
        return r;
    }

    private static Privilege privilege(String name) {
        Privilege p = new Privilege();
        p.setName(name);
        p.setActive(true);
        return p;
    }

    /**
     * The shape the old single column could not express: inherit from a real role
     * AND still sit under a UI container.
     */
    @Test
    public void aRoleCanInheritFromARealRoleWhileRenderingUnderAContainer() {
        when(roleService.getRoleById(90)).thenReturn(receptionPlus);
        when(roleService.getRoleById(4)).thenReturn(reception);
        when(privilegeDAO.getPrivilegesForRole(90)).thenReturn(List.of(resultView));
        when(privilegeDAO.getPrivilegesForRole(4)).thenReturn(List.of(orderCreate));

        Set<String> effective = privilegeService.resolveAllPrivilegesForRole("90");

        assertTrue("own privilege", effective.contains("result:view"));
        assertTrue("inherited from Reception", effective.contains("order:create"));
        // ...and it still points at the container, so the UI can group it.
        assertEquals(Integer.valueOf(70), receptionPlus.getGroupingParent());
    }

    /**
     * Inversion: grouping_parent must NOT grant anything. If resolution ever reads
     * it again, this fails — a container with privileges would silently widen every
     * role rendered under it.
     */
    @Test
    public void groupingParentGrantsNothing() {
        Role underGroupOnly = role(91, "Grouped But Not Inheriting");
        underGroupOnly.setGroupingParent(70); // container only, no parentRoleId

        when(roleService.getRoleById(91)).thenReturn(underGroupOnly);
        when(privilegeDAO.getPrivilegesForRole(91)).thenReturn(List.of(resultView));
        // The container is deliberately given a privilege it must not hand out.
        // lenient(): if resolution is correct this stub is never reached, and that
        // is the point — strict stubs would otherwise fail for the right reason in
        // a confusing way. verify(never()) below asserts it explicitly.
        org.mockito.Mockito.lenient().when(privilegeDAO.getPrivilegesForRole(70)).thenReturn(List.of(orderCreate));

        Set<String> effective = privilegeService.resolveAllPrivilegesForRole("91");

        assertEquals(Set.of("result:view"), effective);
        assertFalse("a UI container must never grant privileges", effective.contains("order:create"));
        // Not merely absent from the result — never consulted at all.
        org.mockito.Mockito.verify(privilegeDAO, org.mockito.Mockito.never()).getPrivilegesForRole(70);
        org.mockito.Mockito.verify(roleService, org.mockito.Mockito.never()).getRoleById(70);
    }

    /** A role with neither pointer resolves to its own privileges only. */
    @Test
    public void noParentMeansNoInheritance() {
        Role standalone = role(92, "Standalone");
        when(roleService.getRoleById(92)).thenReturn(standalone);
        when(privilegeDAO.getPrivilegesForRole(92)).thenReturn(List.of(resultView));

        assertEquals(Set.of("result:view"), privilegeService.resolveAllPrivilegesForRole("92"));
    }

    /** The cycle guard still terminates when the cycle is on the new column. */
    @Test
    public void selfReferenceTerminates() {
        Role loop = role(93, "Loop");
        loop.setParentRoleId(93);
        when(roleService.getRoleById(93)).thenReturn(loop);
        when(privilegeDAO.getPrivilegesForRole(93)).thenReturn(Collections.emptyList());

        assertEquals(Collections.emptySet(), privilegeService.resolveAllPrivilegesForRole("93"));
    }
}
