export const INVENTORY_CLASS = {
  STOCK: "STOCK",
  EQUIPMENT: "EQUIPMENT",
};

export const DEFAULT_STOCK_ITEM_TYPE = "REAGENT";
export const EQUIPMENT_ITEM_TYPE = "EQUIPMENT";

export const INVENTORY_CLASS_OPTIONS = [
  {
    id: INVENTORY_CLASS.STOCK,
    text: "Stock item",
  },
  {
    id: INVENTORY_CLASS.EQUIPMENT,
    text: "Permanent equipment",
  },
];

export const getInventoryClass = (itemType) =>
  itemType === EQUIPMENT_ITEM_TYPE
    ? INVENTORY_CLASS.EQUIPMENT
    : INVENTORY_CLASS.STOCK;

export const isPermanentEquipment = (itemOrType) => {
  const itemType =
    typeof itemOrType === "string" ? itemOrType : itemOrType?.itemType;
  return getInventoryClass(itemType) === INVENTORY_CLASS.EQUIPMENT;
};

export const isStockManaged = (itemOrType) => !isPermanentEquipment(itemOrType);

export const isLotReceivable = (itemOrType) => isStockManaged(itemOrType);

