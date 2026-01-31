import { useCallback, useMemo } from "react";
import { usePermissions } from "./usePermissions";

/**
 * Custom hook for GBD (Genomics & Bioinformatics) workflow action-level permissions
 *
 * Provides utilities to check user permissions for specific actions on GBD pages.
 * Works with the three-layer RBAC system:
 * - Layer 1: System roles (defines which roles exist)
 * - Layer 2: Page-level access (controls page visibility)
 * - Layer 3: Action-level permissions (controls what actions users can perform)
 *
 * Permission levels for GBD workflows:
 * - VIEW: Read-only access
 * - UPDATE: Can edit and update data
 * - FULL: Complete control including save and submit
 *
 * @example
 * const { getPagePermissionLevel, canSaveData } = useGBDPermissions();
 *
 * // Get user's permission level for a page
 * const permLevel = getPagePermissionLevel('Sample Reception & Registration');
 *
 * // Check if user can perform specific actions
 * if (canSaveData(permLevel)) {
 *   // Enable Save button
 * }
 */
export const useGBDPermissions = () => {
  const {
    userSessionDetails,
    hasRole,
    hasAnyRole,
    hasLabUnitRole,
    isGlobalAdmin,
  } = usePermissions();

  const GBD_LAB_UNIT = "GBD";

  /**
   * Custom hasLabUnitRole that DOES NOT bypass checks for Global Admins
   * This ensures GBD permissions respect actual role assignments
   * @param {string} labUnit - Lab unit name or ID
   * @param {string} role - Role name to check
   * @returns {boolean}
   */
  const hasGBDLabUnitRoleStrict = useCallback(
    (labUnit, role) => {
      // FOR GBD PERMISSIONS: DO NOT bypass global admin checks
      // This ensures GBD permissions work based on actual role assignments

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

  const GBD_ROLES = useMemo(
    () => ({
      LAB_TECHNICIAN: "GBD Lab Technician",
      BIOINFORMATICIAN: "GBD Bioinformatician",
      MANAGER: "GBD Manager",
      PRINCIPAL_INVESTIGATOR: "GBD Principal Investigator",
      DATA_MANAGER: "GBD Data Manager",
    }),
    [],
  );

  const GBD_PAGES = useMemo(
    () => ({
      REGISTRATION: "Registration", // Maps to matrix column 1
      EXTRACTION: "Extraction", // Maps to matrix column 2
      QC: "QC", // Maps to matrix column 3
      PCR: "PCR", // Maps to matrix column 4
      LIBRARY_PREP: "Library Prep", // Maps to matrix column 5
      SEQUENCING: "Sequencing", // Maps to matrix column 6
      BIOINFORMATICS: "Bioinformatics", // Maps to matrix column 7
      DATA_RELEASE: "Data Release", // Maps to matrix column 8
      // Legacy names for backward compatibility
      SAMPLE_RECEPTION: "Registration",
      DNA_RNA_EXTRACTION: "Extraction",
      QUALITY_QUANTITY_ASSESSMENT: "QC",
      PCR_AMPLIFICATION: "PCR",
      LIBRARY_PREPARATION: "Library Prep",
      BIOINFORMATICS_ANALYSIS: "Bioinformatics",
    }),
    [],
  );

  /**
   * Get the user's permission level for a specific GBD page
   *
   * Fetches from userSessionDetails.gbdPermissions which should contain
   * permission levels for each page, sourced from the database notebook_page_allowed_roles table.
   *
   * @param {string} pageName - Name of the GBD page
   * @returns {string|null} - Permission level (VIEW, UPDATE, FULL, etc.) or null if no access
   */
  const getPagePermissionLevel = useCallback(
    (pageName) => {
      if (userSessionDetails?.gbdPermissions?.[pageName]) {
        return userSessionDetails.gbdPermissions[pageName];
      }

      const userGBDRoles = Object.values(GBD_ROLES).filter((role) =>
        hasGBDLabUnitRoleStrict(GBD_LAB_UNIT, role),
      );

      if (userGBDRoles.length === 0) {
        return null;
      }

      // Permission map based on the exact matrix from Section 11
      const permissionMap = {
        [GBD_PAGES.REGISTRATION]: {
          // Matrix: Lab Technicians (Yes), Bioinformaticians (View), Lab Manager (Full), Principal Investigator (View), Data Managers (No)
          [GBD_ROLES.LAB_TECHNICIAN]: "YES",
          [GBD_ROLES.BIOINFORMATICIAN]: "VIEW",
          [GBD_ROLES.MANAGER]: "FULL",
          [GBD_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
          // Data Managers excluded per matrix (No)
        },
        [GBD_PAGES.EXTRACTION]: {
          // Matrix: Lab Technicians (Yes), Bioinformaticians (No), Lab Manager (Full), Principal Investigator (View), Data Managers (No)
          [GBD_ROLES.LAB_TECHNICIAN]: "YES",
          // Bioinformaticians excluded per matrix (No)
          [GBD_ROLES.MANAGER]: "FULL",
          [GBD_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
          // Data Managers excluded per matrix (No)
        },
        [GBD_PAGES.QC]: {
          // Matrix: Lab Technicians (Yes), Bioinformaticians (No), Lab Manager (Full), Principal Investigator (View), Data Managers (No)
          [GBD_ROLES.LAB_TECHNICIAN]: "YES",
          // Bioinformaticians excluded per matrix (No)
          [GBD_ROLES.MANAGER]: "FULL",
          [GBD_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
          // Data Managers excluded per matrix (No)
        },
        [GBD_PAGES.PCR]: {
          // Matrix: Lab Technicians (Yes), Bioinformaticians (No), Lab Manager (Full), Principal Investigator (View), Data Managers (No)
          [GBD_ROLES.LAB_TECHNICIAN]: "YES",
          // Bioinformaticians excluded per matrix (No)
          [GBD_ROLES.MANAGER]: "FULL",
          [GBD_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
          // Data Managers excluded per matrix (No)
        },
        [GBD_PAGES.LIBRARY_PREP]: {
          // Matrix: Lab Technicians (Yes), Bioinformaticians (No), Lab Manager (Full), Principal Investigator (View), Data Managers (No)
          [GBD_ROLES.LAB_TECHNICIAN]: "YES",
          // Bioinformaticians excluded per matrix (No)
          [GBD_ROLES.MANAGER]: "FULL",
          [GBD_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
          // Data Managers excluded per matrix (No)
        },
        [GBD_PAGES.SEQUENCING]: {
          // Matrix: Lab Technicians (Yes), Bioinformaticians (View), Lab Manager (Full), Principal Investigator (View), Data Managers (No)
          [GBD_ROLES.LAB_TECHNICIAN]: "YES",
          [GBD_ROLES.BIOINFORMATICIAN]: "VIEW",
          [GBD_ROLES.MANAGER]: "FULL",
          [GBD_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
          // Data Managers excluded per matrix (No)
        },
        [GBD_PAGES.BIOINFORMATICS]: {
          // Matrix: Lab Technicians (No), Bioinformaticians (Full), Lab Manager (View), Principal Investigator (View), Data Managers (View)
          // Lab Technicians excluded per matrix (No)
          [GBD_ROLES.BIOINFORMATICIAN]: "FULL",
          [GBD_ROLES.MANAGER]: "VIEW",
          [GBD_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
          [GBD_ROLES.DATA_MANAGER]: "VIEW",
        },
        [GBD_PAGES.DATA_RELEASE]: {
          // Matrix: Lab Technicians (No), Bioinformaticians (No), Lab Manager (Approve), Principal Investigator (Final Approve), Data Managers (No)
          // Lab Technicians excluded per matrix (No)
          // Bioinformaticians excluded per matrix (No)
          [GBD_ROLES.MANAGER]: "APPROVE",
          [GBD_ROLES.PRINCIPAL_INVESTIGATOR]: "FINAL_APPROVE",
          // Data Managers excluded per matrix (No)
        },
      };

      const pagePermissions = permissionMap[pageName] || {};

      // Permission hierarchy (lowest to highest)
      const permissionHierarchy = [
        "VIEW", // Read-only
        "YES", // Can perform work and update data
        "APPROVE", // Can approve operations
        "FINAL_APPROVE", // Can provide final approval
        "FULL", // Complete control
      ];

      const userPermissionLevels = userGBDRoles
        .map((role) => pagePermissions[role])
        .filter((level) => level !== undefined && level !== null);

      if (userPermissionLevels.length === 0) {
        return null;
      }

      const highestLevel = userPermissionLevels.reduce((highest, current) => {
        const currentIndex = permissionHierarchy.indexOf(current);
        const highestIndex = permissionHierarchy.indexOf(highest);
        return currentIndex > highestIndex ? current : highest;
      });

      return highestLevel || null;
    },
    [GBD_PAGES, GBD_ROLES, hasLabUnitRole, userSessionDetails],
  );

  /**
   * Check if a permission level allows saving/updating data
   * Allows: YES, APPROVE, FINAL_APPROVE, FULL
   * @param {string|null} permissionLevel
   * @returns {boolean}
   */
  const canSaveData = useCallback((permissionLevel) => {
    return ["YES", "APPROVE", "FINAL_APPROVE", "FULL"].includes(
      permissionLevel?.toUpperCase(),
    );
  }, []);

  /**
   * Check if a permission level allows registering new data
   * Allows: YES, APPROVE, FINAL_APPROVE, FULL
   * @param {string|null} permissionLevel
   * @returns {boolean}
   */
  const canRegisterData = useCallback((permissionLevel) => {
    return ["YES", "APPROVE", "FINAL_APPROVE", "FULL"].includes(
      permissionLevel?.toUpperCase(),
    );
  }, []);

  /**
   * Check if user can perform work (YES permission)
   * Allows: YES, APPROVE, FINAL_APPROVE, FULL
   * @param {string|null} permissionLevel
   * @returns {boolean}
   */
  const canPerformWork = useCallback((permissionLevel) => {
    return ["YES", "APPROVE", "FINAL_APPROVE", "FULL"].includes(
      permissionLevel?.toUpperCase(),
    );
  }, []);

  /**
   * Check if user can approve operations
   * Allows: APPROVE, FINAL_APPROVE, FULL
   * @param {string|null} permissionLevel
   * @returns {boolean}
   */
  const canApproveData = useCallback((permissionLevel) => {
    return ["APPROVE", "FINAL_APPROVE", "FULL"].includes(permissionLevel?.toUpperCase());
  }, []);

  /**
   * Check if user can provide final approval
   * Allows: FINAL_APPROVE, FULL
   * @param {string|null} permissionLevel
   * @returns {boolean}
   */
  const canFinalApprove = useCallback((permissionLevel) => {
    return ["FINAL_APPROVE", "FULL"].includes(permissionLevel?.toUpperCase());
  }, []);

  /**
   * Check if user can modify data
   * Allows: YES, APPROVE, FINAL_APPROVE, FULL
   * @param {string|null} permissionLevel
   * @returns {boolean}
   */
  const canModify = useCallback((permissionLevel) => {
    return ["YES", "APPROVE", "FINAL_APPROVE", "FULL"].includes(
      permissionLevel?.toUpperCase(),
    );
  }, []);

  /**
   * Check if a permission level allows full control
   * Allows: FULL only
   * @param {string|null} permissionLevel
   * @returns {boolean}
   */
  const hasFullControl = useCallback((permissionLevel) => {
    return permissionLevel?.toUpperCase() === "FULL";
  }, []);

  /**
   * Check if a permission level is read-only
   * True for: VIEW only
   * @param {string|null} permissionLevel
   * @returns {boolean}
   */
  const isReadOnly = useCallback((permissionLevel) => {
    return permissionLevel?.toUpperCase() === "VIEW";
  }, []);

  /**
   * Check if user has a specific GBD role
   * @param {string} role - One of the GBD_ROLES values
   * @returns {boolean}
   */
  const hasGBDRole = useCallback(
    (role) => {
      return hasGBDLabUnitRoleStrict(GBD_LAB_UNIT, role);
    },
    [hasGBDLabUnitRoleStrict],
  );

  /**
   * Check if user has any of the specified GBD roles
   * @param {string[]} roles - Array of GBD_ROLES values
   * @returns {boolean}
   */
  const hasAnyGBDRole = useCallback(
    (roles) => {
      if (!roles || !Array.isArray(roles)) {
        return false;
      }
      return roles.some((role) => hasGBDRole(role));
    },
    [hasGBDRole],
  );

  /**
   * Get the user's primary GBD role (if any)
   * @returns {string|null}
   */
  const getGBDRole = useCallback(() => {
    return (
      Object.values(GBD_ROLES).find((role) =>
        hasGBDLabUnitRoleStrict(GBD_LAB_UNIT, role),
      ) || null
    );
  }, [GBD_ROLES, hasGBDLabUnitRoleStrict]);

  /**
   * GBD-specific role checks based on GBD lab unit roles
   * These use strict checking that does NOT bypass for Global Admins
   */
  const isGBDLabTechnician = useCallback(
    () => hasGBDLabUnitRoleStrict(GBD_LAB_UNIT, GBD_ROLES.LAB_TECHNICIAN),
    [hasGBDLabUnitRoleStrict],
  );

  const isGBDBioinformatician = useCallback(
    () => hasGBDLabUnitRoleStrict(GBD_LAB_UNIT, GBD_ROLES.BIOINFORMATICIAN),
    [hasGBDLabUnitRoleStrict],
  );

  const isGBDManager = useCallback(
    () => hasGBDLabUnitRoleStrict(GBD_LAB_UNIT, GBD_ROLES.MANAGER),
    [hasGBDLabUnitRoleStrict],
  );

  const isGBDPrincipalInvestigator = useCallback(
    () =>
      hasGBDLabUnitRoleStrict(GBD_LAB_UNIT, GBD_ROLES.PRINCIPAL_INVESTIGATOR),
    [hasGBDLabUnitRoleStrict],
  );

  const isGBDDataManager = useCallback(
    () => hasGBDLabUnitRoleStrict(GBD_LAB_UNIT, GBD_ROLES.DATA_MANAGER),
    [hasGBDLabUnitRoleStrict],
  );

  /**
   * Helper function to check if user has any of the specified GBD lab unit roles
   * Uses strict checking that does NOT bypass for Global Admins
   */
  const hasAnyGBDLabUnitRole = useCallback(
    (roles) => {
      return roles.some((role) => hasGBDLabUnitRoleStrict(GBD_LAB_UNIT, role));
    },
    [hasGBDLabUnitRoleStrict],
  );

  /**
   * Page-specific access checks per GBD permission matrix
   * Based on Section 11 permission matrix requirements
   */
  const canAccessRegistration = useCallback(() => {
    // Matrix: Lab Technicians (Yes), Bioinformaticians (View), Lab Manager (Full), Principal Investigator (View), Data Managers (No)
    const allowedRoles = [
      GBD_ROLES.LAB_TECHNICIAN,
      GBD_ROLES.BIOINFORMATICIAN,
      GBD_ROLES.MANAGER,
      GBD_ROLES.PRINCIPAL_INVESTIGATOR,
    ];
    return hasAnyGBDRole(allowedRoles);
  }, [hasAnyGBDRole, GBD_ROLES]);

  const canAccessExtraction = useCallback(() => {
    // Matrix: Lab Technicians (Yes), Bioinformaticians (No), Lab Manager (Full), Principal Investigator (View), Data Managers (No)
    const allowedRoles = [
      GBD_ROLES.LAB_TECHNICIAN,
      // Bioinformaticians excluded per matrix (No)
      GBD_ROLES.MANAGER,
      GBD_ROLES.PRINCIPAL_INVESTIGATOR,
    ];
    return hasAnyGBDRole(allowedRoles);
  }, [hasAnyGBDRole, GBD_ROLES]);

  const canAccessQC = useCallback(() => {
    // Matrix: Lab Technicians (Yes), Bioinformaticians (No), Lab Manager (Full), Principal Investigator (View), Data Managers (No)
    const allowedRoles = [
      GBD_ROLES.LAB_TECHNICIAN,
      // Bioinformaticians excluded per matrix (No)
      GBD_ROLES.MANAGER,
      GBD_ROLES.PRINCIPAL_INVESTIGATOR,
    ];
    return hasAnyGBDRole(allowedRoles);
  }, [hasAnyGBDRole, GBD_ROLES]);

  const canAccessPCR = useCallback(() => {
    // Matrix: Lab Technicians (Yes), Bioinformaticians (No), Lab Manager (Full), Principal Investigator (View), Data Managers (No)
    const allowedRoles = [
      GBD_ROLES.LAB_TECHNICIAN,
      // Bioinformaticians excluded per matrix (No)
      GBD_ROLES.MANAGER,
      GBD_ROLES.PRINCIPAL_INVESTIGATOR,
    ];
    return hasAnyGBDRole(allowedRoles);
  }, [hasAnyGBDRole, GBD_ROLES]);

  const canAccessLibraryPrep = useCallback(() => {
    // Matrix: Lab Technicians (Yes), Bioinformaticians (No), Lab Manager (Full), Principal Investigator (View), Data Managers (No)
    const allowedRoles = [
      GBD_ROLES.LAB_TECHNICIAN,
      // Bioinformaticians excluded per matrix (No)
      GBD_ROLES.MANAGER,
      GBD_ROLES.PRINCIPAL_INVESTIGATOR,
    ];
    return hasAnyGBDRole(allowedRoles);
  }, [hasAnyGBDRole, GBD_ROLES]);

  const canAccessSequencing = useCallback(() => {
    // Matrix: Lab Technicians (Yes), Bioinformaticians (View), Lab Manager (Full), Principal Investigator (View), Data Managers (No)
    const allowedRoles = [
      GBD_ROLES.LAB_TECHNICIAN,
      GBD_ROLES.BIOINFORMATICIAN,
      GBD_ROLES.MANAGER,
      GBD_ROLES.PRINCIPAL_INVESTIGATOR,
      // Data Managers excluded per matrix (No)
    ];
    return hasAnyGBDRole(allowedRoles);
  }, [hasAnyGBDRole, GBD_ROLES]);

  const canAccessBioinformatics = useCallback(() => {
    // Matrix: Lab Technicians (No), Bioinformaticians (Full), Lab Manager (View), Principal Investigator (View), Data Managers (View)
    const allowedRoles = [
      // Lab Technicians excluded per matrix (No)
      GBD_ROLES.BIOINFORMATICIAN,
      GBD_ROLES.MANAGER,
      GBD_ROLES.PRINCIPAL_INVESTIGATOR,
      GBD_ROLES.DATA_MANAGER,
    ];
    return hasAnyGBDRole(allowedRoles);
  }, [hasAnyGBDRole, GBD_ROLES]);

  const canAccessDataRelease = useCallback(() => {
    // Matrix: Lab Technicians (No), Bioinformaticians (No), Lab Manager (Approve), Principal Investigator (Final Approve), Data Managers (No)
    const allowedRoles = [
      // Lab Technicians excluded per matrix (No)
      // Bioinformaticians excluded per matrix (No)
      GBD_ROLES.MANAGER,
      GBD_ROLES.PRINCIPAL_INVESTIGATOR,
      // Data Managers excluded per matrix (No)
    ];
    return hasAnyGBDRole(allowedRoles);
  }, [hasAnyGBDRole, GBD_ROLES]);

  // Legacy access functions for backward compatibility
  const canAccessSampleReception = canAccessRegistration;
  const canAccessDNARNAExtraction = canAccessExtraction;
  const canAccessQualityQuantityAssessment = canAccessQC;
  const canAccessPCRAmplification = canAccessPCR;
  const canAccessLibraryPreparation = canAccessLibraryPrep;
  const canAccessBioinformaticsAnalysis = canAccessBioinformatics;

  return {
    // Constants
    GBD_ROLES,
    GBD_PAGES,

    // Core permission checks
    getPagePermissionLevel,
    canSaveData,
    canRegisterData,
    canPerformWork,
    canApproveData,
    canFinalApprove,
    canModify,
    hasFullControl,
    isReadOnly,

    // Role checks
    hasGBDRole,
    hasAnyGBDRole,
    getGBDRole,

    // GBD-specific role checks
    isGBDLabTechnician,
    isGBDBioinformatician,
    isGBDManager,
    isGBDPrincipalInvestigator,
    isGBDDataManager,
    hasAnyGBDLabUnitRole,

    // Page-specific access checks (matrix-compliant)
    canAccessRegistration,
    canAccessExtraction,
    canAccessQC,
    canAccessPCR,
    canAccessLibraryPrep,
    canAccessSequencing,
    canAccessBioinformatics,
    canAccessDataRelease,

    // Legacy page access checks (for backward compatibility)
    canAccessSampleReception,
    canAccessDNARNAExtraction,
    canAccessQualityQuantityAssessment,
    canAccessPCRAmplification,
    canAccessLibraryPreparation,
    canAccessBioinformaticsAnalysis,
  };
};

export default useGBDPermissions;
