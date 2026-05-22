import { convertToISODateTime } from "./inventoryDateUtils";

const EQUIPMENT_CONDITIONS = [
  "functional",
  "non-functional",
  "under-repair",
  "decommissioned",
];

export const validateCatalogForm = (formData, context = {}) => {
  const { inventoryDepartmentId, assignableDepartmentsLoading, assignableDepartments } =
    context;

  if (!formData.name?.trim()) {
    return "Item name is required";
  }
  if (!formData.itemType) {
    return "Item type is required";
  }
  if (!formData.units?.trim()) {
    return "Units are required";
  }

  if (formData.itemType === "REAGENT") {
    if (!formData.stabilityAfterOpening || formData.stabilityAfterOpening <= 0) {
      return "Stability after opening is required for reagents and must be greater than 0";
    }
  }

  if (formData.itemType === "CARTRIDGE") {
    if (!formData.compatibleAnalyzers?.trim()) {
      return "Compatible analyzers are required for analyzer cartridges";
    }
    if (
      formData.calibrationRequired &&
      formData.calibrationRequired !== "Y" &&
      formData.calibrationRequired !== "N"
    ) {
      return "Calibration required must be Y or N";
    }
  }

  if (formData.itemType === "EQUIPMENT") {
    if (!formData.modelNumber?.trim()) {
      return "Model number is required for equipment";
    }
    if (!EQUIPMENT_CONDITIONS.includes(formData.equipmentCondition)) {
      return "Invalid equipment condition selected";
    }
  }

  if (formData.itemType === "RDT") {
    if (!formData.testsPerKit || formData.testsPerKit <= 0) {
      return "Tests per kit is required for RDTs and must be greater than 0";
    }
    if (
      formData.individualTracking &&
      formData.individualTracking !== "Y" &&
      formData.individualTracking !== "N"
    ) {
      return "Individual tracking must be Y or N";
    }
  }

  if (formData.itemType === "ENZYME") {
    if (!formData.enzymeType?.trim()) {
      return "Enzyme type is required for enzyme catalog items";
    }
  }

  if (formData.itemType === "HIV_KIT" || formData.itemType === "SYPHILIS_KIT") {
    if (!formData.sourceOrganization?.trim()) {
      return "Source organization is required for HIV/Syphilis kits";
    }
    if (!formData.kitTestType?.trim()) {
      return "Kit test type is required for HIV/Syphilis kits";
    }
    if (!formData.testsPerKit || formData.testsPerKit <= 0) {
      return "Tests per kit is required for HIV/Syphilis kits and must be greater than 0";
    }
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
  const sanitizedData = {
    name: formData.name,
    itemType: formData.itemType,
    category: formData.category,
    manufacturer: formData.manufacturer,
    units: formData.units,
    lowStockThreshold: Number(formData.lowStockThreshold) || 0,
    projectName: formData.projectName || null,
  };

  if (formData.itemType === "REAGENT") {
    sanitizedData.stabilityAfterOpening =
      Number(formData.stabilityAfterOpening) || 0;
    sanitizedData.dilutionNotes = formData.dilutionNotes;
    sanitizedData.storageRequirements = formData.storageRequirements;
    sanitizedData.concentration = formData.concentration;
  } else if (formData.itemType === "CARTRIDGE") {
    sanitizedData.compatibleAnalyzers = formData.compatibleAnalyzers;
    sanitizedData.calibrationRequired = formData.calibrationRequired;
  } else if (formData.itemType === "EQUIPMENT") {
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
  } else if (formData.itemType === "RDT") {
    sanitizedData.testsPerKit = Number(formData.testsPerKit) || 0;
    sanitizedData.individualTracking = formData.individualTracking;
  } else if (formData.itemType === "ENZYME") {
    sanitizedData.enzymeType = formData.enzymeType;
  } else if (
    formData.itemType === "HIV_KIT" ||
    formData.itemType === "SYPHILIS_KIT"
  ) {
    sanitizedData.sourceOrganization = formData.sourceOrganization;
    sanitizedData.kitTestType = formData.kitTestType;
    sanitizedData.testsPerKit = Number(formData.testsPerKit) || 0;
  }

  if (inventoryDepartmentId) {
    const deptNum = parseInt(inventoryDepartmentId, 10);
    if (!Number.isNaN(deptNum)) {
      sanitizedData.departmentTestSectionId = deptNum;
    }
  }

  return sanitizedData;
};
