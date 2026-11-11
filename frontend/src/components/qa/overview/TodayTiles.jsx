import React from "react";
import { useIntl } from "react-intl";
import NcePulseTile from "./NcePulseTile";
import RateTile from "./RateTile";
import TatTile from "./TatTile";

/**
 * The Today row: the same five quality indicators the QI Dashboard shows, in
 * the same order and the same tile. Each tile resolves its own qi_config, so a
 * disabled indicator vanishes here exactly as it does on the dashboard
 * (OGC-711).
 */
const TodayTiles = () => {
  const intl = useIntl();
  const title = intl.formatMessage({ id: "reports.tat.preset.today" });
  return (
    <section className="qa-overview-section" aria-label={title}>
      <div className="qa-sec-head">
        <h3>{title}</h3>
      </div>
      <div className="qa-cs-grid qa-cs-grid-tiles">
        <TatTile />
        <RateTile indicator="REJECTION" />
        <RateTile indicator="AMENDMENT" />
        <RateTile indicator="CALLBACK" />
        <NcePulseTile />
      </div>
    </section>
  );
};

export default TodayTiles;
