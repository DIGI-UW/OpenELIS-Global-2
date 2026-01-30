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
      SAMPLE_RECEPTION: "Sample Reception & Registration",
      DNA_RNA_EXTRACTION: "DNA/RNA Extraction",
      QUALITY_QUANTITY_ASSESSMENT: "Quality & Quantity Assessment",
      PCR_AMPLIFICATION: "PCR Amplification",
      GEL_ELECTROPHORESIS: "Gel Electrophoresis",
      LIBRARY_PREPARATION: "Library Preparation",
      BIOANALYZER_QC: "Bioanalyzer QC",
      SEQUENCING: "Sequencing",
      BIOINFORMATICS_ANALYSIS: "Bioinformatics Analysis & Data Submission",
      STORAGE_ENVIRONMENTAL_MONITORING: "Storage & Environmental Monitoring",
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

      const permissionMap = {
        [GBD_PAGES.SAMPLE_RECEPTION]: {
          [GBD_ROLES.LAB_TECHNICIAN]: "FULL",
          [GBD_ROLES.BIOINFORMATICIAN]: "VIEW",
          [GBD_ROLES.MANAGER]: "FULL",
          [GBD_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
          [GBD_ROLES.DATA_MANAGER]: null,
        },
        [GBD_PAGES.DNA_RNA_EXTRACTION]: {
          [GBD_ROLES.LAB_TECHNICIAN]: "FULL",
          [GBD_ROLES.BIOINFORMATICIAN]: null,
          [GBD_ROLES.MANAGER]: "FULL",
          [GBD_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
          [GBD_ROLES.DATA_MANAGER]: null,
        },
        [GBD_PAGES.QUALITY_QUANTITY_ASSESSMENT]: {
          [GBD_ROLES.LAB_TECHNICIAN]: "FULL",
          [GBD_ROLES.BIOINFORMATICIAN]: null,
          [GBD_ROLES.MANAGER]: "FULL",
          [GBD_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
          [GBD_ROLES.DATA_MANAGER]: null,
        },
        [GBD_PAGES.PCR_AMPLIFICATION]: {
          [GBD_ROLES.LAB_TECHNICIAN]: "FULL",
          [GBD_ROLES.BIOINFORMATICIAN]: null,
          [GBD_ROLES.MANAGER]: "FULL",
          [GBD_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
          [GBD_ROLES.DATA_MANAGER]: null,
        },
        [GBD_PAGES.GEL_ELECTROPHORESIS]: {
          [GBD_ROLES.LAB_TECHNICIAN]: "FULL",
          [GBD_ROLES.BIOINFORMATICIAN]: null,
          [GBD_ROLES.MANAGER]: "FULL",
          [GBD_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
          [GBD_ROLES.DATA_MANAGER]: null,
        },
        [GBD_PAGES.LIBRARY_PREPARATION]: {
          [GBD_ROLES.LAB_TECHNICIAN]: "FULL",
          [GBD_ROLES.BIOINFORMATICIAN]: null,
          [GBD_ROLES.MANAGER]: "FULL",
          [GBD_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
          [GBD_ROLES.DATA_MANAGER]: null,
        },
        [GBD_PAGES.BIOANALYZER_QC]: {
          [GBD_ROLES.LAB_TECHNICIAN]: "FULL",
          [GBD_ROLES.BIOINFORMATICIAN]: null,
          [GBD_ROLES.MANAGER]: "FULL",
          [GBD_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
          [GBD_ROLES.DATA_MANAGER]: null,
        },
        [GBD_PAGES.SEQUENCING]: {
          [GBD_ROLES.LAB_TECHNICIAN]: "FULL",
          [GBD_ROLES.BIOINFORMATICIAN]: "VIEW",
          [GBD_ROLES.MANAGER]: "FULL",
          [GBD_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
          [GBD_ROLES.DATA_MANAGER]: null,
        },
        [GBD_PAGES.BIOINFORMATICS_ANALYSIS]: {
          [GBD_ROLES.LAB_TECHNICIAN]: null,
          [GBD_ROLES.BIOINFORMATICIAN]: "FULL",
          [GBD_ROLES.MANAGER]: "VIEW",
          [GBD_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
          [GBD_ROLES.DATA_MANAGER]: "VIEW",
        },
        [GBD_PAGES.STORAGE_ENVIRONMENTAL_MONITORING]: {
          [GBD_ROLES.LAB_TECHNICIAN]: "FULL",
          [GBD_ROLES.BIOINFORMATICIAN]: null,
          [GBD_ROLES.MANAGER]: "FULL",
          [GBD_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
          [GBD_ROLES.DATA_MANAGER]: "VIEW",
        },
      };

      const pagePermissions = permissionMap[pageName] || {};

      const permissionHierarchy = ["VIEW", "UPDATE", "FULL"];

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
   * Allows: UPDATE, FULL
   * @param {string|null} permissionLevel
   * @returns {boolean}
   */
  const canSaveData = useCallback((permissionLevel) => {
    return permissionLevel && ["UPDATE", "FULL"].includes(permissionLevel);
  }, []);

  /**
   * Check if a permission level allows registering new data
   * Allows: FULL only
   * @param {string|null} permissionLevel
   * @returns {boolean}
   */
  const canRegisterData = useCallback((permissionLevel) => {
    return permissionLevel === "FULL";
  }, []);

  /**
   * Check if a permission level allows updating existing data
   * Allows: UPDATE, FULL
   * @param {string|null} permissionLevel
   * @returns {boolean}
   */
  const canUpdateData = useCallback((permissionLevel) => {
    return permissionLevel && ["UPDATE", "FULL"].includes(permissionLevel);
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
   * True for: VIEW
   * @param {string|null} permissionLevel
   * @returns {boolean}
   */
  const isReadOnly = useCallback((permissionLevel) => {
    return permissionLevel === "VIEW";
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
   * Page-specific access checks per GBD workflow stages
   */
  const canAccessSampleReception = useCallback(() => {
    const allowedRoles = [
      GBD_ROLES.LAB_TECHNICIAN,
      GBD_ROLES.BIOINFORMATICIAN,
      GBD_ROLES.MANAGER,
      GBD_ROLES.PRINCIPAL_INVESTIGATOR,
    ];
    return hasAnyGBDRole(allowedRoles);
  }, [hasAnyGBDRole, GBD_ROLES]);

  // Extraction: Lab Technicians=Yes, Bioinformaticians=No, Lab Manager=Full, Principal Investigator=View, Data Managers=No
  const canAccessDNARNAExtraction = useCallback(() => {
    const allowedRoles = [
      GBD_ROLES.LAB_TECHNICIAN,
      GBD_ROLES.MANAGER,
      GBD_ROLES.PRINCIPAL_INVESTIGATOR,
    ];
    return hasAnyGBDRole(allowedRoles);
  }, [hasAnyGBDRole, GBD_ROLES]);

  // QC: Lab Technicians=Yes, Bioinformaticians=No, Lab Manager=Full, Principal Investigator=View, Data Managers=No
  const canAccessQualityQuantityAssessment = useCallback(() => {
    const allowedRoles = [
      GBD_ROLES.LAB_TECHNICIAN,
      GBD_ROLES.MANAGER,
      GBD_ROLES.PRINCIPAL_INVESTIGATOR,
    ];
    return hasAnyGBDRole(allowedRoles);
  }, [hasAnyGBDRole, GBD_ROLES]);

  // PCR: Lab Technicians=Yes, Bioinformaticians=No, Lab Manager=Full, Principal Investigator=View, Data Managers=No
  const canAccessPCRAmplification = useCallback(() => {
    const allowedRoles = [
      GBD_ROLES.LAB_TECHNICIAN,
      GBD_ROLES.MANAGER,
      GBD_ROLES.PRINCIPAL_INVESTIGATOR,
    ];
    return hasAnyGBDRole(allowedRoles);
  }, [hasAnyGBDRole, GBD_ROLES]);

  // Gel Electrophoresis: Not in screenshot, defaulting to same as PCR
  const canAccessGelElectrophoresis = useCallback(() => {
    const allowedRoles = [
      GBD_ROLES.LAB_TECHNICIAN,
      GBD_ROLES.MANAGER,
      GBD_ROLES.PRINCIPAL_INVESTIGATOR,
    ];
    return hasAnyGBDRole(allowedRoles);
  }, [hasAnyGBDRole, GBD_ROLES]);

  // Library Prep: Lab Technicians=Yes, Bioinformaticians=No, Lab Manager=Full, Principal Investigator=View, Data Managers=No
  const canAccessLibraryPreparation = useCallback(() => {
    const allowedRoles = [
      GBD_ROLES.LAB_TECHNICIAN,
      GBD_ROLES.MANAGER,
      GBD_ROLES.PRINCIPAL_INVESTIGATOR,
    ];
    return hasAnyGBDRole(allowedRoles);
  }, [hasAnyGBDRole, GBD_ROLES]);

  // Bioanalyzer QC: Part of QC, same as Quality & Quantity Assessment
  const canAccessBioanalyzerQC = useCallback(() => {
    const allowedRoles = [
      GBD_ROLES.LAB_TECHNICIAN,
      GBD_ROLES.MANAGER,
      GBD_ROLES.PRINCIPAL_INVESTIGATOR,
    ];
    return hasAnyGBDRole(allowedRoles);
  }, [hasAnyGBDRole, GBD_ROLES]);

  // Sequencing: Lab Technicians=Yes, Bioinformaticians=View, Lab Manager=Full, Principal Investigator=View, Data Managers=No
  const canAccessSequencing = useCallback(() => {
    const allowedRoles = [
      GBD_ROLES.LAB_TECHNICIAN,
      GBD_ROLES.BIOINFORMATICIAN,
      GBD_ROLES.MANAGER,
      GBD_ROLES.PRINCIPAL_INVESTIGATOR,
    ];
    return hasAnyGBDRole(allowedRoles);
  }, [hasAnyGBDRole, GBD_ROLES]);

  // Bioinformatics: Lab Technicians=No, Bioinformaticians=Full, Lab Manager=View, Principal Investigator=View, Data Managers=View
  const canAccessBioinformaticsAnalysis = useCallback(() => {
    const allowedRoles = [
      GBD_ROLES.BIOINFORMATICIAN,
      GBD_ROLES.MANAGER,
      GBD_ROLES.PRINCIPAL_INVESTIGATOR,
      GBD_ROLES.DATA_MANAGER,
    ];
    return hasAnyGBDRole(allowedRoles);
  }, [hasAnyGBDLabUnitRole, GBD_ROLES]);

  // Storage & Environmental Monitoring: Lab Technicians=Full, Bioinformaticians=No, Lab Manager=Full, Principal Investigator=View, Data Managers=View
  const canAccessSampleStorage = useCallback(() => {
    const allowedRoles = [
      GBD_ROLES.LAB_TECHNICIAN,
      GBD_ROLES.MANAGER,
      GBD_ROLES.PRINCIPAL_INVESTIGATOR,
      GBD_ROLES.DATA_MANAGER,
    ];
    return hasAnyGBDRole(allowedRoles);
  }, [hasAnyGBDRole, GBD_ROLES]);

  return {
    GBD_ROLES,
    GBD_PAGES,

    getPagePermissionLevel,
    canSaveData,
    hasFullControl,
    isReadOnly,
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

    // Page-specific access checks
    canAccessSampleReception,
    canAccessDNARNAExtraction,
    canAccessQualityQuantityAssessment,
    canAccessPCRAmplification,
    canAccessGelElectrophoresis,
    canAccessLibraryPreparation,
    canAccessBioanalyzerQC,
    canAccessSequencing,
    canAccessBioinformaticsAnalysis,
    canAccessSampleStorage,
  };
};

export default useGBDPermissions;
