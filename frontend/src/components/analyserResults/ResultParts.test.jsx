import { describe, expect, it } from "vitest";
import { groupTestParts } from "./ResultParts";

const row = (id, tube, componentId = null) => ({
  id,
  testId: "7",
  sampleGroupingNumber: 1,
  accessionNumber: "ACC-1",
  instrumentSpecimenId: tube,
  componentId,
});

describe("groupTestParts", () => {
  it("gives the same test on two tubes of one order a decision each", () => {
    const { decisionHeadIds, partIds } = groupTestParts([
      row("1", "TUBE-1"),
      row("2", "TUBE-2"),
    ]);

    expect([...decisionHeadIds].sort()).toEqual(["1", "2"]);
    expect(partIds.size).toBe(0);
  });

  it("decides a test's components on its tube with it", () => {
    const { decisionHeadIds, partsByHeadId } = groupTestParts([
      row("1", "TUBE-1"),
      row("2", "TUBE-1", "component-log"),
    ]);

    expect([...decisionHeadIds]).toEqual(["1"]);
    expect(partsByHeadId.get("1").map((part) => part.id)).toEqual(["2"]);
  });

  it("counts a row staged without a tube id as its accession's tube", () => {
    const { decisionHeadIds } = groupTestParts([
      row("1", null),
      row("2", null, "component-log"),
    ]);

    expect([...decisionHeadIds]).toEqual(["1"]);
  });
});
