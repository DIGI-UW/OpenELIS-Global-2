import { Permissions } from "../constants/roles";
import {
  disposalPersonas,
  equipmentPersonas,
  notebookApprovalPersonas,
  reportingPersonas,
  resultValidationPersonas,
  sampleProcessingPersonas,
  sampleRegistrationPersonas,
  storagePersonas,
} from "../constants/ahriSrsPersonas";

/** Roles allowed to create or edit storage locations (rooms, devices, shelves, racks). */
export const storageMutationRoles = [...storagePersonas, ...Permissions.MANAGE_EQUIPMENT];

/** Roles allowed to save inventory items (reagents or equipment). */
export const inventorySaveRoles = [
  ...sampleProcessingPersonas,
  ...equipmentPersonas,
];

/** Roles allowed to open catalog item create/edit (aligned with backend save authorization). */
export const inventoryItemMutationRoles = [...inventorySaveRoles];

/** Roles allowed to manage equipment inventory items and usage. */
export const equipmentMutationRoles = [...equipmentPersonas];

/** Roles allowed to run inventory QC workflows on lots. */
export const inventoryQcRoles = [...resultValidationPersonas, ...Permissions.MANAGE_QA];

/** Roles allowed to generate inventory reports. */
export const inventoryReportRoles = [...reportingPersonas, ...Permissions.GENERATE_REPORTS];

/** Roles allowed to register new samples (biorepository intake). */
export const sampleRegistrationRoles = [...sampleRegistrationPersonas];

/** Roles allowed to process samples (aliquots, add tests). */
export const sampleProcessingRoles = [...sampleProcessingPersonas];

/** Roles allowed to approve or finalize notebook entries. */
export const notebookApprovalRoles = [...notebookApprovalPersonas];

/** Roles allowed to view audit trails. */
export const auditTrailViewRoles = [...Permissions.VIEW_AUDIT_TRAIL];
