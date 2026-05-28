import {
  NOTEBOOK_STAGE_ACTIONS,
  normalizeWorkflowType,
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

  it("prefers registry personas over stale page allowedRoles", () => {
    const roles = resolvePageAllowedRoles("biorepository", {
      order: 1,
      allowedRoles: ["Lab Manager"],
    });
    expect(roles).toContain("Sample Collector");
    expect(roles).toContain("Laboratory Technician");
    expect(roles).not.toContain("Lab Manager");
  });

  it("exports stage actions", () => {
    expect(NOTEBOOK_STAGE_ACTIONS.VIEW).toBe("VIEW");
  });

  it("normalizes pathology subtypes to pathology registry key", () => {
    expect(normalizeWorkflowType("fnac")).toBe("pathology");
    expect(normalizeWorkflowType("cytology_liquid_based_pap_smear")).toBe("pathology");
    expect(normalizeWorkflowType("histopathology_biopsy_tissue")).toBe("pathology");
  });

  it("resolves fnac microscopy personas from pathology registry", () => {
    const roles = resolvePageAllowedRoles("fnac", { order: 9 });
    expect(roles).toContain("Junior Researcher");
    expect(roles).toContain("Senior Researcher");
    expect(roles).toContain("Laboratory Technician");
  });

  it("resolves cytology report_print personas from pathology registry", () => {
    const roles = resolvePageAllowedRoles("cytology_liquid_based_pap_smear", {
      order: 10,
    });
    expect(roles).toContain("Lab Manager");
    expect(roles).toContain("Senior Researcher");
  });
});
