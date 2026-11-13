/**
 * OGC-1418 — Validation menu entries retired when Validation became one page
 * with one search. Liquibase deactivates them, and the admin menu pages leave
 * them out, so they cannot be switched back on there to bring a second way
 * into Validation back.
 */
export const RETIRED_VALIDATION_MENU_IDS = new Set([
  "menu_resultvalidation_routine",
  "menu_accession_validation",
  "menu_accession_validation_range",
  "menu_resultvalidation_date",
  "menu_resultvalidation_study",
  "menu_resultvalidation_immunology",
  "menu_resultvalidation_biochemistry",
  "menu_resultvalidation_serology",
  "menu_resultvalidation_virology",
  "menu_resultvalidation_dnapcr",
  "menu_resultvalidation_viralload",
  "menu_resultvalidation_genotyping",
]);

/** The menu tree without the retired entries, at every level. */
export function withoutRetiredMenus(items) {
  return (items || [])
    .filter(({ menu }) => !RETIRED_VALIDATION_MENU_IDS.has(menu?.elementId))
    .map((item) => ({
      ...item,
      childMenus: withoutRetiredMenus(item.childMenus),
    }));
}
