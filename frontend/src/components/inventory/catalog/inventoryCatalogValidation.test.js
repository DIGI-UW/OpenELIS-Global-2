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
        modelNumber: "CF-16",
        equipmentCondition: "functional",
      },
      baseContext,
    );
    expect(error).toBeNull();
  });

  it("requires units for reagent catalog items", () => {
    const error = validateCatalogForm(
      {
        name: "Buffer",
        itemType: "REAGENT",
        units: "",
        stabilityAfterOpening: 30,
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
        modelNumber: "U701",
        equipmentCondition: "functional",
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
