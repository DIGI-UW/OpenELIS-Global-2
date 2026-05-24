import {
  filterOwningDepartments,
  isOwningDepartment,
  OWNERSHIP_PSEUDO_DEPARTMENT_NAMES,
} from "./notebookInventoryScope";

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
});
