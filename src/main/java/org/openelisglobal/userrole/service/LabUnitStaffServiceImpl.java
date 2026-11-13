package org.openelisglobal.userrole.service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.userrole.valueholder.LabUnitRoleMap;
import org.openelisglobal.userrole.valueholder.UserLabUnitRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LabUnitStaffServiceImpl implements LabUnitStaffService {

    static final String ALL_LAB_UNITS = "AllLabUnits";

    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private TestSectionService testSectionService;
    @Autowired
    private SystemUserService systemUserService;

    @Override
    @Transactional(readOnly = true)
    public List<IdValuePair> getActiveUsersWithRole(String roleName, String labUnitName) {
        Role role = roleService.getRoleByName(roleName);
        if (role == null) {
            return List.of();
        }
        TestSection labUnit = testSectionService.getTestSectionByName(labUnitName);
        if (labUnit == null) {
            // The caller names the unit by its identifying test_section.NAME (PR
            // #4684 review). Renaming or translating a unit through the admin UI
            // only edits its Localization, never this name, so those sites still
            // resolve; a site that never had a unit under this exact name (it
            // created or seeded its own instead) would not. Offering nobody there
            // would refuse every newly chosen technician, so this falls back to
            // the unfiltered list the case pages used before this check existed,
            // rather than making that site worse off than before.
            return everyUser();
        }
        // One query, filtered by role and lab unit at the data-access level
        // (mozzy11's PR #4684 review, optional improvement): the previous
        // version loaded every user's lab-unit roles, then fetched each
        // matching user one at a time.
        return userRoleService.getActiveUsersWithRoleOnLabUnit(role.getId(), labUnit.getId()).stream()
                .sorted(Comparator.comparing(IdValuePair::getValue, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    /**
     * Every system user, unfiltered: the lab unit could not be resolved by name, so
     * this mirrors the plain {@code /rest/users} list the case pages read before
     * offering and accepting a technician was restricted to lab-unit staff.
     */
    private List<IdValuePair> everyUser() {
        return systemUserService.getAll().stream().map(user -> new IdValuePair(user.getId(), user.getDisplayName()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasRole(String systemUserId, String roleName, String labUnitName) {
        Role role = roleService.getRoleByName(roleName);
        TestSection labUnit = testSectionService.getTestSectionByName(labUnitName);
        if (role == null || labUnit == null) {
            return false;
        }
        return covers(userRoleService.getUserLabUnitRoles(systemUserId), labUnit.getId(), role.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public void checkTechnician(String newTechnicianId, SystemUser currentTechnician, String labUnitName) {
        if (currentTechnician != null && newTechnicianId.equals(currentTechnician.getId())) {
            return;
        }
        if (testSectionService.getTestSectionByName(labUnitName) == null) {
            // Same unresolved-unit case as getActiveUsersWithRole: accept the
            // technician instead of refusing every save on a site where this
            // name does not resolve to a unit. hasRole() is left reporting the
            // literal, unresolved "false" — this method is where the fallback
            // policy belongs.
            return;
        }
        if (!hasRole(newTechnicianId, Constants.ROLE_RESULTS, labUnitName)) {
            throw new IllegalArgumentException("The technician must have the Results role for " + labUnitName);
        }
    }

    /** Whether the user holds the role on this lab unit or on all lab units. */
    static boolean covers(UserLabUnitRoles roles, String labUnitId, String roleId) {
        if (roles == null || roles.getLabUnitRoleMap() == null) {
            return false;
        }
        for (LabUnitRoleMap map : roles.getLabUnitRoleMap()) {
            Set<String> mapRoles = map.getRoles();
            boolean unitMatches = labUnitId.equals(map.getLabUnit()) || ALL_LAB_UNITS.equals(map.getLabUnit());
            if (unitMatches && mapRoles != null && mapRoles.contains(roleId)) {
                return true;
            }
        }
        return false;
    }
}
