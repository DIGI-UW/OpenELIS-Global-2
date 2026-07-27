import React, { useEffect, useState } from "react";
import { ClickableTile, SkeletonText } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import { useHistory } from "react-router-dom";
import { getFromOpenElisServer, toLocalIsoDate } from "../../utils/Utils";
import { rateTone } from "../qi/qiThresholds";

/**
 * Rejection Rate overview tile (OGC-697/710): rolling-30-day share of started
 * analyses rejected at results entry, from the same /rest/reports/rejection
 * endpoint the QI Dashboard tile uses so the two views stay in sync. Tone
 * comes from the resolved qi_config thresholds (passed down by TodayTiles).
 * Click drills into the detail report.
 */

function windowDates() {
  const to = new Date();
  const from = new Date();
  from.setDate(from.getDate() - 30);
  return { fromDate: toLocalIsoDate(from), toDate: toLocalIsoDate(to) };
}

const RejectionRateTile = ({ config }) => {
  const history = useHistory();
  // undefined = loading, null = fetch yielded no data
  const [summary, setSummary] = useState();

  useEffect(() => {
    let mounted = true;
    const { fromDate, toDate } = windowDates();
    getFromOpenElisServer(
      `/rest/reports/rejection/summary?fromDate=${fromDate}&toDate=${toDate}`,
      (res) => {
        if (mounted) {
          setSummary(res ?? null);
        }
      },
    );
    return () => {
      mounted = false;
    };
  }, []);

  const rate = summary?.ratePercent;
  const tone = rateTone(rate, config);

  return (
    <ClickableTile
      className="qa-live-tile"
      onClick={() => history.push("/qa/qi/rejection")}
    >
      <div className="qa-cs-title">
        <FormattedMessage id="qa.overview.tile.rejectionRate" />
      </div>
      {summary === undefined ? (
        <SkeletonText heading width="40%" />
      ) : (
        <>
          <div
            className={
              "qa-live-count" + (tone !== "gray" ? ` qa-live-${tone}` : "")
            }
          >
            {rate != null ? `${rate.toFixed(2)}%` : "—"}
          </div>
          <div className="qa-live-caption">
            <FormattedMessage id="qa.overview.rejection.caption" />
          </div>
          {summary && summary.totalCount > 0 && (
            <div className="qa-live-caption">
              <FormattedMessage
                id="qa.overview.rejection.ofStarted"
                values={{
                  rejected: summary.rejectedCount,
                  total: summary.totalCount,
                }}
              />
            </div>
          )}
        </>
      )}
    </ClickableTile>
  );
};

export default RejectionRateTile;
