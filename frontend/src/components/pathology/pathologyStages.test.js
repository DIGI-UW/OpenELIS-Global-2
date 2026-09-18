import { createIntl } from "react-intl";
import messages from "../../languages/en.json";
import {
  PATHOLOGY_STAGES,
  IN_PROGRESS_STAGES,
  stageDisplayKey,
  stageLabel,
  inProgressStageIds,
} from "./pathologyStages";

const intl = createIntl({ locale: "en", messages });

describe("PATHOLOGY_STAGES", () => {
  it("lists the eleven bench stages in the order the bench works them", () => {
    expect(PATHOLOGY_STAGES).toEqual([
      "ACCESSIONED",
      "GROSSING",
      "DECALCIFICATION",
      "PROCESSING",
      "EMBEDDING",
      "MICROTOMY",
      "STAINING",
      "COVERSLIPPING",
      "READY_PATHOLOGIST",
      "UNDER_REVIEW",
      "COMPLETED",
    ]);
  });

  it("has a display key in en.json for every stage, and no orphan pathology.stage key", () => {
    const stageKeys = Object.keys(messages).filter((key) =>
      key.startsWith("pathology.stage."),
    );

    PATHOLOGY_STAGES.forEach((stage) => {
      expect(messages[stageDisplayKey(stage)]).toBeDefined();
    });

    expect(stageKeys.sort()).toEqual(
      PATHOLOGY_STAGES.map((stage) => stageDisplayKey(stage)).sort(),
    );
  });
});

describe("IN_PROGRESS_STAGES", () => {
  it("holds nine stages, in bench order, excluding the two with their own tile", () => {
    expect(IN_PROGRESS_STAGES).toHaveLength(9);
    expect(IN_PROGRESS_STAGES).not.toContain("READY_PATHOLOGIST");
    expect(IN_PROGRESS_STAGES).not.toContain("COMPLETED");
    expect(IN_PROGRESS_STAGES).toEqual(
      PATHOLOGY_STAGES.filter(
        (stage) => stage !== "READY_PATHOLOGIST" && stage !== "COMPLETED",
      ),
    );
  });

  it("still counts the stages a bare COMPLETED filter would wrongly include", () => {
    expect(IN_PROGRESS_STAGES).toContain("UNDER_REVIEW");
    expect(IN_PROGRESS_STAGES).toContain("ACCESSIONED");
  });
});

describe("stageLabel", () => {
  it("resolves a known stage to its localized display text", () => {
    expect(stageLabel(intl, "MICROTOMY")).toBe(
      messages["pathology.stage.microtomy"],
    );
  });

  it("falls back rather than breaking on a stage id it does not recognize", () => {
    expect(stageLabel(intl, "FOO", "Foo")).toBe("Foo");
  });
});

describe("inProgressStageIds", () => {
  it("keeps only the served ids and preserves bench order", () => {
    const served = [
      "COMPLETED",
      "STAINING",
      "ACCESSIONED",
      "READY_PATHOLOGIST",
      "GROSSING",
    ];

    expect(inProgressStageIds(served)).toEqual([
      "ACCESSIONED",
      "GROSSING",
      "STAINING",
    ]);
  });
});
