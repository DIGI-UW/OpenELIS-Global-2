import React, { useEffect, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { useHistory } from "react-router-dom";
import { CheckmarkOutline } from "@carbon/icons-react";
import ComingSoon from "./ComingSoon";
import QAEmptyState from "../common/QAEmptyState";
import { getFromOpenElisServer } from "../../utils/Utils";
import {
  NCE_DRILL_URL,
  countCriticalPending,
  fetchNceList,
} from "./nceOverview";
import { fetchOverviewSummary } from "./overviewData";

// Live action-queue row: count badge + label, linked to the drill-through.
// count === undefined -> loading, null -> fetch yielded no data.
const LiveRow = ({ count, labelKey, alert, onClick }) => (
  <button
    type="button"
    className={"qa-live-row" + (alert ? " qa-live-row-alert" : "")}
    onClick={onClick}
  >
    <span className="qa-live-badge">
      {count === undefined ? "…" : count != null ? count : "—"}
    </span>
    <span className="qa-cs-title">
      <FormattedMessage id={labelKey} />
    </span>
    <span className="qa-live-arrow" aria-hidden="true">
      →
    </span>
  </button>
);

/**
 * Attention Required action queue. Live rows: critical NCEs pending
 * acknowledgment (OGC-699 WS-C), QC violations in last 24h and EQA
 * submissions due in 14 days (OGC-694 WS-F, count-only until Phase B/E).
 */
const AttentionRequired = () => {
  const intl = useIntl();
  const history = useHistory();
  const title = intl.formatMessage({ id: "qa.overview.section.attention" });
  // undefined = loading, null = fetch yielded no data
  const [nceList, setNceList] = useState();
  const [summary, setSummary] = useState();
  // OGC-711: hide the critical-NCE row when the NCE indicator is disabled.
  // Fail-open — default true until/unless resolve says enabled === false.
  const [nceEnabled, setNceEnabled] = useState(true);

  useEffect(() => {
    let mounted = true;
    fetchNceList((list) => mounted && setNceList(list));
    fetchOverviewSummary((data) => mounted && setSummary(data));
    getFromOpenElisServer(
      "/rest/qi-config/resolve?indicator=NCE",
      (res) => mounted && res && setNceEnabled(res.enabled !== false),
    );
    return () => {
      mounted = false;
    };
  }, []);

  const criticalNce = nceList ? countCriticalPending(nceList) : nceList;
  const qcViolations = summary ? summary.qc.violations24h : summary;
  const eqaDue = summary ? summary.eqa.dueSoon14d : summary;

  // All live queues loaded and empty — surface a calm "all clear" above the
  // rows (placeholders still render below).
  const allClear =
    (!nceEnabled || criticalNce === 0) && qcViolations === 0 && eqaDue === 0;

  return (
    <section className="qa-overview-section" aria-label={title}>
      <div className="qa-sec-head">
        <h3>{title}</h3>
      </div>
      <div className="qa-cs-rows">
        {allClear && (
          <QAEmptyState
            size="inline"
            icon={CheckmarkOutline}
            titleKey="qa.empty.attention.title"
            subheadKey="qa.empty.attention.subhead"
          />
        )}
        {nceEnabled && (
          <LiveRow
            count={criticalNce}
            labelKey="qa.overview.attention.criticalNce"
            alert={criticalNce > 0}
            onClick={() => history.push(NCE_DRILL_URL)}
          />
        )}
        <ComingSoon
          variant="row"
          titleKey="qa.overview.attention.criticalResults"
          ticket="OGC-714"
        />
        <ComingSoon
          variant="row"
          titleKey="qa.overview.attention.overdueCapas"
          ticket="OGC-707"
        />
        <LiveRow
          count={qcViolations}
          labelKey="qa.overview.attention.qcViolations"
          alert={qcViolations > 0}
          onClick={() => history.push("/qa/qc/alerts")}
        />
        <ComingSoon
          variant="row"
          titleKey="qa.overview.attention.effectivenessReview"
          ticket="OGC-707"
        />
        <LiveRow
          count={eqaDue}
          labelKey="qa.overview.attention.eqaDue"
          onClick={() => history.push("/qa/eqa/orders")}
        />
        <ComingSoon
          variant="row"
          titleKey="qa.overview.attention.nceSla"
          ticket="NCE v2"
        />
      </div>
    </section>
  );
};

export default AttentionRequired;
