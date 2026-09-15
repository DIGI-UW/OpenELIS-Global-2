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
  const path = url.split(/[?#]/)[0].replace(/\/$/, "");
  if (path === LEGACY_CUSTOM_DATA_EXPORT_PATH || path === REPORTS_PATH) {
    return CUSTOM_DATA_EXPORT_PATH + url.slice(url.split(/[?#]/)[0].length);
  }
  return url;
}
