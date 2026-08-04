import {
  buildReferenceQuery,
  parseReferenceQuery,
  updateReferenceQuery,
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

  it("normalizes invalid paging values", () => {
    const parsed = parseReferenceQuery("?page=0&pageSize=999");
    expect(parsed.page).toBe(1);
    expect(parsed.pageSize).toBe(20);
  });

  it("resets the page when a filter changes", () => {
    expect(updateReferenceQuery({ page: 4, q: "" }, { q: "eco" })).toEqual({
      page: 1,
      q: "eco",
    });
  });
});
