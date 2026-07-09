import React from "react";
import { useIntl } from "react-intl";
import ComingSoon from "./ComingSoon";
import AmendmentRateTile from "./AmendmentRateTile";
import NcePulseTile from "./NcePulseTile";

const TodayTiles = () => {
  const intl = useIntl();
  const title = intl.formatMessage({ id: "qa.overview.section.today" });
  return (
    <section className="qa-overview-section" aria-label={title}>
      <div className="qa-sec-head">
        <h3>{title}</h3>
      </div>
      <div className="qa-cs-grid qa-cs-grid-tiles">
        <ComingSoon
          variant="tile"
          titleKey="qa.overview.tile.tat"
          ticket="OGC-696"
        />
        <ComingSoon
          variant="tile"
          titleKey="qa.overview.tile.rejectionRate"
          ticket="OGC-697"
        />
        <AmendmentRateTile />
        <ComingSoon
          variant="tile"
          titleKey="qa.overview.tile.criticalCallback"
          ticket="OGC-714"
        />
        <NcePulseTile />
      </div>
    </section>
  );
};

export default TodayTiles;
