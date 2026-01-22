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
  const { userSessionDetails, hasLabUnitRole, hasRoleForCurrentLabUnit } =
    usePermissions();

  const BIOANALYTICAL_ROLES = useMemo(
    () => ({
      SAMPLE_RECEIVER: "Sample Receiver",
      CHEMICAL_ANALYST: "Chemical Analyst",
      PHARMACIST: "Pharmacist",
      RESEARCHER: "Researcher",
      LAB_SUPERVISOR: "Lab Supervisor",
      STUDY_DIRECTOR: "Study Director",
      QA_OFFICER: "QA Officer",
      DATA_MANAGER: "Data Manager",
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
      // First try to get from userSessionDetails if available
      if (userSessionDetails?.bioanalyticalPermissions?.[pageName]) {
        return userSessionDetails.bioanalyticalPermissions[pageName];
      }

      // Fallback: Check all bioanalytical roles the user has and return the highest permission level
      // This mapping should match the Liquibase migration 049-bioanalytical-permission-levels.xml
      const userBioanalyticalRoles = Object.values(BIOANALYTICAL_ROLES).filter(
        (role) => hasRoleForCurrentLabUnit([role]),
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
      hasRoleForCurrentLabUnit,
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
      return hasRoleForCurrentLabUnit([role]);
    },
    [hasRoleForCurrentLabUnit],
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
        hasRoleForCurrentLabUnit([role]),
      ) || null
    );
  }, [BIOANALYTICAL_ROLES, hasRoleForCurrentLabUnit]);

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
  };
};

export default useBioanalyticalPermissions;
