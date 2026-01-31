import { useCallback, useMemo } from "react";
import { usePermissions } from "./usePermissions";

/**
 * Custom hook for bioanalytical workflow action-level permissions
 *
 * Provides utilities to check user permissions for specific actions on bioanalytical pages.
 * Works with the three-layer RBAC system:
 * - Layer 1: System roles (defines which roles exist)
 * - Layer 2: Page-level access (controls page visibility)
 * - Layer 3: Action-level permissions (controls what actions users can perform)
 *
 * Permission levels for bioanalytical workflows:
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
 * const { getPagePermissionLevel, canRegisterSamples, canSaveData } = useBioanalyticalPermissions();
 *
 * // Get user's permission level for a page
 * const permLevel = getPagePermissionLevel('Sample Reception & Registration');
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
export const useBioanalyticalPermissions = () => {
  const {
    userSessionDetails,
    hasRole,
    hasAnyRole,
    hasLabUnitRole,
    isGlobalAdmin,
  } = usePermissions();

  const BIOANALYTICAL_LAB_UNIT = "Bioanalytical";

  /**
   * Custom hasLabUnitRole that DOES NOT bypass checks for Global Admins
   * This ensures bioanalytical permissions respect actual role assignments
   * @param {string} labUnit - Lab unit name or ID
   * @param {string} role - Role name to check
   * @returns {boolean}
   */
  const hasBioanalyticalLabUnitRoleStrict = useCallback(
    (labUnit, role) => {
      // FOR BIOANALYTICAL PERMISSIONS: DO NOT bypass global admin checks
      // This ensures bioanalytical permissions work based on actual role assignments

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

  const BIOANALYTICAL_ROLES = useMemo(
    () => ({
      SAMPLE_RECEIVER: "Bioanalytical Sample Receiver",
      CHEMICAL_ANALYST: "Bioanalytical Chemical Analyst",
      PHARMACIST: "Bioanalytical Pharmacist",
      RESEARCHER: "Bioanalytical Researcher",
      LAB_SUPERVISOR: "Bioanalytical Lab Supervisor",
      STUDY_DIRECTOR: "Bioanalytical Study Director",
      QA_OFFICER: "Bioanalytical QA Officer",
      DATA_MANAGER: "Bioanalytical Data Manager",
    }),
    [],
  );

  const BIOANALYTICAL_PAGES = useMemo(
    () => ({
      SAMPLE_RECEPTION: "Sample Reception & Registration",
      TEST_ASSIGNMENT: "Test Assignment",
      ANALYTICAL_EXECUTION: "Analytical Execution",
      REPORTING: "Reporting",
      STORAGE_ARCHIVING: "Storage & Archiving",
    }),
    [],
  );

  /**
   * Get the user's permission level for a specific bioanalytical page
   *
   * Fetches from userSessionDetails.bioanalyticalPermissions which should contain
   * permission levels for each page, sourced from the database notebook_page_allowed_roles table.
   *
   * @param {string} pageName - Name of the bioanalytical page
   * @returns {string|null} - Permission level (VIEW, UPDATE, FULL, etc.) or null if no access
   */
  const getPagePermissionLevel = useCallback(
    (pageName) => {
      if (userSessionDetails?.bioanalyticalPermissions?.[pageName]) {
        return userSessionDetails.bioanalyticalPermissions[pageName];
      }

      const userBioanalyticalRoles = Object.values(BIOANALYTICAL_ROLES).filter(
        (role) => hasBioanalyticalLabUnitRoleStrict(BIOANALYTICAL_LAB_UNIT, role),
      );

      if (userBioanalyticalRoles.length === 0) {
        return null;
      }

      // Fallback permission map based on role-page combinations from database
      // This serves as a fallback if backend doesn't provide bioanalyticalPermissions
      const permissionMap = {
        [BIOANALYTICAL_PAGES.SAMPLE_RECEPTION]: {
          [BIOANALYTICAL_ROLES.SAMPLE_RECEIVER]: "REGISTER",
          [BIOANALYTICAL_ROLES.CHEMICAL_ANALYST]: "UPDATE",
          [BIOANALYTICAL_ROLES.PHARMACIST]: "UPDATE",
          [BIOANALYTICAL_ROLES.RESEARCHER]: "VIEW",
          [BIOANALYTICAL_ROLES.LAB_SUPERVISOR]: "FULL",
          [BIOANALYTICAL_ROLES.STUDY_DIRECTOR]: "VIEW",
          [BIOANALYTICAL_ROLES.QA_OFFICER]: "VIEW",
          [BIOANALYTICAL_ROLES.DATA_MANAGER]: "VIEW",
        },
        [BIOANALYTICAL_PAGES.TEST_ASSIGNMENT]: {
          [BIOANALYTICAL_ROLES.SAMPLE_RECEIVER]: "UPDATE",
          [BIOANALYTICAL_ROLES.CHEMICAL_ANALYST]: "UPDATE",
          [BIOANALYTICAL_ROLES.PHARMACIST]: "FULL",
          [BIOANALYTICAL_ROLES.RESEARCHER]: "UPDATE",
          [BIOANALYTICAL_ROLES.LAB_SUPERVISOR]: "FULL",
          [BIOANALYTICAL_ROLES.STUDY_DIRECTOR]: "UPDATE",
          [BIOANALYTICAL_ROLES.QA_OFFICER]: "VIEW",
          [BIOANALYTICAL_ROLES.DATA_MANAGER]: "UPDATE",
        },
        [BIOANALYTICAL_PAGES.ANALYTICAL_EXECUTION]: {
          [BIOANALYTICAL_ROLES.SAMPLE_RECEIVER]: "VIEW",
          [BIOANALYTICAL_ROLES.CHEMICAL_ANALYST]: "FULL",
          [BIOANALYTICAL_ROLES.PHARMACIST]: "FULL",
          [BIOANALYTICAL_ROLES.RESEARCHER]: "VIEW",
          [BIOANALYTICAL_ROLES.LAB_SUPERVISOR]: "FULL",
          [BIOANALYTICAL_ROLES.STUDY_DIRECTOR]: "VIEW",
          [BIOANALYTICAL_ROLES.QA_OFFICER]: "VIEW",
          [BIOANALYTICAL_ROLES.DATA_MANAGER]: "VIEW",
        },
        [BIOANALYTICAL_PAGES.REPORTING]: {
          [BIOANALYTICAL_ROLES.SAMPLE_RECEIVER]: "VIEW",
          [BIOANALYTICAL_ROLES.CHEMICAL_ANALYST]: "LIMITED",
          [BIOANALYTICAL_ROLES.PHARMACIST]: "FULL",
          [BIOANALYTICAL_ROLES.RESEARCHER]: "PROJECT_SPECIFIC",
          [BIOANALYTICAL_ROLES.LAB_SUPERVISOR]: "FULL",
          [BIOANALYTICAL_ROLES.STUDY_DIRECTOR]: "FULL",
          [BIOANALYTICAL_ROLES.QA_OFFICER]: "FULL",
          [BIOANALYTICAL_ROLES.DATA_MANAGER]: "FULL_ANALYTICS",
        },
        [BIOANALYTICAL_PAGES.STORAGE_ARCHIVING]: {
          [BIOANALYTICAL_ROLES.SAMPLE_RECEIVER]: "RESTRICTED",
          [BIOANALYTICAL_ROLES.CHEMICAL_ANALYST]: "UPDATE",
          [BIOANALYTICAL_ROLES.PHARMACIST]: "UPDATE",
          [BIOANALYTICAL_ROLES.RESEARCHER]: "VIEW",
          [BIOANALYTICAL_ROLES.LAB_SUPERVISOR]: "APPROVE",
          [BIOANALYTICAL_ROLES.STUDY_DIRECTOR]: "APPROVE",
          [BIOANALYTICAL_ROLES.QA_OFFICER]: "RESTRICTED",
          [BIOANALYTICAL_ROLES.DATA_MANAGER]: "VIEW",
        },
      };

      const pagePermissions = permissionMap[pageName] || {};

      // Define permission hierarchy (higher index = higher privilege)
      const permissionHierarchy = [
        "RESTRICTED",
        "VIEW",
        "LIMITED",
        "PROJECT_SPECIFIC",
        "REGISTER",
        "UPDATE",
        "APPROVE",
        "FULL_ANALYTICS",
        "FULL",
      ];

      // Get all permission levels for this page from all user roles
      const userPermissionLevels = userBioanalyticalRoles
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
      BIOANALYTICAL_PAGES,
      BIOANALYTICAL_ROLES,
      hasBioanalyticalLabUnitRoleStrict,
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
   * Allows: APPROVE, FULL
   * @param {string|null} permissionLevel
   * @returns {boolean}
   */
  const canApproveData = useCallback((permissionLevel) => {
    return permissionLevel && ["APPROVE", "FULL"].includes(permissionLevel);
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
   * Allows: FULL_ANALYTICS, FULL
   * @param {string|null} permissionLevel
   * @returns {boolean}
   */
  const canAnalytics = useCallback((permissionLevel) => {
    return (
      permissionLevel && ["FULL_ANALYTICS", "FULL"].includes(permissionLevel)
    );
  }, []);

  /**
   * Check if a permission level allows limited modifications
   * Allows: LIMITED, UPDATE, FULL
   * @param {string|null} permissionLevel
   * @returns {boolean}
   */
  const canModify = useCallback((permissionLevel) => {
    return (
      permissionLevel && ["LIMITED", "UPDATE", "FULL"].includes(permissionLevel)
    );
  }, []);

  /**
   * Check if user has a specific bioanalytical role
   * @param {string} role - One of the BIOANALYTICAL_ROLES values
   * @returns {boolean}
   */
  const hasBioanalyticalRole = useCallback(
    (role) => {
      return hasBioanalyticalLabUnitRoleStrict(BIOANALYTICAL_LAB_UNIT, role);
    },
    [hasBioanalyticalLabUnitRoleStrict],
  );

  /**
   * Check if user has any of the specified bioanalytical roles
   * @param {string[]} roles - Array of BIOANALYTICAL_ROLES values
   * @returns {boolean}
   */
  const hasAnyBioanalyticalRole = useCallback(
    (roles) => {
      if (!roles || !Array.isArray(roles)) {
        return false;
      }
      return roles.some((role) => hasBioanalyticalRole(role));
    },
    [hasBioanalyticalRole],
  );

  /**
   * Get the user's primary bioanalytical role (if any)
   * @returns {string|null}
   */
  const getBioanalyticalRole = useCallback(() => {
    return (
      Object.values(BIOANALYTICAL_ROLES).find((role) =>
        hasBioanalyticalLabUnitRoleStrict(BIOANALYTICAL_LAB_UNIT, role),
      ) || null
    );
  }, [BIOANALYTICAL_ROLES, hasBioanalyticalLabUnitRoleStrict]);

  /**
   * Bioanalytical-specific role checks based on bioanalytical lab unit roles
   * These use strict checking that does NOT bypass for Global Admins
   */
  const isBioanalyticalSampleReceiver = useCallback(
    () => hasBioanalyticalLabUnitRoleStrict(BIOANALYTICAL_LAB_UNIT, BIOANALYTICAL_ROLES.SAMPLE_RECEIVER),
    [hasBioanalyticalLabUnitRoleStrict],
  );

  const isBioanalyticalChemicalAnalyst = useCallback(
    () => hasBioanalyticalLabUnitRoleStrict(BIOANALYTICAL_LAB_UNIT, BIOANALYTICAL_ROLES.CHEMICAL_ANALYST),
    [hasBioanalyticalLabUnitRoleStrict],
  );

  const isBioanalyticalPharmacist = useCallback(
    () => hasBioanalyticalLabUnitRoleStrict(BIOANALYTICAL_LAB_UNIT, BIOANALYTICAL_ROLES.PHARMACIST),
    [hasBioanalyticalLabUnitRoleStrict],
  );

  const isBioanalyticalResearcher = useCallback(
    () => hasBioanalyticalLabUnitRoleStrict(BIOANALYTICAL_LAB_UNIT, BIOANALYTICAL_ROLES.RESEARCHER),
    [hasBioanalyticalLabUnitRoleStrict],
  );

  const isBioanalyticalLabSupervisor = useCallback(
    () => hasBioanalyticalLabUnitRoleStrict(BIOANALYTICAL_LAB_UNIT, BIOANALYTICAL_ROLES.LAB_SUPERVISOR),
    [hasBioanalyticalLabUnitRoleStrict],
  );

  const isBioanalyticalStudyDirector = useCallback(
    () => hasBioanalyticalLabUnitRoleStrict(BIOANALYTICAL_LAB_UNIT, BIOANALYTICAL_ROLES.STUDY_DIRECTOR),
    [hasBioanalyticalLabUnitRoleStrict],
  );

  const isBioanalyticalQAOfficer = useCallback(
    () => hasBioanalyticalLabUnitRoleStrict(BIOANALYTICAL_LAB_UNIT, BIOANALYTICAL_ROLES.QA_OFFICER),
    [hasBioanalyticalLabUnitRoleStrict],
  );

  const isBioanalyticalDataManager = useCallback(
    () => hasBioanalyticalLabUnitRoleStrict(BIOANALYTICAL_LAB_UNIT, BIOANALYTICAL_ROLES.DATA_MANAGER),
    [hasBioanalyticalLabUnitRoleStrict],
  );

  /**
   * Helper function to check if user has any of the specified bioanalytical lab unit roles
   * Uses strict checking that does NOT bypass for Global Admins
   */
  const hasAnyBioanalyticalLabUnitRole = useCallback(
    (roles) => {
      return roles.some((role) => hasBioanalyticalLabUnitRoleStrict(BIOANALYTICAL_LAB_UNIT, role));
    },
    [hasBioanalyticalLabUnitRoleStrict],
  );

  /**
   * Page-specific access checks per bioanalytical workflow stages
   */
  const canAccessSampleReception = useCallback(() => {
    const allowedRoles = [
      BIOANALYTICAL_ROLES.SAMPLE_RECEIVER,
      BIOANALYTICAL_ROLES.CHEMICAL_ANALYST,
      BIOANALYTICAL_ROLES.PHARMACIST,
      BIOANALYTICAL_ROLES.RESEARCHER,
      BIOANALYTICAL_ROLES.LAB_SUPERVISOR,
      BIOANALYTICAL_ROLES.STUDY_DIRECTOR,
      BIOANALYTICAL_ROLES.QA_OFFICER,
      BIOANALYTICAL_ROLES.DATA_MANAGER,
    ];
    return hasAnyBioanalyticalRole(allowedRoles);
  }, [hasAnyBioanalyticalRole, BIOANALYTICAL_ROLES]);

  const canAccessTestAssignment = useCallback(() => {
    const allowedRoles = [
      BIOANALYTICAL_ROLES.SAMPLE_RECEIVER,
      BIOANALYTICAL_ROLES.CHEMICAL_ANALYST,
      BIOANALYTICAL_ROLES.PHARMACIST,
      BIOANALYTICAL_ROLES.RESEARCHER,
      BIOANALYTICAL_ROLES.LAB_SUPERVISOR,
      BIOANALYTICAL_ROLES.STUDY_DIRECTOR,
      BIOANALYTICAL_ROLES.DATA_MANAGER,
    ];
    return hasAnyBioanalyticalRole(allowedRoles);
  }, [hasAnyBioanalyticalRole, BIOANALYTICAL_ROLES]);

  const canAccessAnalyticalExecution = useCallback(() => {
    const allowedRoles = [
      BIOANALYTICAL_ROLES.SAMPLE_RECEIVER,
      BIOANALYTICAL_ROLES.CHEMICAL_ANALYST,
      BIOANALYTICAL_ROLES.PHARMACIST,
      BIOANALYTICAL_ROLES.RESEARCHER,
      BIOANALYTICAL_ROLES.LAB_SUPERVISOR,
      BIOANALYTICAL_ROLES.STUDY_DIRECTOR,
      BIOANALYTICAL_ROLES.QA_OFFICER,
      BIOANALYTICAL_ROLES.DATA_MANAGER,
    ];
    return hasAnyBioanalyticalRole(allowedRoles);
  }, [hasAnyBioanalyticalRole, BIOANALYTICAL_ROLES]);

  const canAccessReporting = useCallback(() => {
    const allowedRoles = [
      BIOANALYTICAL_ROLES.SAMPLE_RECEIVER,
      BIOANALYTICAL_ROLES.CHEMICAL_ANALYST,
      BIOANALYTICAL_ROLES.PHARMACIST,
      BIOANALYTICAL_ROLES.RESEARCHER,
      BIOANALYTICAL_ROLES.LAB_SUPERVISOR,
      BIOANALYTICAL_ROLES.STUDY_DIRECTOR,
      BIOANALYTICAL_ROLES.QA_OFFICER,
      BIOANALYTICAL_ROLES.DATA_MANAGER,
    ];
    return hasAnyBioanalyticalRole(allowedRoles);
  }, [hasAnyBioanalyticalRole, BIOANALYTICAL_ROLES]);

  const canAccessStorageArchiving = useCallback(() => {
    const allowedRoles = [
      BIOANALYTICAL_ROLES.SAMPLE_RECEIVER,
      BIOANALYTICAL_ROLES.CHEMICAL_ANALYST,
      BIOANALYTICAL_ROLES.PHARMACIST,
      BIOANALYTICAL_ROLES.RESEARCHER,
      BIOANALYTICAL_ROLES.LAB_SUPERVISOR,
      BIOANALYTICAL_ROLES.STUDY_DIRECTOR,
      BIOANALYTICAL_ROLES.QA_OFFICER,
      BIOANALYTICAL_ROLES.DATA_MANAGER,
    ];
    return hasAnyBioanalyticalRole(allowedRoles);
  }, [hasAnyBioanalyticalRole, BIOANALYTICAL_ROLES]);

  return {
    BIOANALYTICAL_ROLES,
    BIOANALYTICAL_PAGES,

    getPagePermissionLevel,
    canSaveData,
    canRegisterData,
    canApproveData,
    hasFullControl,
    isReadOnly,
    canAnalytics,
    canModify,
    hasBioanalyticalRole,
    hasAnyBioanalyticalRole,
    getBioanalyticalRole,

    // Bioanalytical-specific role checks
    isBioanalyticalSampleReceiver,
    isBioanalyticalChemicalAnalyst,
    isBioanalyticalPharmacist,
    isBioanalyticalResearcher,
    isBioanalyticalLabSupervisor,
    isBioanalyticalStudyDirector,
    isBioanalyticalQAOfficer,
    isBioanalyticalDataManager,
    hasAnyBioanalyticalLabUnitRole,

    // Page-specific access checks
    canAccessSampleReception,
    canAccessTestAssignment,
    canAccessAnalyticalExecution,
    canAccessReporting,
    canAccessStorageArchiving,
  };
};

export default useBioanalyticalPermissions;
