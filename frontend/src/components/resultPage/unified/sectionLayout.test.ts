import {
  isSectionOpen,
  loadSectionLayout,
  rememberSectionChoice,
  resetSectionLayout,
} from "./sectionLayout";

/**
 * OGC-1021 (R2) — FR-C4 sticky layout precedence:
 * remembered user choice > per-result auto-open > collapsed.
 */

function fakeStorage(): Storage {
  const data: Record<string, string> = {};
  return {
    getItem: (k: string) => (k in data ? data[k] : null),
    setItem: (k: string, v: string) => {
      data[k] = v;
    },
    removeItem: (k: string) => {
      delete data[k];
    },
    clear: () => Object.keys(data).forEach((k) => delete data[k]),
    key: () => null,
    get length() {
      return Object.keys(data).length;
    },
  } as Storage;
}

describe("sectionLayout (FR-C4)", () => {
  it("defaults to collapsed when nothing is remembered and no auto-open", () => {
    expect(isSectionOpen({}, "orderInfo", false)).toBe(false);
  });

  it("auto-opens a section with notable content when nothing is remembered", () => {
    expect(isSectionOpen({}, "interpretation", true)).toBe(true);
  });

  it("a remembered choice beats auto-open in both directions", () => {
    // user explicitly closed it — auto-open must not reopen it
    expect(
      isSectionOpen({ interpretation: false }, "interpretation", true),
    ).toBe(false);
    // user explicitly opened it — stays open without auto-open
    expect(isSectionOpen({ orderInfo: true }, "orderInfo", false)).toBe(true);
  });

  it("persists choices across a reload (same storage)", () => {
    const storage = fakeStorage();
    rememberSectionChoice("orderInfo", true, storage);
    rememberSectionChoice("storage", false, storage);
    const reloaded = loadSectionLayout(storage);
    expect(reloaded).toEqual({ orderInfo: true, storage: false });
  });

  it("reset drops every remembered choice (FR-C4 Reset layout)", () => {
    const storage = fakeStorage();
    rememberSectionChoice("orderInfo", true, storage);
    const cleared = resetSectionLayout(storage);
    expect(cleared).toEqual({});
    expect(loadSectionLayout(storage)).toEqual({});
  });

  it("survives corrupted storage content", () => {
    const storage = fakeStorage();
    storage.setItem("oe.results.sectionLayout.v1", "{not json");
    expect(loadSectionLayout(storage)).toEqual({});
  });
});
