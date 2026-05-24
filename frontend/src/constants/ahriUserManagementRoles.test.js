import {
  filterAhriGlobalRoles,
  filterAhriLabUnitRoles,
  filterAhriProjectRoles,
} from "./ahriUserManagementRoles";

describe("ahriUserManagementRoles", () => {
  const labRoles = [
    { roleId: "1", roleName: "Storage Manager" },
    { roleId: "2", roleName: "Sample Collector" },
    { roleId: "3", roleName: "Laboratory Technician" },
    { roleId: "4", roleName: "Analyst" },
    { roleId: "5", roleName: "Biomedical Staff" },
  ];

  it("keeps only SRS lab unit roles in display order", () => {
    const filtered = filterAhriLabUnitRoles(labRoles);
    expect(filtered.map((r) => r.roleName)).toEqual([
      "Sample Collector",
      "Laboratory Technician",
      "Biomedical Staff",
    ]);
  });

  it("filters project roles to SRS set", () => {
    const roles = [
      { roleId: "1", roleName: "Data Manager" },
      { roleId: "2", roleName: "Results" },
    ];
    expect(filterAhriProjectRoles(roles).map((r) => r.roleName)).toEqual([
      "Data Manager",
    ]);
  });

  it("filters global roles to SRS set", () => {
    const roles = [
      { roleId: "1", roleName: "System Admin" },
      { roleId: "2", roleName: "Technician" },
    ];
    expect(filterAhriGlobalRoles(roles).map((r) => r.roleName)).toEqual([
      "System Admin",
    ]);
  });
});
