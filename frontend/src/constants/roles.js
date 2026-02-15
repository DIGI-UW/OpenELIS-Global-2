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
 *
 * FIXME: These role-to-feature mappings are hardcoded. Ideally, this should be
 * configurable via backend configuration properties
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

  // ==========================================================================
  // AHRI Lab Roles - Granular privilege-based roles
  // These map to the role matrix defined in ahri_lab_roles.csv
  // ==========================================================================

  // Job Title / Persona Groupings (for reference, typically not used directly)
  SAMPLE_COLLECTOR: "Sample Collector",
  LABORATORY_TECHNICIAN: "Laboratory Technician",
  JUNIOR_SENIOR_RESEARCHER: "Junior Senior Researcher",
  LAB_MANAGER_SUPERVISOR: "Lab Manager Supervisor",

  // Sample Registration Privileges
  REGISTER_SAMPLES: "Register Samples",
  UPDATE_SAMPLES: "Update Samples",

  // Sample Processing / Analysis Privileges
  VIEW_PROCESSING_WORKFLOWS: "View Processing Workflows",
  UPDATE_PROCESSING_WORKFLOWS: "Update Processing Workflows",
  FULL_PROCESSING_ACCESS: "Full Processing Access",

  // Record Editing Privileges
  EDIT_OWN_RECORDS: "Edit Own Records",
  EDIT_PROCESSING_DATA: "Edit Processing Data",
  EDIT_ALL_RECORDS: "Edit All Records",

  // Result Review & Validation Privileges
  VALIDATE_RESULTS: "Validate Results",
  REVIEW_RESULTS: "Review Results",
  FULL_VALIDATION_ACCESS: "Full Validation Access",

  // Data Analysis & Reporting Privileges
  VIEW_OWN_REPORTS: "View Own Reports",
  AGGREGATE_ANALYZE_DATA: "Aggregate Analyze Data",
  FULL_REPORTING_ACCESS: "Full Reporting Access",

  // Equipment Management Privileges
  MANAGE_EQUIPMENT: "Manage Equipment",

  // Quality Assurance Privileges
  MANAGE_QA: "Manage QA",
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
  ENTER_RESULTS: [
    Roles.RESULTS,
    Roles.TECHNICIAN,
    Roles.SUPERVISOR,
    Roles.LABORATORY_TECHNICIAN,
    Roles.EDIT_PROCESSING_DATA,
  ],

  // Can validate results
  VALIDATE_RESULTS: [
    Roles.VALIDATION,
    Roles.SUPERVISOR,
    Roles.PATHOLOGIST,
    Roles.VALIDATE_RESULTS,
    Roles.FULL_VALIDATION_ACCESS,
    Roles.LAB_MANAGER_SUPERVISOR,
  ],

  // Can review results (without validation authority)
  REVIEW_RESULTS: [
    Roles.REVIEW_RESULTS,
    Roles.JUNIOR_SENIOR_RESEARCHER,
    Roles.SUPERVISOR,
  ],

  // Can view results
  VIEW_RESULTS: [
    Roles.RESULTS,
    Roles.VALIDATION,
    Roles.PATHOLOGIST,
    Roles.CYTOPATHOLOGIST,
    Roles.SUPERVISOR,
    Roles.REPORTS,
    Roles.REVIEW_RESULTS,
    Roles.VIEW_OWN_REPORTS,
  ],

  // ========== Sample Permissions ==========

  // Can register new samples
  REGISTER_SAMPLES: [
    Roles.RECEPTION,
    Roles.SUPERVISOR,
    Roles.REGISTER_SAMPLES,
    Roles.SAMPLE_COLLECTOR,
    Roles.LAB_MANAGER_SUPERVISOR,
  ],

  // Can update existing samples
  UPDATE_SAMPLES: [
    Roles.RECEPTION,
    Roles.TECHNICIAN,
    Roles.SUPERVISOR,
    Roles.UPDATE_SAMPLES,
    Roles.SAMPLE_COLLECTOR,
    Roles.LABORATORY_TECHNICIAN,
    Roles.LAB_MANAGER_SUPERVISOR,
  ],

  // Can receive/register samples (legacy - use REGISTER_SAMPLES)
  RECEIVE_SAMPLES: [
    Roles.RECEPTION,
    Roles.TECHNICIAN,
    Roles.SUPERVISOR,
    Roles.REGISTER_SAMPLES,
    Roles.SAMPLE_COLLECTOR,
  ],

  // Can process samples
  PROCESS_SAMPLES: [
    Roles.TECHNICIAN,
    Roles.SUPERVISOR,
    Roles.FULL_PROCESSING_ACCESS,
    Roles.UPDATE_PROCESSING_WORKFLOWS,
    Roles.LABORATORY_TECHNICIAN,
    Roles.LAB_MANAGER_SUPERVISOR,
  ],

  // Can view processing workflows (read-only)
  VIEW_PROCESSING: [
    Roles.TECHNICIAN,
    Roles.SUPERVISOR,
    Roles.VIEW_PROCESSING_WORKFLOWS,
    Roles.SAMPLE_COLLECTOR,
    Roles.LABORATORY_TECHNICIAN,
    Roles.JUNIOR_SENIOR_RESEARCHER,
  ],

  // ========== Record Editing Permissions ==========

  // Can edit own records only
  EDIT_OWN_RECORDS: [
    Roles.TECHNICIAN,
    Roles.EDIT_OWN_RECORDS,
    Roles.SAMPLE_COLLECTOR,
    Roles.LABORATORY_TECHNICIAN,
  ],

  // Can edit all records
  EDIT_ALL_RECORDS: [
    Roles.SUPERVISOR,
    Roles.GLOBAL_ADMIN,
    Roles.EDIT_ALL_RECORDS,
    Roles.LAB_MANAGER_SUPERVISOR,
    Roles.JUNIOR_SENIOR_RESEARCHER,
  ],

  // ========== Report Permissions ==========

  // Can view own reports only
  VIEW_OWN_REPORTS: [
    Roles.TECHNICIAN,
    Roles.VIEW_OWN_REPORTS,
    Roles.LABORATORY_TECHNICIAN,
  ],

  // Can aggregate and analyze data
  AGGREGATE_DATA: [
    Roles.SUPERVISOR,
    Roles.REPORTS,
    Roles.AGGREGATE_ANALYZE_DATA,
    Roles.JUNIOR_SENIOR_RESEARCHER,
    Roles.LAB_MANAGER_SUPERVISOR,
  ],

  // Can generate reports
  GENERATE_REPORTS: [
    Roles.REPORTS,
    Roles.SUPERVISOR,
    Roles.GLOBAL_ADMIN,
    Roles.FULL_REPORTING_ACCESS,
    Roles.LAB_MANAGER_SUPERVISOR,
  ],

  // ========== Equipment & QA Permissions ==========

  // Can manage equipment
  MANAGE_EQUIPMENT: [
    Roles.SUPERVISOR,
    Roles.GLOBAL_ADMIN,
    Roles.MANAGE_EQUIPMENT,
    Roles.LAB_MANAGER_SUPERVISOR,
  ],

  // Can manage quality assurance
  MANAGE_QA: [
    Roles.SUPERVISOR,
    Roles.GLOBAL_ADMIN,
    Roles.MANAGE_QA,
    Roles.QA_AUDITOR,
    Roles.LAB_MANAGER_SUPERVISOR,
  ],

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
  // AHRI Lab Role Groups
  AHRI_LAB_ROLES: [
    Roles.SAMPLE_COLLECTOR,
    Roles.LABORATORY_TECHNICIAN,
    Roles.JUNIOR_SENIOR_RESEARCHER,
    Roles.LAB_MANAGER_SUPERVISOR,
  ],
};

export default Roles;
