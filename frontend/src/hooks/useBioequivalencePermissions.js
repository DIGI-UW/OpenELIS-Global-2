import { useCallback, useMemo } from "react";
import { usePermissions } from "./usePermissions";

/**
 * Custom hook for bioequivalence workflow action-level permissions
 *
 * Provides utilities to check user permissions for specific actions on bioequivalence pages.
 * Works with the three-layer RBAC system:
 * - Layer 1: System roles (defines which roles exist)
 * - Layer 2: Page-level access (controls page visibility)
 * - Layer 3: Action-level permissions (controls what actions users can perform)
 *
 * Permission levels for bioequivalence workflows:
 * - VIEW: Read-only access
 * - REGISTER: Can register/import samples
 * - UPDATE: Can edit and update data
 * - FULL: Complete control including save and submit
 * - LIMITED: Restricted access with minimal modifications
 * - APPROVE: Can approve operations
 * - PROJECT_SPECIFIC: Variable access by project
 * - FULL_ANALYTICS: Complete analytics and reporting access
 * - RESTRICTED: Severely limited access
 *
 * @example
 * const { getPagePermissionLevel, canRegisterSamples, canSaveData } = useBioequivalencePermissions();
 *
 * // Get user's permission level for a page
 * const permLevel = getPagePermissionLevel('Sample Reception');
 *
 * // Check if user can perform specific actions
 * if (canSaveData(permLevel)) {
 *   // Enable Save button
 * }
 *
 * if (canApproveData(permLevel)) {
 *   // Show Approve button
 * }
 */
export const useBioequivalencePermissions = () => {
  const {
    userSessionDetails,
    hasRole,
    hasAnyRole,
    hasLabUnitRole,
    isGlobalAdmin,
  } = usePermissions();

  const BIOEQUIVALENCE_LAB_UNIT = "Bioequivalence Laboratory";

  /**
   * Custom hasLabUnitRole that DOES NOT bypass checks for Global Admins
   * This ensures bioequivalence permissions respect actual role assignments
   * @param {string} labUnit - Lab unit name or ID
   * @param {string} role - Role name to check
   * @returns {boolean}
   */
  const hasBioequivalenceLabUnitRoleStrict = useCallback(
    (labUnit, role) => {
      // FOR BIOEQUIVALENCE PERMISSIONS: DO NOT bypass global admin checks
      // This ensures bioequivalence permissions work based on actual role assignments

      if (!userSessionDetails?.userLabRolesMap) {
        return false;
      }

      // Check "AllLabUnits" for global lab access
      const allLabUnitsRoles =
        userSessionDetails.userLabRolesMap["AllLabUnits"] || [];
      if (allLabUnitsRoles.includes(role)) {
        return true;
      }

      // Check specific lab unit
      const labUnitRoles = userSessionDetails.userLabRolesMap[labUnit] || [];
      return labUnitRoles.includes(role);
    },
    [userSessionDetails?.userLabRolesMap],
  );

  const BIOEQUIVALENCE_ROLES = useMemo(
    () => ({
      SAMPLE_RECEIVER: "Bioequivalence Sample Receiver",
      CHEMICAL_ANALYST: "Bioequivalence Chemical Analyst",
      PHARMACIST: "Bioequivalence Pharmacist",
      RESEARCHER: "Bioequivalence Researcher",
      LAB_SUPERVISOR: "Bioequivalence Lab Supervisor",
      STUDY_DIRECTOR: "Bioequivalence Study Director",
      QA_OFFICER: "Bioequivalence QA Officer",
      DATA_MANAGER: "Bioequivalence Data Manager",
    }),
    [],
  );

  const BIOEQUIVALENCE_PAGES = useMemo(
    () => ({
      SAMPLE_RECEPTION: "Sample Reception",
      TEST_ASSIGNMENT: "Test Assignment",
      ANALYTICAL_EXECUTION: "Analytical Execution",
      RESULT_ENTRY: "Result Entry",
      VALIDATION: "Validation",
      REPORTING: "Reporting & Release",
      STORAGE_ARCHIVING: "Post-Test Storage & Archiving",
    }),
    [],
  );

  /**
   * Get the user's permission level for a specific bioequivalence page
   *
   * Fetches from userSessionDetails.bioequivalencePermissions which should contain
   * permission levels for each page, sourced from the database notebook_page_allowed_roles table.
   *
   * @param {string} pageName - Name of the bioequivalence page
   * @returns {string|null} - Permission level (VIEW, UPDATE, FULL, etc.) or null if no access
   */
  const getPagePermissionLevel = useCallback(
    (pageName) => {
      // First try to get from userSessionDetails if available
      if (userSessionDetails?.bioequivalencePermissions?.[pageName]) {
        return userSessionDetails.bioequivalencePermissions[pageName];
      }

      const userBioequivalenceRoles = Object.values(
        BIOEQUIVALENCE_ROLES,
      ).filter((role) =>
        hasBioequivalenceLabUnitRoleStrict(BIOEQUIVALENCE_LAB_UNIT, role),
      );

      if (userBioequivalenceRoles.length === 0) {
        return null;
      }

      // Permission map based on test.pdf Section 11 permission matrix
      // This serves as a fallback if backend doesn't provide bioequivalencePermissions
      const permissionMap = {
        [BIOEQUIVALENCE_PAGES.SAMPLE_RECEPTION]: {
          // Matrix: Sample Receivers (Yes), Chemical Analysts (Update), Pharmacists (Update), Researchers (View), Lab Supervisors (Full), Study Directors (View), QA Officers (View), Data Managers (No)
          [BIOEQUIVALENCE_ROLES.SAMPLE_RECEIVER]: "REGISTER",
          [BIOEQUIVALENCE_ROLES.CHEMICAL_ANALYST]: "UPDATE",
          [BIOEQUIVALENCE_ROLES.PHARMACIST]: "UPDATE",
          [BIOEQUIVALENCE_ROLES.RESEARCHER]: "VIEW",
          [BIOEQUIVALENCE_ROLES.LAB_SUPERVISOR]: "FULL",
          [BIOEQUIVALENCE_ROLES.STUDY_DIRECTOR]: "VIEW",
          [BIOEQUIVALENCE_ROLES.QA_OFFICER]: "VIEW",
          // Data Manager excluded per matrix (No)
        },
        [BIOEQUIVALENCE_PAGES.TEST_ASSIGNMENT]: {
          // Matrix: Sample Receivers (No), Chemical Analysts (View), Pharmacists (Full), Researchers (View), Lab Supervisors (Full), Study Directors (View), QA Officers (View), Data Managers (No)
          // Sample Receiver excluded per matrix (No)
          [BIOEQUIVALENCE_ROLES.CHEMICAL_ANALYST]: "VIEW",
          [BIOEQUIVALENCE_ROLES.PHARMACIST]: "FULL",
          [BIOEQUIVALENCE_ROLES.RESEARCHER]: "VIEW",
          [BIOEQUIVALENCE_ROLES.LAB_SUPERVISOR]: "FULL",
          [BIOEQUIVALENCE_ROLES.STUDY_DIRECTOR]: "VIEW",
          [BIOEQUIVALENCE_ROLES.QA_OFFICER]: "VIEW",
          // Data Manager excluded per matrix (No)
        },
        [BIOEQUIVALENCE_PAGES.ANALYTICAL_EXECUTION]: {
          // Matrix: Sample Receivers (No), Chemical Analysts (Full), Pharmacists (Full), Researchers (View), Lab Supervisors (Full), Study Directors (View), QA Officers (View), Data Managers (No)
          // Sample Receiver excluded per matrix (No)
          [BIOEQUIVALENCE_ROLES.CHEMICAL_ANALYST]: "FULL",
          [BIOEQUIVALENCE_ROLES.PHARMACIST]: "FULL",
          [BIOEQUIVALENCE_ROLES.RESEARCHER]: "VIEW",
          [BIOEQUIVALENCE_ROLES.LAB_SUPERVISOR]: "FULL",
          [BIOEQUIVALENCE_ROLES.STUDY_DIRECTOR]: "VIEW",
          [BIOEQUIVALENCE_ROLES.QA_OFFICER]: "VIEW",
          // Data Manager excluded per matrix (No)
        },
        [BIOEQUIVALENCE_PAGES.RESULT_ENTRY]: {
          // Matrix: Sample Receivers (No), Chemical Analysts (Full), Pharmacists (Full), Researchers (View), Lab Supervisors (Full), Study Directors (View), QA Officers (View), Data Managers (View)
          // Sample Receiver excluded per matrix (No)
          [BIOEQUIVALENCE_ROLES.CHEMICAL_ANALYST]: "FULL",
          [BIOEQUIVALENCE_ROLES.PHARMACIST]: "FULL",
          [BIOEQUIVALENCE_ROLES.RESEARCHER]: "VIEW",
          [BIOEQUIVALENCE_ROLES.LAB_SUPERVISOR]: "FULL",
          [BIOEQUIVALENCE_ROLES.STUDY_DIRECTOR]: "VIEW",
          [BIOEQUIVALENCE_ROLES.QA_OFFICER]: "VIEW",
          [BIOEQUIVALENCE_ROLES.DATA_MANAGER]: "VIEW",
        },
        [BIOEQUIVALENCE_PAGES.VALIDATION]: {
          // Matrix: Sample Receivers (No), Chemical Analysts (Validate), Pharmacists (Validate), Researchers (Review), Lab Supervisors (Final Approval), Study Directors (Final Approval), QA Officers (Final Approval), Data Managers (No)
          // Sample Receiver excluded per matrix (No)
          [BIOEQUIVALENCE_ROLES.CHEMICAL_ANALYST]: "VALIDATE",
          [BIOEQUIVALENCE_ROLES.PHARMACIST]: "VALIDATE",
          [BIOEQUIVALENCE_ROLES.RESEARCHER]: "REVIEW",
          [BIOEQUIVALENCE_ROLES.LAB_SUPERVISOR]: "APPROVE",
          [BIOEQUIVALENCE_ROLES.STUDY_DIRECTOR]: "APPROVE",
          [BIOEQUIVALENCE_ROLES.QA_OFFICER]: "APPROVE",
          // Data Manager excluded per matrix (No)
        },
        [BIOEQUIVALENCE_PAGES.REPORTING]: {
          // Matrix: Sample Receivers (No), Chemical Analysts (Limited), Pharmacists (Full), Researchers (Project-specific), Lab Supervisors (Full), Study Directors (Full), QA Officers (Full), Data Managers (Full Analytics)
          // Sample Receiver excluded per matrix (No)
          [BIOEQUIVALENCE_ROLES.CHEMICAL_ANALYST]: "LIMITED",
          [BIOEQUIVALENCE_ROLES.PHARMACIST]: "FULL",
          [BIOEQUIVALENCE_ROLES.RESEARCHER]: "PROJECT_SPECIFIC",
          [BIOEQUIVALENCE_ROLES.LAB_SUPERVISOR]: "FULL",
          [BIOEQUIVALENCE_ROLES.STUDY_DIRECTOR]: "FULL",
          [BIOEQUIVALENCE_ROLES.QA_OFFICER]: "FULL",
          [BIOEQUIVALENCE_ROLES.DATA_MANAGER]: "FULL_ANALYTICS",
        },
        [BIOEQUIVALENCE_PAGES.STORAGE_ARCHIVING]: {
          // Matrix: Sample Receivers (No), Chemical Analysts (No), Pharmacists (No), Researchers (No), Lab Supervisors (Approve), Study Directors (Approve), QA Officers (No), Data Managers (No)
          // Most roles excluded per matrix (No)
          [BIOEQUIVALENCE_ROLES.LAB_SUPERVISOR]: "APPROVE",
          [BIOEQUIVALENCE_ROLES.STUDY_DIRECTOR]: "APPROVE",
          // All other roles excluded per matrix
        },
      };

      const pagePermissions = permissionMap[pageName] || {};

      // Define permission hierarchy (higher index = higher privilege)
      const permissionHierarchy = [
        "RESTRICTED",
        "VIEW",
        "LIMITED",
        "REVIEW",
        "PROJECT_SPECIFIC",
        "REGISTER",
        "UPDATE",
        "VALIDATE",
        "APPROVE",
        "FULL_ANALYTICS",
        "FULL",
      ];

      // Get all permission levels for this page from all user roles
      const userPermissionLevels = userBioequivalenceRoles
        .map((role) => pagePermissions[role])
        .filter((level) => level !== undefined);

      // If no permissions found, return null
      if (userPermissionLevels.length === 0) {
        return null;
      }

      // Return the highest permission level among all user roles
      const highestLevel = userPermissionLevels.reduce((highest, current) => {
        const currentIndex = permissionHierarchy.indexOf(current);
        const highestIndex = permissionHierarchy.indexOf(highest);
        return currentIndex > highestIndex ? current : highest;
      });

      return highestLevel || null;
    },
    [
      BIOEQUIVALENCE_PAGES,
      BIOEQUIVALENCE_ROLES,
      hasBioequivalenceLabUnitRoleStrict,
      userSessionDetails,
    ],
  );

  /**
   * Check if a permission level allows saving/updating data
   * Allows: UPDATE, FULL, REGISTER
   * @param {string|null} permissionLevel
   * @returns {boolean}
   */
  const canSaveData = useCallback((permissionLevel) => {
    return (
      permissionLevel &&
      ["UPDATE", "FULL", "REGISTER"].includes(permissionLevel)
    );
  }, []);

  /**
   * Check if a permission level allows registering/importing data
   * Allows: REGISTER, UPDATE, FULL
   * @param {string|null} permissionLevel
   * @returns {boolean}
   */
  const canRegisterData = useCallback((permissionLevel) => {
    return (
      permissionLevel &&
      ["REGISTER", "UPDATE", "FULL"].includes(permissionLevel)
    );
  }, []);

  /**
   * Check if a permission level allows approving operations
   * Allows: APPROVE, FULL, FULL_ANALYTICS
   * @param {string|null} permissionLevel
   * @returns {boolean}
   */
  const canApproveData = useCallback((permissionLevel) => {
    return (
      permissionLevel &&
      ["APPROVE", "FULL", "FULL_ANALYTICS"].includes(permissionLevel)
    );
  }, []);

  /**
   * Check if a permission level allows full control
   * Allows: FULL only
   * @param {string|null} permissionLevel
   * @returns {boolean}
   */
  const hasFullControl = useCallback((permissionLevel) => {
    return permissionLevel === "FULL";
  }, []);

  /**
   * Check if a permission level is read-only
   * True for: VIEW, RESTRICTED
   * @param {string|null} permissionLevel
   * @returns {boolean}
   */
  const isReadOnly = useCallback((permissionLevel) => {
    return permissionLevel && ["VIEW", "RESTRICTED"].includes(permissionLevel);
  }, []);

  /**
   * Check if a permission level allows analytics/reporting operations
   * Allows: FULL_ANALYTICS, FULL, PROJECT_SPECIFIC
   * @param {string|null} permissionLevel
   * @returns {boolean}
   */
  const canAnalytics = useCallback((permissionLevel) => {
    return (
      permissionLevel &&
      ["FULL_ANALYTICS", "FULL", "PROJECT_SPECIFIC"].includes(permissionLevel)
    );
  }, []);

  /**
   * Check if a permission level allows limited modifications
   * Allows: LIMITED, UPDATE, FULL, PROJECT_SPECIFIC, APPROVE
   * @param {string|null} permissionLevel
   * @returns {boolean}
   */
  const canModify = useCallback((permissionLevel) => {
    return (
      permissionLevel &&
      ["LIMITED", "UPDATE", "FULL", "PROJECT_SPECIFIC", "APPROVE"].includes(
        permissionLevel,
      )
    );
  }, []);

  /**
   * Check if a permission level allows validation operations
   * Allows: VALIDATE, APPROVE, FULL
   * @param {string|null} permissionLevel
   * @returns {boolean}
   */
  const canValidate = useCallback((permissionLevel) => {
    return (
      permissionLevel &&
      ["VALIDATE", "APPROVE", "FULL"].includes(permissionLevel)
    );
  }, []);

  /**
   * Check if a permission level allows review operations
   * Allows: REVIEW, VALIDATE, APPROVE, FULL
   * @param {string|null} permissionLevel
   * @returns {boolean}
   */
  const canReview = useCallback((permissionLevel) => {
    return (
      permissionLevel &&
      ["REVIEW", "VALIDATE", "APPROVE", "FULL"].includes(permissionLevel)
    );
  }, []);

  /**
   * Check if user has a specific bioequivalence role
   * @param {string} role - One of the BIOEQUIVALENCE_ROLES values
   * @returns {boolean}
   */
  const hasBioequivalenceRole = useCallback(
    (role) => {
      return hasBioequivalenceLabUnitRoleStrict(BIOEQUIVALENCE_LAB_UNIT, role);
    },
    [hasBioequivalenceLabUnitRoleStrict],
  );

  /**
   * Check if user has any of the specified bioequivalence roles
   * @param {string[]} roles - Array of BIOEQUIVALENCE_ROLES values
   * @returns {boolean}
   */
  const hasAnyBioequivalenceRole = useCallback(
    (roles) => {
      if (!roles || !Array.isArray(roles)) {
        return false;
      }
      return roles.some((role) => hasBioequivalenceRole(role));
    },
    [hasBioequivalenceRole],
  );

  /**
   * Get the user's primary bioequivalence role (if any)
   * @returns {string|null}
   */
  const getBioequivalenceRole = useCallback(() => {
    return (
      Object.values(BIOEQUIVALENCE_ROLES).find((role) =>
        hasBioequivalenceLabUnitRoleStrict(BIOEQUIVALENCE_LAB_UNIT, role),
      ) || null
    );
  }, [BIOEQUIVALENCE_ROLES, hasBioequivalenceLabUnitRoleStrict]);

  /**
   * Bioequivalence-specific role checks based on bioequivalence lab unit roles
   * These use strict checking that does NOT bypass for Global Admins
   */
  const isBioequivalenceSampleReceiver = useCallback(
    () =>
      hasBioequivalenceLabUnitRoleStrict(
        BIOEQUIVALENCE_LAB_UNIT,
        BIOEQUIVALENCE_ROLES.SAMPLE_RECEIVER,
      ),
    [hasBioequivalenceLabUnitRoleStrict],
  );

  const isBioequivalenceChemicalAnalyst = useCallback(
    () =>
      hasBioequivalenceLabUnitRoleStrict(
        BIOEQUIVALENCE_LAB_UNIT,
        BIOEQUIVALENCE_ROLES.CHEMICAL_ANALYST,
      ),
    [hasBioequivalenceLabUnitRoleStrict],
  );

  const isBioequivalencePharmacist = useCallback(
    () =>
      hasBioequivalenceLabUnitRoleStrict(
        BIOEQUIVALENCE_LAB_UNIT,
        BIOEQUIVALENCE_ROLES.PHARMACIST,
      ),
    [hasBioequivalenceLabUnitRoleStrict],
  );

  const isBioequivalenceResearcher = useCallback(
    () =>
      hasBioequivalenceLabUnitRoleStrict(
        BIOEQUIVALENCE_LAB_UNIT,
        BIOEQUIVALENCE_ROLES.RESEARCHER,
      ),
    [hasBioequivalenceLabUnitRoleStrict],
  );

  const isBioequivalenceLabSupervisor = useCallback(
    () =>
      hasBioequivalenceLabUnitRoleStrict(
        BIOEQUIVALENCE_LAB_UNIT,
        BIOEQUIVALENCE_ROLES.LAB_SUPERVISOR,
      ),
    [hasBioequivalenceLabUnitRoleStrict],
  );

  const isBioequivalenceStudyDirector = useCallback(
    () =>
      hasBioequivalenceLabUnitRoleStrict(
        BIOEQUIVALENCE_LAB_UNIT,
        BIOEQUIVALENCE_ROLES.STUDY_DIRECTOR,
      ),
    [hasBioequivalenceLabUnitRoleStrict],
  );

  const isBioequivalenceQAOfficer = useCallback(
    () =>
      hasBioequivalenceLabUnitRoleStrict(
        BIOEQUIVALENCE_LAB_UNIT,
        BIOEQUIVALENCE_ROLES.QA_OFFICER,
      ),
    [hasBioequivalenceLabUnitRoleStrict],
  );

  const isBioequivalenceDataManager = useCallback(
    () =>
      hasBioequivalenceLabUnitRoleStrict(
        BIOEQUIVALENCE_LAB_UNIT,
        BIOEQUIVALENCE_ROLES.DATA_MANAGER,
      ),
    [hasBioequivalenceLabUnitRoleStrict],
  );

  /**
   * Helper function to check if user has any of the specified bioequivalence lab unit roles
   * Uses strict checking that does NOT bypass for Global Admins
   */
  const hasAnyBioequivalenceLabUnitRole = useCallback(
    (roles) => {
      return roles.some((role) =>
        hasBioequivalenceLabUnitRoleStrict(BIOEQUIVALENCE_LAB_UNIT, role),
      );
    },
    [hasBioequivalenceLabUnitRoleStrict],
  );

  /**
   * Page-specific access checks per bioequivalence workflow stages
   * Based on test.pdf Section 11 permission matrix
   */
  const canAccessSampleReception = useCallback(() => {
    // Matrix: Sample Receivers (Yes), Chemical Analysts (Update), Pharmacists (Update), Researchers (View), Lab Supervisors (Full), Study Directors (View), QA Officers (View), Data Managers (No)
    const allowedRoles = [
      BIOEQUIVALENCE_ROLES.SAMPLE_RECEIVER,
      BIOEQUIVALENCE_ROLES.CHEMICAL_ANALYST,
      BIOEQUIVALENCE_ROLES.PHARMACIST,
      BIOEQUIVALENCE_ROLES.RESEARCHER,
      BIOEQUIVALENCE_ROLES.LAB_SUPERVISOR,
      BIOEQUIVALENCE_ROLES.STUDY_DIRECTOR,
      BIOEQUIVALENCE_ROLES.QA_OFFICER,
      // Data Manager excluded per matrix (No)
    ];
    return hasAnyBioequivalenceRole(allowedRoles);
  }, [hasAnyBioequivalenceRole, BIOEQUIVALENCE_ROLES]);

  const canAccessTestAssignment = useCallback(() => {
    // Matrix: Sample Receivers (No), Chemical Analysts (View), Pharmacists (Full), Researchers (View), Lab Supervisors (Full), Study Directors (View), QA Officers (View), Data Managers (No)
    const allowedRoles = [
      // Sample Receiver excluded per matrix (No)
      BIOEQUIVALENCE_ROLES.CHEMICAL_ANALYST,
      BIOEQUIVALENCE_ROLES.PHARMACIST,
      BIOEQUIVALENCE_ROLES.RESEARCHER,
      BIOEQUIVALENCE_ROLES.LAB_SUPERVISOR,
      BIOEQUIVALENCE_ROLES.STUDY_DIRECTOR,
      BIOEQUIVALENCE_ROLES.QA_OFFICER,
      // Data Manager excluded per matrix (No)
    ];
    return hasAnyBioequivalenceRole(allowedRoles);
  }, [hasAnyBioequivalenceRole, BIOEQUIVALENCE_ROLES]);

  const canAccessAnalyticalExecution = useCallback(() => {
    // Matrix: Sample Receivers (No), Chemical Analysts (Full), Pharmacists (Full), Researchers (View), Lab Supervisors (Full), Study Directors (View), QA Officers (View), Data Managers (No)
    const allowedRoles = [
      // Sample Receiver excluded per matrix (No)
      BIOEQUIVALENCE_ROLES.CHEMICAL_ANALYST,
      BIOEQUIVALENCE_ROLES.PHARMACIST,
      BIOEQUIVALENCE_ROLES.RESEARCHER,
      BIOEQUIVALENCE_ROLES.LAB_SUPERVISOR,
      BIOEQUIVALENCE_ROLES.STUDY_DIRECTOR,
      BIOEQUIVALENCE_ROLES.QA_OFFICER,
      // Data Manager excluded per matrix (No)
    ];
    return hasAnyBioequivalenceRole(allowedRoles);
  }, [hasAnyBioequivalenceRole, BIOEQUIVALENCE_ROLES]);

  const canAccessReporting = useCallback(() => {
    // Matrix: Sample Receivers (No), Chemical Analysts (Limited), Pharmacists (Full), Researchers (Project-specific), Lab Supervisors (Full), Study Directors (Full), QA Officers (Full), Data Managers (Full Analytics)
    const allowedRoles = [
      // Sample Receiver excluded per matrix (No)
      BIOEQUIVALENCE_ROLES.CHEMICAL_ANALYST,
      BIOEQUIVALENCE_ROLES.PHARMACIST,
      BIOEQUIVALENCE_ROLES.RESEARCHER,
      BIOEQUIVALENCE_ROLES.LAB_SUPERVISOR,
      BIOEQUIVALENCE_ROLES.STUDY_DIRECTOR,
      BIOEQUIVALENCE_ROLES.QA_OFFICER,
      BIOEQUIVALENCE_ROLES.DATA_MANAGER,
    ];
    return hasAnyBioequivalenceRole(allowedRoles);
  }, [hasAnyBioequivalenceRole, BIOEQUIVALENCE_ROLES]);

  const canAccessStorageArchiving = useCallback(() => {
    // Matrix: Sample Receivers (No), Chemical Analysts (No), Pharmacists (No), Researchers (No), Lab Supervisors (Approve), Study Directors (Approve), QA Officers (No), Data Managers (No)
    const allowedRoles = [
      // Only Lab Supervisors and Study Directors have access
      BIOEQUIVALENCE_ROLES.LAB_SUPERVISOR,
      BIOEQUIVALENCE_ROLES.STUDY_DIRECTOR,
      // All other roles excluded per matrix (No)
    ];
    return hasAnyBioequivalenceRole(allowedRoles);
  }, [hasAnyBioequivalenceRole, BIOEQUIVALENCE_ROLES]);

  return {
    BIOEQUIVALENCE_ROLES,
    BIOEQUIVALENCE_PAGES,

    getPagePermissionLevel,
    canSaveData,
    canRegisterData,
    canApproveData,
    hasFullControl,
    isReadOnly,
    canAnalytics,
    canModify,
    canValidate,
    canReview,
    hasBioequivalenceRole,
    hasAnyBioequivalenceRole,
    getBioequivalenceRole,

    // Bioequivalence-specific role checks
    isBioequivalenceSampleReceiver,
    isBioequivalenceChemicalAnalyst,
    isBioequivalencePharmacist,
    isBioequivalenceResearcher,
    isBioequivalenceLabSupervisor,
    isBioequivalenceStudyDirector,
    isBioequivalenceQAOfficer,
    isBioequivalenceDataManager,
    hasAnyBioequivalenceLabUnitRole,

    // Page-specific access checks
    canAccessSampleReception,
    canAccessTestAssignment,
    canAccessAnalyticalExecution,
    canAccessReporting,
    canAccessStorageArchiving,
  };
};

export default useBioequivalencePermissions;
