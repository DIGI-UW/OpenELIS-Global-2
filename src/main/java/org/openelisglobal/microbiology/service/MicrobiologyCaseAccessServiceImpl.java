package org.openelisglobal.microbiology.service;

import java.util.LinkedHashSet;
import java.util.Set;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.systemuser.controller.UnifiedSystemUserController;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.userrole.service.UserRoleService;
import org.openelisglobal.userrole.valueholder.UserLabUnitRoles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MicrobiologyCaseAccessServiceImpl implements MicrobiologyCaseAccessService {
    private final MicroCaseDAO cases;
    private final UserService users;
    private final RoleService roles;
    private final UserRoleService userRoles;

    public MicrobiologyCaseAccessServiceImpl(MicroCaseDAO cases, UserService users, RoleService roles,
            UserRoleService userRoles) {
        this.cases = cases;
        this.users = users;
        this.roles = roles;
        this.userRoles = userRoles;
    }

    @Override
    public Set<String> permittedLabUnitIds(String userId, String roleName) {
        if (userId == null || userId.isBlank())
            return Set.of();
        if (userRoles.userInRole(userId, Constants.ROLE_GLOBAL_ADMIN))
            return null;
        var role = roles.getRoleByName(roleName);
        if (role == null)
            return Set.of();
        UserLabUnitRoles assignments = users.getUserLabUnitRoles(userId);
        Set<String> ids = new LinkedHashSet<>();
        if (assignments != null && assignments.getLabUnitRoleMap() != null) {
            for (var assignment : assignments.getLabUnitRoleMap()) {
                if (assignment.getRoles() != null && assignment.getRoles().contains(role.getId())) {
                    if (UnifiedSystemUserController.ALL_LAB_UNITS.equals(assignment.getLabUnit()))
                        return null;
                    if (assignment.getLabUnit() != null)
                        ids.add(assignment.getLabUnit());
                }
            }
        }
        return ids;
    }

    @Override
    public boolean hasLabUnitRole(String userId, String labUnitId, String roleName) {
        if (labUnitId == null)
            return false;
        Set<String> ids = permittedLabUnitIds(userId, roleName);
        return ids == null || ids.contains(labUnitId);
    }

    @Override
    public boolean canReadLabUnit(String userId, String unitId) {
        return hasLabUnitRole(userId, unitId, Constants.ROLE_RESULTS)
                || hasLabUnitRole(userId, unitId, Constants.ROLE_VALIDATION);
    }

    @Override
    public boolean canAccessCase(String caseId, String userId, boolean administrator) {
        MicroCase c = cases.get(caseId).orElse(null);
        return c != null && canReadLabUnit(userId, c.getLabUnitId());
    }

    @Override
    public boolean canAccessSampleItem(String sampleItemId, String userId, boolean administrator) {
        return cases.getBySampleItem(sampleItemId).stream().anyMatch(c -> canReadLabUnit(userId, c.getLabUnitId()));
    }

}
