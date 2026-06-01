import {
  filterOwningDepartments,
  isOwningDepartment,
  loadNotebookEquipmentOptions,
  loadNotebookDepartmentIds,
  loadNotebookScopedInventory,
  mergeInventoryOptionsWithLinkedSelections,
  NOTEBOOK_INVENTORY_SCOPE_STATUS,
  OWNERSHIP_PSEUDO_DEPARTMENT_NAMES,
} from "./notebookInventoryScope";

jest.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
}));

const { getFromOpenElisServer } = require("../../utils/Utils");

describe("notebookInventoryScope owning departments", () => {
  it("excludes All Lab Units pseudo department", () => {
    expect(
      isOwningDepartment({ name: OWNERSHIP_PSEUDO_DEPARTMENT_NAMES[1] }),
    ).toBe(false);
  });

  it("keeps real AHRI departments", () => {
    const filtered = filterOwningDepartments([
      { id: "1", name: "Biorepository Laboratory" },
      { id: "2", name: "All Lab Units" },
    ]);
    expect(filtered).toHaveLength(1);
    expect(filtered[0].name).toBe("Biorepository Laboratory");
  });

  it("falls back to inferred department ids when legacy notebooks have no linked departments", (done) => {
    getFromOpenElisServer.mockImplementation((endpoint, callback) => {
      if (endpoint === "/rest/notebook/58/departments") {
        callback([]);
        return;
      }
      if (endpoint === "/rest/notebook/view/58") {
        callback({
          id: 58,
          title: "Viral Vaccine - Lab 1",
          departments: [],
        });
        return;
      }
      if (endpoint === "/rest/inventory/items/assignable-departments") {
        callback([
          { id: 179, name: "Viral Vaccine" },
          { id: 11, name: "Genomics & Bioinformatics Laboratory" },
        ]);
      }
    });

    loadNotebookDepartmentIds(58, (ids) => {
      expect(ids).toEqual([179]);
      done();
    });
  });

  it("returns department scope unavailable metadata when notebook departments cannot be resolved", (done) => {
    getFromOpenElisServer.mockImplementation((endpoint, callback) => {
      if (endpoint === "/rest/notebook/77/departments") {
        callback(undefined, new Error("boom"));
        return;
      }
      callback(undefined, new Error("boom"));
    });

    loadNotebookScopedInventory(77, "/rest/inventory/instruments?status=active", (items, error, meta) => {
      expect(items).toEqual([]);
      expect(error).toBeTruthy();
      expect(meta.scopeStatus).toBe(
        NOTEBOOK_INVENTORY_SCOPE_STATUS.DEPARTMENT_SCOPE_UNAVAILABLE,
      );
      done();
    });
  });

  it("marks empty scoped stock results as noInventoryLots", (done) => {
    getFromOpenElisServer.mockImplementation((endpoint, callback) => {
      if (endpoint === "/rest/notebook/88/departments") {
        callback([{ id: 17, name: "Biorepository Laboratory" }]);
        return;
      }
      expect(endpoint).toContain("departmentIds=17");
      callback([]);
    });

    loadNotebookScopedInventory(88, "/rest/inventory/instruments?status=active&requireLots=true", (items, error, meta) => {
      expect(error).toBeUndefined();
      expect(items).toEqual([]);
      expect(meta.scopeStatus).toBe(NOTEBOOK_INVENTORY_SCOPE_STATUS.NO_INVENTORY_LOTS);
      done();
    });
  });

  it("marks empty equipment results as noInventoryEquipment", (done) => {
    getFromOpenElisServer.mockImplementation((endpoint, callback) => {
      if (endpoint === "/rest/notebook/89/departments") {
        callback([{ id: 17, name: "Biorepository Laboratory" }]);
        return;
      }
      expect(endpoint).toContain("departmentIds=17");
      callback([]);
    });

    loadNotebookEquipmentOptions(
      89,
      (departmentIds) =>
        `/rest/inventory/instruments?status=active&itemTypes=EQUIPMENT&departmentIds=${departmentIds[0]}`,
      (items, error, meta) => {
        expect(error).toBeNull();
        expect(items).toEqual([]);
        expect(meta.scopeStatus).toBe(
          NOTEBOOK_INVENTORY_SCOPE_STATUS.NO_INVENTORY_EQUIPMENT,
        );
        done();
      },
    );
  });

  it("preserves linked selections that are missing from department inventory", () => {
    const merged = mergeInventoryOptionsWithLinkedSelections(
      [{ id: "1", value: "Centrifuge" }],
      [{ id: "2", value: "Thermocycler" }],
    );

    expect(merged).toHaveLength(2);
    expect(merged[1]).toMatchObject({
      id: "2",
      unavailableInDepartmentInventory: true,
    });
  });
});
