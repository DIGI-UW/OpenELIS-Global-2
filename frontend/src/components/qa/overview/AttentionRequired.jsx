import React, { useEffect, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { useHistory } from "react-router-dom";
import ComingSoon from "./ComingSoon";
import {
  NCE_DRILL_URL,
  countCriticalPending,
  fetchNceList,
} from "./nceOverview";

const ROWS = [
  { titleKey: "qa.overview.attention.criticalResults", ticket: "OGC-714" },
  { titleKey: "qa.overview.attention.overdueCapas", ticket: "OGC-707" },
  { titleKey: "qa.overview.attention.qcViolations", ticket: "OGC-700" },
  { titleKey: "qa.overview.attention.effectivenessReview", ticket: "OGC-707" },
  { titleKey: "qa.overview.attention.eqaDue", ticket: "OGC-721" },
  { titleKey: "qa.overview.attention.nceSla", ticket: "NCE v2" },
];

// Live row (OGC-699): count of critical NCEs pending acknowledgment, linked
// to the NCE register pre-filtered to that slice.
const NceCriticalRow = () => {
  const history = useHistory();
  // undefined = loading, null = fetch yielded no data
  const [nceList, setNceList] = useState();

  useEffect(() => {
    let mounted = true;
    fetchNceList((list) => {
      if (mounted) {
        setNceList(list);
      }
    });
    return () => {
      mounted = false;
    };
  }, []);

  const count = nceList ? countCriticalPending(nceList) : null;

  return (
    <button
      type="button"
      className={"qa-live-row" + (count > 0 ? " qa-live-row-alert" : "")}
      onClick={() => history.push(NCE_DRILL_URL)}
    >
      <span className="qa-live-badge">
        {nceList === undefined ? "…" : count != null ? count : "—"}
      </span>
      <span className="qa-cs-title">
        <FormattedMessage id="qa.overview.attention.criticalNce" />
      </span>
      <span className="qa-live-arrow" aria-hidden="true">
        →
      </span>
    </button>
  );
};

const AttentionRequired = () => {
  const intl = useIntl();
  const title = intl.formatMessage({ id: "qa.overview.section.attention" });
  return (
    <section className="qa-overview-section" aria-label={title}>
      <div className="qa-sec-head">
        <h3>{title}</h3>
      </div>
      <div className="qa-cs-rows">
        <NceCriticalRow />
        {ROWS.map((row) => (
          <ComingSoon key={row.titleKey} variant="row" {...row} />
        ))}
      </div>
    </section>
  );
};

export default AttentionRequired;
