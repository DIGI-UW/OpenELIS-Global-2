/**
 * OGC-1223 FR-11: one helper renders a provider's name wherever a screen has
 * room for a single string, dropping a missing part cleanly rather than
 * leaving a leading space or a stray comma.
 */

import {
  providerDisplayName,
  titledProviderName,
  titledProviderNameFamilyFirst,
} from "./providerDisplayName";

describe("titledProviderName", () => {
  it("reads title, given name, family name", () => {
    expect(titledProviderName("Dr", "John", "Kila")).toBe("Dr John Kila");
  });

  it("leaves no leading space when there is no title", () => {
    expect(titledProviderName(null, "John", "Kila")).toBe("John Kila");
    expect(titledProviderName("", "John", "Kila")).toBe("John Kila");
    expect(titledProviderName("  ", "John", "Kila")).toBe("John Kila");
  });

  it("leaves no double space when a name part is missing", () => {
    expect(titledProviderName("Dr", null, "Kila")).toBe("Dr Kila");
    expect(titledProviderName("Dr", "John", null)).toBe("Dr John");
  });

  it("is empty rather than blank when nothing is known", () => {
    expect(titledProviderName(null, null, null)).toBe("");
  });

  it("trims what it is given", () => {
    expect(titledProviderName(" Dr ", " John ", " Kila ")).toBe("Dr John Kila");
  });
});

describe("titledProviderNameFamilyFirst", () => {
  it("keeps the title with the given name", () => {
    expect(titledProviderNameFamilyFirst("Dr", "John", "Kila")).toBe(
      "Kila, Dr John",
    );
  });

  it("drops the comma when there is no given name", () => {
    expect(titledProviderNameFamilyFirst(null, null, "Kila")).toBe("Kila");
  });

  it("reads as before when there is no title", () => {
    expect(titledProviderNameFamilyFirst(null, "John", "Kila")).toBe(
      "Kila, John",
    );
  });
});

describe("providerDisplayName", () => {
  it("prefers the resolved abbreviation over the raw code", () => {
    expect(
      providerDisplayName({
        titleCode: "DR",
        titleAbbreviation: "Dr",
        firstName: "John",
        lastName: "Kila",
      }),
    ).toBe("Dr John Kila");
  });

  it("falls back to the code when the title is no longer configured", () => {
    expect(
      providerDisplayName({
        titleCode: "HEO",
        firstName: "Anna",
        lastName: "Wanpis",
      }),
    ).toBe("HEO Anna Wanpis");
  });

  it("is empty for no provider at all", () => {
    expect(providerDisplayName(null)).toBe("");
  });
});
