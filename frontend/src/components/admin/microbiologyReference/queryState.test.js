import {
  buildReferenceQuery,
  parseReferenceQuery,
  updateReferenceQuery,
  validStatusesForSection,
} from "./queryState";

describe("microbiology reference query state", () => {
  it("round-trips supported state in a canonical order", () => {
    const parsed = parseReferenceQuery(
      "?pageSize=50&q=coli&status=ACTIVE&page=3&method=MIC",
    );
    expect(buildReferenceQuery(parsed)).toBe(
      "q=coli&status=ACTIVE&method=MIC&sort=name&page=3&pageSize=50",
    );
  });

  it("normalizes invalid paging, status, and sort values", () => {
    const parsed = parseReferenceQuery(
      "?page=0&pageSize=999&status=UNKNOWN&sort=unsafe",
    );
    expect(parsed.page).toBe(1);
    expect(parsed.pageSize).toBe(20);
    expect(parsed.status).toBe("ALL");
    expect(parsed.sort).toBe("name");
  });

  it("accepts lifecycle statuses only for breakpoint pages", () => {
    expect(
      parseReferenceQuery(
        "?status=LOADED",
        validStatusesForSection("breakpoints"),
      ).status,
    ).toBe("LOADED");
    expect(
      parseReferenceQuery(
        "?status=LOADED",
        validStatusesForSection("organisms"),
      ).status,
    ).toBe("ALL");
  });

  it("resets the page when a filter changes", () => {
    expect(updateReferenceQuery({ page: 4, q: "" }, { q: "eco" })).toEqual({
      page: 1,
      q: "eco",
    });
  });

  it("preserves paging when linkable editor state changes", () => {
    expect(
      updateReferenceQuery({ page: 4, edit: "" }, { edit: "organism-1" }),
    ).toEqual({ page: 4, edit: "organism-1" });
  });
});
