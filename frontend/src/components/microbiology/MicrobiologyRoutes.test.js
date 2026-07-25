import {
  getMicrobiologyCaseUrl,
  getMicrobiologyWorklistUrl,
  parseMicrobiologyCaseSearch,
  parseMicrobiologyWorklistSearch,
} from "./MicrobiologyRoutes";

describe("MicrobiologyRoutes", () => {
  it("composes worklist filters in a deterministic order", () => {
    expect(
      getMicrobiologyWorklistUrl({
        urgency: "HIGH",
        workflow: "BACTERIOLOGY",
        due: "AST_REVIEW",
        sort: "newest",
      }),
    ).toBe(
      "/Microbiology/worklist?workflow=BACTERIOLOGY&urgency=HIGH&due=AST_REVIEW&sort=newest",
    );
  });

  it("drops unsupported worklist state while parsing", () => {
    expect(
      parseMicrobiologyWorklistSearch(
        "?workflow=BACTERIOLOGY&sort=unsupported&unknown=value",
      ),
    ).toEqual({
      workflow: "BACTERIOLOGY",
      urgency: "",
      due: "",
      sort: "priority",
    });
  });

  it("preserves worklist context and a valid section in a case URL", () => {
    expect(
      getMicrobiologyCaseUrl("case / 1", {
        workflow: "BACTERIOLOGY",
        urgency: "HIGH",
        section: "isolates",
      }),
    ).toBe(
      "/Microbiology/cases/case%20%2F%201?workflow=BACTERIOLOGY&urgency=HIGH&section=isolates",
    );
    expect(
      parseMicrobiologyCaseSearch(
        "?workflow=BACTERIOLOGY&urgency=HIGH&section=isolates",
      ),
    ).toEqual({
      workflow: "BACTERIOLOGY",
      urgency: "HIGH",
      due: "",
      sort: "priority",
      section: "isolates",
    });
  });
});
