import { Permissions } from "../constants/roles";

/** Roles allowed to create or edit storage locations (rooms, devices, shelves, racks). */
export const storageMutationRoles = [
  ...Permissions.UPDATE_SAMPLES,
  ...Permissions.MANAGE_EQUIPMENT,
];

/** Roles allowed to save inventory items (reagents or equipment). */
export const inventorySaveRoles = [
  ...Permissions.UPDATE_SAMPLES,
  ...Permissions.MANAGE_EQUIPMENT,
];

/** Roles allowed to open catalog item create/edit (aligned with backend save authorization). */
export const inventoryItemMutationRoles = [...inventorySaveRoles];

/** Roles allowed to manage equipment inventory items and usage. */
export const equipmentMutationRoles = [...Permissions.MANAGE_EQUIPMENT];

/** Roles allowed to run inventory QC workflows on lots. */
export const inventoryQcRoles = [...Permissions.MANAGE_QA];

/** Roles allowed to generate inventory reports. */
export const inventoryReportRoles = [...Permissions.GENERATE_REPORTS];

/** Roles allowed to register new samples (biorepository intake). */
export const sampleRegistrationRoles = [...Permissions.REGISTER_SAMPLES];

/** Roles allowed to process samples (aliquots, add tests). */
export const sampleProcessingRoles = [...Permissions.PROCESS_SAMPLES];

/** Roles allowed to approve or finalize notebook entries. */
export const notebookApprovalRoles = [...Permissions.APPROVE_NOTEBOOK_ENTRY];

/** Roles allowed to view audit trails. */
export const auditTrailViewRoles = [...Permissions.VIEW_AUDIT_TRAIL];
