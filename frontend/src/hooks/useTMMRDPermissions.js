import { usePermissions } from './usePermissions';

/**
 * TMMRD (Traditional Medicine & Modern Research Development) Permissions Hook
 *
 * Based on bioanalytical permissions model with TMMRD-specific adaptations
 * Implements three-layer RBAC system per TMMRD SRS Section 11:
 *
 * Layer 1: System Roles (Lab Technician, Researcher, Pharmacognosist, Lab Manager, Principal Investigator)
 * Layer 2: Page-level Access (which roles can access each page)
 * Layer 3: Action-level Permissions (what actions roles can perform)
 *
 * Permission Levels:
 * - VIEW: Read-only access
 * - REGISTER: Can register/import samples (Stage 1)
 * - UPDATE: Can edit and update data
 * - APPROVE: Can approve operations
 * - FULL: Complete control including save and submit
 * - LIMITED: Restricted access with minimal modifications
 * - YES: Can perform work (legacy from SRS table)
 */
export const useTMMRDPermissions = () => {
  const { userSessionDetails, hasRole, hasAnyRole } = usePermissions();

  // Permission hierarchy (lowest to highest)
  const permissionHierarchy = [
    "VIEW",           // Read-only
    "LIMITED",        // Minimal modifications
    "REGISTER",       // Can import/register samples
    "UPDATE",         // Can edit data
    "YES",           // Can perform work (legacy compatibility)
    "APPROVE",        // Can approve operations
    "FULL"           // Complete control
  ];

  /**
   * Get the user's permission level for a specific page based on their roles
   */
  const getPagePermissionLevel = (pageName) => {
    if (!userSessionDetails?.roles) {
      return "VIEW";
    }

    // Debug logging can be enabled if needed
    // console.log("TMMRD Permissions Debug:", {
    //   pageName,
    //   userRoles: userSessionDetails.roles
    // });

    // Determine permission level based on user's highest role
    // Using the TMMRD role hierarchy per SRS Section 11
    if (hasRole("Principal Investigator")) {
      return "FULL";
    }
    if (hasRole("Lab Manager")) {
      return "FULL";
    }
    if (hasRole("Pharmacognosist")) {
      return "APPROVE";
    }
    if (hasRole("Researcher")) {
      return "UPDATE";
    }
    if (hasRole("Lab Technician")) {
      return "REGISTER";
    }

    // Default for users without TMMRD roles
    return "VIEW";
  };

  /**
   * Check if user can save/update data
   */
  const canSaveData = (permissionLevel) => {
    return ["UPDATE", "YES", "REGISTER", "APPROVE", "FULL"].includes(permissionLevel?.toUpperCase());
  };

  /**
   * Check if user can register/import samples
   */
  const canRegisterData = (permissionLevel) => {
    return ["REGISTER", "UPDATE", "YES", "APPROVE", "FULL"].includes(permissionLevel?.toUpperCase());
  };

  /**
   * Check if user can approve operations
   */
  const canApproveData = (permissionLevel) => {
    return ["APPROVE", "FULL"].includes(permissionLevel?.toUpperCase());
  };

  /**
   * Check if user has full control
   */
  const hasFullControl = (permissionLevel) => {
    return permissionLevel?.toUpperCase() === "FULL";
  };

  /**
   * Check if user has read-only access
   */
  const isReadOnly = (permissionLevel) => {
    return ["VIEW", "LIMITED"].includes(permissionLevel?.toUpperCase());
  };

  /**
   * Check if user can modify data
   */
  const canModify = (permissionLevel) => {
    return ["LIMITED", "UPDATE", "YES", "REGISTER", "APPROVE", "FULL"].includes(permissionLevel?.toUpperCase());
  };

  /**
   * Check if user can perform work (legacy YES permission)
   */
  const canPerformWork = (permissionLevel) => {
    return ["YES", "UPDATE", "REGISTER", "APPROVE", "FULL"].includes(permissionLevel?.toUpperCase());
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
   */
  const isTMMRDLabTechnician = () => hasRole("Lab Technician");
  const isTMMRDResearcher = () => hasRole("Researcher");
  const isTMMRDPharmacognosist = () => hasRole("Pharmacognosist");
  const isTMMRDLabManager = () => hasRole("Lab Manager");
  const isTMMRDPrincipalInvestigator = () => hasRole("Principal Investigator");

  /**
   * Stage-specific permission checks per TMMRD workflow
   */
  const canAccessStage1 = () => {
    // Sample Intake & Registration - Lab Technicians lead, others view
    const allowedRoles = ["Lab Technician", "Researcher", "Pharmacognosist", "Lab Manager", "Principal Investigator"];
    return hasAnyRole(allowedRoles);
  };

  const canAccessStage2 = () => {
    // Authentication & Storage - Pharmacognosists lead
    const allowedRoles = ["Pharmacognosist", "Lab Manager", "Principal Investigator"];
    return hasAnyRole(allowedRoles);
  };

  const canAccessStage3to4 = () => {
    // Preparation & Extraction - Lab Technicians and Researchers
    const allowedRoles = ["Lab Technician", "Researcher", "Pharmacognosist", "Lab Manager", "Principal Investigator"];
    return hasAnyRole(allowedRoles);
  };

  const canAccessStage5to6 = () => {
    // Analytics & Testing - Researchers lead
    const allowedRoles = ["Researcher", "Pharmacognosist", "Lab Manager", "Principal Investigator"];
    return hasAnyRole(allowedRoles);
  };

  const canAccessStage7 = () => {
    // Formulation - Pharmacognosists lead
    const allowedRoles = ["Lab Technician", "Researcher", "Pharmacognosist", "Lab Manager", "Principal Investigator"];
    return hasAnyRole(allowedRoles);
  };

  const canAccessStage8 = () => {
    // Archival - Management only
    const allowedRoles = ["Lab Manager", "Principal Investigator"];
    return hasAnyRole(allowedRoles);
  };

  return {
    // Core permission checks
    getPagePermissionLevel,
    canSaveData,
    canRegisterData,
    canApproveData,
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

    // Stage-specific access checks
    canAccessStage1,
    canAccessStage2,
    canAccessStage3to4,
    canAccessStage5to6,
    canAccessStage7,
    canAccessStage8,

    // Utility
    permissionHierarchy
  };
};

export default useTMMRDPermissions;