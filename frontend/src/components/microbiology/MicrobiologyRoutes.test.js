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
        stage: "AST_IN_PROGRESS",
        due: "AST_REVIEW",
        q: "blood culture",
        sort: "newest",
        page: 3,
        pageSize: 50,
      }),
    ).toBe(
      "/Microbiology/worklist?workflow=BACTERIOLOGY&stage=AST_IN_PROGRESS&urgency=HIGH&due=AST_REVIEW&q=blood+culture&sort=newest&page=3&pageSize=50",
    );
  });

  it("drops unsupported worklist state while parsing", () => {
    expect(
      parseMicrobiologyWorklistSearch(
        "?workflow=BACTERIOLOGY&sort=unsupported&unknown=value",
      ),
    ).toEqual({
      workflow: "BACTERIOLOGY",
      stage: "",
      urgency: "",
      due: "",
      q: "",
      sort: "priority",
      page: 1,
      pageSize: 20,
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
      stage: "",
      due: "",
      q: "",
      sort: "priority",
      page: 1,
      pageSize: 20,
      section: "isolates",
    });
  });

  it("keeps the amendment workflow addressable in the case URL", () => {
    expect(getMicrobiologyCaseUrl("case-1", { section: "amendment" })).toBe(
      "/Microbiology/cases/case-1?section=amendment",
    );
    expect(parseMicrobiologyCaseSearch("?section=amendment").section).toBe(
      "amendment",
    );
  });
});
