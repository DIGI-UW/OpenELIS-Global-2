/**
 * Centralized role and permission definitions for OpenELIS-Global (AHRI)
 *
 * Role names MUST match system_role.name in the database exactly.
 * Three scopes: Global, Department/Lab Unit, Project.
 *
 * Usage:
 *   import { Roles, Permissions } from "../constants/roles";
 *   <PermissionGate roles={Permissions.ENTER_RESULTS}>
 *     <Button>Save Result</Button>
 *   </PermissionGate>
 */

export const Roles = {
  // ── Global roles (grouping_parent = 'Global Roles') ──────────────────────
  GLOBAL_ADMIN: "Global Administrator",
  ADMINISTRATIVE_STAFF: "Administrative Staff",
  IT_SUPPORT_STAFF: "IT Support Staff",
  EQA_PERSONNEL: "EQA Personnel",
  EXTERNAL_STAKEHOLDERS: "External Stakeholders",
  AUDIT_TRAIL: "Audit Trail", // legacy system role, kept for audit page gate

  // ── Department / Lab Unit roles (grouping_parent = 'Lab Unit Roles') ─────
  SAMPLE_COLLECTORS: "Sample Collectors",
  LABORATORY_TECHNICIANS: "Laboratory Technicians",
  RESEARCHERS: "Researchers",
  LAB_MANAGERS: "Lab Managers",
  BIOMEDICAL_STAFF: "Biomedical Staff",

  // ── Project roles (grouping_parent = 'Project Roles') ────────────────────
  PROJECT_PI: "Principal Investigator",
  PROJECT_COORDINATOR: "Project Coordinators",
  DATA_MANAGER: "Data Managers",

  // ── Notebook / legacy lab roles still active in DB ───────────────────────
  RECEPTION: "Reception",
  RESULTS: "Results",
  VALIDATION: "Validation",
  REPORTS: "Reports",
  NOTEBOOK_ADMIN: "Notebook Administrator",
  NOTEBOOK_ENTRY_CREATOR: "Notebook Entry Creator",
};

export const Permissions = {
  // ── Notebook ──────────────────────────────────────────────────────────────
  CREATE_OR_EDIT_NOTEBOOK: [
    Roles.GLOBAL_ADMIN,
    Roles.NOTEBOOK_ADMIN,
    Roles.LAB_MANAGERS,
  ],

  // @deprecated — use CREATE_OR_EDIT_NOTEBOOK
  CREATE_NOTEBOOK_ENTRY: [
    Roles.GLOBAL_ADMIN,
    Roles.NOTEBOOK_ADMIN,
    Roles.NOTEBOOK_ENTRY_CREATOR,
    Roles.LAB_MANAGERS,
    Roles.LABORATORY_TECHNICIANS,
    Roles.RESULTS,
  ],

  CREATE_OR_EDIT_NOTEBOOK_ENTRY: [
    Roles.GLOBAL_ADMIN,
    Roles.NOTEBOOK_ADMIN,
    Roles.NOTEBOOK_ENTRY_CREATOR,
    Roles.LAB_MANAGERS,
    Roles.LABORATORY_TECHNICIANS,
    Roles.RESULTS,
  ],

  APPROVE_NOTEBOOK_ENTRY: [
    Roles.GLOBAL_ADMIN,
    Roles.NOTEBOOK_ADMIN,
    Roles.LAB_MANAGERS,
  ],

  // ── Samples ───────────────────────────────────────────────────────────────
  REGISTER_SAMPLES: [
    Roles.GLOBAL_ADMIN,
    Roles.RECEPTION,
    Roles.SAMPLE_COLLECTORS,
    Roles.LAB_MANAGERS,
  ],

  UPDATE_SAMPLES: [
    Roles.GLOBAL_ADMIN,
    Roles.RECEPTION,
    Roles.LABORATORY_TECHNICIANS,
    Roles.RESEARCHERS,
    Roles.LAB_MANAGERS,
    Roles.PROJECT_COORDINATOR,
  ],

  PROCESS_SAMPLES: [
    Roles.GLOBAL_ADMIN,
    Roles.LABORATORY_TECHNICIANS,
    Roles.LAB_MANAGERS,
    Roles.PROJECT_COORDINATOR,
  ],

  VIEW_PROCESSING: [
    Roles.GLOBAL_ADMIN,
    Roles.SAMPLE_COLLECTORS,
    Roles.LABORATORY_TECHNICIANS,
    Roles.RESEARCHERS,
    Roles.LAB_MANAGERS,
    Roles.PROJECT_COORDINATOR,
  ],

  // ── Results ───────────────────────────────────────────────────────────────
  ENTER_RESULTS: [
    Roles.GLOBAL_ADMIN,
    Roles.RESULTS,
    Roles.LABORATORY_TECHNICIANS,
    Roles.LAB_MANAGERS,
    Roles.PROJECT_COORDINATOR,
  ],

  VALIDATE_RESULTS: [
    Roles.GLOBAL_ADMIN,
    Roles.VALIDATION,
    Roles.LAB_MANAGERS,
    Roles.PROJECT_COORDINATOR,
  ],

  REVIEW_RESULTS: [Roles.GLOBAL_ADMIN, Roles.RESEARCHERS, Roles.LAB_MANAGERS],

  VIEW_RESULTS: [
    Roles.GLOBAL_ADMIN,
    Roles.RESULTS,
    Roles.VALIDATION,
    Roles.REPORTS,
    Roles.RESEARCHERS,
    Roles.LAB_MANAGERS,
    Roles.DATA_MANAGER,
  ],

  // ── Reports ───────────────────────────────────────────────────────────────
  GENERATE_REPORTS: [
    Roles.GLOBAL_ADMIN,
    Roles.REPORTS,
    Roles.LAB_MANAGERS,
    Roles.ADMINISTRATIVE_STAFF,
  ],

  AGGREGATE_DATA: [
    Roles.GLOBAL_ADMIN,
    Roles.REPORTS,
    Roles.RESEARCHERS,
    Roles.LAB_MANAGERS,
    Roles.DATA_MANAGER,
  ],

  // External stakeholders: approved dashboards/exports only, no raw data
  VIEW_APPROVED_REPORTS: [
    Roles.GLOBAL_ADMIN,
    Roles.REPORTS,
    Roles.LAB_MANAGERS,
    Roles.ADMINISTRATIVE_STAFF,
    Roles.EXTERNAL_STAKEHOLDERS,
    Roles.DATA_MANAGER,
  ],

  // ── Equipment ─────────────────────────────────────────────────────────────
  MANAGE_EQUIPMENT: [
    Roles.GLOBAL_ADMIN,
    Roles.LAB_MANAGERS,
    Roles.BIOMEDICAL_STAFF,
  ],

  VIEW_EQUIPMENT: [
    Roles.GLOBAL_ADMIN,
    Roles.LAB_MANAGERS,
    Roles.BIOMEDICAL_STAFF,
    Roles.LABORATORY_TECHNICIANS,
  ],

  // ── QA ────────────────────────────────────────────────────────────────────
  MANAGE_QA: [Roles.GLOBAL_ADMIN, Roles.LAB_MANAGERS, Roles.EQA_PERSONNEL],

  // ── Admin ─────────────────────────────────────────────────────────────────
  MANAGE_USERS: [Roles.GLOBAL_ADMIN, Roles.ADMINISTRATIVE_STAFF],

  VIEW_AUDIT_TRAIL: [Roles.GLOBAL_ADMIN, Roles.AUDIT_TRAIL],

  SYSTEM_ADMIN: [Roles.GLOBAL_ADMIN],

  // ── Project ───────────────────────────────────────────────────────────────
  VIEW_PROJECT: [
    Roles.GLOBAL_ADMIN,
    Roles.PROJECT_PI,
    Roles.PROJECT_COORDINATOR,
    Roles.DATA_MANAGER,
  ],

  APPROVE_PROJECT: [Roles.GLOBAL_ADMIN, Roles.PROJECT_PI],

  VIEW_PROJECT_WORKPLAN: [
    Roles.GLOBAL_ADMIN,
    Roles.PROJECT_PI,
    Roles.PROJECT_COORDINATOR,
  ],

  PROJECT_REPORTS: [Roles.GLOBAL_ADMIN, Roles.DATA_MANAGER],
};

/**
 * @deprecated Use Permissions instead.
 */
export const RoleGroups = {
  NOTEBOOK_ADMINS: Permissions.CREATE_OR_EDIT_NOTEBOOK,
  ADMIN_ROLES: Permissions.MANAGE_USERS,
  RESULTS_VIEWERS: Permissions.VIEW_RESULTS,
};

export default Roles;
