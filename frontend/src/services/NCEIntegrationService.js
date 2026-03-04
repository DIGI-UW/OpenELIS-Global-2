/**
 * NCE Integration Service
 *
 * API client for inline NCE creation, result-NCE associations,
 * delta check alert management, and quality badge endpoints.
 */

import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
  putToOpenElisServer,
} from "../components/utils/Utils";

// --- NCE Inline Reporting ---

/**
 * POST /rest/results/{resultId}/nce
 */
export const createNCEFromResult = (resultId, nceData, callback) => {
  postToOpenElisServerJsonResponse(
    `/rest/results/${resultId}/nce`,
    JSON.stringify(nceData),
    callback,
  );
};

/**
 * GET /rest/results/{resultId}/nce-badge
 */
export const getResultNCEBadge = (resultId, callback) => {
  getFromOpenElisServer(`/rest/results/${resultId}/nce-badge`, callback);
};

// --- Delta Check Alerts ---

/**
 * GET /rest/delta-check/alerts
 */
export const getDeltaCheckAlerts = (filters, callback) => {
  let endpoint = "/rest/delta-check/alerts";
  const params = new URLSearchParams();

  if (filters?.analysisIds?.length) {
    filters.analysisIds.forEach((id) => params.append("analysisIds", id));
  }
  if (filters?.status) {
    params.append("status", filters.status);
  }

  const queryString = params.toString();
  if (queryString) {
    endpoint += `?${queryString}`;
  }

  getFromOpenElisServer(endpoint, callback);
};

/**
 * PUT /rest/delta-check/alerts/{alertId}/dismiss
 */
export const dismissDeltaCheckAlert = (alertId, dismissData, callback) => {
  putToOpenElisServer(
    `/rest/delta-check/alerts/${alertId}/dismiss`,
    JSON.stringify(dismissData),
    callback,
  );
};

/**
 * POST /rest/delta-check/alerts/{alertId}/escalate-nce
 */
export const escalateDeltaCheckAlert = (alertId, escalateData, callback) => {
  postToOpenElisServerJsonResponse(
    `/rest/delta-check/alerts/${alertId}/escalate-nce`,
    JSON.stringify(escalateData),
    callback,
  );
};

// --- US4 Scenario 3: NCE Summary for FHIR Screens ---

/**
 * GET /rest/nce-summary?labOrderNumber=...
 * Used by FHIR patient result screens (US4 Scenario 3)
 */
export const getNCESummaryByLabOrder = (labOrderNumber, callback) => {
  getFromOpenElisServer(
    `/rest/nce-summary?labOrderNumber=${encodeURIComponent(labOrderNumber)}`,
    callback,
  );
};

// --- Prompt Dismissal Audit ---

/**
 * POST /rest/nce/prompt-dismissal
 * Records when a user dismisses a critical/extreme value alert without creating an NCE.
 */
export const recordPromptDismissal = (dismissalData, callback) => {
  postToOpenElisServerJsonResponse(
    "/rest/nce/prompt-dismissal",
    JSON.stringify(dismissalData),
    callback,
  );
};

// --- Per-Result Delta Check ---

/**
 * GET /rest/results/{resultId}/delta-check
 */
export const getResultDeltaCheck = (resultId, callback) => {
  getFromOpenElisServer(`/rest/results/${resultId}/delta-check`, callback);
};

// --- FR-020: Critical/Extreme Value Detection ---

const EXTREME_HIGH_MULTIPLIER = 3.0;
const EXTREME_LOW_MULTIPLIER = 0.3;

/**
 * Check if a result value is critical or extreme relative to normal range.
 * Returns { isCritical, isExtreme, reason } or null if no alert needed.
 *
 * @param {number|string} resultValue - The numeric result value
 * @param {Object} row - The result row with range fields:
 *   lowerCritical, higherCritical, lowerNormalRange, upperNormalRange
 */
export const checkCriticalOrExtremeValue = (resultValue, row) => {
  const value = parseFloat(String(resultValue).replace(/^[<>]/, ""));
  if (isNaN(value)) return null;

  // Check critical range first (most severe)
  if (
    row.lowerCritical &&
    row.higherCritical &&
    row.lowerCritical !== row.higherCritical
  ) {
    if (value < row.lowerCritical || value > row.higherCritical) {
      return {
        isCritical: true,
        isExtreme: false,
        reason: "critical",
      };
    }
  }

  // Check extreme values: >3x or <0.3x normal limits
  const lowNormal = parseFloat(row.lowerNormalRange);
  const highNormal = parseFloat(row.upperNormalRange);
  if (!isNaN(lowNormal) && !isNaN(highNormal) && lowNormal !== highNormal) {
    if (highNormal > 0 && value > highNormal * EXTREME_HIGH_MULTIPLIER) {
      return {
        isCritical: false,
        isExtreme: true,
        reason: "extreme_high",
      };
    }
    if (lowNormal > 0 && value < lowNormal * EXTREME_LOW_MULTIPLIER) {
      return {
        isCritical: false,
        isExtreme: true,
        reason: "extreme_low",
      };
    }
  }

  return null;
};
