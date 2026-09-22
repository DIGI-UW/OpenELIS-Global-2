import { createIntl } from "react-intl";
import messages from "../../languages/en.json";
import {
  PATHOLOGY_STAGES,
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
  it("drops only the two stages that have their own tile, keeping the served order", () => {
    const served = [
      "ACCESSIONED",
      "GROSSING",
      "STAINING",
      "READY_PATHOLOGIST",
      "COMPLETED",
    ];

    expect(inProgressStageIds(served)).toEqual([
      "ACCESSIONED",
      "GROSSING",
      "STAINING",
    ]);
  });

  it("carries through a stage this build has never heard of", () => {
    // The grouping is derived from what the server serves, not from a second
    // copy of the stage names held here, so a stage added to PathologyStatus
    // and served by DisplayListService stays in the in-progress grouping the
    // backend tile is already counting it in, instead of silently vanishing
    // from the dashboard.
    expect(
      inProgressStageIds(["GROSSING", "DEHYDRATION", "COMPLETED"]),
    ).toEqual(["GROSSING", "DEHYDRATION"]);
  });

  it("covers every stage the eleven-stage list offers except those two", () => {
    expect(inProgressStageIds(PATHOLOGY_STAGES)).toHaveLength(9);
    expect(inProgressStageIds(PATHOLOGY_STAGES)).toContain("UNDER_REVIEW");
    expect(inProgressStageIds(PATHOLOGY_STAGES)).not.toContain(
      "READY_PATHOLOGIST",
    );
  });
});
