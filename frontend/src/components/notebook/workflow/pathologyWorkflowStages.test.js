import {
  normalizeWorkflowType,
  resolvePageAllowedRoles,
} from "../../../constants/ahriWorkflowRegistry";

const PATHOLOGY_WORKFLOW_STAGE_MAP = {
  histopathology_biopsy_tissue: new Set([
    1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
  ]),
  peripheral_smear_bone_marrow_morphology: new Set([
    1, 2, 7, 8, 9, 10, 11, 12, 13,
  ]),
  fnac: new Set([1, 2, 7, 8, 9, 10, 11, 12, 13]),
  cytology_liquid_based_pap_smear: new Set([1, 2, 5, 7, 8, 9, 10, 11, 12, 13]),
};

const ALL_SUBTYPES = Object.keys(PATHOLOGY_WORKFLOW_STAGE_MAP);

describe("pathology workflow subtypes", () => {
  it.each(ALL_SUBTYPES)("normalizes %s to canonical pathology RBAC key", (subtype) => {
    expect(normalizeWorkflowType(subtype)).toBe("pathology");
  });

  it("histopathology shows full 13-stage subset", () => {
    expect(PATHOLOGY_WORKFLOW_STAGE_MAP.histopathology_biopsy_tissue.size).toBe(
      13,
    );
    expect(
      PATHOLOGY_WORKFLOW_STAGE_MAP.histopathology_biopsy_tissue.has(3),
    ).toBe(true);
  });

  it("peripheral smear skips tissue processing stages 3-6", () => {
    const stages = PATHOLOGY_WORKFLOW_STAGE_MAP.peripheral_smear_bone_marrow_morphology;
    expect(stages.has(3)).toBe(false);
    expect(stages.has(7)).toBe(true);
  });

  it("fnac matches peripheral smear stage subset", () => {
    const fnac = PATHOLOGY_WORKFLOW_STAGE_MAP.fnac;
    const peripheral =
      PATHOLOGY_WORKFLOW_STAGE_MAP.peripheral_smear_bone_marrow_morphology;
    expect([...fnac].sort()).toEqual([...peripheral].sort());
  });

  it("cytology includes sample processing (5) but not gross/cassette/block", () => {
    const stages = PATHOLOGY_WORKFLOW_STAGE_MAP.cytology_liquid_based_pap_smear;
    expect(stages.has(5)).toBe(true);
    expect(stages.has(3)).toBe(false);
    expect(stages.has(6)).toBe(false);
  });

  it.each(ALL_SUBTYPES)(
    "resolves microscopy personas for subtype %s via pathology registry",
    (subtype) => {
      const roles = resolvePageAllowedRoles(subtype, { order: 9 });
      expect(roles).toContain("Senior Researcher");
      expect(roles.length).toBeGreaterThan(0);
    },
  );

  it.each(ALL_SUBTYPES)(
    "resolves report_print personas for subtype %s via pathology registry",
    (subtype) => {
      const roles = resolvePageAllowedRoles(subtype, { order: 10 });
      expect(roles).toContain("Lab Manager");
      expect(roles).toContain("Senior Researcher");
    },
  );
});
