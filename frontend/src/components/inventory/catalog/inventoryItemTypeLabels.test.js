import {
  isLotReceivableType,
  isEquipmentType,
  isExpiryTrackedType,
} from "./inventoryItemTypeLabels";

describe("inventoryItemTypeLabels", () => {
  it("treats equipment as non-lot-receivable", () => {
    expect(isEquipmentType("EQUIPMENT")).toBe(true);
    expect(isLotReceivableType("EQUIPMENT")).toBe(false);
  });

  it("allows stock types for lot receiving", () => {
    expect(isLotReceivableType("REAGENT")).toBe(true);
    expect(isLotReceivableType("CONSUMABLE")).toBe(true);
    expect(isLotReceivableType("CARTRIDGE")).toBe(true);
  });

  it("tracks expiry for reagent and consumable types only", () => {
    expect(isExpiryTrackedType("REAGENT")).toBe(true);
    expect(isExpiryTrackedType("CONSUMABLE")).toBe(true);
    expect(isExpiryTrackedType("EQUIPMENT")).toBe(false);
  });
});
