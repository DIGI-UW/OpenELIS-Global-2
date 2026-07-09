import React, { useEffect, useState } from "react";
import { ClickableTile, SkeletonText } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import { useHistory } from "react-router-dom";
import { formatTat } from "../../reports/tat/tatUtils";
import { fetchTatRollup } from "./overviewData";

// tatDelta tone → the shared qa-live color classes (faster TAT is good/green).
const DELTA_COLOR = { good: "qa-live-green", bad: "qa-live-amber" };
const TAT_DRILL_URL = "/qa/qi/dashboard";

/**
 * Average TAT — 30-day mean receipt-to-validation with its prior-window delta
 * (OGC-696). Rides the deduped fetchTatRollup already fetched by the QI pillar
 * chip / inspector Q3, so it adds no request. Drills to the QI Dashboard (its
 * detail /qa/qi/tat is RESULTS/REPORTS-gated, narrower than this audience).
 */
const TatTile = () => {
  const history = useHistory();
  // undefined = loading, null = fetch yielded no runs
  const [tat, setTat] = useState();

  useEffect(() => {
    let mounted = true;
    fetchTatRollup((data) => mounted && setTat(data));
    return () => {
      mounted = false;
    };
  }, []);

  return (
    <ClickableTile
      className="qa-live-tile"
      onClick={() => history.push(TAT_DRILL_URL)}
    >
      <div className="qa-cs-title">
        <FormattedMessage id="qa.overview.tile.tat" />
      </div>
      {tat === undefined ? (
        <SkeletonText heading width="40%" />
      ) : tat === null ? (
        <>
          <div className="qa-live-count">—</div>
          <div className="qa-live-caption">
            <FormattedMessage id="qa.overview.pillar.qi.noData" />
          </div>
        </>
      ) : (
        <>
          <div className="qa-live-count">{formatTat(tat.mean)}</div>
          <div className="qa-live-caption">
            {tat.arrow && tat.text ? (
              <span className={DELTA_COLOR[tat.tone] || ""}>
                {tat.arrow} {tat.text}{" "}
              </span>
            ) : null}
            <FormattedMessage
              id="qa.qi.dashboard.tile.tat.vsPriorDays"
              values={{ days: 30 }}
            />
          </div>
        </>
      )}
    </ClickableTile>
  );
};

export default TatTile;
