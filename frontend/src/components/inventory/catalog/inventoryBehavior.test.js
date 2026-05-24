import {
  getInventoryClass,
  INVENTORY_CLASS,
  isLotReceivable,
  isPermanentEquipment,
  isStockManaged,
} from "./inventoryBehavior";

describe("inventoryBehavior", () => {
  it("treats only EQUIPMENT as permanent equipment", () => {
    expect(getInventoryClass("EQUIPMENT")).toBe(INVENTORY_CLASS.EQUIPMENT);
    expect(isPermanentEquipment("EQUIPMENT")).toBe(true);
    expect(isPermanentEquipment("REAGENT")).toBe(false);
  });

  it("treats all non-equipment item types as stock managed", () => {
    expect(getInventoryClass("CARTRIDGE")).toBe(INVENTORY_CLASS.STOCK);
    expect(isStockManaged("CONSUMABLE")).toBe(true);
    expect(isLotReceivable("REAGENT")).toBe(true);
    expect(isLotReceivable("EQUIPMENT")).toBe(false);
  });
});
