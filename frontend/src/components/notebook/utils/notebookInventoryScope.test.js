import {
  filterOwningDepartments,
  isOwningDepartment,
  loadNotebookDepartmentIds,
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
});
