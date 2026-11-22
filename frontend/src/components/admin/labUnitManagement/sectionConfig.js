/**
 * Single source of truth for the Lab Unit editor's section keys, in SideNav
 * order. Imported by both the editor (LabUnitManagement) and the global nav
 * (AdminSideNav) so the routed sections and the nav items never drift.
 * Labels resolve via i18n `label.labUnit.section.{key}`.
 */
export const LAB_UNIT_SECTIONS = [
  "basic-info",
  "assigned-tests",
  "display-order",
];

export const DEFAULT_LAB_UNIT_SECTION = LAB_UNIT_SECTIONS[0]; // "basic-info"

export const isValidLabUnitSection = (s) => LAB_UNIT_SECTIONS.includes(s);
