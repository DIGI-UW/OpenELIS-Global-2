import {
  formatUnitOptionsFromUomResponse,
  DEFAULT_EQUIPMENT_UNIT,
} from "./inventoryUnitOptions";

describe("inventoryUnitOptions", () => {
  it("maps existingUomList id/value pairs and adds one add-new option", () => {
    const options = formatUnitOptionsFromUomResponse({
      existingUomList: [
        { id: "1", value: "mL" },
        { id: "2", value: "each" },
      ],
    });
    expect(options.map((o) => o.text)).toEqual(["mL", "each", "Add new unit..."]);
    expect(options.filter((o) => o.id === "__add_new__")).toHaveLength(1);
  });

  it("deduplicates units and falls back when list is empty", () => {
    const options = formatUnitOptionsFromUomResponse({
      existingUomList: [
        { id: "1", value: "mL" },
        { id: "1", value: "mL" },
      ],
    });
    expect(options.filter((o) => o.text === "mL")).toHaveLength(1);
    expect(options[options.length - 1].id).toBe("__add_new__");

    const fallback = formatUnitOptionsFromUomResponse(null);
    expect(fallback.some((o) => o.text === "each")).toBe(true);
    expect(fallback.filter((o) => o.id === "__add_new__")).toHaveLength(1);
  });

  it("uses each as default equipment unit constant", () => {
    expect(DEFAULT_EQUIPMENT_UNIT).toBe("each");
  });
});
