import React from "react";
import { useIntl } from "react-intl";
import ComingSoon from "./ComingSoon";
import AmendmentRateTile from "./AmendmentRateTile";
import NcePulseTile from "./NcePulseTile";
import TatTile from "./TatTile";
import useQiEnabled from "../qi/useQiEnabled";

// OGC-711 disable cascade — reach the Overview's QI-indicator tiles too, so a
// disabled indicator vanishes here the same way it does on the QI Dashboard.
const GATED_INDICATORS = ["TAT", "AMENDMENT", "NCE"];

const TodayTiles = () => {
  const intl = useIntl();
  const title = intl.formatMessage({ id: "qa.overview.section.today" });
  const { isEnabled } = useQiEnabled(GATED_INDICATORS);
  return (
    <section className="qa-overview-section" aria-label={title}>
      <div className="qa-sec-head">
        <h3>{title}</h3>
      </div>
      <div className="qa-cs-grid qa-cs-grid-tiles">
        {isEnabled("TAT") && <TatTile />}
        <ComingSoon
          variant="tile"
          titleKey="qa.overview.tile.rejectionRate"
          ticket="OGC-697"
        />
        {isEnabled("AMENDMENT") && <AmendmentRateTile />}
        <ComingSoon
          variant="tile"
          titleKey="qa.overview.tile.criticalCallback"
          ticket="OGC-714"
        />
        {isEnabled("NCE") && <NcePulseTile />}
      </div>
    </section>
  );
};

export default TodayTiles;
