import React, { useEffect, useState } from "react";
import { ClickableTile, SkeletonText } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import { useHistory } from "react-router-dom";
import { getFromOpenElisServer } from "../../utils/Utils";

/**
 * Amendment Rate overview tile (OGC-698): rolling-30-day share of released
 * results corrected after their patient report, from the same
 * /rest/reports/amendment endpoint the QI Dashboard tile uses so the two
 * views stay in sync. Click drills into the detail report.
 */

// ponytail: v1 hard-coded OGC-698 targets (<0.5 green, <1 amber, >=1 red);
// per-lab configuration arrives with QI config in v8.
const rateColor = (rate) => (rate < 0.5 ? "green" : rate < 1 ? "amber" : "red");

function windowDates() {
  const to = new Date();
  const from = new Date();
  from.setDate(from.getDate() - 30);
  const fmt = (d) => d.toISOString().split("T")[0];
  return { fromDate: fmt(from), toDate: fmt(to) };
}

const AmendmentRateTile = () => {
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
              "qa-live-count" +
              (rate != null ? ` qa-live-${rateColor(rate)}` : "")
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
