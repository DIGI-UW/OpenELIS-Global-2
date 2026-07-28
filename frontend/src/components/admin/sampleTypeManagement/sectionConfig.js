/**
 * Single source of truth for the Sample Type editor's section keys, in
 * SideNav order. Imported by both the editor (SampleTypeManagement) and the
 * global nav (AdminSideNav) so the routed sections and the nav items never
 * drift. Labels resolve via i18n `label.sampleType.section.{key}`.
 */
export const SAMPLE_TYPE_SECTIONS = [
  "basic-info",
  "associated-tests",
  "display-order",
  "disposal",
  "terminology",
];

export const DEFAULT_SAMPLE_TYPE_SECTION = SAMPLE_TYPE_SECTIONS[0]; // "basic-info"

export const isValidSampleTypeSection = (s) => SAMPLE_TYPE_SECTIONS.includes(s);
