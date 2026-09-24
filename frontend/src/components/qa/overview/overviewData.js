import { toLocalIsoDate } from "../../utils/Utils";
import { useServerData } from "../../utils/useServerData";
import { tatDelta } from "../../reports/tat/tatUtils";
import { isoDaysFromToday, weekStart } from "../common/qaDates";

/**
 * Shared data hooks for the QA Overview aggregators (OGC-694).
 *
 * Every read goes through useServerData, so the slots that mount together —
 * five tiles, the attention queue, the pillar chips, the inspector answers —
 * share one request per endpoint instead of each firing their own. NCE-derived
 * counters are computed client-side from that one dashboard payload, which
 * already carries every event's full history.
 */

export const OVERVIEW_SUMMARY_URL = "/rest/qa/overview/summary";

export const useOverviewSummary = () => {
  const query = useServerData(OVERVIEW_SUMMARY_URL);
  return {
    loading: query.isLoading,
    summary: query.data?.week ? query.data : null,
  };
};

// Accreditation portfolio summary (OGC-686): counts per status plus
// worstStatus, which is null when no non-inactive body exists.
export const useAccreditationSummary = () => {
  const query = useServerData("/rest/accreditation/summary");
  return {
    loading: query.isLoading,
    accreditation:
      typeof query.data?.totalBodies === "number" ? query.data : null,
  };
};

// Critical-callback compliance summary for a window (OGC-714/715):
// {enabled, criticalCount, confirmedCount, compliancePercent, target}. When
// the CALLBACK indicator is disabled the response says enabled=false —
// callers hide their surface (same cascade as the QI Dashboard tile). Pass a
// falsy window to hold the read until the caller knows which week to ask for.
export const callbackSummaryUrl = (fromDate, toDate) =>
  `/rest/critical-callback/summary?fromDate=${fromDate}&toDate=${toDate}`;

export const useCallbackSummary = (fromDate, toDate) => {
  const query = useServerData(
    fromDate && toDate ? callbackSummaryUrl(fromDate, toDate) : null,
  );
  return { loading: query.isLoading, callbacks: query.data ?? null };
};

// ---- Week window ----

// Local-Monday fallback, used only when the summary fetch yields no server
// boundary; when the summary is available its week.weekStart/weekStartInstant
// win so all This-Week counters share the server's window.
export { weekStart };

// ---- NCE weekly counters (over the nceOverview.useNceList payload) ----

// reportDate is a plain yyyy-mm-dd string; compare as strings to avoid
// UTC-midnight parsing skew at the Monday boundary.
export const newNcesThisWeek = (
  list,
  weekStartDate = toLocalIsoDate(weekStart()),
) => list.filter((nce) => nce.reportDate && nce.reportDate >= weekStartDate);

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

// "CAPAs completed" just counts closed CAPAs — no effectiveness check until
// OGC-707 adds verification. A "CAPA trail" means the event history mentions
// a corrective action or a CAPA status.
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

// ---- TAT rollup for the QI pillar chip / inspector Q3 / Today tile ----

const TAT_WINDOW_DAYS = 30;

const tatQuery = (from, to) =>
  `/rest/reports/tat/summary?fromDate=${from}&toDate=${to}` +
  `&segment=RECEIPT_TO_VALIDATION&calculationMode=CALENDAR&breakdownBy=LAB_UNIT`;

/**
 * Mean receipt-to-validation TAT over the last 30 days with its delta against
 * the equal-length window before it. `tat` is null once loaded when the window
 * has no completed runs at all.
 */
export const useTatRollup = () => {
  const current = useServerData(
    tatQuery(isoDaysFromToday(-TAT_WINDOW_DAYS), toLocalIsoDate(new Date())),
  );
  const prior = useServerData(
    tatQuery(
      isoDaysFromToday(-(2 * TAT_WINDOW_DAYS + 1)),
      isoDaysFromToday(-(TAT_WINDOW_DAYS + 1)),
    ),
  );
  const loading = current.isLoading || prior.isLoading;
  return {
    loading,
    tat:
      !loading && current.data?.totalCount > 0
        ? { mean: current.data.mean, ...tatDelta(current.data, prior.data) }
        : null,
  };
};
