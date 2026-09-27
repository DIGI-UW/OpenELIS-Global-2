import React from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { useHistory } from "react-router-dom";
import { CheckmarkOutline } from "@carbon/icons-react";
import ComingSoon from "./ComingSoon";
import QAEmptyState from "../common/QAEmptyState";
import useQiConfig from "../qi/useQiConfig";
import {
  NCE_DRILL_URL,
  countCriticalPending,
  countEffectivenessReviewsDue,
  useNceList,
} from "./nceOverview";
import { useCallbackSummary, useOverviewSummary } from "./overviewData";
import { toLocalIsoDate } from "../../utils/Utils";
import { useServerData } from "../../utils/useServerData";
import { isoDaysFromToday } from "../common/qaDates";
import { deriveStatus } from "../common/capa";

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

// undefined while the read is in flight, null when it yielded nothing.
const counter = (loading, data, compute) =>
  loading ? undefined : data ? compute(data) : null;

/**
 * Attention Required action queue. Live rows: critical NCEs pending
 * acknowledgment (OGC-699), critical results in the last 24h
 * (OGC-714/715), overdue CAPAs (OGC-707), QC violations in last 24h and EQA
 * submissions due in 14 days (OGC-694).
 */
const AttentionRequired = () => {
  const intl = useIntl();
  const history = useHistory();
  const title = intl.formatMessage({ id: "qa.overview.section.attention" });
  const { loading: nceLoading, nceList } = useNceList();
  const { loading: summaryLoading, summary } = useOverviewSummary();
  const { loading: callbacksLoading, callbacks } = useCallbackSummary(
    isoDaysFromToday(-1),
    toLocalIsoDate(new Date()),
  );
  const capaQuery = useServerData("/rest/nce/capa-register");
  // OGC-711: hide the critical-NCE row when the NCE indicator is disabled.
  const { enabled: nceEnabled } = useQiConfig("NCE");

  const criticalNce = counter(nceLoading, nceList, countCriticalPending);
  const reviewsDue = counter(nceLoading, nceList, countEffectivenessReviewsDue);
  const qcViolations = counter(
    summaryLoading,
    summary,
    (s) => s.qc.violations24h,
  );
  const eqaDue = counter(summaryLoading, summary, (s) => s.eqa.dueSoon14d);
  // hidden when the opt-in CALLBACK indicator is off (cascade)
  const callbackShown = callbacks?.enabled !== false;
  const criticalResults = counter(
    callbacksLoading,
    callbacks,
    (c) => c.criticalCount,
  );
  const unconfirmed = callbacks
    ? callbacks.criticalCount - callbacks.confirmedCount
    : 0;
  const today = toLocalIsoDate(new Date());
  const overdueCapas = counter(
    capaQuery.isLoading,
    Array.isArray(capaQuery.data) ? capaQuery.data : null,
    (rows) =>
      rows.filter((row) => deriveStatus(row, today) === "overdue").length,
  );

  // All live queues loaded and empty — surface a calm "all clear" above the
  // rows (placeholders still render below).
  const allClear =
    (!nceEnabled || criticalNce === 0) &&
    qcViolations === 0 &&
    eqaDue === 0 &&
    (!callbackShown || criticalResults === 0) &&
    overdueCapas === 0 &&
    reviewsDue === 0;

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
        {callbackShown && (
          <LiveRow
            count={criticalResults}
            labelKey="qa.overview.attention.criticalResults"
            alert={unconfirmed > 0}
            onClick={() => history.push("/qa/qi/callback")}
          />
        )}
        <LiveRow
          count={overdueCapas}
          labelKey="qa.overview.attention.overdueCapas"
          alert={overdueCapas > 0}
          onClick={() => history.push("/qa/qms/capa-register")}
        />
        <LiveRow
          count={qcViolations}
          labelKey="qa.overview.attention.qcViolations"
          alert={qcViolations > 0}
          onClick={() => history.push("/qa/qc/alerts")}
        />
        <LiveRow
          count={reviewsDue}
          labelKey="qa.overview.attention.effectivenessReview"
          alert={reviewsDue > 0}
          onClick={() => history.push("/NceDashboard?status=CAPA")}
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
