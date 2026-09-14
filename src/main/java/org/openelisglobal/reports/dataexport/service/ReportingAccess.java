package org.openelisglobal.reports.dataexport.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hibernate.ObjectNotFoundException;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.systemuser.controller.UnifiedSystemUserController;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportingAccess {
    private final UserRoleService roles;
    private final SystemUserService users;
    private final UserService userService;

    @Autowired
    public ReportingAccess(UserRoleService roles, SystemUserService users, UserService userService) {
        this.roles = roles;
        this.users = users;
        this.userService = userService;
    }

    public void requireReports(String owner) {
        SystemUser user;
        try {
            user = users.get(owner);
        } catch (ObjectNotFoundException missingUser) {
            user = null;
        }
        if (user == null || !"Y".equals(user.getIsActive())
                || !roles.userInRole(owner, List.of(Constants.ROLE_GLOBAL_ADMIN, Constants.ROLE_REPORTS))) {
            ReportingAudit.denied(owner, null);
            throw new ReportingException(403, "reporting.access.denied");
        }
    }

    public List<IdValuePair> requestSections(String owner) {
        requireReports(owner);
        return userService.getUserTestSections(owner, null);
    }

    public void requireScope(String owner, List<String> requested) {
        requireReports(owner);
        if (requested.isEmpty()) {
            ReportingAudit.denied(owner, null);
            throw new ReportingException(403, "reporting.access.noSections");
        }
        if (roles.userInRole(owner, Constants.ROLE_GLOBAL_ADMIN))
            return;
        var mapping = roles.getUserLabUnitRoles(owner);
        Set<String> allowed = mapping == null || mapping.getLabUnitRoleMap() == null ? Set.of()
                : mapping.getLabUnitRoleMap().stream().filter(
                        m -> m != null && m.getLabUnit() != null && m.getRoles() != null && !m.getRoles().isEmpty())
                        .map(m -> m.getLabUnit()).collect(Collectors.toSet());
        if (!allowed.contains(UnifiedSystemUserController.ALL_LAB_UNITS) && !allowed.containsAll(requested)) {
            ReportingAudit.denied(owner, null);
            throw new ReportingException(403, "reporting.access.scopeChanged");
        }
    }
}
