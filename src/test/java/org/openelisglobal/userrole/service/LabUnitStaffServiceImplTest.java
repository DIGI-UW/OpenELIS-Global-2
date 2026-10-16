package org.openelisglobal.userrole.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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
        // 10: Results on Cytology; 11: Results on all units; 12: Reports on Cytology;
        // 13: Results on Pathology; 14: Results on Cytology but deactivated
        List<UserLabUnitRoles> all = List.of(labRoles(10, Map.of(CYTOLOGY, Set.of(RESULTS))),
                labRoles(11, Map.of("AllLabUnits", Set.of(RESULTS, REPORTS))),
                labRoles(12, Map.of(CYTOLOGY, Set.of(REPORTS))), labRoles(13, Map.of(PATHOLOGY, Set.of(RESULTS))),
                labRoles(14, Map.of(CYTOLOGY, Set.of(RESULTS))));
        when(userRoleService.getAllUserLabUnitRoles()).thenReturn(all);
        all.forEach(r -> when(userRoleService.getUserLabUnitRoles(String.valueOf(r.getId()))).thenReturn(r));
        user("10", "Kayla", "Y");
        user("11", "Andre", "Y");
        user("12", "Kevin", "Y");
        user("13", "Priya", "Y");
        user("14", "Leaver", "N");
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

    @Test
    public void unknownRoleOrUnit_listsNobody() {
        assertTrue(service.getActiveUsersWithRole("Results", "No Such Unit").isEmpty());
        assertTrue(service.getActiveUsersWithRole("No Such Role", "Cytology").isEmpty());
    }

    @Test
    public void hasRole_checksTheRoleAndTheUnit() {
        assertTrue(service.hasRole("10", "Results", "Cytology"));
        assertFalse(service.hasRole("10", "Results", "Pathology"));
        assertTrue(service.hasRole("11", "Results", "Pathology"));
        assertFalse(service.hasRole("12", "Results", "Cytology"));
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
}
