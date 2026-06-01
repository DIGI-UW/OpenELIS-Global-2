import { convertToISODateTime } from "./inventoryDateUtils";
import { DEFAULT_EQUIPMENT_UNIT } from "./inventoryUnitOptions";
import { isPermanentEquipment } from "./inventoryBehavior";

export const validateCatalogForm = (formData, context = {}) => {
  const { inventoryDepartmentId, assignableDepartmentsLoading, assignableDepartments } =
    context;

  if (!formData.name?.trim()) {
    return "Item name is required";
  }
  const permanentEquipment = isPermanentEquipment(formData.itemType);

  if (!formData.itemType) {
    return "Inventory class is required";
  }
  if (!permanentEquipment && !formData.category?.trim()) {
    return "Category is required for stock items";
  }
  if (!permanentEquipment && !formData.units?.trim()) {
    return "Units are required";
  }

  if (assignableDepartmentsLoading) {
    return "Loading departments…";
  }
  if (assignableDepartments.length === 0) {
    return "No lab unit / department is assigned to your account. Contact an administrator.";
  }
  if (!inventoryDepartmentId) {
    return "Select a department (lab unit) for this catalog item.";
  }

  return null;
};

export const buildCatalogPayload = (formData, inventoryDepartmentId) => {
  const permanentEquipment = isPermanentEquipment(formData.itemType);
  const sanitizedData = {
    name: formData.name,
    itemType: formData.itemType,
    category: formData.category,
    manufacturer: formData.manufacturer,
    units:
      permanentEquipment
        ? DEFAULT_EQUIPMENT_UNIT
        : formData.units,
    lowStockThreshold: Number(formData.lowStockThreshold) || 0,
    projectName: formData.projectName || null,
  };

  if (formData.itemType === "REAGENT") {
    sanitizedData.stabilityAfterOpening =
      Number(formData.stabilityAfterOpening) || null;
    sanitizedData.dilutionNotes = formData.dilutionNotes;
    sanitizedData.storageRequirements = formData.storageRequirements;
    sanitizedData.concentration = formData.concentration;
  }

  if (formData.itemType !== "EQUIPMENT") {
    sanitizedData.compatibleAnalyzers = formData.compatibleAnalyzers || null;
    sanitizedData.calibrationRequired = formData.calibrationRequired || "N";
  } else {
    sanitizedData.equipmentCondition = formData.equipmentCondition;
    sanitizedData.modelNumber = formData.modelNumber;
    sanitizedData.serialNumber = formData.serialNumber;
    sanitizedData.ahriTag = formData.ahriTag;
    sanitizedData.compatibleAnalyzers = formData.compatibleAnalyzers || null;
    sanitizedData.calibrationRequired = formData.calibrationRequired || "N";
    if (formData.analyzerId) {
      sanitizedData.analyzerId = formData.analyzerId;
    }
    if (formData.installationDate) {
      sanitizedData.installationDate = convertToISODateTime(
        formData.installationDate,
      );
    }
    if (formData.lastServiceDate) {
      sanitizedData.lastServiceDate = convertToISODateTime(
        formData.lastServiceDate,
      );
    }
    if (formData.lastMaintenanceDate) {
      sanitizedData.lastMaintenanceDate = convertToISODateTime(
        formData.lastMaintenanceDate,
      );
    }
    if (formData.nextMaintenanceDate) {
      sanitizedData.nextMaintenanceDate = convertToISODateTime(
        formData.nextMaintenanceDate,
      );
    }
  }

  if (inventoryDepartmentId) {
    const deptNum = parseInt(inventoryDepartmentId, 10);
    if (!Number.isNaN(deptNum)) {
      sanitizedData.departmentTestSectionId = deptNum;
    }
  }

  return sanitizedData;
};
