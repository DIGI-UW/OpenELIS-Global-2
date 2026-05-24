import {
  validateCatalogForm,
  buildCatalogPayload,
} from "./inventoryCatalogValidation";

const baseContext = {
  inventoryDepartmentId: "10",
  assignableDepartmentsLoading: false,
  assignableDepartments: [{ id: "10", value: "Bacteriology" }],
};

describe("inventoryCatalogValidation", () => {
  it("allows equipment save without units in form", () => {
    const error = validateCatalogForm(
      {
        name: "Centrifuge",
        itemType: "EQUIPMENT",
        units: "",
      },
      baseContext,
    );
    expect(error).toBeNull();
  });

  it("requires category for stock catalog items", () => {
    const error = validateCatalogForm(
      {
        name: "Buffer",
        itemType: "REAGENT",
        category: "",
        units: "mL",
      },
      baseContext,
    );
    expect(error).toBe("Category is required for stock items");
  });

  it("requires units for stock catalog items", () => {
    const error = validateCatalogForm(
      {
        name: "Buffer",
        itemType: "REAGENT",
        category: "Reagent",
        units: "",
      },
      baseContext,
    );
    expect(error).toBe("Units are required");
  });

  it("defaults equipment units to each in payload", () => {
    const payload = buildCatalogPayload(
      {
        name: "Freezer",
        itemType: "EQUIPMENT",
        units: "",
        category: "",
        manufacturer: "",
        lowStockThreshold: 0,
        projectName: "",
      },
      "10",
    );
    expect(payload.units).toBe("each");
  });
});
