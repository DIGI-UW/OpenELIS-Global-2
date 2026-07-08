import React from "react";
import { useIntl } from "react-intl";
import ComingSoon from "./ComingSoon";

const STATS = [
  { titleKey: "qa.overview.week.newNces", ticket: "OGC-694" },
  { titleKey: "qa.overview.week.ncesResolved", ticket: "OGC-694" },
  { titleKey: "qa.overview.week.capasCompleted", ticket: "OGC-694" },
  { titleKey: "qa.overview.week.qcViolations", ticket: "OGC-694" },
  { titleKey: "qa.overview.week.criticalResults", ticket: "OGC-714" },
  { titleKey: "qa.overview.week.eqaSubmissions", ticket: "OGC-694" },
  { titleKey: "qa.overview.week.auditEntries", ticket: "OGC-694" },
  { titleKey: "qa.overview.week.signatureEvents", ticket: "OGC-694" },
];

const ThisWeek = () => {
  const intl = useIntl();
  const title = intl.formatMessage({ id: "qa.overview.section.thisWeek" });
  return (
    <section className="qa-overview-section" aria-label={title}>
      <div className="qa-sec-head">
        <h3>{title}</h3>
      </div>
      <div className="qa-cs-grid qa-cs-grid-stats">
        {STATS.map((stat) => (
          <ComingSoon key={stat.titleKey} variant="stat" {...stat} />
        ))}
      </div>
    </section>
  );
};

export default ThisWeek;
