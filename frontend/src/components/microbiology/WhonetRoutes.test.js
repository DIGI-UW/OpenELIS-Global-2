import {
  buildWhonetSearch,
  getWhonetMappingRepairUrl,
  parseWhonetSearch,
} from "./WhonetRoutes";

describe("WhonetRoutes", () => {
  const now = new Date(2026, 7, 4, 12, 0, 0);

  it("defaults to the previous complete month and emits every canonical field", () => {
    const state = parseWhonetSearch("", now);

    expect(state).toEqual({
      from: "2026-07-01",
      to: "2026-07-31",
      significance: "CLINICALLY_SIGNIFICANT",
      dedup: "FIRST_ISOLATE_7_DAY",
      step: "configure",
      page: 1,
      pageSize: 20,
    });
    expect(buildWhonetSearch(state, now)).toBe(
      "from=2026-07-01&to=2026-07-31&significance=CLINICALLY_SIGNIFICANT&dedup=FIRST_ISOLATE_7_DAY&step=configure&page=1&pageSize=20",
    );
  });

  it("normalizes unsupported values without dropping a valid reporting period", () => {
    expect(
      parseWhonetSearch(
        "?from=2026-06-01&to=2026-06-30&significance=UNKNOWN&dedup=UNKNOWN&step=preview&page=3&pageSize=50",
        now,
      ),
    ).toEqual({
      from: "2026-06-01",
      to: "2026-06-30",
      significance: "CLINICALLY_SIGNIFICANT",
      dedup: "FIRST_ISOLATE_7_DAY",
      step: "preview",
      page: 3,
      pageSize: 50,
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
});
