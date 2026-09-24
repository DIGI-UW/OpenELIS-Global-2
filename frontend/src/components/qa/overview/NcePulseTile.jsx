import React from "react";
import { useIntl } from "react-intl";
import QITile from "../qi/QITile";
import useQiConfig from "../qi/useQiConfig";
import {
  NCE_DRILL_URL,
  countCriticalPending,
  countInCorrectiveAction,
  pulseColor,
  useNceList,
} from "./nceOverview";

/**
 * NCE Pulse — current-state count of critical NCEs pending acknowledgment
 * (OGC-699). Deliberately not a trend: no sparkline.
 */
const NcePulseTile = () => {
  const intl = useIntl();
  const { enabled } = useQiConfig("NCE");
  const { loading, nceList } = useNceList();

  if (!enabled) {
    return null;
  }

  const count = nceList ? countCriticalPending(nceList) : null;

  return (
    <QITile
      testId="qa-overview-tile-nce"
      titleKey="qa.qi.dashboard.tile.ncePulse.label"
      accent={count != null ? pulseColor(count) : "blue"}
      loading={loading}
      primary={count != null ? String(count) : "—"}
      targetLine={intl.formatMessage({
        id: "qa.qi.dashboard.tile.ncePulse.criticalPending",
      })}
      secondary={
        nceList
          ? intl.formatMessage(
              { id: "qa.qi.dashboard.tile.ncePulse.inCorrectiveAction" },
              { count: countInCorrectiveAction(nceList) },
            )
          : null
      }
      detailPath={NCE_DRILL_URL}
    />
  );
};

export default NcePulseTile;
