import { getFromOpenElisServer } from "../../utils/Utils";

/**
 * REST client for the Vector Surveillance reporting endpoints (OGC-585 / V-04).
 * Mirrors the modern Reports-page pattern (see TATReport) — thin wrappers over
 * {@link getFromOpenElisServer}. All figures are computed on demand by the
 * backend from OpenELIS's own data (FR-011); this client is read-only.
 *
 * Endpoints (see contracts/vector-surveillance-api.yaml):
 *   GET /rest/reports/vector-surveillance/indices?dateFrom&dateTo&siteId
 *   GET /rest/reports/vector-surveillance/sites
 */

/**
 * Build the query string for the /indices endpoint. Dates are expected to be
 * already URL-encoded (via encodeDate, as ReportByDate does). siteId is
 * optional — omitted entirely when not selected (all-sites scope).
 */
export const buildIndicesQueryString = ({ dateFrom, dateTo, siteId }) => {
  const params = [];
  if (dateFrom) params.push(`dateFrom=${dateFrom}`);
  if (dateTo) params.push(`dateTo=${dateTo}`);
  if (siteId) params.push(`siteId=${siteId}`);
  return params.join("&");
};

/**
 * Fetch the computed surveillance indices for a scope.
 *
 * @param {{dateFrom: string, dateTo: string, siteId?: (string|number)}} scope
 * @param {(payload: (object|undefined)) => void} callback receives the
 *   SurveillanceIndicesDTO payload, or undefined on network/parse error.
 */
export const getSurveillanceIndices = (scope, callback) => {
  const qs = buildIndicesQueryString(scope);
  getFromOpenElisServer(
    `/rest/reports/vector-surveillance/indices?${qs}`,
    callback,
  );
};

/**
 * Fetch the sampling sites available as a filter option (SiteOption[]).
 *
 * @param {(sites: (Array|undefined)) => void} callback
 */
export const getSurveillanceSites = (callback) => {
  getFromOpenElisServer(`/rest/reports/vector-surveillance/sites`, callback);
};
