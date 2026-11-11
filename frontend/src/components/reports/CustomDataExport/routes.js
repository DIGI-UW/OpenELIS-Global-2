export const REPORTS_PATH = "/reports";
export const CUSTOM_DATA_EXPORT_PATH = `${REPORTS_PATH}/custom-data-export`;
export const LEGACY_CUSTOM_DATA_EXPORT_PATH = "/CustomDataExport";
export const REPORTING_ROUTE_PATHS = [
  CUSTOM_DATA_EXPORT_PATH,
  REPORTS_PATH,
  LEGACY_CUSTOM_DATA_EXPORT_PATH,
];

// Normalize persisted menu destinations while older database rows and saved
// bookmarks transition to the canonical reporting workspace.
export function canonicalReportingUrl(url) {
  if (!url) return url;
  const raw = url.split(/[?#]/)[0];
  const path = raw.replace(/\/$/, "");
  // The canonical path is normalized too, not just the legacy aliases. Opening
  // the workspace at /reports/custom-data-export/ otherwise returns the
  // trailing-slash form unchanged, so callers comparing against
  // CUSTOM_DATA_EXPORT_PATH miss, and reportingMenuDestination skips its
  // query-merging branch and drops preserved review parameters.
  if (
    path === LEGACY_CUSTOM_DATA_EXPORT_PATH ||
    path === REPORTS_PATH ||
    path === CUSTOM_DATA_EXPORT_PATH
  ) {
    return CUSTOM_DATA_EXPORT_PATH + url.slice(raw.length);
  }
  return url;
}
