import React from "react";
import { useIntl } from "react-intl";
import ComingSoon from "./ComingSoon";

const TILES = [
  { titleKey: "qa.overview.tile.tat", ticket: "OGC-696" },
  { titleKey: "qa.overview.tile.rejectionRate", ticket: "OGC-697" },
  { titleKey: "qa.overview.tile.amendmentRate", ticket: "OGC-698" },
  { titleKey: "qa.overview.tile.criticalCallback", ticket: "OGC-714" },
  { titleKey: "qa.overview.tile.ncePulse", ticket: "OGC-699" },
];

const TodayTiles = () => {
  const intl = useIntl();
  const title = intl.formatMessage({ id: "qa.overview.section.today" });
  return (
    <section className="qa-overview-section" aria-label={title}>
      <div className="qa-sec-head">
        <h3>{title}</h3>
      </div>
      <div className="qa-cs-grid qa-cs-grid-tiles">
        {TILES.map((tile) => (
          <ComingSoon key={tile.titleKey} variant="tile" {...tile} />
        ))}
      </div>
    </section>
  );
};

export default TodayTiles;
