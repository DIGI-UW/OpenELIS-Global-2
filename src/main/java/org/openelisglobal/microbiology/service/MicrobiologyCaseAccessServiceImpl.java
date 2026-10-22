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
    private final org.openelisglobal.systemuser.service.SystemUserService systemUsers;
    private final org.openelisglobal.login.service.LoginUserService logins;

    public MicrobiologyCaseAccessServiceImpl(MicroCaseDAO cases, UserService users, RoleService roles,
            UserRoleService userRoles, org.openelisglobal.systemuser.service.SystemUserService systemUsers,
            org.openelisglobal.login.service.LoginUserService logins) {
        this.cases = cases;
        this.users = users;
        this.roles = roles;
        this.userRoles = userRoles;
        this.systemUsers = systemUsers;
        this.logins = logins;
    }

    @Override
    public Set<String> permittedLabUnitIds(String userId, String roleName) {
        if (userId == null || userId.isBlank())
            return Set.of();
        if (isAdministrator(userId))
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
        if (isCurrentSingleSignOnUser(userId)) {
            users.getUserTestSections(userId, role.getId()).forEach(unit -> ids.add(unit.getId()));
        }
        return ids;
    }

    private boolean isCurrentSingleSignOnUser(String userId) {
        var attributes = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof org.springframework.web.context.request.ServletRequestAttributes servlet))
            return false;
        var session = servlet.getRequest().getSession(false);
        if (session == null || !(session.getAttribute("userSessionData")
                instanceof org.openelisglobal.login.valueholder.UserSessionData user)
                || !userId.equals(String.valueOf(user.getSystemUserId())))
            return false;
        var context = session.getAttribute(
                org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        if (!(context instanceof org.springframework.security.core.context.SecurityContext security)
                || security.getAuthentication() == null || !security.getAuthentication().isAuthenticated())
            return false;
        var principal = security.getAuthentication().getPrincipal();
        return principal instanceof org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal
                || principal instanceof org.springframework.security.oauth2.core.user.DefaultOAuth2User;
    }

    private boolean isAdministrator(String userId) {
        if (userRoles.userInRole(userId, Constants.ROLE_GLOBAL_ADMIN) || userRoles.userInRole(userId, "Admin")) {
            return true;
        }
        var user = systemUsers.get(userId);
        if (user == null || user.getLoginName() == null) {
            return false;
        }
        var login = logins.getUserProfile(user.getLoginName());
        return login != null
                && org.openelisglobal.common.action.IActionConstants.YES.equalsIgnoreCase(login.getIsAdmin());
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
