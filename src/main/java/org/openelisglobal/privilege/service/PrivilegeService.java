package org.openelisglobal.privilege.service;

import java.util.List;
import java.util.Set;
import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.privilege.valueholder.Privilege;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Read-only service for resolving privileges attached to roles and users.
 *
 * <p>
 * This service does NOT extend {@code BaseObjectService} because privilege
 * records are managed through Liquibase seed data and configuration — no
 * runtime CRUD is required.
 */
@CrossDomainService(callers = "CustomUserDetailsService — called by Spring Security during authentication, before any user privilege context exists")
public interface PrivilegeService {

    /**
     * Returns the privileges directly assigned to the given role (no inheritance).
     *
     * @param roleId the string representation of the role numeric PK
     * @return list of active privileges; never null
     */
    List<Privilege> getDirectPrivilegesForRole(String roleId);

    /**
     * Resolves all privilege names for a role, following parent-role inheritance
     * recursively.
     *
     * <p>
     * If the role is {@code Global Administrator} the sentinel value
     * {@code Set.of("*")} is returned, meaning the role has every privilege.
     *
     * @param roleId the string representation of the role numeric PK
     * @return set of privilege name strings; never null
     */
    Set<String> resolveAllPrivilegesForRole(String roleId);

    /**
     * Returns the union of all privileges held by a system user across all of their
     * roles.
     *
     * <p>
     * If the user holds the Global Administrator role, the sentinel value
     * {@code Set.of("*")} is returned immediately.
     *
     * @param systemUserId the system user identifier
     * @return set of privilege name strings; never null
     */
    Set<String> getAllPrivilegesForUser(String systemUserId);

    /**
     * Returns all active privilege records in the system. Used to expand the Global
     * Administrator sentinel into a concrete authority list.
     *
     * @return list of all active privileges; never null
     */
    List<Privilege> getAllPrivileges();

    /**
     * Full effective privilege RECORDS for a role — direct plus inherited, with the
     * Global Administrator sentinel expanded to the whole catalog. Powers the admin
     * UI privilege summary panel (spec 012 T041/T042), so it is gated at the
     * privilege the user-management persona holds.
     *
     * @param roleId the string representation of the role numeric PK
     * @return effective privileges sorted by category then name; never null
     */
    @PreAuthorize("hasAuthority('PRIV_USER_MANAGE')")
    List<Privilege> getEffectivePrivilegesForRole(String roleId);

    /**
     * Replaces a role's DIRECT privilege grants with exactly {@code privilegeIds}.
     * Inherited privileges are not touched: they belong to the parent role.
     *
     * <p>
     * <b>This is a privilege-granting operation.</b> A holder of
     * {@code PRIV_ROLE_MANAGE} can grant any privilege in the catalogue to any
     * role, including a role they themselves hold — so the capability is equivalent
     * to full system access, by design. It is deliberately NOT restricted to
     * privileges the caller already holds. Grant {@code role:manage} accordingly.
     *
     * <p>
     * Global Administrator is not editable here: it holds every privilege through
     * the {@code "*"} sentinel rather than through grant rows, so editing its rows
     * would silently do nothing.
     *
     * @return the privileges the role directly holds afterwards
     */
    @PreAuthorize("hasAuthority('PRIV_ROLE_MANAGE')")
    List<Privilege> replaceDirectPrivilegesForRole(String roleId, java.util.Collection<Integer> privilegeIds);

    /** The full privilege catalogue, for the role editor's checklist. */
    @PreAuthorize("hasAuthority('PRIV_ROLE_VIEW')")
    List<Privilege> getPrivilegeCatalogue();
}
