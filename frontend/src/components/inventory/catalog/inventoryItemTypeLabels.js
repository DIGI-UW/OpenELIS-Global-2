import {
  isPermanentEquipment,
  isLotReceivable,
} from "./inventoryBehavior";

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
  (types || ["REAGENT", "EQUIPMENT"])
    .map((type) => ({
      id: type,
      text: getItemTypeLabel(type),
    }));

export const isExpiryTrackedType = (itemType) => !isPermanentEquipment(itemType);

export const isEquipmentType = (itemType) => isPermanentEquipment(itemType);

/** Stock lots are received for consumable inventory, not instruments. */
export const isLotReceivableType = (itemType) => itemType && isLotReceivable(itemType);

export const isCartridgeType = (itemType) => itemType === "CARTRIDGE";
