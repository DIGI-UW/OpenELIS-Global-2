import React from "react";
import { useIntl } from "react-intl";
import ComingSoon from "./ComingSoon";

const PILLARS = [
  { titleKey: "sideNav.label.qa.qc", ticket: "OGC-694" },
  { titleKey: "banner.menu.eqa", ticket: "OGC-721" },
  { titleKey: "sideNav.label.qa.qi", ticket: "OGC-695" },
  { titleKey: "sideNav.label.qa.qms", ticket: "OGC-699" },
];

const PillarStatus = () => {
  const intl = useIntl();
  const title = intl.formatMessage({ id: "qa.overview.section.pillars" });
  return (
    <section className="qa-overview-section" aria-label={title}>
      <div className="qa-sec-head">
        <h3>{title}</h3>
      </div>
      <div className="qa-cs-grid qa-cs-grid-pillars">
        {PILLARS.map((pillar) => (
          <ComingSoon key={pillar.titleKey} variant="tile" {...pillar} />
        ))}
      </div>
    </section>
  );
};

export default PillarStatus;
