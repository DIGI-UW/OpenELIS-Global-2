package org.openelisglobal.userrole.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.userrole.valueholder.LabUnitRoleMap;
import org.openelisglobal.userrole.valueholder.UserLabUnitRoles;

@RunWith(MockitoJUnitRunner.Silent.class)
public class LabUnitStaffServiceImplTest {

    private static final String RESULTS = "5";
    private static final String REPORTS = "7";
    private static final String CYTOLOGY = "165";
    private static final String PATHOLOGY = "163";

    @Mock
    private UserRoleService userRoleService;
    @Mock
    private RoleService roleService;
    @Mock
    private TestSectionService testSectionService;
    @Mock
    private SystemUserService systemUserService;
    @InjectMocks
    private LabUnitStaffServiceImpl service;

    @Before
    public void setUp() {
        role(RESULTS, "Results");
        role(REPORTS, "Reports");
        section(CYTOLOGY, "Cytology");
        section(PATHOLOGY, "Pathology");

        // getActiveUsersWithRoleOnLabUnit is the one-query replacement for
        // loading every user's lab-unit roles and checking them one at a time
        // (PR #4684 review, optional improvement): the filtering by role and
        // unit now happens in UserLabUnitRolesDAOImpl's SQL, which a plain
        // Mockito unit test cannot exercise. These stubs stand in for what
        // that query would answer for the fixture below; the "called once, not
        // per candidate" contract is what the tests here actually check.
        // 10: Results on Cytology; 11: Results on all units; 13: Results on
        // Pathology.
        when(userRoleService.getActiveUsersWithRoleOnLabUnit(RESULTS, CYTOLOGY))
                .thenReturn(List.of(new IdValuePair("10", "Kayla"), new IdValuePair("11", "Andre")));
        when(userRoleService.getActiveUsersWithRoleOnLabUnit(RESULTS, PATHOLOGY))
                .thenReturn(List.of(new IdValuePair("11", "Andre"), new IdValuePair("13", "Priya")));

        // hasRole() and checkTechnician() still look up one user's own
        // lab-unit roles at a time (never a loop over every user), so that
        // path is untouched and still exercised against this fixture.
        // 10: Results on Cytology; 11: Results on all units; 12: Reports on Cytology;
        // 13: Results on Pathology; 14: Results on Cytology but deactivated
        List<UserLabUnitRoles> all = List.of(labRoles(10, Map.of(CYTOLOGY, Set.of(RESULTS))),
                labRoles(11, Map.of("AllLabUnits", Set.of(RESULTS, REPORTS))),
                labRoles(12, Map.of(CYTOLOGY, Set.of(REPORTS))), labRoles(13, Map.of(PATHOLOGY, Set.of(RESULTS))),
                labRoles(14, Map.of(CYTOLOGY, Set.of(RESULTS))));
        all.forEach(r -> when(userRoleService.getUserLabUnitRoles(String.valueOf(r.getId()))).thenReturn(r));

        List<SystemUser> everyone = List.of(user("10", "Kayla", "Y"), user("11", "Andre", "Y"),
                user("12", "Kevin", "Y"), user("13", "Priya", "Y"), user("14", "Leaver", "N"));
        when(systemUserService.getAll()).thenReturn(everyone);
    }

    private void role(String id, String name) {
        Role role = new Role();
        role.setId(id);
        role.setName(name);
        when(roleService.getRoleByName(name)).thenReturn(role);
    }

    private void section(String id, String name) {
        TestSection section = new TestSection();
        section.setId(id);
        when(testSectionService.getTestSectionByName(name)).thenReturn(section);
    }

    private SystemUser user(String id, String firstName, String active) {
        SystemUser user = new SystemUser();
        user.setId(id);
        user.setFirstName(firstName);
        user.setLastName("");
        user.setIsActive(active);
        when(systemUserService.get(id)).thenReturn(user);
        return user;
    }

    private static UserLabUnitRoles labRoles(int userId, Map<String, Set<String>> byUnit) {
        UserLabUnitRoles roles = new UserLabUnitRoles();
        roles.setId(userId);
        Set<LabUnitRoleMap> maps = new HashSet<>();
        byUnit.forEach((unit, roleIds) -> {
            LabUnitRoleMap map = new LabUnitRoleMap();
            map.setLabUnit(unit);
            map.setRoles(roleIds);
            maps.add(map);
        });
        roles.setLabUnitRoleMap(maps);
        return roles;
    }

    private static Set<String> ids(List<IdValuePair> pairs) {
        return pairs.stream().map(IdValuePair::getId).collect(Collectors.toSet());
    }

    @Test
    public void technicians_areActiveUsersWithResultsOnTheUnitOrOnAllUnits() {
        assertEquals(Set.of("10", "11"), ids(service.getActiveUsersWithRole("Results", "Cytology")));
        assertEquals(Set.of("11", "13"), ids(service.getActiveUsersWithRole("Results", "Pathology")));
    }

    /**
     * Regression test for mozzy11's optional PR #4684 scaling suggestion: the old
     * implementation loaded every user's lab-unit roles ({@code
     * getAllUserLabUnitRoles()}) and then fetched each matching user one at a time
     * ({@code systemUserService.get(id)}) to read their active flag and display
     * name. The fix answers both from a single query. If someone reverted the query
     * and went back to that per-user loop, {@code systemUserService} would be hit
     * again here and this fails; with the fix, nothing calls it.
     */
    @Test
    public void activeUsersWithRole_isOneQueryNotOnePerCandidate() {
        service.getActiveUsersWithRole("Results", "Cytology");

        verify(userRoleService, times(1)).getActiveUsersWithRoleOnLabUnit(RESULTS, CYTOLOGY);
        verifyZeroInteractions(systemUserService);
    }

    @Test
    public void unknownRole_listsNobody() {
        // the role itself does not exist: a genuine caller error, not an
        // unresolvable lab unit, so this must stay empty rather than fall back
        assertTrue(service.getActiveUsersWithRole("No Such Role", "Cytology").isEmpty());
    }

    /**
     * Regression test for the PR #4684 review: a lab unit is looked up by its
     * identifying test_section.NAME. Renaming or translating a unit through the
     * admin UI only edits its Localization, never that name, so those sites still
     * resolve; a site whose unit was created or seeded under a different name —
     * here standing in as "No Such Unit", which {@code
     * testSectionService} cannot resolve — must not end up with an empty technician
     * list. Every newly chosen technician would otherwise be refused. This is also
     * the inversion test: with the pre-fix lookup (unit not found -&gt; empty
     * list), this assertion fails.
     */
    @Test
    public void unresolvedLabUnit_fallsBackToEveryUserInsteadOfNobody() {
        List<IdValuePair> fallback = service.getActiveUsersWithRole("Results", "No Such Unit");
        assertEquals(Set.of("10", "11", "12", "13", "14"), ids(fallback));
        // the fallback reads every user directly; it must not also query the
        // lab-unit-scoped list for a unit that was never resolved
        verifyZeroInteractions(userRoleService);
    }

    @Test
    public void hasRole_checksTheRoleAndTheUnit() {
        assertTrue(service.hasRole("10", "Results", "Cytology"));
        assertFalse(service.hasRole("10", "Results", "Pathology"));
        assertTrue(service.hasRole("11", "Results", "Pathology"));
        assertFalse(service.hasRole("12", "Results", "Cytology"));
        // hasRole() itself keeps reporting the literal, unresolved answer; the
        // fallback policy for an unresolved unit lives in checkTechnician() and
        // getActiveUsersWithRole(), not here.
        assertFalse(service.hasRole("10", "Results", "No Such Unit"));
    }

    @Test
    public void newTechnicianWithResultsOnTheUnit_isAccepted() {
        service.checkTechnician("10", null, "Cytology");
        service.checkTechnician("11", user("13", "Priya", "Y"), "Cytology");
    }

    @Test(expected = IllegalArgumentException.class)
    public void newTechnicianWithoutResultsOnTheUnit_isRefused() {
        service.checkTechnician("12", null, "Cytology");
    }

    @Test(expected = IllegalArgumentException.class)
    public void technicianFromAnotherUnit_isRefused() {
        service.checkTechnician("13", user("10", "Kayla", "Y"), "Cytology");
    }

    @Test
    public void keepingTheTechnicianAlreadyOnTheCase_isAllowed() {
        // saved before this check existed: the case must stay editable
        service.checkTechnician("12", user("12", "Kevin", "Y"), "Cytology");
    }

    /**
     * Regression test for the PR #4684 review, on the save side: when the lab unit
     * cannot be resolved by name, the save must not refuse a technician who would
     * otherwise have no Results role anywhere ("12" only has Reports). With the
     * pre-fix check (unresolved unit -&gt; hasRole() false -&gt; refused), this
     * throws; it must not.
     */
    @Test
    public void unresolvedLabUnit_acceptsAnyNewTechnicianOnSave() {
        service.checkTechnician("12", null, "No Such Unit");
    }
}
