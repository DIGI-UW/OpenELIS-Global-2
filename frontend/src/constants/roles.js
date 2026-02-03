/**
 * Centralized role and permission definitions for OpenELIS-Global
 *
 * This file defines:
 * 1. Roles - Base system roles (must match backend role names)
 * 2. Permissions - Permission-based groups for feature access control
 *
 * Usage:
 *   import { Roles, Permissions } from "../constants/roles";
 *   <PermissionGate roles={Permissions.CREATE_OR_EDIT_NOTEBOOK}>
 *     <AddButton />
 *   </PermissionGate>
 */

/**
 * Base system roles - must match backend role names exactly
 * These are the actual role names stored in the database
 */
export const Roles = {
  // System/Global Roles
  GLOBAL_ADMIN: "Global Administrator",
  USER_ACCOUNT_ADMIN: "User Account Administrator",
  AUDIT_TRAIL: "Audit Trail",

  // Core Lab Roles
  TECHNICIAN: "Technician",
  SUPERVISOR: "Supervisor",
  RESULTS: "Results",
  VALIDATION: "Validation",
  RECEPTION: "Reception",
  REPORTS: "Reports",
  PATHOLOGIST: "Pathologist",
  CYTOPATHOLOGIST: "Cytopathologist",

  // Notebook-specific role
  NOTEBOOK_ADMIN: "Notebook Administrator",

  // Bioanalytical Laboratory Roles
  BIOANALYTICAL_ANALYST: "Bioanalytical Analyst",
  BIOANALYTICAL_SUPERVISOR: "Bioanalytical Supervisor",
  SAMPLE_RECEIVER: "Sample Receiver",
  CHEMICAL_ANALYST: "Chemical Analyst",
  PHARMACIST: "Pharmacist",
  STUDY_DIRECTOR: "Study Director",
  QA_AUDITOR: "QA Auditor",
};

/**
 * Permission-based role groups for feature access control
 *
 * Define permissions by what action they allow, not by organization.
 * This keeps the code generic and reusable across deployments.
 *
 * Usage:
 *   <PermissionGate roles={Permissions.CREATE_OR_EDIT_NOTEBOOK}>
 *     <CreateButton />
 *   </PermissionGate>
 */
export const Permissions = {
  // ========== Notebook Permissions ==========

  // Can create or edit notebook templates
  CREATE_OR_EDIT_NOTEBOOK: [Roles.GLOBAL_ADMIN, Roles.NOTEBOOK_ADMIN],

  // Can view notebook templates
  VIEW_NOTEBOOK: [
    Roles.GLOBAL_ADMIN,
    Roles.NOTEBOOK_ADMIN,
    Roles.SUPERVISOR,
    Roles.TECHNICIAN,
  ],

  // Can create or edit notebook entries (instances)
  CREATE_OR_EDIT_NOTEBOOK_ENTRY: [
    Roles.GLOBAL_ADMIN,
    Roles.NOTEBOOK_ADMIN,
    Roles.SUPERVISOR,
    Roles.TECHNICIAN,
    Roles.RESULTS,
  ],

  // @deprecated Use CREATE_OR_EDIT_NOTEBOOK_ENTRY instead
  CREATE_NOTEBOOK_ENTRY: [
    Roles.GLOBAL_ADMIN,
    Roles.NOTEBOOK_ADMIN,
    Roles.SUPERVISOR,
    Roles.TECHNICIAN,
    Roles.RESULTS,
  ],

  // Can approve/lock/finalize notebook entries
  APPROVE_NOTEBOOK_ENTRY: [
    Roles.GLOBAL_ADMIN,
    Roles.NOTEBOOK_ADMIN,
    Roles.SUPERVISOR,
  ],

  // ========== Results Permissions ==========

  // Can enter results
  ENTER_RESULTS: [Roles.RESULTS, Roles.TECHNICIAN, Roles.SUPERVISOR],

  // Can validate results
  VALIDATE_RESULTS: [Roles.VALIDATION, Roles.SUPERVISOR, Roles.PATHOLOGIST],

  // Can view results
  VIEW_RESULTS: [
    Roles.RESULTS,
    Roles.VALIDATION,
    Roles.PATHOLOGIST,
    Roles.CYTOPATHOLOGIST,
    Roles.SUPERVISOR,
    Roles.REPORTS,
  ],

  // ========== Sample Permissions ==========

  // Can receive/register samples
  RECEIVE_SAMPLES: [Roles.RECEPTION, Roles.TECHNICIAN, Roles.SUPERVISOR],

  // Can process samples
  PROCESS_SAMPLES: [Roles.TECHNICIAN, Roles.SUPERVISOR],

  // ========== Report Permissions ==========

  // Can generate reports
  GENERATE_REPORTS: [Roles.REPORTS, Roles.SUPERVISOR, Roles.GLOBAL_ADMIN],

  // ========== Admin Permissions ==========

  // Can manage users
  MANAGE_USERS: [Roles.GLOBAL_ADMIN, Roles.USER_ACCOUNT_ADMIN],

  // Can view audit trail
  VIEW_AUDIT_TRAIL: [Roles.GLOBAL_ADMIN, Roles.AUDIT_TRAIL],

  // Full system administration
  SYSTEM_ADMIN: [Roles.GLOBAL_ADMIN],

  // ========== Bioanalytical Permissions ==========

  // Can receive and log bioanalytical samples
  BIOANALYTICAL_SAMPLE_RECEPTION: [
    Roles.GLOBAL_ADMIN,
    Roles.SAMPLE_RECEIVER,
    Roles.BIOANALYTICAL_SUPERVISOR,
    Roles.RECEPTION,
  ],

  // Can perform bioanalytical analysis and instrument operations
  BIOANALYTICAL_ANALYSIS: [
    Roles.GLOBAL_ADMIN,
    Roles.BIOANALYTICAL_ANALYST,
    Roles.CHEMICAL_ANALYST,
    Roles.BIOANALYTICAL_SUPERVISOR,
    Roles.TECHNICIAN,
  ],

  // Can review and approve bioanalytical results
  BIOANALYTICAL_REVIEW: [
    Roles.GLOBAL_ADMIN,
    Roles.BIOANALYTICAL_SUPERVISOR,
    Roles.PHARMACIST,
    Roles.STUDY_DIRECTOR,
    Roles.SUPERVISOR,
  ],

  // Can perform QC activities and validation
  BIOANALYTICAL_QC: [
    Roles.GLOBAL_ADMIN,
    Roles.BIOANALYTICAL_ANALYST,
    Roles.CHEMICAL_ANALYST,
    Roles.BIOANALYTICAL_SUPERVISOR,
    Roles.QA_AUDITOR,
  ],

  // Can access study management and reporting features
  BIOANALYTICAL_STUDY_MANAGEMENT: [
    Roles.GLOBAL_ADMIN,
    Roles.STUDY_DIRECTOR,
    Roles.BIOANALYTICAL_SUPERVISOR,
    Roles.PHARMACIST,
  ],

  // Can perform final report generation and approval
  BIOANALYTICAL_REPORTING: [
    Roles.GLOBAL_ADMIN,
    Roles.STUDY_DIRECTOR,
    Roles.BIOANALYTICAL_SUPERVISOR,
    Roles.PHARMACIST,
    Roles.REPORTS,
  ],

  // Can audit bioanalytical processes
  BIOANALYTICAL_AUDIT: [
    Roles.GLOBAL_ADMIN,
    Roles.QA_AUDITOR,
    Roles.STUDY_DIRECTOR,
    Roles.AUDIT_TRAIL,
  ],
};

/**
 * @deprecated Use Permissions instead. RoleGroups kept for backwards compatibility.
 */
export const RoleGroups = {
  // Backwards compatibility - maps to new Permissions
  NOTEBOOK_ADMINS: Permissions.CREATE_OR_EDIT_NOTEBOOK,
  ADMIN_ROLES: Permissions.MANAGE_USERS,
  RESULTS_VIEWERS: Permissions.VIEW_RESULTS,
  LAB_ROLES: [
    Roles.TECHNICIAN,
    Roles.SUPERVISOR,
    Roles.RESULTS,
    Roles.VALIDATION,
    Roles.RECEPTION,
    Roles.REPORTS,
  ],
  PATHOLOGY_ROLES: [Roles.PATHOLOGIST, Roles.CYTOPATHOLOGIST],
  BIOANALYTICAL_ROLES: [
    Roles.BIOANALYTICAL_ANALYST,
    Roles.BIOANALYTICAL_SUPERVISOR,
    Roles.SAMPLE_RECEIVER,
    Roles.CHEMICAL_ANALYST,
    Roles.PHARMACIST,
    Roles.STUDY_DIRECTOR,
    Roles.QA_AUDITOR,
  ],
};

export default Roles;