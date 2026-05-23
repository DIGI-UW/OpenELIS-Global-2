import {
  buildLinkedEquipmentInstrumentsUrl,
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
});
