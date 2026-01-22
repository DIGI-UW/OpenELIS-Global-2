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
  const { userSessionDetails, hasLabUnitRole, hasRoleForCurrentLabUnit } =
    usePermissions();

  const BIOEQUIVALENCE_ROLES = useMemo(
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

  const BIOEQUIVALENCE_PAGES = useMemo(
    () => ({
      SAMPLE_RECEPTION: "Sample Reception",
      TEST_ASSIGNMENT: "Test Assignment",
      ANALYTICAL_EXECUTION: "Analytical Execution",
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

      // Fallback: Check all bioequivalence roles the user has and return the highest permission level
      // This mapping should match the Liquibase migration for bioequivalence permission levels
      const userBioequivalenceRoles = Object.values(
        BIOEQUIVALENCE_ROLES,
      ).filter((role) => hasRoleForCurrentLabUnit([role]));

      if (userBioequivalenceRoles.length === 0) {
        return null;
      }

      // Fallback permission map based on role-page combinations from database
      // This serves as a fallback if backend doesn't provide bioequivalencePermissions
      const permissionMap = {
        [BIOEQUIVALENCE_PAGES.SAMPLE_RECEPTION]: {
          [BIOEQUIVALENCE_ROLES.SAMPLE_RECEIVER]: "REGISTER",
          [BIOEQUIVALENCE_ROLES.CHEMICAL_ANALYST]: "UPDATE",
          [BIOEQUIVALENCE_ROLES.PHARMACIST]: "UPDATE",
          [BIOEQUIVALENCE_ROLES.RESEARCHER]: "VIEW",
          [BIOEQUIVALENCE_ROLES.LAB_SUPERVISOR]: "FULL",
          [BIOEQUIVALENCE_ROLES.STUDY_DIRECTOR]: "VIEW",
          [BIOEQUIVALENCE_ROLES.QA_OFFICER]: "VIEW",
          [BIOEQUIVALENCE_ROLES.DATA_MANAGER]: "VIEW",
        },
        [BIOEQUIVALENCE_PAGES.TEST_ASSIGNMENT]: {
          [BIOEQUIVALENCE_ROLES.SAMPLE_RECEIVER]: "UPDATE",
          [BIOEQUIVALENCE_ROLES.CHEMICAL_ANALYST]: "UPDATE",
          [BIOEQUIVALENCE_ROLES.PHARMACIST]: "FULL",
          [BIOEQUIVALENCE_ROLES.RESEARCHER]: "UPDATE",
          [BIOEQUIVALENCE_ROLES.LAB_SUPERVISOR]: "FULL",
          [BIOEQUIVALENCE_ROLES.STUDY_DIRECTOR]: "UPDATE",
          [BIOEQUIVALENCE_ROLES.QA_OFFICER]: "VIEW",
          [BIOEQUIVALENCE_ROLES.DATA_MANAGER]: "UPDATE",
        },
        [BIOEQUIVALENCE_PAGES.ANALYTICAL_EXECUTION]: {
          [BIOEQUIVALENCE_ROLES.SAMPLE_RECEIVER]: "VIEW",
          [BIOEQUIVALENCE_ROLES.CHEMICAL_ANALYST]: "FULL",
          [BIOEQUIVALENCE_ROLES.PHARMACIST]: "FULL",
          [BIOEQUIVALENCE_ROLES.RESEARCHER]: "VIEW",
          [BIOEQUIVALENCE_ROLES.LAB_SUPERVISOR]: "FULL",
          [BIOEQUIVALENCE_ROLES.STUDY_DIRECTOR]: "VIEW",
          [BIOEQUIVALENCE_ROLES.QA_OFFICER]: "VIEW",
          [BIOEQUIVALENCE_ROLES.DATA_MANAGER]: "VIEW",
        },
        [BIOEQUIVALENCE_PAGES.REPORTING]: {
          [BIOEQUIVALENCE_ROLES.SAMPLE_RECEIVER]: "VIEW",
          [BIOEQUIVALENCE_ROLES.CHEMICAL_ANALYST]: "LIMITED",
          [BIOEQUIVALENCE_ROLES.PHARMACIST]: "FULL",
          [BIOEQUIVALENCE_ROLES.RESEARCHER]: "PROJECT_SPECIFIC",
          [BIOEQUIVALENCE_ROLES.LAB_SUPERVISOR]: "FULL",
          [BIOEQUIVALENCE_ROLES.STUDY_DIRECTOR]: "FULL",
          [BIOEQUIVALENCE_ROLES.QA_OFFICER]: "FULL",
          [BIOEQUIVALENCE_ROLES.DATA_MANAGER]: "FULL_ANALYTICS",
        },
        [BIOEQUIVALENCE_PAGES.STORAGE_ARCHIVING]: {
          [BIOEQUIVALENCE_ROLES.SAMPLE_RECEIVER]: "RESTRICTED",
          [BIOEQUIVALENCE_ROLES.CHEMICAL_ANALYST]: "UPDATE",
          [BIOEQUIVALENCE_ROLES.PHARMACIST]: "UPDATE",
          [BIOEQUIVALENCE_ROLES.RESEARCHER]: "VIEW",
          [BIOEQUIVALENCE_ROLES.LAB_SUPERVISOR]: "APPROVE",
          [BIOEQUIVALENCE_ROLES.STUDY_DIRECTOR]: "APPROVE",
          [BIOEQUIVALENCE_ROLES.QA_OFFICER]: "RESTRICTED",
          [BIOEQUIVALENCE_ROLES.DATA_MANAGER]: "VIEW",
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
   * Check if user has a specific bioequivalence role
   * @param {string} role - One of the BIOEQUIVALENCE_ROLES values
   * @returns {boolean}
   */
  const hasBioequivalenceRole = useCallback(
    (role) => {
      return hasRoleForCurrentLabUnit([role]);
    },
    [hasRoleForCurrentLabUnit],
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
        hasRoleForCurrentLabUnit([role]),
      ) || null
    );
  }, [BIOEQUIVALENCE_ROLES, hasRoleForCurrentLabUnit]);

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
    hasBioequivalenceRole,
    hasAnyBioequivalenceRole,
    getBioequivalenceRole,
  };
};

export default useBioequivalencePermissions;
