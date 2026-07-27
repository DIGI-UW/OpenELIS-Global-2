import React, { useEffect, useState } from "react";
import { Tile } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { fetchNceList } from "./nceOverview";
import {
  capasCompletedThisWeek,
  fetchCallbackSummary,
  fetchOverviewSummary,
  ncesResolvedThisWeek,
  newNcesThisWeek,
  severityBreakdown,
  weekStart,
} from "./overviewData";
import { toLocalIsoDate } from "../../utils/Utils";

const Stat = ({ labelKey, value, sub, loading }) => {
  const intl = useIntl();
  return (
    <Tile className="qa-cs-stat qa-live-stat">
      <div className="qa-cs-title">
        <FormattedMessage id={labelKey} />
      </div>
      <div className="qa-live-stat-value">
        {loading ? "…" : value != null ? intl.formatNumber(value) : "—"}
      </div>
      {sub ? <div className="qa-live-caption">{sub}</div> : null}
    </Tile>
  );
};

/**
 * This-Week counters (OGC-694 WS-F). NCE numbers are client-side over the
 * shared NCE fetch; QC/EQA/audit/e-sig come from the overview summary
 * endpoint; critical results from the callback compliance summary
 * (OGC-714/715).
 */
const ThisWeek = () => {
  const intl = useIntl();
  const title = intl.formatMessage({ id: "qa.overview.section.thisWeek" });
  // undefined = loading, null = fetch yielded no data
  const [nceList, setNceList] = useState();
  const [summary, setSummary] = useState();
  const [callbacks, setCallbacks] = useState();

  useEffect(() => {
    let mounted = true;
    fetchNceList((list) => mounted && setNceList(list));
    fetchOverviewSummary((data) => mounted && setSummary(data));
    return () => {
      mounted = false;
    };
  }, []);

  // Critical results share the server week boundary once the summary lands
  // (local-Monday fallback when it fails), like the NCE counters above.
  const summaryLoaded = summary !== undefined;
  const weekFrom =
    (summary && summary.week.weekStart) || toLocalIsoDate(weekStart());
  useEffect(() => {
    if (!summaryLoaded) {
      return undefined;
    }
    let mounted = true;
    fetchCallbackSummary(weekFrom, toLocalIsoDate(new Date()), (res) => {
      if (mounted) {
        setCallbacks(res);
      }
    });
    return () => {
      mounted = false;
    };
  }, [summaryLoaded, weekFrom]);

  // NCE counters wait for the summary too: its server week boundary keeps
  // them on the same window as the backend-computed counters (falls back to
  // the local Monday when the summary fetch fails).
  const nceLoading = nceList === undefined || summary === undefined;
  const weekStartDate = (summary && summary.week.weekStart) || undefined;
  const weekStartMs =
    summary && summary.week.weekStartInstant
      ? Date.parse(summary.week.weekStartInstant)
      : undefined;
  const newNces =
    !nceLoading && nceList ? newNcesThisWeek(nceList, weekStartDate) : null;
  const severity =
    newNces && newNces.length > 0 ? severityBreakdown(newNces) : null;
  const ruleEntries = summary
    ? Object.entries(summary.qc.weekRuleBreakdown || {}).slice(0, 2)
    : [];

  return (
    <section className="qa-overview-section" aria-label={title}>
      <div className="qa-sec-head">
        <h3>{title}</h3>
      </div>
      <div className="qa-cs-grid qa-cs-grid-stats">
        <Stat
          labelKey="qa.overview.week.newNces"
          loading={nceLoading}
          value={newNces ? newNces.length : null}
          sub={
            severity
              ? intl.formatMessage(
                  { id: "qa.overview.week.severityBreakdown" },
                  severity,
                )
              : null
          }
        />
        <Stat
          labelKey="qa.overview.week.ncesResolved"
          loading={nceLoading}
          value={
            !nceLoading && nceList
              ? ncesResolvedThisWeek(nceList, weekStartMs)
              : null
          }
        />
        <Stat
          labelKey="qa.overview.week.capasCompleted"
          loading={nceLoading}
          value={
            !nceLoading && nceList
              ? capasCompletedThisWeek(nceList, weekStartMs)
              : null
          }
        />
        <Stat
          labelKey="qa.overview.week.qcViolations"
          loading={summary === undefined}
          value={summary ? summary.qc.violationsThisWeek : null}
          sub={
            ruleEntries.length
              ? ruleEntries.map(([code, n]) => `${n} × ${code}`).join(" · ")
              : null
          }
        />
        {callbacks?.enabled !== false && (
          <Stat
            labelKey="qa.overview.week.criticalResults"
            loading={callbacks === undefined}
            value={callbacks ? callbacks.criticalCount : null}
            sub={
              callbacks && callbacks.criticalCount > 0
                ? intl.formatMessage(
                    { id: "qa.overview.week.confirmedCount" },
                    { count: callbacks.confirmedCount },
                  )
                : null
            }
          />
        )}
        <Stat
          labelKey="qa.overview.week.eqaSubmissions"
          loading={summary === undefined}
          value={summary ? summary.eqa.open : null}
          sub={
            summary
              ? intl.formatMessage({ id: "qa.overview.week.currentlyOpen" }) +
                (summary.eqa.overdue > 0
                  ? " · " +
                    intl.formatMessage(
                      { id: "qa.overview.week.overdueCount" },
                      { count: summary.eqa.overdue },
                    )
                  : "")
              : null
          }
        />
        <Stat
          labelKey="qa.overview.week.auditEntries"
          loading={summary === undefined}
          value={summary ? summary.week.auditEntries : null}
        />
        <Stat
          labelKey="qa.overview.week.signatureEvents"
          loading={summary === undefined}
          value={summary ? summary.week.signatureEvents : null}
        />
      </div>
    </section>
  );
};

export default ThisWeek;
