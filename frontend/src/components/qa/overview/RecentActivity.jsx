import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { FormattedMessage, useIntl } from "react-intl";
import { fetchNceList } from "./nceOverview";
import { fetchOverviewSummary, nceActivityRows } from "./overviewData";
import QAEmptyState from "../common/QAEmptyState";

const DAY_MS = 24 * 60 * 60 * 1000;
const MAX_ROWS = 8;

// Activities without a dedicated message fall back to the UPDATED wording.
const NCE_ACTIVITY_KEYS = new Set([
  "CREATED",
  "ACKNOWLEDGED",
  "ASSIGNED",
  "NOTE_ADDED",
  "STATUS_CHANGED",
  "CORRECTIVE_ACTION",
  "RESOLVED",
]);

const rowText = (intl, row) => {
  const system = intl.formatMessage({ id: "qa.overview.activity.system" });
  if (row.type === "NCE") {
    const key = NCE_ACTIVITY_KEYS.has(row.activity)
      ? `qa.overview.activity.nce.${row.activity}`
      : "qa.overview.activity.nce.UPDATED";
    return intl.formatMessage(
      { id: key },
      { actor: row.actor || system, nce: row.nceNumber || "NCE" },
    );
  }
  if (row.type === "ESIG") {
    return intl.formatMessage(
      { id: "qa.overview.activity.esig" },
      {
        actor: row.actor || system,
        meaning: intl.formatMessage({
          id: `qa.overview.activity.meaning.${row.meaning}`,
          defaultMessage: row.meaning,
        }),
        record: `${row.recordType || ""} #${row.recordId != null ? row.recordId : ""}`,
      },
    );
  }
  return intl.formatMessage(
    { id: "qa.overview.activity.qc" },
    {
      rule: row.ruleCode,
      severity: row.severity,
      instrument: row.instrumentName,
    },
  );
};

// Feed only spans 24h, so a row is either today (show the time) or yesterday.
const rowWhen = (intl, timestamp) => {
  const date = new Date(timestamp);
  const time = intl.formatTime(date);
  return date.toDateString() === new Date().toDateString()
    ? time
    : intl.formatMessage({ id: "qa.overview.activity.yesterday" }, { time });
};

/**
 * Recent Activity feed (OGC-694 WS-F): merges NCE history (client side, from
 * the shared NCE fetch) with e-signature events and QC alerts (server side)
 * over the last 24 hours.
 */
const RecentActivity = () => {
  const intl = useIntl();
  const title = intl.formatMessage({ id: "qa.overview.section.activity" });
  // undefined = loading, null = fetch yielded no data
  const [nceList, setNceList] = useState();
  const [summary, setSummary] = useState();

  useEffect(() => {
    let mounted = true;
    fetchNceList((list) => mounted && setNceList(list));
    fetchOverviewSummary((data) => mounted && setSummary(data));
    return () => {
      mounted = false;
    };
  }, []);

  const loading = nceList === undefined || summary === undefined;
  let rows = [];
  if (!loading) {
    const since = Date.now() - DAY_MS;
    rows = [
      ...nceActivityRows(nceList || [], since),
      ...((summary && summary.activity) || []),
    ]
      .filter((row) => row.timestamp)
      .sort((a, b) => Date.parse(b.timestamp) - Date.parse(a.timestamp))
      .slice(0, MAX_ROWS);
  }

  return (
    <section className="qa-overview-section" aria-label={title}>
      <div className="qa-sec-head">
        <h3>{title}</h3>
        <Link className="qa-sec-link" to="/qa/qms/audit-trail">
          <FormattedMessage id="qa.overview.activity.viewAudit" /> ↗
        </Link>
      </div>
      <div className="qa-cs-rows">
        {loading ? (
          <div className="qa-cs qa-cs-row">
            <div className="qa-live-caption">…</div>
          </div>
        ) : rows.length === 0 ? (
          <QAEmptyState
            size="inline"
            titleKey="qa.overview.activity.empty"
            subheadKey="qa.empty.activity.subhead"
          />
        ) : (
          rows.map((row, i) => (
            <div className="qa-activity-row" key={`${row.timestamp}-${i}`}>
              <span className="qa-activity-time">
                {rowWhen(intl, row.timestamp)}
              </span>
              <span className="qa-activity-text">{rowText(intl, row)}</span>
            </div>
          ))
        )}
      </div>
    </section>
  );
};

export default RecentActivity;
