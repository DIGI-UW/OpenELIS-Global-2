import React, { useEffect, useState } from "react";
import { ClickableTile, SkeletonText } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import { useHistory } from "react-router-dom";
import { toLocalIsoDate } from "../../utils/Utils";
import { fetchCallbackSummary } from "./overviewData";

/**
 * Critical Callback Compliance overview tile (OGC-714/715): rolling-30-day
 * read-back compliance from the same /rest/critical-callback/summary the QI
 * Dashboard tile uses. Rendering is gated upstream on the opt-in CALLBACK
 * indicator; the summary's own target drives the tone (green at/above, red
 * below — compliance is HIGHER_BETTER).
 */

function windowDates() {
  const to = new Date();
  const from = new Date();
  from.setDate(from.getDate() - 30);
  return { fromDate: toLocalIsoDate(from), toDate: toLocalIsoDate(to) };
}

const CallbackComplianceTile = () => {
  const history = useHistory();
  // undefined = loading, null = fetch yielded no data
  const [summary, setSummary] = useState();

  useEffect(() => {
    let mounted = true;
    const { fromDate, toDate } = windowDates();
    fetchCallbackSummary(fromDate, toDate, (res) => {
      if (mounted) {
        setSummary(res);
      }
    });
    return () => {
      mounted = false;
    };
  }, []);

  const pct = summary?.compliancePercent;
  const tone =
    pct != null && summary?.target != null
      ? pct >= summary.target
        ? "green"
        : "red"
      : null;

  return (
    <ClickableTile
      className="qa-live-tile"
      onClick={() => history.push("/qa/qi/callback")}
    >
      <div className="qa-cs-title">
        <FormattedMessage id="qa.overview.tile.criticalCallback" />
      </div>
      {summary === undefined ? (
        <SkeletonText heading width="40%" />
      ) : (
        <>
          <div className={"qa-live-count" + (tone ? ` qa-live-${tone}` : "")}>
            {pct != null ? `${pct.toFixed(2)}%` : "—"}
          </div>
          <div className="qa-live-caption">
            <FormattedMessage id="qa.overview.callback.caption" />
          </div>
          <div className="qa-live-caption">
            {summary && summary.criticalCount > 0 ? (
              <FormattedMessage
                id="qa.overview.callback.ofCritical"
                values={{
                  confirmed: summary.confirmedCount,
                  critical: summary.criticalCount,
                }}
              />
            ) : (
              <FormattedMessage id="qa.overview.callback.none" />
            )}
          </div>
        </>
      )}
    </ClickableTile>
  );
};

export default CallbackComplianceTile;
