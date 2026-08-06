import {
  getMicrobiologyCurrentStep,
  getMicrobiologyCurrentStepSection,
} from "./MicrobiologyCaseState";

describe("MicrobiologyCaseState", () => {
  it.each([
    [{ workflowType: "UNASSIGNED", stage: "RECEIVED" }, "case-info"],
    [{ workflowType: "BACTERIOLOGY", stage: "RECEIVED" }, "setup"],
    [{ workflowType: "BACTERIOLOGY", stage: "INCUBATING" }, "setup"],
    [{ workflowType: "BACTERIOLOGY", stage: "GROWTH_DETECTED" }, "isolates"],
    [{ workflowType: "BACTERIOLOGY", stage: "IDENTIFICATION" }, "isolates"],
    [{ workflowType: "BACTERIOLOGY", stage: "AST_IN_PROGRESS" }, "ast"],
    [{ workflowType: "BACTERIOLOGY", stage: "REVIEW_READY" }, "reports"],
    [{ workflowType: "BACTERIOLOGY", stage: "FINAL_RELEASED" }, "reports"],
    [{ workflowType: "BACTERIOLOGY", stage: "LOST_SPECIMEN" }, "case-info"],
  ])("maps %o to the authoritative current section", (detail, expected) => {
    expect(getMicrobiologyCurrentStepSection(detail)).toBe(expected);
    expect(getMicrobiologyCurrentStep(detail).section).toBe(expected);
  });

  it("focuses an open amendment before the persisted stage", () => {
    expect(
      getMicrobiologyCurrentStepSection({
        workflowType: "BACTERIOLOGY",
        stage: "FINAL_RELEASED",
        finalReleaseState: "AMENDMENT_IN_PROGRESS",
      }),
    ).toBe("amendment");
  });

  it("never chooses timeline as a default step", () => {
    const stages = [
      "RECEIVED",
      "SETUP_RECORDED",
      "INCUBATING",
      "GROWTH_DETECTED",
      "NO_GROWTH_READY",
      "IDENTIFICATION",
      "AST_READY",
      "AST_IN_PROGRESS",
      "REVIEW_READY",
      "PRELIM_RELEASED",
      "FINAL_RELEASED",
      "AMENDED",
      "REJECTED",
      "LOST_SPECIMEN",
      "LOST_SPECIMEN_POSITIVE",
    ];
    stages.forEach((stage) =>
      expect(
        getMicrobiologyCurrentStepSection({
          workflowType: "BACTERIOLOGY",
          stage,
        }),
      ).not.toBe("timeline"),
    );
  });
});
