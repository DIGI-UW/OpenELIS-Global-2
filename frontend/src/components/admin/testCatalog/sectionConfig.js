/**
 * OGC-949 / #3504 — single source of truth for the Test Catalog editor's v1
 * section keys, in SideNav order. Imported by both the editor
 * (TestCatalogEditor) and the global nav (AdminSideNav) so the routed sections
 * and the nav items never drift. Labels resolve via i18n
 * `label.testCatalog.section.{key}`. Domain-independent in v1 (mirrors the
 * backend envelope's applicableSections).
 *
 * Built sections: basic-info, sample-results, methods, ranges, storage.
 * Placeholder until their own milestones (M9–M12): panels, terminology,
 * analyzers, display-order — they route + show the "pending" placeholder.
 */
export const V1_SECTIONS = [
  "basic-info",
  "sample-results",
  "methods",
  "ranges",
  "storage",
  "panels",
  "labels",
  "terminology",
  "reagents",
  "analyzers",
  "alerts",
  "reflex-calc",
  "localization",
  "display-order",
];

export const DEFAULT_SECTION = V1_SECTIONS[0]; // "basic-info"

export const isValidSection = (s) => V1_SECTIONS.includes(s);
