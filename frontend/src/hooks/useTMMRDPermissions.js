import { usePermissions } from "./usePermissions";

/**
 * TMMRD (Traditional Medicine & Modern Research Development) Permissions Hook
 *
 * Based on permission matrix from Section 11 requirements
 * Implements three-layer RBAC system per TMMRD SRS Section 11:
 *
 * Layer 1: System Roles (Lab Technician, Researcher, Pharmacognosist, Lab Manager, Principal Investigator)
 * Layer 2: Page-level Access (which roles can access each page)
 * Layer 3: Action-level Permissions (what actions roles can perform)
 *
 * Permission Levels from Matrix:
 * - VIEW: Read-only access
 * - YES: Can perform work and update data
 * - APPROVE: Can approve operations
 * - FULL: Complete control including save and submit
 * - FINAL_APPROVE: Can provide final approval
 * - NO: No access (excluded from page/function)
 */
export const useTMMRDPermissions = () => {
  const { userSessionDetails, hasRole, hasAnyRole, hasLabUnitRole, isGlobalAdmin } =
    usePermissions();

  // Traditional Medicine lab unit name
  const TMMRD_LAB_UNIT = "TraditionalMedicine";

  // Traditional Medicine Roles
  const TMMRD_ROLES = {
    LAB_TECHNICIAN: "TMMRD Lab Technician",
    RESEARCHER: "TMMRD Researcher",
    PHARMACOGNOSIST: "TMMRD Pharmacognosist",
    LAB_MANAGER: "TMMRD Lab Manager",
    PRINCIPAL_INVESTIGATOR: "TMMRD Principal Investigator",
  };

  // Traditional Medicine Pages
  const TMMRD_PAGES = {
    REGISTRATION: "Registration",
    AUTHENTICATION: "Authentication",
    PROCESSING: "Processing",
    EXTRACTION: "Extraction",
    ANALYSIS: "Analysis",
    PRODUCT_DEVELOPMENT: "Product Development",
    APPROVAL: "Approval",
    HERBARIUM: "Herbarium",
  };

  // Permission hierarchy (lowest to highest)
  const permissionHierarchy = [
    "VIEW", // Read-only
    "YES", // Can perform work and update data
    "APPROVE", // Can approve operations
    "FINAL_APPROVE", // Can provide final approval
    "FULL", // Complete control
  ];

  /**
   * Get the user's permission level for a specific page based on the permission matrix
   */
  const getPagePermissionLevel = (pageName) => {
    // Global admins get full access
    if (isGlobalAdmin()) {
      return "FULL";
    }

    if (!userSessionDetails?.userLabRolesMap) {
      return null;
    }

    const userTMMRDRoles = Object.values(TMMRD_ROLES).filter((role) => {
      // Check "AllLabUnits" for global lab access
      const allLabUnitsRoles = userSessionDetails.userLabRolesMap["AllLabUnits"] || [];
      if (allLabUnitsRoles.includes(role)) {
        return true;
      }
      // Check specific lab unit
      const labUnitRoles = userSessionDetails.userLabRolesMap[TMMRD_LAB_UNIT] || [];
      return labUnitRoles.includes(role);
    });

    if (userTMMRDRoles.length === 0) {
      return null;
    }

    // Permission map based on the permission matrix from Section 11
    const permissionMap = {
      [TMMRD_PAGES.REGISTRATION]: {
        // Matrix: Lab Technicians (Yes), Researchers (Yes), Pharmacognosists (View), Lab Manager (Full), Principal Investigator (View)
        [TMMRD_ROLES.LAB_TECHNICIAN]: "YES",
        [TMMRD_ROLES.RESEARCHER]: "YES",
        [TMMRD_ROLES.PHARMACOGNOSIST]: "VIEW",
        [TMMRD_ROLES.LAB_MANAGER]: "FULL",
        [TMMRD_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
      },
      [TMMRD_PAGES.AUTHENTICATION]: {
        // Matrix: Lab Technicians (No), Researchers (No), Pharmacognosists (Yes), Lab Manager (Full), Principal Investigator (View)
        // Lab Technicians and Researchers excluded per matrix (No)
        [TMMRD_ROLES.PHARMACOGNOSIST]: "YES",
        [TMMRD_ROLES.LAB_MANAGER]: "FULL",
        [TMMRD_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
      },
      [TMMRD_PAGES.PROCESSING]: {
        // Matrix: Lab Technicians (Yes), Researchers (Yes), Pharmacognosists (View), Lab Manager (Full), Principal Investigator (View)
        [TMMRD_ROLES.LAB_TECHNICIAN]: "YES",
        [TMMRD_ROLES.RESEARCHER]: "YES",
        [TMMRD_ROLES.PHARMACOGNOSIST]: "VIEW",
        [TMMRD_ROLES.LAB_MANAGER]: "FULL",
        [TMMRD_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
      },
      [TMMRD_PAGES.EXTRACTION]: {
        // Matrix: Lab Technicians (Yes), Researchers (Yes), Pharmacognosists (View), Lab Manager (Full), Principal Investigator (View)
        [TMMRD_ROLES.LAB_TECHNICIAN]: "YES",
        [TMMRD_ROLES.RESEARCHER]: "YES",
        [TMMRD_ROLES.PHARMACOGNOSIST]: "VIEW",
        [TMMRD_ROLES.LAB_MANAGER]: "FULL",
        [TMMRD_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
      },
      [TMMRD_PAGES.ANALYSIS]: {
        // Matrix: Lab Technicians (Yes), Researchers (Yes), Pharmacognosists (View), Lab Manager (Full), Principal Investigator (View)
        [TMMRD_ROLES.LAB_TECHNICIAN]: "YES",
        [TMMRD_ROLES.RESEARCHER]: "YES",
        [TMMRD_ROLES.PHARMACOGNOSIST]: "VIEW",
        [TMMRD_ROLES.LAB_MANAGER]: "FULL",
        [TMMRD_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
      },
      [TMMRD_PAGES.PRODUCT_DEVELOPMENT]: {
        // Matrix: Lab Technicians (Yes), Researchers (Yes), Pharmacognosists (Yes), Lab Manager (Full), Principal Investigator (View)
        [TMMRD_ROLES.LAB_TECHNICIAN]: "YES",
        [TMMRD_ROLES.RESEARCHER]: "YES",
        [TMMRD_ROLES.PHARMACOGNOSIST]: "YES",
        [TMMRD_ROLES.LAB_MANAGER]: "FULL",
        [TMMRD_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
      },
      [TMMRD_PAGES.APPROVAL]: {
        // Matrix: Lab Technicians (No), Researchers (No), Pharmacognosists (Approve), Lab Manager (Final Approve), Principal Investigator (Final Approve)
        // Lab Technicians and Researchers excluded per matrix (No)
        [TMMRD_ROLES.PHARMACOGNOSIST]: "APPROVE",
        [TMMRD_ROLES.LAB_MANAGER]: "FINAL_APPROVE",
        [TMMRD_ROLES.PRINCIPAL_INVESTIGATOR]: "FINAL_APPROVE",
      },
      [TMMRD_PAGES.HERBARIUM]: {
        // Matrix: Lab Technicians (View), Researchers (View), Pharmacognosists (Full), Lab Manager (Full), Principal Investigator (View)
        [TMMRD_ROLES.LAB_TECHNICIAN]: "VIEW",
        [TMMRD_ROLES.RESEARCHER]: "VIEW",
        [TMMRD_ROLES.PHARMACOGNOSIST]: "FULL",
        [TMMRD_ROLES.LAB_MANAGER]: "FULL",
        [TMMRD_ROLES.PRINCIPAL_INVESTIGATOR]: "VIEW",
      },
    };

    const pagePermissions = permissionMap[pageName] || {};

    // Get all permission levels for this page from all user roles
    const userPermissionLevels = userTMMRDRoles
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
  };

  /**
   * Check if user can save/update data
   * Allows: YES, APPROVE, FINAL_APPROVE, FULL
   */
  const canSaveData = (permissionLevel) => {
    return ["YES", "APPROVE", "FINAL_APPROVE", "FULL"].includes(
      permissionLevel?.toUpperCase(),
    );
  };

  /**
   * Check if user can register/import samples
   * Allows: YES, APPROVE, FINAL_APPROVE, FULL
   */
  const canRegisterData = (permissionLevel) => {
    return ["YES", "APPROVE", "FINAL_APPROVE", "FULL"].includes(
      permissionLevel?.toUpperCase(),
    );
  };

  /**
   * Check if user can approve operations
   * Allows: APPROVE, FINAL_APPROVE, FULL
   */
  const canApproveData = (permissionLevel) => {
    return ["APPROVE", "FINAL_APPROVE", "FULL"].includes(permissionLevel?.toUpperCase());
  };

  /**
   * Check if user can provide final approval
   * Allows: FINAL_APPROVE, FULL
   */
  const canFinalApprove = (permissionLevel) => {
    return ["FINAL_APPROVE", "FULL"].includes(permissionLevel?.toUpperCase());
  };

  /**
   * Check if user has full control
   * Allows: FULL only
   */
  const hasFullControl = (permissionLevel) => {
    return permissionLevel?.toUpperCase() === "FULL";
  };

  /**
   * Check if user has read-only access
   * True for: VIEW only
   */
  const isReadOnly = (permissionLevel) => {
    return permissionLevel?.toUpperCase() === "VIEW";
  };

  /**
   * Check if user can modify data
   * Allows: YES, APPROVE, FINAL_APPROVE, FULL
   */
  const canModify = (permissionLevel) => {
    return ["YES", "APPROVE", "FINAL_APPROVE", "FULL"].includes(
      permissionLevel?.toUpperCase(),
    );
  };

  /**
   * Check if user can perform work (YES permission)
   * Allows: YES, APPROVE, FINAL_APPROVE, FULL
   */
  const canPerformWork = (permissionLevel) => {
    return ["YES", "APPROVE", "FINAL_APPROVE", "FULL"].includes(
      permissionLevel?.toUpperCase(),
    );
  };

  /**
   * Check if user can authenticate samples
   * Allows: YES, APPROVE, FINAL_APPROVE, FULL (for Authentication page specifically)
   */
  const canAuthenticate = (permissionLevel) => {
    return ["YES", "APPROVE", "FINAL_APPROVE", "FULL"].includes(
      permissionLevel?.toUpperCase(),
    );
  };

  // Note: hasRole and hasAnyRole are now provided by usePermissions() hook

  /**
   * Get permission level hierarchy rank (higher number = more permissions)
   */
  const getPermissionRank = (permissionLevel) => {
    const index = permissionHierarchy.indexOf(permissionLevel?.toUpperCase());
    return index >= 0 ? index : 0;
  };

  /**
   * Check if user has at least the specified permission level
   */
  const hasAtLeastPermission = (currentLevel, requiredLevel) => {
    return getPermissionRank(currentLevel) >= getPermissionRank(requiredLevel);
  };

  /**
   * TMMRD-specific role checks based on SRS Section 11
   * Using lab unit roles for Traditional Medicine with TMMRD prefix
   */
  const isTMMRDLabTechnician = () =>
    hasLabUnitRole(TMMRD_LAB_UNIT, TMMRD_ROLES.LAB_TECHNICIAN);
  const isTMMRDResearcher = () =>
    hasLabUnitRole(TMMRD_LAB_UNIT, TMMRD_ROLES.RESEARCHER);
  const isTMMRDPharmacognosist = () =>
    hasLabUnitRole(TMMRD_LAB_UNIT, TMMRD_ROLES.PHARMACOGNOSIST);
  const isTMMRDLabManager = () =>
    hasLabUnitRole(TMMRD_LAB_UNIT, TMMRD_ROLES.LAB_MANAGER);
  const isTMMRDPrincipalInvestigator = () =>
    hasLabUnitRole(TMMRD_LAB_UNIT, TMMRD_ROLES.PRINCIPAL_INVESTIGATOR);

  /**
   * Helper function to check if user has any of the specified TMMRD lab unit roles
   */
  const hasAnyTMMRDRole = (roles) => {
    return roles.some((role) => hasLabUnitRole(TMMRD_LAB_UNIT, role));
  };

  /**
   * Page-specific access checks per TMMRD permission matrix
   * Based on Section 11 permission matrix requirements
   */
  const canAccessRegistration = () => {
    // Matrix: Lab Technicians (Yes), Researchers (Yes), Pharmacognosists (View), Lab Manager (Full), Principal Investigator (View)
    const allowedRoles = [
      TMMRD_ROLES.LAB_TECHNICIAN,
      TMMRD_ROLES.RESEARCHER,
      TMMRD_ROLES.PHARMACOGNOSIST,
      TMMRD_ROLES.LAB_MANAGER,
      TMMRD_ROLES.PRINCIPAL_INVESTIGATOR,
    ];
    return hasAnyTMMRDRole(allowedRoles);
  };

  const canAccessAuthentication = () => {
    // Matrix: Lab Technicians (No), Researchers (No), Pharmacognosists (Yes), Lab Manager (Full), Principal Investigator (View)
    const allowedRoles = [
      // Lab Technicians and Researchers excluded per matrix (No)
      TMMRD_ROLES.PHARMACOGNOSIST,
      TMMRD_ROLES.LAB_MANAGER,
      TMMRD_ROLES.PRINCIPAL_INVESTIGATOR,
    ];
    return hasAnyTMMRDRole(allowedRoles);
  };

  const canAccessProcessing = () => {
    // Matrix: Lab Technicians (Yes), Researchers (Yes), Pharmacognosists (View), Lab Manager (Full), Principal Investigator (View)
    const allowedRoles = [
      TMMRD_ROLES.LAB_TECHNICIAN,
      TMMRD_ROLES.RESEARCHER,
      TMMRD_ROLES.PHARMACOGNOSIST,
      TMMRD_ROLES.LAB_MANAGER,
      TMMRD_ROLES.PRINCIPAL_INVESTIGATOR,
    ];
    return hasAnyTMMRDRole(allowedRoles);
  };

  const canAccessExtraction = () => {
    // Matrix: Lab Technicians (Yes), Researchers (Yes), Pharmacognosists (View), Lab Manager (Full), Principal Investigator (View)
    const allowedRoles = [
      TMMRD_ROLES.LAB_TECHNICIAN,
      TMMRD_ROLES.RESEARCHER,
      TMMRD_ROLES.PHARMACOGNOSIST,
      TMMRD_ROLES.LAB_MANAGER,
      TMMRD_ROLES.PRINCIPAL_INVESTIGATOR,
    ];
    return hasAnyTMMRDRole(allowedRoles);
  };

  const canAccessAnalysis = () => {
    // Matrix: Lab Technicians (Yes), Researchers (Yes), Pharmacognosists (View), Lab Manager (Full), Principal Investigator (View)
    const allowedRoles = [
      TMMRD_ROLES.LAB_TECHNICIAN,
      TMMRD_ROLES.RESEARCHER,
      TMMRD_ROLES.PHARMACOGNOSIST,
      TMMRD_ROLES.LAB_MANAGER,
      TMMRD_ROLES.PRINCIPAL_INVESTIGATOR,
    ];
    return hasAnyTMMRDRole(allowedRoles);
  };

  const canAccessProductDevelopment = () => {
    // Matrix: Lab Technicians (Yes), Researchers (Yes), Pharmacognosists (Yes), Lab Manager (Full), Principal Investigator (View)
    const allowedRoles = [
      TMMRD_ROLES.LAB_TECHNICIAN,
      TMMRD_ROLES.RESEARCHER,
      TMMRD_ROLES.PHARMACOGNOSIST,
      TMMRD_ROLES.LAB_MANAGER,
      TMMRD_ROLES.PRINCIPAL_INVESTIGATOR,
    ];
    return hasAnyTMMRDRole(allowedRoles);
  };

  const canAccessApproval = () => {
    // Matrix: Lab Technicians (No), Researchers (No), Pharmacognosists (Approve), Lab Manager (Final Approve), Principal Investigator (Final Approve)
    const allowedRoles = [
      // Lab Technicians and Researchers excluded per matrix (No)
      TMMRD_ROLES.PHARMACOGNOSIST,
      TMMRD_ROLES.LAB_MANAGER,
      TMMRD_ROLES.PRINCIPAL_INVESTIGATOR,
    ];
    return hasAnyTMMRDRole(allowedRoles);
  };

  const canAccessHerbarium = () => {
    // Matrix: Lab Technicians (View), Researchers (View), Pharmacognosists (Full), Lab Manager (Full), Principal Investigator (View)
    const allowedRoles = [
      TMMRD_ROLES.LAB_TECHNICIAN,
      TMMRD_ROLES.RESEARCHER,
      TMMRD_ROLES.PHARMACOGNOSIST,
      TMMRD_ROLES.LAB_MANAGER,
      TMMRD_ROLES.PRINCIPAL_INVESTIGATOR,
    ];
    return hasAnyTMMRDRole(allowedRoles);
  };

  // Legacy stage access functions (kept for backward compatibility)
  const canAccessStage1 = canAccessRegistration;
  const canAccessStage2 = canAccessAuthentication;
  const canAccessStage3to4 = () => canAccessProcessing() || canAccessExtraction();
  const canAccessStage5to6 = () => canAccessAnalysis() || canAccessProductDevelopment();
  const canAccessStage7 = canAccessProductDevelopment;
  const canAccessStage8 = canAccessHerbarium;

  return {
    // Core permission checks
    getPagePermissionLevel,
    canSaveData,
    canRegisterData,
    canApproveData,
    canFinalApprove,
    canAuthenticate,
    hasFullControl,
    isReadOnly,
    canModify,
    canPerformWork,

    // Role checks (provided by usePermissions hook)
    hasAnyRole,
    hasRole,

    // Permission hierarchy
    getPermissionRank,
    hasAtLeastPermission,

    // TMMRD-specific role checks
    isTMMRDLabTechnician,
    isTMMRDResearcher,
    isTMMRDPharmacognosist,
    isTMMRDLabManager,
    isTMMRDPrincipalInvestigator,
    hasAnyTMMRDRole,

    // Page-specific access checks (TMMRD workflow pages)
    canAccessRegistration,
    canAccessAuthentication,
    canAccessProcessing,
    canAccessExtraction,
    canAccessAnalysis,
    canAccessProductDevelopment,
    canAccessApproval,
    canAccessHerbarium,

    // Legacy stage access checks (for backward compatibility)
    canAccessStage1,
    canAccessStage2,
    canAccessStage3to4,
    canAccessStage5to6,
    canAccessStage7,
    canAccessStage8,

    // Constants
    TMMRD_ROLES,
    TMMRD_PAGES,
    TMMRD_LAB_UNIT,
    permissionHierarchy,
  };
};

export default useTMMRDPermissions;
