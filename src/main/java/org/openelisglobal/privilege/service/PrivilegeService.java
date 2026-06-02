package org.openelisglobal.privilege.service;

import java.util.List;
import java.util.Set;
import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.privilege.valueholder.Privilege;

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
}
