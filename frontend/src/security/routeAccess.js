/**
 * Pure helpers for route and menu access: global roles (Spring) plus lab-unit roles
 * (userLabRolesMap) so department-only users are not blocked at the UI.
 */
import { Roles as CoreRoles } from "../components/utils/Utils";
import { Roles as ExtRoles, Permissions } from "../constants/roles";

const uniq = (arr) => [...new Set((arr || []).filter(Boolean))];

/**
 * @param {object} userSessionDetails session JSON from {@code /session}
 * @returns {string[]} non-AllLabUnits keys that have at least one role
 */
export function getDepartmentLabUnitKeys(userSessionDetails) {
  if (!userSessionDetails?.userLabRolesMap) {
    return [];
  }
  return Object.keys(userSessionDetails.userLabRolesMap).filter(
    (k) =>
      k &&
      k !== "AllLabUnits" &&
      Array.isArray(userSessionDetails.userLabRolesMap[k]) &&
      userSessionDetails.userLabRolesMap[k].length > 0,
  );
}

/**
 * When only one department is assigned, use it for role checks (mirrors server auto-select).
 */
export function getEffectiveLabUnitNameForRoleCheck(userSessionDetails) {
  const login = userSessionDetails?.loginLabUnit;
  if (login) {
    return login;
  }
  const departmentKeys = getDepartmentLabUnitKeys(userSessionDetails);
  return departmentKeys.length === 1 ? departmentKeys[0] : null;
}

/**
 * True if the user has any of the role names via global roles, AllLabUnits, or any department row.
 * Global Administrator always allowed.
 */
export function sessionHasAnyRole(userSessionDetails, allowedRoleNames) {
  if (!allowedRoleNames || allowedRoleNames.length === 0) {
    return true;
  }
  if (!userSessionDetails?.authenticated) {
    return false;
  }
  const global = userSessionDetails.roles || [];
  if (global.includes(CoreRoles.GLOBAL_ADMIN)) {
    return true;
  }
  for (const name of allowedRoleNames) {
    if (global.includes(name)) {
      return true;
    }
  }
  const map = userSessionDetails.userLabRolesMap;
  if (!map) {
    return false;
  }
  const allLab = map["AllLabUnits"] || [];
  for (const name of allowedRoleNames) {
    if (allLab.includes(name)) {
      return true;
    }
  }
  const activeLabUnit = getEffectiveLabUnitNameForRoleCheck(userSessionDetails);
  if (activeLabUnit) {
    const labRoles = map[activeLabUnit] || [];
    for (const name of allowedRoleNames) {
      if (labRoles.includes(name)) {
        return true;
      }
    }
  }

  for (const departmentKey of getDepartmentLabUnitKeys(userSessionDetails)) {
    const labRoles = map[departmentKey] || [];
    for (const name of allowedRoleNames) {
      if (labRoles.includes(name)) {
        return true;
      }
    }
  }
  return false;
}

// --- Presets: global roles from Utils + lab-unit role display names (must match DB) ---

export const ROUTE_ROLES_STORAGE = uniq([
  CoreRoles.RECEPTION,
  CoreRoles.RESULTS,
  CoreRoles.GLOBAL_ADMIN,
  ExtRoles.STORAGE_MANAGER,
  ExtRoles.SAMPLE_COLLECTOR,
  ExtRoles.LABORATORY_TECHNICIAN,
  ExtRoles.LABORATORY_TECHNICIANS,
  ExtRoles.SUPERVISOR,
  ExtRoles.TECHNICIAN,
  ExtRoles.LAB_MANAGER_SUPERVISOR,
  ExtRoles.REGISTER_SAMPLES,
  ExtRoles.UPDATE_SAMPLES,
  "Sample Receiver",
  "Disposal Officer",
  "Archivist",
]);

export const ROUTE_ROLES_INVENTORY = uniq([
  CoreRoles.RESULTS,
  CoreRoles.GLOBAL_ADMIN,
  ExtRoles.SUPERVISOR,
  ExtRoles.TECHNICIAN,
  ExtRoles.LABORATORY_TECHNICIAN,
  ExtRoles.LABORATORY_TECHNICIANS,
  ExtRoles.LAB_MANAGER_SUPERVISOR,
  ExtRoles.REGISTER_SAMPLES,
  ...Permissions.MANAGE_EQUIPMENT,
]);

export const ROUTE_ROLES_EQUIPMENT_USAGE = ROUTE_ROLES_INVENTORY;

export const ROUTE_ROLES_SAMPLE_MANAGEMENT = uniq([
  CoreRoles.RECEPTION,
  CoreRoles.RESULTS,
  CoreRoles.GLOBAL_ADMIN,
  ...Permissions.UPDATE_SAMPLES,
  ...Permissions.REGISTER_SAMPLES,
]);

export const ROUTE_ROLES_WORKPLAN = uniq([
  CoreRoles.RESULTS,
  CoreRoles.GLOBAL_ADMIN,
  ExtRoles.TECHNICIAN,
  ExtRoles.SUPERVISOR,
  ExtRoles.LABORATORY_TECHNICIAN,
  ExtRoles.LABORATORY_TECHNICIANS,
  ExtRoles.LAB_MANAGER_SUPERVISOR,
  ...Permissions.ENTER_RESULTS,
]);

export const ROUTE_ROLES_RESULT_ENTRY = uniq([
  CoreRoles.RESULTS,
  CoreRoles.GLOBAL_ADMIN,
  ...Permissions.ENTER_RESULTS,
]);

export const ROUTE_ROLES_RECEPTION = uniq([
  CoreRoles.RECEPTION,
  CoreRoles.GLOBAL_ADMIN,
  ...Permissions.REGISTER_SAMPLES,
  ExtRoles.SAMPLE_COLLECTOR,
]);

/** Menu rules: exact path or prefix (prefix must not match exact-only routes first) */
const PATH_RULES = [
  { exact: "/PatientResults", roles: ROUTE_ROLES_RESULT_ENTRY },
  /** Sub-paths only; exact `/PatientResults` handled above */
  { prefix: "/PatientResults", roles: ROUTE_ROLES_RECEPTION },
  { prefix: "/Storage", roles: ROUTE_ROLES_STORAGE },
  { prefix: "/inventory", roles: ROUTE_ROLES_INVENTORY },
  { prefix: "/equipment-usage", roles: ROUTE_ROLES_EQUIPMENT_USAGE },
  { prefix: "/SampleManagement", roles: ROUTE_ROLES_SAMPLE_MANAGEMENT },
  { prefix: "/WorkPlan", roles: ROUTE_ROLES_WORKPLAN },
  { prefix: "/Workplan", roles: ROUTE_ROLES_WORKPLAN },
  { prefix: "/result", roles: ROUTE_ROLES_RESULT_ENTRY },
  { prefix: "/LogbookResults", roles: ROUTE_ROLES_RESULT_ENTRY },
  { prefix: "/AccessionResults", roles: ROUTE_ROLES_RESULT_ENTRY },
  { prefix: "/StatusResults", roles: ROUTE_ROLES_RESULT_ENTRY },
  { prefix: "/RangeResults", roles: ROUTE_ROLES_RESULT_ENTRY },
  { prefix: "/ReferredOutTests", roles: ROUTE_ROLES_RESULT_ENTRY },
  { prefix: "/SamplePatientEntry", roles: ROUTE_ROLES_RECEPTION },
  { prefix: "/SampleEdit", roles: ROUTE_ROLES_RECEPTION },
  { prefix: "/ModifyOrder", roles: ROUTE_ROLES_RECEPTION },
  { prefix: "/SampleBatchEntrySetup", roles: ROUTE_ROLES_RECEPTION },
  { prefix: "/ElectronicOrders", roles: ROUTE_ROLES_RECEPTION },
  { prefix: "/PrintBarcode", roles: ROUTE_ROLES_RECEPTION },
  { prefix: "/PatientManagement", roles: ROUTE_ROLES_RECEPTION },
  { prefix: "/PatientHistory", roles: ROUTE_ROLES_RECEPTION },
  { prefix: "/Aliquot", roles: ROUTE_ROLES_RECEPTION },
  { prefix: "/genericProgram", roles: ROUTE_ROLES_RECEPTION },
  { prefix: "/programView", roles: ROUTE_ROLES_RECEPTION },
  { prefix: "/FreezerMonitoring", roles: ROUTE_ROLES_STORAGE },
];

/**
 * @param {string} pathname app path e.g. /Storage/rooms
 * @param {object} userSessionDetails
 * @returns {boolean|null} true/false if a rule exists, null if no client rule (show link)
 */
export function canAccessPath(pathname, userSessionDetails) {
  if (!pathname) {
    return null;
  }
  const exactRule = PATH_RULES.find((r) => r.exact === pathname);
  if (exactRule) {
    return sessionHasAnyRole(userSessionDetails, exactRule.roles);
  }
  const prefixRule = PATH_RULES.find(
    (r) =>
      r.prefix &&
      (pathname === r.prefix || pathname.startsWith(r.prefix + "/")),
  );
  if (!prefixRule) {
    return null;
  }
  return sessionHasAnyRole(userSessionDetails, prefixRule.roles);
}

/**
 * @param {string} actionURL full or relative URL from menu
 */
export function canAccessMenuUrl(actionURL, userSessionDetails) {
  if (!actionURL || !userSessionDetails?.authenticated) {
    return true;
  }
  try {
    const u = new URL(actionURL, window.location.origin);
    const decision = canAccessPath(u.pathname, userSessionDetails);
    if (decision === null) {
      return true;
    }
    return decision;
  } catch {
    return true;
  }
}
