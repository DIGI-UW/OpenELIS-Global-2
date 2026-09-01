package org.openelisglobal.userrole.service;

import java.util.Collection;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.userrole.valueholder.LabUnitRoleMap;
import org.openelisglobal.userrole.valueholder.UserLabUnitRoles;
import org.openelisglobal.userrole.valueholder.UserRole;
import org.openelisglobal.userrole.valueholder.UserRolePK;
import org.springframework.security.access.prepost.PreAuthorize;

@CrossDomainService(callers = "Authentication and session introspection resolve a user's roles before/while"
        + " the privilege context is established (getRoleIdsForUser is ungated); administrative user-role"
        + " management methods remain individually gated with PRIV_USER_ROLE_*")
public interface UserRoleService extends BaseObjectService<UserRole, UserRolePK> {

    /**
     * Ungated identity read: called while BUILDING the caller's authorities —
     * during authentication (CustomUserDetailsService) and session introspection
     * (/session) — so no privilege can be required to discover one's own roles.
     */
    List<Integer> getRoleIdsForUser(String userId);

    /**
     * Ungated identity read, for the same reason as {@link #getRoleIdsForUser}: in
     * every production caller but one the {@code userId} is the CALLER'S OWN id,
     * and asking "am I a global admin / may I cancel a sample" is self-identity,
     * not an administrative read.
     *
     * <p>
     * Gating this on {@code PRIV_USER_ROLE_VIEW} broke every non-admin: no base
     * role is granted {@code user_role:view} in 012-004, and
     * {@code MenuController.isGlobalScopeUser} calls this as its first statement on
     * {@code GET /rest/menu} — hit on every page load, with no try/catch — so the
     * navigation tree 403'd for Reception/Results/Validation/Reports users. Same
     * path in OrderSearchRestController, PatientDashBoardProvider,
     * StorageLocationRestController and SampleEdit{,Rest}Controller.
     *
     * <p>
     * The one caller that asks about OTHER users is
     * {@code UnifiedSystemUserRestController#getUsersWithRole} ({@code GET
     * /rest/users/{roleName}}), which is intentionally left open: it returns only
     * id+display name and populates the pathologist/technician dropdowns on the
     * Pathology, Cytology and Immunohistochemistry case views, which non-admin
     * clinical staff use. It leaks no role membership beyond the role already named
     * in the URL. The administrative surface that exposes a user's full assignment
     * set stays gated at {@code PRIV_USER_MANAGE} on
     * {@code /rest/UnifiedSystemUser}.
     */
    boolean userInRole(String userId, String roleName);

    /** @see #userInRole(String, String) — same ungated self-identity rationale. */
    boolean userInRole(String userId, Collection<String> roleNames);

    @PreAuthorize("hasAuthority('PRIV_USER_ROLE_MANAGE')")
    void saveOrUpdateUserLabUnitRoles(UserLabUnitRoles labRoles);

    @PreAuthorize("hasAuthority('PRIV_USER_ROLE_VIEW')")
    UserLabUnitRoles getUserLabUnitRoles(String userId);

    @PreAuthorize("hasAuthority('PRIV_USER_ROLE_MANAGE')")
    void deleteLabUnitRoleMap(LabUnitRoleMap roleMap);

    @PreAuthorize("hasAuthority('PRIV_USER_ROLE_MANAGE')")
    List<UserLabUnitRoles> getAllUserLabUnitRoles();

    @PreAuthorize("hasAuthority('PRIV_USER_ROLE_VIEW')")
    List<String> getUserIdsForRole(String roleName);
}
