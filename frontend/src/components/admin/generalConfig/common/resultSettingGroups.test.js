import { describe, expect, it } from "vitest";
import { groupResultSettings } from "./resultSettingGroups";

const setting = (name) => ({ id: name, name, value: "false" });

describe("result settings grouped by purpose (OGC-1364 v3)", () => {
  it("puts each setting in its group, in the listed order", () => {
    const groups = groupResultSettings([
      setting("autoFillTechNameUser"),
      setting("ResultTechnicianName"),
      setting("allowBulkReleaseClear"),
      setting("retestNoteRequired"),
    ]);
    expect(groups.map((group) => group.id)).toEqual([
      "validation",
      "resultEntry",
      "modification",
    ]);
    expect(groups[1].settings.map((s) => s.name)).toEqual([
      "ResultTechnicianName",
      "autoFillTechNameUser",
    ]);
  });

  it("groups pathology and microbiology settings by their prefix", () => {
    const groups = groupResultSettings([
      setting("pathology.stage.STAINING.enabled"),
      setting("pathology.identifier.separator"),
      setting("microbiologyDefaultBreakpointVersion"),
    ]);
    expect(groups.map((group) => group.id)).toEqual([
      "pathology",
      "microbiology",
    ]);
    expect(groups[0].settings.map((s) => s.name)).toEqual([
      "pathology.identifier.separator",
      "pathology.stage.STAINING.enabled",
    ]);
  });

  it("shows validate all results under Validation while the policy is off", () => {
    const groups = groupResultSettings([setting("validate all results")], {
      policyEnabled: false,
    });
    expect(groups.map((group) => group.id)).toEqual(["validation"]);
  });

  it("never hides a setting it does not know, and leaves out the replaced switch", () => {
    const groups = groupResultSettings(
      [setting("validate all results"), setting("somethingNew")],
      { policyEnabled: true },
    );
    expect(groups).toEqual([
      {
        id: "other",
        titleKey: "resultSettings.group.other",
        settings: [setting("somethingNew")],
      },
    ]);
  });
});
