import { useCallback, useMemo } from "react";
import { usePermissions } from "./usePermissions";

/**
 * Custom hook for VirologyLab (Virology Laboratory) workflow action-level permissions
 *
 * Provides utilities to check user permissions for specific actions on VirologyLab pages.
 * Works with the three-layer RBAC system:
 * - Layer 1: System roles (defines which roles exist)
 * - Layer 2: Page-level access (controls page visibility)
 * - Layer 3: Action-level permissions (controls what actions users can perform)
 *
 * Permission levels for VirologyLab workflows:
 * - VIEW: Read-only access
 * - UPDATE: Can edit and update data
 * - FULL: Complete control including save and submit
 *
 * @example
 * const { getPagePermissionLevel, canSaveData } = useVirologyLabPermissions();
 *
 * // Get user's permission level for a page
 * const permLevel = getPagePermissionLevel('Sample Reception & Registration');
 *
 * // Check if user can perform specific actions
 * if (canSaveData(permLevel)) {
 *   // Enable Save button
 * }
 */
export const useVirologyLabPermissions = () => {
  const {
    userSessionDetails,
    hasRole,
    hasAnyRole,
    hasLabUnitRole,
    isGlobalAdmin,
  } = usePermissions();

  const VIROLOGY_LAB_UNIT = "VirologyLab";

  /**
   * Custom hasLabUnitRole that DOES NOT bypass checks for Global Admins
   * This ensures VirologyLab permissions respect actual role assignments
   * @param {string} labUnit - Lab unit name or ID
   * @param {string} role - Role name to check
   * @returns {boolean}
   */
  const hasVirologyLabLabUnitRoleStrict = useCallback(
    (labUnit, role) => {
      // FOR VIROLOGYLAB PERMISSIONS: DO NOT bypass global admin checks
      // This ensures VirologyLab permissions work based on actual role assignments

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

  const VIROLOGY_LAB_ROLES = useMemo(
    () => ({
      LAB_TECHNICIAN: "VirologyLab Lab Technician",
      BIOINFORMATICIAN: "VirologyLab Bioinformatician",
      MANAGER: "VirologyLab Manager",
      PRINCIPAL_INVESTIGATOR: "VirologyLab PI",
      DATA_MANAGER: "VirologyLab Data Manager",
    }),
    [],
  );

  const VIROLOGY_LAB_PAGES = useMemo(
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
   * Get the user's permission level for a specific VirologyLab page
   *
   * Fetches from userSessionDetails.virologyLabPermissions which should contain
   * permission levels for each page, sourced from the database notebook_page_allowed_roles table.
   *
   * @param {string} pageName - Name of the VirologyLab page
   * @returns {string|null} - Permission level (VIEW, UPDATE, FULL, etc.) or null if no access
   */
  const getPagePermissionLevel = useCallback(
    (pageName) => {
      if (userSessionDetails?.virologyLabPermissions?.[pageName]) {
        return userSessionDetails.virologyLabPermissions[pageName];
      }

      const userVirologyLabRoles = Object.values(VIROLOGY_LAB_ROLES).filter((role) =>
        hasVirologyLabLabUnitRoleStrict(VIROLOGY_LAB_UNIT, role),
      );

      if (userVirologyLabRoles.length === 0) {
        return null;
      }

      const permissionMap = {
        [VIROLOGY_LAB_PAGES.SAMPLE_RECEPTION]: {
          [VIROLOGY_LAB_ROLES.LAB_TECHNICIAN]: "FULL",
          [VIROLOGY_LAB_ROLES.BIOINFORMATICIAN]: "VIEW",
          [VIROLOGY_LAB_ROLES.MANAGER]: "FULL",
          [VIROLOGY_LAB_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
          [VIROLOGY_LAB_ROLES.DATA_MANAGER]: null,
        },
        [VIROLOGY_LAB_PAGES.DNA_RNA_EXTRACTION]: {
          [VIROLOGY_LAB_ROLES.LAB_TECHNICIAN]: "FULL",
          [VIROLOGY_LAB_ROLES.BIOINFORMATICIAN]: null,
          [VIROLOGY_LAB_ROLES.MANAGER]: "FULL",
          [VIROLOGY_LAB_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
          [VIROLOGY_LAB_ROLES.DATA_MANAGER]: null,
        },
        [VIROLOGY_LAB_PAGES.QUALITY_QUANTITY_ASSESSMENT]: {
          [VIROLOGY_LAB_ROLES.LAB_TECHNICIAN]: "FULL",
          [VIROLOGY_LAB_ROLES.BIOINFORMATICIAN]: null,
          [VIROLOGY_LAB_ROLES.MANAGER]: "FULL",
          [VIROLOGY_LAB_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
          [VIROLOGY_LAB_ROLES.DATA_MANAGER]: null,
        },
        [VIROLOGY_LAB_PAGES.PCR_AMPLIFICATION]: {
          [VIROLOGY_LAB_ROLES.LAB_TECHNICIAN]: "FULL",
          [VIROLOGY_LAB_ROLES.BIOINFORMATICIAN]: null,
          [VIROLOGY_LAB_ROLES.MANAGER]: "FULL",
          [VIROLOGY_LAB_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
          [VIROLOGY_LAB_ROLES.DATA_MANAGER]: null,
        },
        [VIROLOGY_LAB_PAGES.GEL_ELECTROPHORESIS]: {
          [VIROLOGY_LAB_ROLES.LAB_TECHNICIAN]: "FULL",
          [VIROLOGY_LAB_ROLES.BIOINFORMATICIAN]: null,
          [VIROLOGY_LAB_ROLES.MANAGER]: "FULL",
          [VIROLOGY_LAB_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
          [VIROLOGY_LAB_ROLES.DATA_MANAGER]: null,
        },
        [VIROLOGY_LAB_PAGES.LIBRARY_PREPARATION]: {
          [VIROLOGY_LAB_ROLES.LAB_TECHNICIAN]: "FULL",
          [VIROLOGY_LAB_ROLES.BIOINFORMATICIAN]: null,
          [VIROLOGY_LAB_ROLES.MANAGER]: "FULL",
          [VIROLOGY_LAB_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
          [VIROLOGY_LAB_ROLES.DATA_MANAGER]: null,
        },
        [VIROLOGY_LAB_PAGES.BIOANALYZER_QC]: {
          [VIROLOGY_LAB_ROLES.LAB_TECHNICIAN]: "FULL",
          [VIROLOGY_LAB_ROLES.BIOINFORMATICIAN]: null,
          [VIROLOGY_LAB_ROLES.MANAGER]: "FULL",
          [VIROLOGY_LAB_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
          [VIROLOGY_LAB_ROLES.DATA_MANAGER]: null,
        },
        [VIROLOGY_LAB_PAGES.SEQUENCING]: {
          [VIROLOGY_LAB_ROLES.LAB_TECHNICIAN]: "FULL",
          [VIROLOGY_LAB_ROLES.BIOINFORMATICIAN]: "VIEW",
          [VIROLOGY_LAB_ROLES.MANAGER]: "FULL",
          [VIROLOGY_LAB_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
          [VIROLOGY_LAB_ROLES.DATA_MANAGER]: null,
        },
        [VIROLOGY_LAB_PAGES.BIOINFORMATICS_ANALYSIS]: {
          [VIROLOGY_LAB_ROLES.LAB_TECHNICIAN]: null,
          [VIROLOGY_LAB_ROLES.BIOINFORMATICIAN]: "FULL",
          [VIROLOGY_LAB_ROLES.MANAGER]: "VIEW",
          [VIROLOGY_LAB_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
          [VIROLOGY_LAB_ROLES.DATA_MANAGER]: "VIEW",
        },
        [VIROLOGY_LAB_PAGES.STORAGE_ENVIRONMENTAL_MONITORING]: {
          [VIROLOGY_LAB_ROLES.LAB_TECHNICIAN]: "FULL",
          [VIROLOGY_LAB_ROLES.BIOINFORMATICIAN]: null,
          [VIROLOGY_LAB_ROLES.MANAGER]: "FULL",
          [VIROLOGY_LAB_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
          [VIROLOGY_LAB_ROLES.DATA_MANAGER]: "VIEW",
        },
      };

      const pagePermissions = permissionMap[pageName] || {};

      const permissionHierarchy = ["VIEW", "UPDATE", "FULL"];

      const userPermissionLevels = userVirologyLabRoles
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
    [VIROLOGY_LAB_PAGES, VIROLOGY_LAB_ROLES, hasLabUnitRole, userSessionDetails],
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
   * Check if user has a specific VirologyLab role
   * @param {string} role - One of the VIROLOGY_LAB_ROLES values
   * @returns {boolean}
   */
  const hasVirologyLabRole = useCallback(
    (role) => {
      return hasVirologyLabLabUnitRoleStrict(VIROLOGY_LAB_UNIT, role);
    },
    [hasVirologyLabLabUnitRoleStrict],
  );

  /**
   * Check if user has any of the specified VirologyLab roles
   * @param {string[]} roles - Array of VIROLOGY_LAB_ROLES values
   * @returns {boolean}
   */
  const hasAnyVirologyLabRole = useCallback(
    (roles) => {
      if (!roles || !Array.isArray(roles)) {
        return false;
      }
      return roles.some((role) => hasVirologyLabRole(role));
    },
    [hasVirologyLabRole],
  );

  /**
   * Get the user's primary VirologyLab role (if any)
   * @returns {string|null}
   */
  const getVirologyLabRole = useCallback(() => {
    return (
      Object.values(VIROLOGY_LAB_ROLES).find((role) =>
        hasVirologyLabLabUnitRoleStrict(VIROLOGY_LAB_UNIT, role),
      ) || null
    );
  }, [VIROLOGY_LAB_ROLES, hasVirologyLabLabUnitRoleStrict]);

  /**
   * VirologyLab-specific role checks based on VirologyLab lab unit roles
   * These use strict checking that does NOT bypass for Global Admins
   */
  const isVirologyLabLabTechnician = useCallback(
    () => hasVirologyLabLabUnitRoleStrict(VIROLOGY_LAB_UNIT, VIROLOGY_LAB_ROLES.LAB_TECHNICIAN),
    [hasVirologyLabLabUnitRoleStrict],
  );

  const isVirologyLabBioinformatician = useCallback(
    () => hasVirologyLabLabUnitRoleStrict(VIROLOGY_LAB_UNIT, VIROLOGY_LAB_ROLES.BIOINFORMATICIAN),
    [hasVirologyLabLabUnitRoleStrict],
  );

  const isVirologyLabManager = useCallback(
    () => hasVirologyLabLabUnitRoleStrict(VIROLOGY_LAB_UNIT, VIROLOGY_LAB_ROLES.MANAGER),
    [hasVirologyLabLabUnitRoleStrict],
  );

  const isVirologyLabPrincipalInvestigator = useCallback(
    () =>
      hasVirologyLabLabUnitRoleStrict(VIROLOGY_LAB_UNIT, VIROLOGY_LAB_ROLES.PRINCIPAL_INVESTIGATOR),
    [hasVirologyLabLabUnitRoleStrict],
  );

  const isVirologyLabDataManager = useCallback(
    () => hasVirologyLabLabUnitRoleStrict(VIROLOGY_LAB_UNIT, VIROLOGY_LAB_ROLES.DATA_MANAGER),
    [hasVirologyLabLabUnitRoleStrict],
  );

  /**
   * Helper function to check if user has any of the specified VirologyLab lab unit roles
   * Uses strict checking that does NOT bypass for Global Admins
   */
  const hasAnyVirologyLabLabUnitRole = useCallback(
    (roles) => {
      return roles.some((role) => hasVirologyLabLabUnitRoleStrict(VIROLOGY_LAB_UNIT, role));
    },
    [hasVirologyLabLabUnitRoleStrict],
  );

  /**
   * Page-specific access checks per VirologyLab workflow stages
   */
  const canAccessSampleReception = useCallback(() => {
    const allowedRoles = [
      VIROLOGY_LAB_ROLES.LAB_TECHNICIAN,
      VIROLOGY_LAB_ROLES.BIOINFORMATICIAN,
      VIROLOGY_LAB_ROLES.MANAGER,
      VIROLOGY_LAB_ROLES.PRINCIPAL_INVESTIGATOR,
    ];
    return hasAnyVirologyLabRole(allowedRoles);
  }, [hasAnyVirologyLabRole, VIROLOGY_LAB_ROLES]);

  // Extraction: Lab Technicians=Yes, Bioinformaticians=No, Lab Manager=Full, Principal Investigator=View, Data Managers=No
  const canAccessDNARNAExtraction = useCallback(() => {
    const allowedRoles = [
      VIROLOGY_LAB_ROLES.LAB_TECHNICIAN,
      VIROLOGY_LAB_ROLES.MANAGER,
      VIROLOGY_LAB_ROLES.PRINCIPAL_INVESTIGATOR,
    ];
    return hasAnyVirologyLabRole(allowedRoles);
  }, [hasAnyVirologyLabRole, VIROLOGY_LAB_ROLES]);

  // QC: Lab Technicians=Yes, Bioinformaticians=No, Lab Manager=Full, Principal Investigator=View, Data Managers=No
  const canAccessQualityQuantityAssessment = useCallback(() => {
    const allowedRoles = [
      VIROLOGY_LAB_ROLES.LAB_TECHNICIAN,
      VIROLOGY_LAB_ROLES.MANAGER,
      VIROLOGY_LAB_ROLES.PRINCIPAL_INVESTIGATOR,
    ];
    return hasAnyVirologyLabRole(allowedRoles);
  }, [hasAnyVirologyLabRole, VIROLOGY_LAB_ROLES]);

  // PCR: Lab Technicians=Yes, Bioinformaticians=No, Lab Manager=Full, Principal Investigator=View, Data Managers=No
  const canAccessPCRAmplification = useCallback(() => {
    const allowedRoles = [
      VIROLOGY_LAB_ROLES.LAB_TECHNICIAN,
      VIROLOGY_LAB_ROLES.MANAGER,
      VIROLOGY_LAB_ROLES.PRINCIPAL_INVESTIGATOR,
    ];
    return hasAnyVirologyLabRole(allowedRoles);
  }, [hasAnyVirologyLabRole, VIROLOGY_LAB_ROLES]);

  // Gel Electrophoresis: Not in screenshot, defaulting to same as PCR
  const canAccessGelElectrophoresis = useCallback(() => {
    const allowedRoles = [
      VIROLOGY_LAB_ROLES.LAB_TECHNICIAN,
      VIROLOGY_LAB_ROLES.MANAGER,
      VIROLOGY_LAB_ROLES.PRINCIPAL_INVESTIGATOR,
    ];
    return hasAnyVirologyLabRole(allowedRoles);
  }, [hasAnyVirologyLabRole, VIROLOGY_LAB_ROLES]);

  // Library Prep: Lab Technicians=Yes, Bioinformaticians=No, Lab Manager=Full, Principal Investigator=View, Data Managers=No
  const canAccessLibraryPreparation = useCallback(() => {
    const allowedRoles = [
      VIROLOGY_LAB_ROLES.LAB_TECHNICIAN,
      VIROLOGY_LAB_ROLES.MANAGER,
      VIROLOGY_LAB_ROLES.PRINCIPAL_INVESTIGATOR,
    ];
    return hasAnyVirologyLabRole(allowedRoles);
  }, [hasAnyVirologyLabRole, VIROLOGY_LAB_ROLES]);

  // Bioanalyzer QC: Part of QC, same as Quality & Quantity Assessment
  const canAccessBioanalyzerQC = useCallback(() => {
    const allowedRoles = [
      VIROLOGY_LAB_ROLES.LAB_TECHNICIAN,
      VIROLOGY_LAB_ROLES.MANAGER,
      VIROLOGY_LAB_ROLES.PRINCIPAL_INVESTIGATOR,
    ];
    return hasAnyVirologyLabRole(allowedRoles);
  }, [hasAnyVirologyLabRole, VIROLOGY_LAB_ROLES]);

  // Sequencing: Lab Technicians=Yes, Bioinformaticians=View, Lab Manager=Full, Principal Investigator=View, Data Managers=No
  const canAccessSequencing = useCallback(() => {
    const allowedRoles = [
      VIROLOGY_LAB_ROLES.LAB_TECHNICIAN,
      VIROLOGY_LAB_ROLES.BIOINFORMATICIAN,
      VIROLOGY_LAB_ROLES.MANAGER,
      VIROLOGY_LAB_ROLES.PRINCIPAL_INVESTIGATOR,
    ];
    return hasAnyVirologyLabRole(allowedRoles);
  }, [hasAnyVirologyLabRole, VIROLOGY_LAB_ROLES]);

  // Bioinformatics: Lab Technicians=No, Bioinformaticians=Full, Lab Manager=View, Principal Investigator=View, Data Managers=View
  const canAccessBioinformaticsAnalysis = useCallback(() => {
    const allowedRoles = [
      VIROLOGY_LAB_ROLES.BIOINFORMATICIAN,
      VIROLOGY_LAB_ROLES.MANAGER,
      VIROLOGY_LAB_ROLES.PRINCIPAL_INVESTIGATOR,
      VIROLOGY_LAB_ROLES.DATA_MANAGER,
    ];
    return hasAnyVirologyLabRole(allowedRoles);
  }, [hasAnyVirologyLabLabUnitRole, VIROLOGY_LAB_ROLES]);

  // Storage & Environmental Monitoring: Lab Technicians=Full, Bioinformaticians=No, Lab Manager=Full, Principal Investigator=View, Data Managers=View
  const canAccessSampleStorage = useCallback(() => {
    const allowedRoles = [
      VIROLOGY_LAB_ROLES.LAB_TECHNICIAN,
      VIROLOGY_LAB_ROLES.MANAGER,
      VIROLOGY_LAB_ROLES.PRINCIPAL_INVESTIGATOR,
      VIROLOGY_LAB_ROLES.DATA_MANAGER,
    ];
    return hasAnyVirologyLabRole(allowedRoles);
  }, [hasAnyVirologyLabRole, VIROLOGY_LAB_ROLES]);

  return {
    VIROLOGY_LAB_ROLES,
    VIROLOGY_LAB_PAGES,

    getPagePermissionLevel,
    canSaveData,
    hasFullControl,
    isReadOnly,
    hasVirologyLabRole,
    hasAnyVirologyLabRole,
    getVirologyLabRole,

    // VirologyLab-specific role checks
    isVirologyLabLabTechnician,
    isVirologyLabBioinformatician,
    isVirologyLabManager,
    isVirologyLabPrincipalInvestigator,
    isVirologyLabDataManager,
    hasAnyVirologyLabLabUnitRole,

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

export default useVirologyLabPermissions;