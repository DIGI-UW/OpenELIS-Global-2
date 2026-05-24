/**
 * AHRI SRS user-management role allowlists.
 * Lab unit roles: SRS §2.3 user classes and §4 access-control matrix (p. 59–61).
 * Must stay aligned with AHRIRoleCatalog.java and Constants.java.
 */

/** Display order for Lab Unit Roles checkboxes */
export const AHRI_LAB_UNIT_ROLE_NAMES = [
  "Sample Collector",
  "Laboratory Technician",
  "Junior Researcher",
  "Senior Researcher",
  "Lab Manager",
  "Biomedical Staff",
];

/** Display order for Project Roles */
export const AHRI_PROJECT_ROLE_NAMES = [
  "Principal Investigator",
  "Project Coordinator",
  "Data Manager",
];

/** Display order for Global Roles */
export const AHRI_GLOBAL_ROLE_NAMES = [
  "Global Administrator",
  "System Admin",
  "User Account Administrator",
  "Audit Trail",
  "Administrative Staff",
  "IT Support Staff",
  "EQA Personnel",
  "External Stakeholders",
];

const labUnitRoleSet = new Set(
  AHRI_LAB_UNIT_ROLE_NAMES.map((name) => normalizeRoleName(name)),
);
const projectRoleSet = new Set(
  AHRI_PROJECT_ROLE_NAMES.map((name) => normalizeRoleName(name)),
);
const globalRoleSet = new Set(
  AHRI_GLOBAL_ROLE_NAMES.map((name) => normalizeRoleName(name)),
);

function normalizeRoleName(name) {
  if (!name) {
    return "";
  }
  return String(name).trim().toLowerCase().replace(/\s+/g, " ");
}

function sortByAllowlist(roles, allowlist) {
  const order = new Map(
    allowlist.map((name, index) => [normalizeRoleName(name), index]),
  );
  return [...roles].sort((a, b) => {
    const ai = order.get(normalizeRoleName(a.roleName)) ?? Number.MAX_SAFE_INTEGER;
    const bi = order.get(normalizeRoleName(b.roleName)) ?? Number.MAX_SAFE_INTEGER;
    if (ai !== bi) {
      return ai - bi;
    }
    return String(a.roleName).localeCompare(String(b.roleName));
  });
}

/**
 * @param {{ roleId: string, roleName: string }[]} roles
 */
export function filterAhriLabUnitRoles(roles) {
  if (!roles || !Array.isArray(roles)) {
    return [];
  }
  const filtered = roles.filter((role) =>
    labUnitRoleSet.has(normalizeRoleName(role?.roleName)),
  );
  return sortByAllowlist(filtered, AHRI_LAB_UNIT_ROLE_NAMES);
}

/**
 * @param {{ roleId: string, roleName: string }[]} roles
 */
export function filterAhriProjectRoles(roles) {
  if (!roles || !Array.isArray(roles)) {
    return [];
  }
  const filtered = roles.filter((role) =>
    projectRoleSet.has(normalizeRoleName(role?.roleName)),
  );
  return sortByAllowlist(filtered, AHRI_PROJECT_ROLE_NAMES);
}

/**
 * @param {{ roleId: string, roleName: string }[]} roles
 */
export function filterAhriGlobalRoles(roles) {
  if (!roles || !Array.isArray(roles)) {
    return [];
  }
  const filtered = roles.filter((role) =>
    globalRoleSet.has(normalizeRoleName(role?.roleName)),
  );
  return sortByAllowlist(filtered, AHRI_GLOBAL_ROLE_NAMES);
}
