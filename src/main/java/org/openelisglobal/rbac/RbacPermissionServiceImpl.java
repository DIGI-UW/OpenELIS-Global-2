package org.openelisglobal.rbac;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.userrole.service.UserRoleService;
import org.openelisglobal.userrole.valueholder.LabUnitRoleMap;
import org.openelisglobal.userrole.valueholder.UserLabUnitRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RbacPermissionServiceImpl implements RbacPermissionService {

    private static final Map<RbacAction, Set<String>> ACTION_ROLES = new EnumMap<>(RbacAction.class);

    static {
        put(RbacAction.REGISTER_SAMPLES,
                Constants.ROLE_RECEPTION, Constants.ROLE_SAMPLE_COLLECTOR, Constants.ROLE_LAB_MANAGER);
        put(RbacAction.PROCESS_SAMPLES,
                "Technician", Constants.ROLE_LABORATORY_TECHNICIAN, Constants.ROLE_LAB_MANAGER,
                Constants.ROLE_PROJECT_COORDINATOR);
        put(RbacAction.UPDATE_SAMPLES,
                Constants.ROLE_RECEPTION, "Technician", Constants.ROLE_LABORATORY_TECHNICIAN,
                Constants.ROLE_JUNIOR_RESEARCHER, Constants.ROLE_SENIOR_RESEARCHER, Constants.ROLE_LAB_MANAGER,
                Constants.ROLE_PROJECT_COORDINATOR);
        put(RbacAction.VALIDATE_RESULTS,
                Constants.ROLE_VALIDATION, Constants.ROLE_LAB_MANAGER, Constants.ROLE_PROJECT_COORDINATOR);
        put(RbacAction.REVIEW_RESULTS,
                "Results", Constants.ROLE_JUNIOR_RESEARCHER, Constants.ROLE_SENIOR_RESEARCHER,
                Constants.ROLE_LAB_MANAGER, Constants.ROLE_PRINCIPAL_INVESTIGATOR, Constants.ROLE_DATA_MANAGER);
        put(RbacAction.GENERATE_REPORTS,
                Constants.ROLE_REPORTS, Constants.ROLE_LAB_MANAGER, Constants.ROLE_ADMINISTRATIVE_STAFF,
                Constants.ROLE_EXTERNAL_STAKEHOLDERS, Constants.ROLE_DATA_MANAGER);
        put(RbacAction.MANAGE_QA,
                Constants.ROLE_LAB_MANAGER, Constants.ROLE_EQA_PERSONNEL);
        put(RbacAction.APPROVE_NOTEBOOK_ENTRY,
                Constants.ROLE_NOTEBOOK_ADMIN, Constants.ROLE_LAB_MANAGER, Constants.ROLE_PRINCIPAL_INVESTIGATOR);
        put(RbacAction.VIEW_AUDIT_TRAIL,
                Constants.ROLE_AUDIT_TRAIL, Constants.ROLE_IT_SUPPORT_STAFF);
        put(RbacAction.SYSTEM_ADMIN,
                Constants.ROLE_SYSTEM_ADMIN);
        put(RbacAction.MANAGE_EQUIPMENT,
                Constants.ROLE_BIOMEDICAL_STAFF, Constants.ROLE_LAB_MANAGER);
    }

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private RoleService roleService;

    @Override
    @Transactional(readOnly = true)
    public boolean hasPermission(HttpServletRequest request, RbacAction action) {
        String sysUserId = getSysUserId(request);
        if (sysUserId == null || action == null) {
            return false;
        }

        if (userRoleService.userInRole(sysUserId, Constants.ROLE_GLOBAL_ADMIN)
                || userRoleService.userInRole(sysUserId, Constants.ROLE_SYSTEM_ADMIN)) {
            return true;
        }

        Set<String> allowedRoles = ACTION_ROLES.getOrDefault(action, Set.of());
        if (allowedRoles.isEmpty()) {
            return false;
        }

        Set<String> userRoles = getRoleNames(sysUserId, request);
        return userRoles.stream().anyMatch(allowedRoles::contains);
    }

    private Set<String> getRoleNames(String sysUserId, HttpServletRequest request) {
        Set<String> names = new HashSet<>();
        addRoleNames(names, userRoleService.getRoleIdsForUser(sysUserId));

        UserLabUnitRoles labUnitRoles = userRoleService.getUserLabUnitRoles(sysUserId);
        if (labUnitRoles == null || labUnitRoles.getLabUnitRoleMap() == null) {
            return names;
        }

        String activeLabUnit = getActiveLabUnit(request);
        for (LabUnitRoleMap map : labUnitRoles.getLabUnitRoleMap()) {
            if (map == null || map.getRoles() == null) {
                continue;
            }
            String mappedLabUnit = map.getLabUnit();
            if (mappedLabUnit == null) {
                continue;
            }
            if ("AllLabUnits".equalsIgnoreCase(mappedLabUnit.trim()) || sameLabUnit(mappedLabUnit, activeLabUnit)) {
                addRoleNames(names, map.getRoles());
            }
        }
        return names;
    }

    private void addRoleNames(Set<String> names, Collection<String> roleIds) {
        if (roleIds == null) {
            return;
        }
        for (String roleId : roleIds) {
            if (roleId == null || roleId.isBlank()) {
                continue;
            }
            Role role = resolveRole(roleId);
            if (role != null && role.getName() != null && !role.getName().isBlank()) {
                names.add(role.getName().trim());
            } else {
                names.add(roleId.trim());
            }
        }
    }

    private Role resolveRole(String roleId) {
        try {
            return roleService.getRoleById(roleId);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String getSysUserId(HttpServletRequest request) {
        UserSessionData usd = getUserSessionData(request);
        return usd == null ? null : String.valueOf(usd.getSystemUserId());
    }

    private String getActiveLabUnit(HttpServletRequest request) {
        UserSessionData usd = getUserSessionData(request);
        if (usd == null || usd.getLoginLabUnit() <= 0) {
            return null;
        }
        return String.valueOf(usd.getLoginLabUnit());
    }

    private UserSessionData getUserSessionData(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        UserSessionData usd = (UserSessionData) request.getSession().getAttribute(IActionConstants.USER_SESSION_DATA);
        if (usd == null) {
            usd = (UserSessionData) request.getAttribute(IActionConstants.USER_SESSION_DATA);
        }
        return usd;
    }

    private boolean sameLabUnit(String left, String right) {
        return left != null && right != null
                && left.trim().toLowerCase(Locale.ROOT).equals(right.trim().toLowerCase(Locale.ROOT));
    }

    private static void put(RbacAction action, String... roleNames) {
        Set<String> names = new HashSet<>();
        for (String roleName : roleNames) {
            if (roleName != null && !roleName.isBlank()) {
                names.add(roleName.trim());
            }
        }
        ACTION_ROLES.put(action, Set.copyOf(names));
    }
}
