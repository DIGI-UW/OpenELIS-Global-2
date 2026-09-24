import React from "react";
import { useIntl } from "react-intl";
import { formatTat } from "../../reports/tat/tatUtils";
import QITile from "../qi/QITile";
import useQiConfig from "../qi/useQiConfig";
import { useTatRollup } from "./overviewData";

/**
 * Average TAT — 30-day mean receipt-to-validation with its prior-window delta
 * (OGC-696). Rides the same cached rollup the QI pillar chip and inspector Q3
 * read, so it adds no request. Drills to the QI Dashboard (its detail
 * /qa/qi/tat is RESULTS/REPORTS-gated, narrower than this audience).
 */
const TatTile = () => {
  const intl = useIntl();
  const { enabled } = useQiConfig("TAT");
  const { loading, tat } = useTatRollup();

  if (!enabled) {
    return null;
  }

  return (
    <QITile
      testId="qa-overview-tile-tat"
      titleKey="qa.qi.dashboard.tile.tat.label"
      loading={loading}
      primary={tat ? formatTat(tat.mean) : "—"}
      delta={tat?.arrow && tat.text ? tat : null}
      targetLine={intl.formatMessage(
        {
          id: tat
            ? "qa.qi.dashboard.tile.tat.vsPriorDays"
            : "qa.overview.pillar.qi.noData",
        },
        { days: 30 },
      )}
      detailPath="/qa/qi/dashboard"
    />
  );
};

export default TatTile;
