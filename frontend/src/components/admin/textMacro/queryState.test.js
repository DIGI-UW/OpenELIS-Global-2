import {
  buildMacroLibraryQuery,
  DEFAULT_MACRO_LIBRARY_QUERY,
  parseMacroLibraryQuery,
  updateMacroLibraryQuery,
} from "./queryState";

describe("macro library query state", () => {
  it("normalizes all bookmarkable list and action state", () => {
    expect(
      parseMacroLibraryQuery(
        "?q=gpc&context=MICROBIOLOGY_CULTURE_ACTIVITY&status=inactive&sort=updated%3Adesc&page=3&pageSize=50&edit=macro-1",
      ),
    ).toEqual({
      q: "gpc",
      context: "MICROBIOLOGY_CULTURE_ACTIVITY",
      status: "inactive",
      sort: "updated:desc",
      page: 3,
      pageSize: 50,
      edit: "macro-1",
    });
  });

  it("canonicalizes invalid values and resets page when filters change", () => {
    expect(parseMacroLibraryQuery("?status=bad&page=0&pageSize=17")).toEqual(
      DEFAULT_MACRO_LIBRARY_QUERY,
    );
    expect(
      updateMacroLibraryQuery(
        { ...DEFAULT_MACRO_LIBRARY_QUERY, page: 4 },
        { q: "growth" },
      ),
    ).toEqual({ ...DEFAULT_MACRO_LIBRARY_QUERY, q: "growth" });
  });

  it("round-trips create state and preserves the list query", () => {
    const query = {
      ...DEFAULT_MACRO_LIBRARY_QUERY,
      q: "growth",
      status: "all",
      edit: "new",
    };
    expect(parseMacroLibraryQuery(`?${buildMacroLibraryQuery(query)}`)).toEqual(
      query,
    );
  });
});
