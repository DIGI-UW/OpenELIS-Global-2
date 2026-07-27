import { getFromOpenElisServer, toLocalIsoDate } from "../../utils/Utils";
import { tatDelta } from "../../reports/tat/tatUtils";

/**
 * Shared data helpers for the QA Overview aggregators (OGC-694 WS-F).
 *
 * Server aggregates (QC, EQA, audit, e-sig) come from one deduped fetch of
 * /rest/qa/overview/summary. NCE-derived counters reuse the nceOverview.js
 * fetch and are computed client-side — the dashboard payload already carries
 * every event's full history, so no new backend is needed for them.
 */

/**
 * Dedupe a callback-style fetch: overview slots mounting together share one
 * request; the cache clears on settle (identity-guarded so the reset survives
 * synchronous callbacks) and a fresh mount refetches current data.
 */
export const dedupedFetch = (run) => {
  let inflight = null;
  return (callback) => {
    if (!inflight) {
      const request = new Promise(run);
      inflight = request;
      request.then(() => {
        if (inflight === request) {
          inflight = null;
        }
      });
    }
    inflight.then(callback);
  };
};

export const fetchOverviewSummary = dedupedFetch((resolve) => {
  getFromOpenElisServer("/rest/qa/overview/summary", (data) =>
    resolve(data && data.week ? data : null),
  );
});

// C.4 critical-callback compliance summary for a window (OGC-714/715):
// {enabled, criticalCount, confirmedCount, compliancePercent, target}. When
// the CALLBACK indicator is disabled the response says enabled=false —
// callers hide their surface (same cascade as the QI Dashboard tile).
export const fetchCallbackSummary = (fromDate, toDate, callback) => {
  getFromOpenElisServer(
    `/rest/critical-callback/summary?fromDate=${fromDate}&toDate=${toDate}`,
    (res) => callback(res ?? null),
  );
};

// ---- Week window ----

// Local-Monday fallback, used only when the summary fetch yields no server
// boundary; when the summary is available its week.weekStart/weekStartInstant
// win so all This-Week counters share the server's window.
export const weekStart = (now = new Date()) => {
  const d = new Date(now);
  d.setDate(d.getDate() - ((d.getDay() + 6) % 7));
  d.setHours(0, 0, 0, 0);
  return d;
};

const pad = (n) => String(n).padStart(2, "0");
const isoDate = (d) =>
  `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;

// ---- NCE weekly counters (over the nceOverview.fetchNceList payload) ----

// reportDate is a plain yyyy-mm-dd string; compare as strings to avoid
// UTC-midnight parsing skew at the Monday boundary.
export const newNcesThisWeek = (list, weekStartDate = isoDate(weekStart())) =>
  list.filter((nce) => nce.reportDate && nce.reportDate >= weekStartDate);

export const severityBreakdown = (list) => {
  const counts = { critical: 0, major: 0, minor: 0 };
  list.forEach((nce) => {
    if (nce.severity === "CRITICAL") counts.critical++;
    else if (nce.severity === "MAJOR") counts.major++;
    else if (nce.severity === "MINOR" || nce.severity === "LOW") counts.minor++;
  });
  return counts;
};

// The legacy QMS workflow writes status "Completed" (activity RESOLVED); the
// NCE register vocabulary uses "Closed". Prefer the structured newValue the
// worker records on transitions; fall back to the description for older rows.
const RESOLUTION_RE = /closed|completed|resolved/i;
const isResolutionEvent = (h) =>
  h.activity === "RESOLVED" ||
  (h.activity === "STATUS_CHANGED" &&
    RESOLUTION_RE.test(h.newValue || h.description || ""));

const resolvedSince = (nce, sinceMs) =>
  (nce.history || []).some(
    (h) =>
      h.timestamp && Date.parse(h.timestamp) >= sinceMs && isResolutionEvent(h),
  );

export const ncesResolvedThisWeek = (
  list,
  weekStartMs = weekStart().getTime(),
) => list.filter((nce) => resolvedSince(nce, weekStartMs)).length;

// ponytail: "CAPAs completed" is closed-count only (no effective/pending-review
// split) until OGC-707 adds CAPA verification; a CAPA trail is any corrective
// action or CAPA status mention in the event history.
const CAPA_RE = /capa|corrective/i;
const hasCapaTrail = (nce) =>
  (nce.history || []).some(
    (h) =>
      h.activity === "CORRECTIVE_ACTION" ||
      CAPA_RE.test(h.newValue || h.description || ""),
  );

export const capasCompletedThisWeek = (
  list,
  weekStartMs = weekStart().getTime(),
) =>
  list.filter((nce) => resolvedSince(nce, weekStartMs) && hasCapaTrail(nce))
    .length;

// ---- Recent-Activity rows from NCE histories (client half of the feed) ----

export const nceActivityRows = (list, sinceMs) =>
  list.flatMap((nce) =>
    (nce.history || [])
      .filter((h) => h.timestamp && Date.parse(h.timestamp) >= sinceMs)
      .map((h) => ({
        type: "NCE",
        activity: h.activity,
        actor: h.userName,
        nceNumber: nce.nceNumber,
        timestamp: h.timestamp,
      })),
  );

// ---- TAT rollup for the QI pillar chip / inspector Q3 ----

const DAY_MS = 24 * 60 * 60 * 1000;
const TAT_WINDOW_DAYS = 30;

const tatQuery = (from, to) =>
  `/rest/reports/tat/summary?fromDate=${from}&toDate=${to}` +
  `&segment=RECEIPT_TO_VALIDATION&calculationMode=CALENDAR&breakdownBy=LAB_UNIT`;

export const fetchTatRollup = dedupedFetch((resolve) => {
  const to = new Date();
  const from = new Date(to.getTime() - TAT_WINDOW_DAYS * DAY_MS);
  const priorTo = new Date(from.getTime() - DAY_MS);
  const priorFrom = new Date(priorTo.getTime() - TAT_WINDOW_DAYS * DAY_MS);
  let current;
  let prior;
  let pending = 2;
  const finish = () => {
    if (--pending > 0) return;
    if (!current || !(current.totalCount > 0)) {
      resolve(null);
      return;
    }
    resolve({ mean: current.mean, ...tatDelta(current, prior) });
  };
  getFromOpenElisServer(
    tatQuery(toLocalIsoDate(from), toLocalIsoDate(to)),
    (res) => {
      current = res;
      finish();
    },
  );
  getFromOpenElisServer(
    tatQuery(toLocalIsoDate(priorFrom), toLocalIsoDate(priorTo)),
    (res) => {
      prior = res;
      finish();
    },
  );
});
