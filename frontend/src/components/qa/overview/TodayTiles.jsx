import React from "react";
import { useIntl } from "react-intl";
import AmendmentRateTile from "./AmendmentRateTile";
import CallbackComplianceTile from "./CallbackComplianceTile";
import NcePulseTile from "./NcePulseTile";
import RejectionRateTile from "./RejectionRateTile";
import TatTile from "./TatTile";
import useQiEnabled from "../qi/useQiEnabled";

// OGC-711 disable cascade — reach the Overview's QI-indicator tiles too, so a
// disabled indicator vanishes here the same way it does on the QI Dashboard.
// CALLBACK ships disabled (opt-in) — hidden until enabled in QI Configuration.
const GATED_INDICATORS = ["TAT", "REJECTION", "AMENDMENT", "NCE", "CALLBACK"];

const TodayTiles = () => {
  const intl = useIntl();
  const title = intl.formatMessage({ id: "qa.overview.section.today" });
  const { isEnabled, getConfig } = useQiEnabled(GATED_INDICATORS);
  return (
    <section className="qa-overview-section" aria-label={title}>
      <div className="qa-sec-head">
        <h3>{title}</h3>
      </div>
      <div className="qa-cs-grid qa-cs-grid-tiles">
        {isEnabled("TAT") && <TatTile />}
        {isEnabled("REJECTION") && (
          <RejectionRateTile config={getConfig("REJECTION")} />
        )}
        {isEnabled("AMENDMENT") && (
          <AmendmentRateTile config={getConfig("AMENDMENT")} />
        )}
        {isEnabled("CALLBACK") && <CallbackComplianceTile />}
        {isEnabled("NCE") && <NcePulseTile />}
      </div>
    </section>
  );
};

export default TodayTiles;
