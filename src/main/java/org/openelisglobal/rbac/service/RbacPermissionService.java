package org.openelisglobal.rbac.service;

import java.util.List;
import java.util.Set;

/**
 * TR-03/TR-04/TR-05: Unified permission engine applied to all modules.
 * Evaluates: module + action + department + project + role.
 * Enforces department filtering on every query.
 */
public interface RbacPermissionService {

    /**
     * Check if user has permission for a module action.
     * Evaluates global roles first, then department roles, then project roles.
     */
    boolean hasPermission(String systemUserId, String module, String action);

    /**
     * Check if user has permission scoped to a specific department.
     * Returns true if user has global role OR department role for that department.
     */
    boolean hasPermission(String systemUserId, String module, String action, String departmentId);

    /**
     * Check if user has permission scoped to a specific project.
     */
    boolean hasPermissionForProject(String systemUserId, String module, String action, String projectId);

    /**
     * TR-04: Get the list of department IDs the user is allowed to access.
     * Returns null if user has global unrestricted access (System Admin).
     * Returns empty list if user has no department access.
     */
    List<String> getAllowedDepartments(String systemUserId);

    /**
     * TR-04: Get the list of project IDs the user is allowed to access.
     */
    List<String> getAllowedProjects(String systemUserId);

    /**
     * Get all roles (global + department + project) for a user.
     */
    Set<String> getAllRolesForUser(String systemUserId);

    /**
     * Check if user has a specific global role.
     */
    boolean hasGlobalRole(String systemUserId, String roleName);

    /**
     * Check if user has a specific role in a department.
     */
    boolean hasDepartmentRole(String systemUserId, String departmentId, String roleName);

    /**
     * Check if user has a specific role in a project.
     */
    boolean hasProjectRole(String systemUserId, String projectId, String roleName);
}
