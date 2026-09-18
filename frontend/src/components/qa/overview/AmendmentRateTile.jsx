import React, { useEffect, useState } from "react";
import { ClickableTile, SkeletonText } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import { useHistory } from "react-router-dom";
import { getFromOpenElisServer, toLocalIsoDate } from "../../utils/Utils";
import { rateTone } from "../qi/qiThresholds";

/**
 * Amendment Rate overview tile (OGC-698): rolling-30-day share of released
 * results corrected after their patient report, from the same
 * /rest/reports/amendment endpoint the QI Dashboard tile uses so the two
 * views stay in sync. Tone comes from the resolved qi_config thresholds
 * (passed down by TodayTiles — the v1 hard-coded bands retired with OGC-710).
 * Click drills into the detail report.
 */

function windowDates() {
  const to = new Date();
  const from = new Date();
  from.setDate(from.getDate() - 30);
  return { fromDate: toLocalIsoDate(from), toDate: toLocalIsoDate(to) };
}

const AmendmentRateTile = ({ config }) => {
  const history = useHistory();
  // undefined = loading, null = fetch yielded no data
  const [summary, setSummary] = useState();

  useEffect(() => {
    let mounted = true;
    const { fromDate, toDate } = windowDates();
    getFromOpenElisServer(
      `/rest/reports/amendment/summary?fromDate=${fromDate}&toDate=${toDate}`,
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
      onClick={() => history.push("/qa/qi/amendment")}
    >
      <div className="qa-cs-title">
        <FormattedMessage id="qa.overview.tile.amendmentRate" />
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
            <FormattedMessage id="qa.overview.amendment.caption" />
          </div>
          {summary && summary.releasedCount > 0 && (
            <div className="qa-live-caption">
              <FormattedMessage
                id="qa.overview.amendment.ofReleased"
                values={{
                  amended: summary.amendedCount,
                  released: summary.releasedCount,
                }}
              />
            </div>
          )}
        </>
      )}
    </ClickableTile>
  );
};

export default AmendmentRateTile;
