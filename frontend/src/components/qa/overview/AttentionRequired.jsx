import React from "react";
import { useIntl } from "react-intl";
import ComingSoon from "./ComingSoon";

const ROWS = [
  { titleKey: "qa.overview.attention.criticalNce", ticket: "OGC-694" },
  { titleKey: "qa.overview.attention.criticalResults", ticket: "OGC-714" },
  { titleKey: "qa.overview.attention.overdueCapas", ticket: "OGC-707" },
  { titleKey: "qa.overview.attention.qcViolations", ticket: "OGC-700" },
  { titleKey: "qa.overview.attention.effectivenessReview", ticket: "OGC-707" },
  { titleKey: "qa.overview.attention.eqaDue", ticket: "OGC-721" },
  { titleKey: "qa.overview.attention.nceSla", ticket: "NCE v2" },
];

const AttentionRequired = () => {
  const intl = useIntl();
  const title = intl.formatMessage({ id: "qa.overview.section.attention" });
  return (
    <section className="qa-overview-section" aria-label={title}>
      <div className="qa-sec-head">
        <h3>{title}</h3>
      </div>
      <div className="qa-cs-rows">
        {ROWS.map((row) => (
          <ComingSoon key={row.titleKey} variant="row" {...row} />
        ))}
      </div>
    </section>
  );
};

export default AttentionRequired;
