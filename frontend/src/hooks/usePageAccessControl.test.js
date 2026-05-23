import {
  NOTEBOOK_STAGE_ACTIONS,
  resolvePageAllowedRoles,
} from "../constants/ahriWorkflowRegistry";

jest.mock("./usePermissions", () => ({
  usePermissions: () => ({
    hasRoleForCurrentLabUnit: (roles) => roles.includes("Laboratory Technician"),
  }),
}));

describe("usePageAccessControl (registry integration)", () => {
  it("resolves biorepository intake personas for stage 1", () => {
    const roles = resolvePageAllowedRoles("biorepository", { order: 1 });
    expect(roles).toContain("Sample Collector");
    expect(roles).not.toContain("Storage Manager");
  });

  it("returns empty for unknown workflow (fail-closed)", () => {
    expect(resolvePageAllowedRoles("unknown_lab", { order: 1 })).toEqual([]);
  });

  it("prefers explicit page allowedRoles when present", () => {
    const roles = resolvePageAllowedRoles("biorepository", {
      order: 1,
      allowedRoles: ["Lab Manager"],
    });
    expect(roles).toEqual(["Lab Manager"]);
  });

  it("exports stage actions", () => {
    expect(NOTEBOOK_STAGE_ACTIONS.VIEW).toBe("VIEW");
  });
});
