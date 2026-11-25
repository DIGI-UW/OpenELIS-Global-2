import {
  getMicrobiologyCurrentStep,
  getMicrobiologyCurrentStepSection,
} from "./MicrobiologyCaseState";

describe("MicrobiologyCaseState", () => {
  it.each([
    [{ stage: "RECEIVED" }, "setup"],
    [{ stage: "INCUBATING" }, "setup"],
    [{ stage: "GROWTH_DETECTED" }, "isolates"],
    [{ stage: "IDENTIFICATION" }, "isolates"],
    [{ stage: "AST_IN_PROGRESS" }, "ast"],
    [{ stage: "REVIEW_READY" }, "reports"],
    [{ stage: "FINAL_RELEASED" }, "reports"],
    [{ stage: "LOST_SPECIMEN" }, "case-info"],
  ])("maps %o to the authoritative current section", (detail, expected) => {
    expect(getMicrobiologyCurrentStepSection(detail)).toBe(expected);
    expect(getMicrobiologyCurrentStep(detail).section).toBe(expected);
  });

  it("focuses an open amendment before the persisted stage", () => {
    expect(
      getMicrobiologyCurrentStepSection({
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
      expect(getMicrobiologyCurrentStepSection({ stage })).not.toBe("timeline"),
    );
  });
});
