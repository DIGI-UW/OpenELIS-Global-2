import {
  getMicrobiologyCaseUrl,
  getMicrobiologyWorklistUrl,
  parseMicrobiologyCaseSearch,
  parseMicrobiologyWorklistSearch,
} from "./MicrobiologyRoutes";

describe("MicrobiologyRoutes", () => {
  const now = new Date(2026, 7, 4, 12, 0, 0);

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

  it("composes a canonical AST grain and status before shared filters", () => {
    expect(
      getMicrobiologyWorklistUrl(
        {
          grain: "ast",
          status: "results-in",
          urgency: "HIGH",
          q: "E. coli",
        },
        now,
      ),
    ).toBe(
      "/Microbiology/worklist?grain=ast&status=results-in&from=2026-08-01&to=2026-08-31&urgency=HIGH&q=E.+coli",
    );
  });

  it("defaults an AST worklist period to this month and round-trips surveillance filters", () => {
    const state = parseMicrobiologyWorklistSearch(
      "?grain=ast&specimen=urine&specimen=blood&origin=INPATIENT&organism=organism-1&significance=NORMAL_FLORA",
      now,
    );

    expect(state).toMatchObject({
      from: "2026-08-01",
      to: "2026-08-31",
      specimen: ["blood", "urine"],
      origin: ["INPATIENT"],
      organism: ["organism-1"],
      significance: ["NORMAL_FLORA"],
    });
    expect(getMicrobiologyWorklistUrl(state, now)).toBe(
      "/Microbiology/worklist?grain=ast&from=2026-08-01&to=2026-08-31&specimen=blood&specimen=urine&organism=organism-1&origin=INPATIENT&significance=NORMAL_FLORA",
    );
  });

  it("preserves the reviewed AST view as canonical worklist state", () => {
    const url = getMicrobiologyWorklistUrl(
      {
        grain: "ast",
        status: "reviewed",
        q: "LAB-1001",
      },
      now,
    );

    expect(url).toBe(
      "/Microbiology/worklist?grain=ast&status=reviewed&from=2026-08-01&to=2026-08-31&q=LAB-1001",
    );
    expect(parseMicrobiologyWorklistSearch(url.split("?")[1])).toMatchObject({
      grain: "ast",
      status: "reviewed",
      q: "LAB-1001",
    });
  });

  it("drops unsupported worklist state while parsing", () => {
    expect(
      parseMicrobiologyWorklistSearch(
        "?workflow=BACTERIOLOGY&sort=unsupported&unknown=value",
      ),
    ).toEqual({
      grain: "cultures",
      status: "",
      from: "",
      to: "",
      specimen: [],
      organism: [],
      origin: [],
      significance: [],
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

  it("drops a status that does not belong to the selected grain", () => {
    expect(
      parseMicrobiologyWorklistSearch("?grain=ast&status=growth"),
    ).toMatchObject({
      grain: "ast",
      status: "",
    });
    expect(
      parseMicrobiologyWorklistSearch("?grain=cultures&status=results-in"),
    ).toMatchObject({
      grain: "cultures",
      status: "",
      from: "",
      to: "",
      specimen: [],
      organism: [],
      origin: [],
      significance: [],
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
      grain: "cultures",
      status: "",
      from: "",
      to: "",
      specimen: [],
      organism: [],
      origin: [],
      significance: [],
      workflow: "BACTERIOLOGY",
      urgency: "HIGH",
      stage: "",
      due: "",
      q: "",
      sort: "priority",
      page: 1,
      pageSize: 20,
      section: "isolates",
      action: "",
      targetType: "",
      targetId: "",
      astRunId: "",
      astIsolateId: "",
    });
  });

  it("preserves the exact AST run and worklist grain in a case URL", () => {
    const url = getMicrobiologyCaseUrl(
      "case-1",
      {
        grain: "ast",
        status: "results-in",
        section: "ast",
        astIsolateId: "isolate-1",
        astRunId: "run / 1",
      },
      now,
    );

    expect(url).toBe(
      "/Microbiology/cases/case-1?grain=ast&status=results-in&from=2026-08-01&to=2026-08-31&section=ast&astIsolateId=isolate-1&astRunId=run+%2F+1",
    );
    expect(parseMicrobiologyCaseSearch(url.split("?")[1])).toMatchObject({
      grain: "ast",
      status: "results-in",
      section: "ast",
      astIsolateId: "isolate-1",
      astRunId: "run / 1",
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

  it("preserves a fixed critical communication target in canonical case state", () => {
    const url = getMicrobiologyCaseUrl("case-1", {
      section: "critical-communication",
      action: "log-critical",
      targetType: "ISOLATE",
      targetId: "isolate-1",
    });

    expect(url).toBe(
      "/Microbiology/cases/case-1?section=critical-communication&action=log-critical&targetType=ISOLATE&targetId=isolate-1",
    );
    expect(parseMicrobiologyCaseSearch(url.split("?")[1])).toMatchObject({
      section: "critical-communication",
      action: "log-critical",
      targetType: "ISOLATE",
      targetId: "isolate-1",
    });
  });

  it("keeps nonconformance actions in canonical case state", () => {
    expect(
      getMicrobiologyCaseUrl("case-1", {
        section: "nonconformance",
        action: "report-nce",
      }),
    ).toBe(
      "/Microbiology/cases/case-1?section=nonconformance&action=report-nce",
    );
    expect(
      parseMicrobiologyCaseSearch("?section=nonconformance&action=mark-lost"),
    ).toMatchObject({
      section: "nonconformance",
      action: "mark-lost",
      targetType: "",
      targetId: "",
    });
  });

  it("keeps culture observation actions in canonical case state", () => {
    expect(
      getMicrobiologyCaseUrl("case-1", {
        section: "setup",
        action: "mark-positive",
      }),
    ).toBe("/Microbiology/cases/case-1?section=setup&action=mark-positive");
    expect(
      parseMicrobiologyCaseSearch("?section=setup&action=mark-no-growth"),
    ).toMatchObject({
      section: "setup",
      action: "mark-no-growth",
    });
  });

  it("keeps inoculation form actions in canonical case state", () => {
    const url = getMicrobiologyCaseUrl("case-1", {
      q: "UATMICRO001",
      sort: "newest",
      section: "setup",
      action: "start-inoculation",
    });

    expect(url).toBe(
      "/Microbiology/cases/case-1?q=UATMICRO001&sort=newest&section=setup&action=start-inoculation",
    );
    expect(parseMicrobiologyCaseSearch(url.split("?")[1])).toMatchObject({
      q: "UATMICRO001",
      sort: "newest",
      section: "setup",
      action: "start-inoculation",
    });
  });

  it("keeps the protocol-only bench action in canonical case state", () => {
    expect(
      getMicrobiologyCaseUrl("case-1", {
        section: "setup",
        action: "change-protocol",
      }),
    ).toBe("/Microbiology/cases/case-1?section=setup&action=change-protocol");
    expect(
      parseMicrobiologyCaseSearch("?section=setup&action=set-protocol"),
    ).toMatchObject({
      section: "setup",
      action: "set-protocol",
    });
  });

  it("keeps a new AST attempt focused on its source run", () => {
    const url = getMicrobiologyCaseUrl(
      "case-1",
      {
        grain: "ast",
        section: "ast",
        action: "new-ast-attempt",
        astIsolateId: "isolate-1",
        astRunId: "run-1",
      },
      now,
    );

    expect(url).toBe(
      "/Microbiology/cases/case-1?grain=ast&from=2026-08-01&to=2026-08-31&section=ast&astIsolateId=isolate-1&astRunId=run-1&action=new-ast-attempt",
    );
    expect(parseMicrobiologyCaseSearch(url.split("?")[1])).toMatchObject({
      grain: "ast",
      section: "ast",
      action: "new-ast-attempt",
      astIsolateId: "isolate-1",
      astRunId: "run-1",
    });
  });
});
