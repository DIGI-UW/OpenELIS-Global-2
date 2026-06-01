import { useContext, useCallback, useMemo } from "react";
import UserSessionDetailsContext from "../UserSessionDetailsContext";
import { Roles, RoleGroups } from "../constants/roles";

/**
 * Custom hook for permission checking
 *
 * Provides utilities to check user roles (both global and lab-unit specific)
 * and determine access to various features.
 *
 * @example
 * const { hasRole, hasAnyRole, hasLabUnitRole, canManageNotebookTemplates } = usePermissions();
 *
 * // Check single global role
 * if (hasRole(Roles.GLOBAL_ADMIN)) { ... }
 *
 * // Check if user has any of multiple roles
 * if (hasAnyRole([Roles.TECHNICIAN, Roles.SUPERVISOR])) { ... }
 *
 * // Check lab-unit specific role
 * if (hasLabUnitRole("Cytology", Roles.TECHNICIAN)) { ... }
 *
 * // Check notebook template management permission
 * if (canManageNotebookTemplates()) { ... }
 */
export const usePermissions = () => {
  const { userSessionDetails } = useContext(UserSessionDetailsContext);

  /**
   * Check if user has a specific global role
   * @param {string} role - Role name to check
   * @returns {boolean}
   */
  const hasRole = useCallback(
    (role) => {
      if (
        !userSessionDetails?.roles ||
        !Array.isArray(userSessionDetails.roles)
      ) {
        return false;
      }
      return userSessionDetails.roles.includes(role);
    },
    [userSessionDetails?.roles],
  );

  /**
   * Check if user has any of the specified global roles
   * @param {string[]} roles - Array of role names to check
   * @returns {boolean}
   */
  const hasAnyRole = useCallback(
    (roles) => {
      if (!roles || !Array.isArray(roles)) {
        return false;
      }
      return roles.some((role) => hasRole(role));
    },
    [hasRole],
  );

  /**
   * Check if user has all of the specified global roles
   * @param {string[]} roles - Array of role names to check
   * @returns {boolean}
   */
  const hasAllRoles = useCallback(
    (roles) => {
      if (!roles || !Array.isArray(roles)) {
        return false;
      }
      return roles.every((role) => hasRole(role));
    },
    [hasRole],
  );

  /**
   * Check if user is a global administrator (super user)
   * Global Admins bypass all location/lab unit restrictions
   * @returns {boolean}
   */
  const isGlobalAdminUser = useCallback(() => {
    if (userSessionDetails?.loginName === "admin") {
      return true;
    }
    if (!userSessionDetails?.roles || !Array.isArray(userSessionDetails.roles)) {
      return false;
    }
    return userSessionDetails.roles.includes(Roles.GLOBAL_ADMIN);
  }, [userSessionDetails?.loginName, userSessionDetails?.roles]);

  /**
   * Check if user has a specific role for a given lab unit
   * Also checks "AllLabUnits" for users with global lab access
   * Global Admins bypass all lab unit restrictions (super users)
   * @param {string} labUnit - Lab unit name or ID
   * @param {string} role - Role name to check
   * @returns {boolean}
   */
  const hasLabUnitRole = useCallback(
    (labUnit, role) => {
      // Global Admins are super users - bypass all location restrictions
      if (isGlobalAdminUser()) {
        return true;
      }

      if (!userSessionDetails?.userLabRolesMap) {
        return false;
      }

      // First check "AllLabUnits" for global lab access
      const allLabUnitsRoles =
        userSessionDetails.userLabRolesMap["AllLabUnits"] || [];
      if (allLabUnitsRoles.includes(role)) {
        return true;
      }

      // Then check specific lab unit
      const labUnitRoles = userSessionDetails.userLabRolesMap[labUnit] || [];
      return labUnitRoles.includes(role);
    },
    [userSessionDetails?.userLabRolesMap, isGlobalAdminUser],
  );

  /**
   * Check if user has any of the specified roles for a given lab unit
   * @param {string} labUnit - Lab unit name or ID
   * @param {string[]} roles - Array of role names to check
   * @returns {boolean}
   */
  const hasAnyLabUnitRole = useCallback(
    (labUnit, roles) => {
      if (!roles || !Array.isArray(roles)) {
        return false;
      }
      return roles.some((role) => hasLabUnitRole(labUnit, role));
    },
    [hasLabUnitRole],
  );

  /**
   * Check if user is a global administrator (super user)
   * @returns {boolean}
   */
  const isGlobalAdmin = useMemo(() => isGlobalAdminUser(), [isGlobalAdminUser]);

  /**
   * Check if user can manage notebook templates (create/edit/delete)
   * Requires Global Admin or Notebook Administrator role
   * @returns {boolean}
   */
  const canManageNotebookTemplates = useMemo(
    () => hasAnyRole(RoleGroups.NOTEBOOK_ADMINS),
    [hasAnyRole],
  );

  /**
   * Get the user's current login lab unit
   * @returns {string|null}
   */
  const loginLabUnit = useMemo(
    () => userSessionDetails?.loginLabUnit || null,
    [userSessionDetails?.loginLabUnit],
  );

  /**
   * Check if user has any role for their current login lab unit
   * Global Admins bypass all lab unit restrictions (super users)
   * Also checks global roles and "AllLabUnits" for users with global lab access
   * @param {string[]} roles - Array of role names to check
   * @returns {boolean}
   */
  const hasRoleForCurrentLabUnit = useCallback(
    (roleList) => {
      if (!roleList || !Array.isArray(roleList)) {
        return false;
      }
      if (!userSessionDetails?.authenticated) {
        return false;
      }
      if (isGlobalAdminUser()) {
        return true;
      }
      const globalRoles = userSessionDetails.roles || [];
      if (roleList.some((r) => globalRoles.includes(r))) {
        return true;
      }
      const map = userSessionDetails.userLabRolesMap || {};
      const allLabRoles = map["AllLabUnits"] || [];
      if (roleList.some((r) => allLabRoles.includes(r))) {
        return true;
      }
      const activeLabUnit = userSessionDetails.loginLabUnit;
      if (!activeLabUnit) {
        return false;
      }
      const activeRoles = map[activeLabUnit] || [];
      return roleList.some((r) => activeRoles.includes(r));
    },
    [userSessionDetails, isGlobalAdminUser],
  );

  /**
   * SRS workflow stage personas for the active department only.
   * Global admins bypass. Users with AllLabUnits keep cross-lab persona access
   * for notebook stage UX, matching the broader permission model used elsewhere.
   */
  const hasPersonaForActiveDepartment = useCallback(
    (roleList) => {
      if (!roleList || !Array.isArray(roleList)) {
        return false;
      }
      if (!userSessionDetails?.authenticated) {
        return false;
      }
      if (isGlobalAdminUser()) {
        return true;
      }
      const map = userSessionDetails.userLabRolesMap || {};
      const allLabRoles = map["AllLabUnits"] || [];
      if (roleList.some((r) => allLabRoles.includes(r))) {
        return true;
      }
      const activeLabUnit = userSessionDetails.loginLabUnit;
      if (!activeLabUnit) {
        return false;
      }
      const activeRoles = map[activeLabUnit] || [];
      return roleList.some((r) => activeRoles.includes(r));
    },
    [userSessionDetails, isGlobalAdminUser],
  );

  /**
   * Check if user is authenticated
   * @returns {boolean}
   */
  const isAuthenticated = useMemo(
    () => userSessionDetails?.authenticated === true,
    [userSessionDetails?.authenticated],
  );

  return {
    // Core utilities
    hasRole,
    hasAnyRole,
    hasAllRoles,
    hasLabUnitRole,
    hasAnyLabUnitRole,
    hasRoleForCurrentLabUnit,
    hasPersonaForActiveDepartment,

    // Convenience flags
    isGlobalAdmin,
    isAuthenticated,
    loginLabUnit,

    // Feature-specific permissions
    canManageNotebookTemplates,

    // Raw session data (for advanced use cases)
    userSessionDetails,
  };
};

export default usePermissions;
