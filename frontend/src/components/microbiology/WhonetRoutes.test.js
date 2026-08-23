import {
  buildWhonetSearch,
  clearWhonetWorklistScope,
  getWhonetMappingRepairUrl,
  getWhonetDateRange,
  getWhonetExportUrlFromWorklist,
  parseWhonetSearch,
} from "./WhonetRoutes";

describe("WhonetRoutes", () => {
  const now = new Date(2026, 7, 4, 12, 0, 0);

  it("defaults to the previous complete month and emits every canonical field", () => {
    const state = parseWhonetSearch("", now);

    expect(state).toEqual({
      from: "2026-07-01",
      to: "2026-07-31",
      specimen: [],
      organism: [],
      origin: [],
      significance: ["CLINICALLY_SIGNIFICANT"],
      includeScreening: false,
      includeUnspecified: false,
      dedup: "FIRST_ISOLATE_7_DAY",
      step: "configure",
      page: 1,
      pageSize: 20,
      source: "",
    });
    expect(buildWhonetSearch(state, now)).toBe(
      "from=2026-07-01&to=2026-07-31&significance=CLINICALLY_SIGNIFICANT&includeScreening=false&includeUnspecified=false&dedup=FIRST_ISOLATE_7_DAY&step=configure&page=1&pageSize=20",
    );
  });

  it("uses full calendar boundaries for every reporting-period preset", () => {
    expect(getWhonetDateRange("THIS_MONTH", now)).toEqual({
      from: "2026-08-01",
      to: "2026-08-31",
    });
    expect(getWhonetDateRange("LAST_MONTH", now)).toEqual({
      from: "2026-07-01",
      to: "2026-07-31",
    });
    expect(getWhonetDateRange("THIS_QUARTER", now)).toEqual({
      from: "2026-07-01",
      to: "2026-09-30",
    });
  });

  it("defaults a worklist-sourced entry without dates to this month", () => {
    expect(parseWhonetSearch("?source=ast-worklist", now)).toMatchObject({
      from: "2026-08-01",
      to: "2026-08-31",
      source: "ast-worklist",
    });
  });

  it("carries only structured surveillance scope from the AST worklist", () => {
    expect(
      getWhonetExportUrlFromWorklist(
        {
          grain: "ast",
          from: "2026-08-01",
          to: "2026-08-31",
          specimen: ["urine", "blood"],
          organism: ["organism-2"],
          origin: ["INPATIENT"],
          significance: ["NORMAL_FLORA"],
          status: "results-in",
          workflow: "BACTERIOLOGY",
          urgency: "HIGH",
          q: "LAB-001",
          sort: "newest",
          page: 4,
          pageSize: 50,
        },
        now,
      ),
    ).toBe(
      "/Microbiology/whonet?from=2026-08-01&to=2026-08-31&specimen=blood&specimen=urine&organism=organism-2&origin=INPATIENT&significance=NORMAL_FLORA&includeScreening=false&includeUnspecified=false&dedup=FIRST_ISOLATE_7_DAY&source=ast-worklist&step=configure&page=1&pageSize=20",
    );
  });

  it("clears worklist provenance and restores direct Reports defaults", () => {
    const worklistState = parseWhonetSearch(
      "?from=2026-08-01&to=2026-08-31&specimen=blood&organism=organism-1&origin=INPATIENT&significance=NORMAL_FLORA&source=ast-worklist",
      now,
    );

    expect(clearWhonetWorklistScope(worklistState, now)).toEqual(
      parseWhonetSearch("", now),
    );
  });

  it("round-trips sorted repeated population filters without dropping selections", () => {
    const search =
      "?from=2026-06-01&to=2026-06-30&specimen=urine&specimen=blood&organism=org-2&organism=org-1&origin=OUTPATIENT&significance=NORMAL_FLORA&significance=CLINICALLY_SIGNIFICANT&includeScreening=true&includeUnspecified=true&dedup=NONE&step=preview&page=3&pageSize=50";

    const state = parseWhonetSearch(search, now);

    expect(state).toMatchObject({
      specimen: ["blood", "urine"],
      organism: ["org-1", "org-2"],
      origin: ["OUTPATIENT"],
      significance: ["CLINICALLY_SIGNIFICANT", "NORMAL_FLORA"],
      includeScreening: true,
      includeUnspecified: true,
    });
    expect(buildWhonetSearch(state, now)).toBe(
      "from=2026-06-01&to=2026-06-30&specimen=blood&specimen=urine&organism=org-1&organism=org-2&origin=OUTPATIENT&significance=CLINICALLY_SIGNIFICANT&significance=NORMAL_FLORA&includeScreening=true&includeUnspecified=true&dedup=NONE&step=preview&page=3&pageSize=50",
    );
  });

  it("preserves the meaning of legacy all-isolate links", () => {
    const state = parseWhonetSearch("?significance=ALL", now);

    expect(state.significance).toEqual([
      "CLINICALLY_SIGNIFICANT",
      "CONTAMINANT",
      "NORMAL_FLORA",
      "UNKNOWN",
    ]);
    expect(buildWhonetSearch(state, now)).toContain(
      "significance=CLINICALLY_SIGNIFICANT&significance=CONTAMINANT&significance=NORMAL_FLORA&significance=UNKNOWN",
    );
  });

  it("normalizes unsupported values without dropping a valid reporting period", () => {
    expect(
      parseWhonetSearch(
        "?from=2026-06-01&to=2026-06-30&significance=INVALID&dedup=UNKNOWN&step=preview&page=3&pageSize=50",
        now,
      ),
    ).toEqual({
      from: "2026-06-01",
      to: "2026-06-30",
      specimen: [],
      organism: [],
      origin: [],
      significance: ["CLINICALLY_SIGNIFICANT"],
      includeScreening: false,
      includeUnspecified: false,
      dedup: "FIRST_ISOLATE_7_DAY",
      step: "preview",
      page: 3,
      pageSize: 50,
      source: "",
    });
  });

  it("replaces impossible calendar dates with the previous complete month", () => {
    expect(
      parseWhonetSearch("?from=2026-02-30&to=2026-04-31", now),
    ).toMatchObject({
      from: "2026-07-01",
      to: "2026-07-31",
    });
  });

  it("builds exact mapping repair URLs while browser history retains preview context", () => {
    expect(getWhonetMappingRepairUrl("organisms", "organism / 1")).toBe(
      "/MasterListsPage/MicrobiologyReference/organisms?edit=organism+%2F+1",
    );
    expect(getWhonetMappingRepairUrl("antibiotics", "antibiotic-1")).toBe(
      "/MasterListsPage/MicrobiologyReference/antibiotics?edit=antibiotic-1",
    );
    expect(
      buildWhonetSearch(parseWhonetSearch("?step=preview", now), now),
    ).toContain("step=preview");
  });

  it("builds specimen repair URLs with an exact local preview return", () => {
    const returnTo =
      "/Microbiology/whonet?from=2026-07-01&to=2026-07-31&significance=CLINICALLY_SIGNIFICANT&dedup=FIRST_ISOLATE_7_DAY&step=preview&page=2&pageSize=50";

    expect(
      getWhonetMappingRepairUrl("specimen-types", "sample type / 1", returnTo),
    ).toBe(
      "/MasterListsPage/SampleTypeManagement/sample%20type%20%2F%201/basic-info?focus=whonet&returnTo=%2FMicrobiology%2Fwhonet%3Ffrom%3D2026-07-01%26to%3D2026-07-31%26significance%3DCLINICALLY_SIGNIFICANT%26dedup%3DFIRST_ISOLATE_7_DAY%26step%3Dpreview%26page%3D2%26pageSize%3D50",
    );
  });

  it("does not preserve external return destinations", () => {
    expect(
      getWhonetMappingRepairUrl(
        "specimen-types",
        "sample-type-1",
        "https://example.org/not-openelis",
      ),
    ).toBe(
      "/MasterListsPage/SampleTypeManagement/sample-type-1/basic-info?focus=whonet",
    );
  });
});
