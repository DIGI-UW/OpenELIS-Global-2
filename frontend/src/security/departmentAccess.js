/**
 * Department scope helpers (ownership before RBAC). Mirrors backend
 * {@code DepartmentIsolationService} rules for UI gates.
 */
import { Roles } from "../constants/roles";
import { getEffectiveLabUnitNameForRoleCheck } from "./routeAccess";

/**
 * Global Administrator or AllLabUnits may view all lab-scoped departments and
 * pick department when creating department-owned records. System Admin remains
 * limited to system/admin functions unless also granted AllLabUnits.
 */
export function hasUnrestrictedDepartmentAccess(userSessionDetails) {
  if (!userSessionDetails?.authenticated) {
    return false;
  }
  const global = userSessionDetails.roles || [];
  if (global.includes(Roles.GLOBAL_ADMIN)) {
    return true;
  }
  const allLab = userSessionDetails.userLabRolesMap?.AllLabUnits;
  return Array.isArray(allLab) && allLab.length > 0;
}

/**
 * Restricted users must have an active login lab unit (or a single assignable
 * department) before department-scoped data or mutations are allowed.
 */
export function hasActiveDepartmentScope(userSessionDetails) {
  if (!userSessionDetails?.authenticated) {
    return false;
  }
  if (hasUnrestrictedDepartmentAccess(userSessionDetails)) {
    return true;
  }
  return Boolean(getEffectiveLabUnitNameForRoleCheck(userSessionDetails));
}

/**
 * Department access first, then RBAC role names (lab-unit scoped to active department).
 */
export function canPerformDepartmentScopedAction(
  userSessionDetails,
  allowedRoleNames,
  sessionHasAnyRole,
) {
  if (!hasActiveDepartmentScope(userSessionDetails)) {
    return false;
  }
  if (!allowedRoleNames || allowedRoleNames.length === 0) {
    return true;
  }
  return sessionHasAnyRole(userSessionDetails, allowedRoleNames);
}
