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
        TestSection labUnit = testSectionService.getTestSectionByName(labUnitName);
        if (role == null || labUnit == null) {
            return List.of();
        }
        return userRoleService.getAllUserLabUnitRoles().stream()
                .filter(roles -> covers(roles, labUnit.getId(), role.getId()))
                .map(roles -> systemUserService.get(String.valueOf(roles.getId())))
                .filter(user -> user != null && "Y".equals(user.getIsActive()))
                .sorted(Comparator.comparing(SystemUser::getDisplayName, String.CASE_INSENSITIVE_ORDER))
                .map(user -> new IdValuePair(user.getId(), user.getDisplayName())).collect(Collectors.toList());
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
