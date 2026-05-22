/** Item types shown in catalog create/edit dropdown (excludes half-implemented types). */
export const CATALOG_CREATABLE_ITEM_TYPES = [
  "REAGENT",
  "CARTRIDGE",
  "EQUIPMENT",
  "CONSUMABLE",
  "RDT",
  "HIV_KIT",
  "SYPHILIS_KIT",
];

const ITEM_TYPE_LABELS = {
  REAGENT: "Reagent",
  CARTRIDGE: "Analyzer cartridge",
  EQUIPMENT: "Equipment",
  CONSUMABLE: "Consumable",
  RDT: "RDT (Rapid Diagnostic Test)",
  HIV_KIT: "HIV Test Kit",
  SYPHILIS_KIT: "Syphilis Test Kit",
  ENZYME: "Enzyme",
  ANTIBIOTICS: "Antibiotics",
};

export const getItemTypeLabel = (type) =>
  ITEM_TYPE_LABELS[type] || type || "";

export const formatItemTypesForDropdown = (types) =>
  (types || [])
    .filter((type) => CATALOG_CREATABLE_ITEM_TYPES.includes(type))
    .map((type) => ({
      id: type,
      text: getItemTypeLabel(type),
    }));

export const isExpiryTrackedType = (itemType) =>
  ["REAGENT", "RDT", "HIV_KIT", "SYPHILIS_KIT", "CARTRIDGE", "CONSUMABLE"].includes(
    itemType,
  );

export const isEquipmentType = (itemType) => itemType === "EQUIPMENT";

/** Stock lots are received for consumable inventory, not instruments. */
export const isLotReceivableType = (itemType) => itemType && !isEquipmentType(itemType);

export const isCartridgeType = (itemType) => itemType === "CARTRIDGE";
