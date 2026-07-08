import { getFromOpenElisServer } from "../../utils/Utils";

/**
 * Shared NCE client-filter util for the QA Overview (OGC-699 WS-C).
 *
 * There is no acknowledgment column on nce_event: acknowledging an NCE
 * transitions status "Pending" -> "Under Investigation" (NceEnhancement
 * REST controller), so "critical pending acknowledgment" is exactly
 * severity CRITICAL + status Pending. WS-F reuses these predicates for
 * the QMS pillar chip and This-Week counters.
 */

export const NCE_DRILL_URL = "/NceDashboard?severity=CRITICAL&status=Pending";

// Deduped fetch: overview slots mounting together share one request; the
// cache clears on resolve so a fresh mount refetches current data.
let inflight = null;
export const fetchNceList = (callback) => {
  if (!inflight) {
    const request = new Promise((resolve) => {
      getFromOpenElisServer("/rest/nce/dashboard", (data) =>
        resolve(data && Array.isArray(data.nceList) ? data.nceList : null),
      );
    });
    inflight = request;
    // Clear after settle (identity-guarded) so the reset survives callbacks
    // that fire synchronously, e.g. cached responses or test mocks.
    request.then(() => {
      if (inflight === request) {
        inflight = null;
      }
    });
  }
  inflight.then(callback);
};

export const countCriticalPending = (list) =>
  list.filter((nce) => nce.severity === "CRITICAL" && nce.status === "Pending")
    .length;

export const countInCorrectiveAction = (list) =>
  list.filter((nce) => nce.status === "Corrective Action").length;

// ponytail: v1 hard-coded thresholds (0 green, 1-4 amber, >=5 red) per
// OGC-699; per-lab configuration arrives with QI config in v8.
export const pulseColor = (count) =>
  count === 0 ? "green" : count < 5 ? "amber" : "red";
