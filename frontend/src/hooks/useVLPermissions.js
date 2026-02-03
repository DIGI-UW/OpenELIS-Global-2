import { useCallback, useMemo } from "react";
import { usePermissions } from "./usePermissions";

/**
 * Custom hook for VL (Genomics & Bioinformatics) workflow action-level permissions
 *
 * Provides utilities to check user permissions for specific actions on VL pages.
 * Works with the three-layer RBAC system:
 * - Layer 1: System roles (defines which roles exist)
 * - Layer 2: Page-level access (controls page visibility)
 * - Layer 3: Action-level permissions (controls what actions users can perform)
 *
 * Permission levels for VL workflows:
 * - VIEW: Read-only access
 * - UPDATE: Can edit and update data
 * - FULL: Complete control including save and submit
 *
 * @example
 * const { getPagePermissionLevel, canSaveData } = useVLPermissions();
 *
 * // Get user's permission level for a page
 * const permLevel = getPagePermissionLevel('Sample Reception & Registration');
 *
 * // Check if user can perform specific actions
 * if (canSaveData(permLevel)) {
 *   // Enable Save button
 * }
 */
export const useVLPermissions = () => {
  const {
    userSessionDetails,
    hasRole,
    hasAnyRole,
    hasLabUnitRole,
    isGlobalAdmin,
  } = usePermissions();

  const VL_LAB_UNIT = "VL";

  /**
   * Custom hasLabUnitRole that DOES NOT bypass checks for Global Admins
   * This ensures VL permissions respect actual role assignments
   * @param {string} labUnit - Lab unit name or ID
   * @param {string} role - Role name to check
   * @returns {boolean}
   */
  const hasVLLabUnitRoleStrict = useCallback(
    (labUnit, role) => {
      // FOR VL PERMISSIONS: DO NOT bypass global admin checks
      // This ensures VL permissions work based on actual role assignments

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

  const VL_ROLES = useMemo(
    () => ({
      LAB_TECHNICIAN: "VL Lab Technician",
      BIOINFORMATICIAN: "VL Bioinformatician",
      MANAGER: "VL Manager",
      PRINCIPAL_INVESTIGATOR: "VL Principal Investigator",
      DATA_MANAGER: "VL Data Manager",
    }),
    [],
  );

  const VL_PAGES = useMemo(
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
   * Get the user's permission level for a specific VL page
   *
   * Fetches from userSessionDetails.gbdPermissions which should contain
   * permission levels for each page, sourced from the database notebook_page_allowed_roles table.
   *
   * @param {string} pageName - Name of the VL page
   * @returns {string|null} - Permission level (VIEW, UPDATE, FULL, etc.) or null if no access
   */
  const getPagePermissionLevel = useCallback(
    (pageName) => {
      if (userSessionDetails?.gbdPermissions?.[pageName]) {
        return userSessionDetails.gbdPermissions[pageName];
      }

      const userVLRoles = Object.values(VL_ROLES).filter((role) =>
        hasVLLabUnitRoleStrict(VL_LAB_UNIT, role),
      );

      if (userVLRoles.length === 0) {
        return null;
      }

      const permissionMap = {
        [VL_PAGES.SAMPLE_RECEPTION]: {
          [VL_ROLES.LAB_TECHNICIAN]: "FULL",
          [VL_ROLES.BIOINFORMATICIAN]: "VIEW",
          [VL_ROLES.MANAGER]: "FULL",
          [VL_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
          [VL_ROLES.DATA_MANAGER]: null,
        },
        [VL_PAGES.DNA_RNA_EXTRACTION]: {
          [VL_ROLES.LAB_TECHNICIAN]: "FULL",
          [VL_ROLES.BIOINFORMATICIAN]: null,
          [VL_ROLES.MANAGER]: "FULL",
          [VL_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
          [VL_ROLES.DATA_MANAGER]: null,
        },
        [VL_PAGES.QUALITY_QUANTITY_ASSESSMENT]: {
          [VL_ROLES.LAB_TECHNICIAN]: "FULL",
          [VL_ROLES.BIOINFORMATICIAN]: null,
          [VL_ROLES.MANAGER]: "FULL",
          [VL_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
          [VL_ROLES.DATA_MANAGER]: null,
        },
        [VL_PAGES.PCR_AMPLIFICATION]: {
          [VL_ROLES.LAB_TECHNICIAN]: "FULL",
          [VL_ROLES.BIOINFORMATICIAN]: null,
          [VL_ROLES.MANAGER]: "FULL",
          [VL_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
          [VL_ROLES.DATA_MANAGER]: null,
        },
        [VL_PAGES.GEL_ELECTROPHORESIS]: {
          [VL_ROLES.LAB_TECHNICIAN]: "FULL",
          [VL_ROLES.BIOINFORMATICIAN]: null,
          [VL_ROLES.MANAGER]: "FULL",
          [VL_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
          [VL_ROLES.DATA_MANAGER]: null,
        },
        [VL_PAGES.LIBRARY_PREPARATION]: {
          [VL_ROLES.LAB_TECHNICIAN]: "FULL",
          [VL_ROLES.BIOINFORMATICIAN]: null,
          [VL_ROLES.MANAGER]: "FULL",
          [VL_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
          [VL_ROLES.DATA_MANAGER]: null,
        },
        [VL_PAGES.BIOANALYZER_QC]: {
          [VL_ROLES.LAB_TECHNICIAN]: "FULL",
          [VL_ROLES.BIOINFORMATICIAN]: null,
          [VL_ROLES.MANAGER]: "FULL",
          [VL_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
          [VL_ROLES.DATA_MANAGER]: null,
        },
        [VL_PAGES.SEQUENCING]: {
          [VL_ROLES.LAB_TECHNICIAN]: "FULL",
          [VL_ROLES.BIOINFORMATICIAN]: "VIEW",
          [VL_ROLES.MANAGER]: "FULL",
          [VL_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
          [VL_ROLES.DATA_MANAGER]: null,
        },
        [VL_PAGES.BIOINFORMATICS_ANALYSIS]: {
          [VL_ROLES.LAB_TECHNICIAN]: null,
          [VL_ROLES.BIOINFORMATICIAN]: "FULL",
          [VL_ROLES.MANAGER]: "VIEW",
          [VL_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
          [VL_ROLES.DATA_MANAGER]: "VIEW",
        },
        [VL_PAGES.STORAGE_ENVIRONMENTAL_MONITORING]: {
          [VL_ROLES.LAB_TECHNICIAN]: "FULL",
          [VL_ROLES.BIOINFORMATICIAN]: null,
          [VL_ROLES.MANAGER]: "FULL",
          [VL_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
          [VL_ROLES.DATA_MANAGER]: "VIEW",
        },
      };

      const pagePermissions = permissionMap[pageName] || {};

      const permissionHierarchy = ["VIEW", "UPDATE", "FULL"];

      const userPermissionLevels = userVLRoles
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
    [VL_PAGES, VL_ROLES, hasLabUnitRole, userSessionDetails],
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
   * Check if user has a specific VL role
   * @param {string} role - One of the VL_ROLES values
   * @returns {boolean}
   */
  const hasVLRole = useCallback(
    (role) => {
      return hasVLLabUnitRoleStrict(VL_LAB_UNIT, role);
    },
    [hasVLLabUnitRoleStrict],
  );

  /**
   * Check if user has any of the specified VL roles
   * @param {string[]} roles - Array of VL_ROLES values
   * @returns {boolean}
   */
  const hasAnyVLRole = useCallback(
    (roles) => {
      if (!roles || !Array.isArray(roles)) {
        return false;
      }
      return roles.some((role) => hasVLRole(role));
    },
    [hasVLRole],
  );

  /**
   * Get the user's primary VL role (if any)
   * @returns {string|null}
   */
  const getVLRole = useCallback(() => {
    return (
      Object.values(VL_ROLES).find((role) =>
        hasVLLabUnitRoleStrict(VL_LAB_UNIT, role),
      ) || null
    );
  }, [VL_ROLES, hasVLLabUnitRoleStrict]);

  /**
   * VL-specific role checks based on VL lab unit roles
   * These use strict checking that does NOT bypass for Global Admins
   */
  const isVLLabTechnician = useCallback(
    () => hasVLLabUnitRoleStrict(VL_LAB_UNIT, VL_ROLES.LAB_TECHNICIAN),
    [hasVLLabUnitRoleStrict],
  );

  const isVLBioinformatician = useCallback(
    () => hasVLLabUnitRoleStrict(VL_LAB_UNIT, VL_ROLES.BIOINFORMATICIAN),
    [hasVLLabUnitRoleStrict],
  );

  const isVLManager = useCallback(
    () => hasVLLabUnitRoleStrict(VL_LAB_UNIT, VL_ROLES.MANAGER),
    [hasVLLabUnitRoleStrict],
  );

  const isVLPrincipalInvestigator = useCallback(
    () =>
      hasVLLabUnitRoleStrict(VL_LAB_UNIT, VL_ROLES.PRINCIPAL_INVESTIGATOR),
    [hasVLLabUnitRoleStrict],
  );

  const isVLDataManager = useCallback(
    () => hasVLLabUnitRoleStrict(VL_LAB_UNIT, VL_ROLES.DATA_MANAGER),
    [hasVLLabUnitRoleStrict],
  );

  /**
   * Helper function to check if user has any of the specified VL lab unit roles
   * Uses strict checking that does NOT bypass for Global Admins
   */
  const hasAnyVLLabUnitRole = useCallback(
    (roles) => {
      return roles.some((role) => hasVLLabUnitRoleStrict(VL_LAB_UNIT, role));
    },
    [hasVLLabUnitRoleStrict],
  );

  /**
   * Page-specific access checks per VL workflow stages
   */
  const canAccessSampleReception = useCallback(() => {
    const allowedRoles = [
      VL_ROLES.LAB_TECHNICIAN,
      VL_ROLES.BIOINFORMATICIAN,
      VL_ROLES.MANAGER,
      VL_ROLES.PRINCIPAL_INVESTIGATOR,
    ];
    return hasAnyVLRole(allowedRoles);
  }, [hasAnyVLRole, VL_ROLES]);

  // Extraction: Lab Technicians=Yes, Bioinformaticians=No, Lab Manager=Full, Principal Investigator=View, Data Managers=No
  const canAccessDNARNAExtraction = useCallback(() => {
    const allowedRoles = [
      VL_ROLES.LAB_TECHNICIAN,
      VL_ROLES.MANAGER,
      VL_ROLES.PRINCIPAL_INVESTIGATOR,
    ];
    return hasAnyVLRole(allowedRoles);
  }, [hasAnyVLRole, VL_ROLES]);

  // QC: Lab Technicians=Yes, Bioinformaticians=No, Lab Manager=Full, Principal Investigator=View, Data Managers=No
  const canAccessQualityQuantityAssessment = useCallback(() => {
    const allowedRoles = [
      VL_ROLES.LAB_TECHNICIAN,
      VL_ROLES.MANAGER,
      VL_ROLES.PRINCIPAL_INVESTIGATOR,
    ];
    return hasAnyVLRole(allowedRoles);
  }, [hasAnyVLRole, VL_ROLES]);

  // PCR: Lab Technicians=Yes, Bioinformaticians=No, Lab Manager=Full, Principal Investigator=View, Data Managers=No
  const canAccessPCRAmplification = useCallback(() => {
    const allowedRoles = [
      VL_ROLES.LAB_TECHNICIAN,
      VL_ROLES.MANAGER,
      VL_ROLES.PRINCIPAL_INVESTIGATOR,
    ];
    return hasAnyVLRole(allowedRoles);
  }, [hasAnyVLRole, VL_ROLES]);

  // Gel Electrophoresis: Not in screenshot, defaulting to same as PCR
  const canAccessGelElectrophoresis = useCallback(() => {
    const allowedRoles = [
      VL_ROLES.LAB_TECHNICIAN,
      VL_ROLES.MANAGER,
      VL_ROLES.PRINCIPAL_INVESTIGATOR,
    ];
    return hasAnyVLRole(allowedRoles);
  }, [hasAnyVLRole, VL_ROLES]);

  // Library Prep: Lab Technicians=Yes, Bioinformaticians=No, Lab Manager=Full, Principal Investigator=View, Data Managers=No
  const canAccessLibraryPreparation = useCallback(() => {
    const allowedRoles = [
      VL_ROLES.LAB_TECHNICIAN,
      VL_ROLES.MANAGER,
      VL_ROLES.PRINCIPAL_INVESTIGATOR,
    ];
    return hasAnyVLRole(allowedRoles);
  }, [hasAnyVLRole, VL_ROLES]);

  // Bioanalyzer QC: Part of QC, same as Quality & Quantity Assessment
  const canAccessBioanalyzerQC = useCallback(() => {
    const allowedRoles = [
      VL_ROLES.LAB_TECHNICIAN,
      VL_ROLES.MANAGER,
      VL_ROLES.PRINCIPAL_INVESTIGATOR,
    ];
    return hasAnyVLRole(allowedRoles);
  }, [hasAnyVLRole, VL_ROLES]);

  // Sequencing: Lab Technicians=Yes, Bioinformaticians=View, Lab Manager=Full, Principal Investigator=View, Data Managers=No
  const canAccessSequencing = useCallback(() => {
    const allowedRoles = [
      VL_ROLES.LAB_TECHNICIAN,
      VL_ROLES.BIOINFORMATICIAN,
      VL_ROLES.MANAGER,
      VL_ROLES.PRINCIPAL_INVESTIGATOR,
    ];
    return hasAnyVLRole(allowedRoles);
  }, [hasAnyVLRole, VL_ROLES]);

  // Bioinformatics: Lab Technicians=No, Bioinformaticians=Full, Lab Manager=View, Principal Investigator=View, Data Managers=View
  const canAccessBioinformaticsAnalysis = useCallback(() => {
    const allowedRoles = [
      VL_ROLES.BIOINFORMATICIAN,
      VL_ROLES.MANAGER,
      VL_ROLES.PRINCIPAL_INVESTIGATOR,
      VL_ROLES.DATA_MANAGER,
    ];
    return hasAnyVLRole(allowedRoles);
  }, [hasAnyVLLabUnitRole, VL_ROLES]);

  // Storage & Environmental Monitoring: Lab Technicians=Full, Bioinformaticians=No, Lab Manager=Full, Principal Investigator=View, Data Managers=View
  const canAccessSampleStorage = useCallback(() => {
    const allowedRoles = [
      VL_ROLES.LAB_TECHNICIAN,
      VL_ROLES.MANAGER,
      VL_ROLES.PRINCIPAL_INVESTIGATOR,
      VL_ROLES.DATA_MANAGER,
    ];
    return hasAnyVLRole(allowedRoles);
  }, [hasAnyVLRole, VL_ROLES]);

  return {
    VL_ROLES,
    VL_PAGES,

    getPagePermissionLevel,
    canSaveData,
    hasFullControl,
    isReadOnly,
    hasVLRole,
    hasAnyVLRole,
    getVLRole,

    // VL-specific role checks
    isVLLabTechnician,
    isVLBioinformatician,
    isVLManager,
    isVLPrincipalInvestigator,
    isVLDataManager,
    hasAnyVLLabUnitRole,

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

export default useVLPermissions;
