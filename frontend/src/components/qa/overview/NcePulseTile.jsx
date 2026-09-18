import React, { useEffect, useState } from "react";
import { ClickableTile, SkeletonText } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import { useHistory } from "react-router-dom";
import {
  NCE_DRILL_URL,
  countCriticalPending,
  countInCorrectiveAction,
  fetchNceList,
  pulseColor,
} from "./nceOverview";

/**
 * NCE Pulse — current-state count of critical NCEs pending acknowledgment
 * (OGC-699). Deliberately not a trend: no sparkline. Standalone so the QI
 * Dashboard can render the same tile.
 */
const NcePulseTile = () => {
  const history = useHistory();
  // undefined = loading, null = fetch yielded no data
  const [nceList, setNceList] = useState();

  useEffect(() => {
    let mounted = true;
    fetchNceList((list) => {
      if (mounted) {
        setNceList(list);
      }
    });
    return () => {
      mounted = false;
    };
  }, []);

  const count = nceList ? countCriticalPending(nceList) : null;

  return (
    <ClickableTile
      className="qa-live-tile"
      onClick={() => history.push(NCE_DRILL_URL)}
    >
      <div className="qa-cs-title">
        <FormattedMessage id="qa.overview.tile.ncePulse" />
      </div>
      {nceList === undefined ? (
        <SkeletonText heading width="40%" />
      ) : (
        <>
          <div
            className={
              "qa-live-count" +
              (count != null ? ` qa-live-${pulseColor(count)}` : "")
            }
          >
            {count != null ? count : "—"}
          </div>
          <div className="qa-live-caption">
            <FormattedMessage id="qa.overview.ncePulse.criticalPending" />
          </div>
          {nceList && (
            <div className="qa-live-caption">
              <FormattedMessage
                id="qa.overview.ncePulse.inCorrectiveAction"
                values={{ count: countInCorrectiveAction(nceList) }}
              />
            </div>
          )}
        </>
      )}
    </ClickableTile>
  );
};

export default NcePulseTile;
