import {
  buildLinkedEquipmentInstrumentsUrl,
  buildLinkedStockInventoryUrl,
  mapLinkedEquipmentOptions,
} from "./notebookLinkedEquipment";

describe("notebookLinkedEquipment", () => {
  test("buildLinkedEquipmentInstrumentsUrl requests EQUIPMENT only with department filter", () => {
    const url = buildLinkedEquipmentInstrumentsUrl([7, 9]);
    expect(url).toContain("/rest/inventory/instruments?");
    expect(url).toContain("itemTypes=EQUIPMENT");
    expect(url).toContain("departmentIds=7");
    expect(url).toContain("departmentIds=9");
    expect(url).not.toContain("CARTRIDGE");
    expect(url).not.toContain("REAGENT");
  });

  test("mapLinkedEquipmentOptions excludes non-equipment types from labels", () => {
    const options = mapLinkedEquipmentOptions([
      { id: "1", name: "Centrifuge", itemType: "EQUIPMENT" },
      { id: "2", name: "Cartridge kit", itemType: "CARTRIDGE" },
    ]);
    expect(options).toHaveLength(2);
    expect(options[0].value).toBe("Centrifuge");
    expect(options[1].itemType).toBe("CARTRIDGE");
  });

  test("buildLinkedStockInventoryUrl requests stock item types separately from equipment", () => {
    const url = buildLinkedStockInventoryUrl([{ id: 7 }]);
    expect(url).toContain("requireLots=true");
    expect(url).toContain("itemTypes=REAGENT");
    expect(url).toContain("itemTypes=CONSUMABLE");
    expect(url).toContain("itemTypes=CARTRIDGE");
    expect(url).toContain("departmentIds=7");
    expect(url).not.toContain("itemTypes=EQUIPMENT");
  });
});
