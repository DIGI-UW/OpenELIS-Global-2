/**
 * OGC-224 — single source of truth for the Panel editor's section keys, in
 * SideNav order (FRS v2.2: Basic Info / Tests / Terminology as SideNav
 * submenus, not tabs). Imported by the editor (PanelEditor) and the global
 * nav (AdminSideNav) so the routed sections and the nav items never drift.
 * Labels resolve via i18n `label.panel.section.{key}`.
 *
 * The registry grows with the OGC-224 slices: basic-info (C2), tests (C3),
 * terminology (C4). Vector Config is deferred to the Environmental/Vector
 * phase and is not registered while only Clinical is enabled.
 */
export const PANEL_SECTIONS = ["basic-info", "tests"];

export const DEFAULT_PANEL_SECTION = PANEL_SECTIONS[0];

export const isValidPanelSection = (s) => PANEL_SECTIONS.includes(s);
