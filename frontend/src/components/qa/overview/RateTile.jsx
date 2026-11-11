import React from "react";
import { useIntl } from "react-intl";
import { useServerData } from "../../utils/useServerData";
import { lastDays } from "../common/qaDates";
import QITile from "../qi/QITile";
import useQiConfig from "../qi/useQiConfig";
import { rateTone } from "../qi/qiThresholds";
import { callbackSummaryUrl } from "./overviewData";

/**
 * The QA Overview's rolling-30-day rate tiles (OGC-697/698/714-715). Each
 * reads the same summary endpoint its QI Dashboard tile does, so the two views
 * stay in sync, and tones against the resolved qi_config thresholds (OGC-710).
 * Rendering is gated on the indicator's enabled flag (OGC-711), which is what
 * keeps the opt-in CALLBACK tile hidden until a lab turns it on.
 */

const RATES = {
  REJECTION: {
    titleKey: "qa.qi.dashboard.tile.rejection.label",
    captionKey: "qa.overview.rejection.caption",
    detailPath: "/qa/qi/rejection",
    summaryUrl: ({ fromDate, toDate }) =>
      `/rest/reports/rejection/summary?fromDate=${fromDate}&toDate=${toDate}`,
    rate: (summary) => summary.ratePercent,
    secondary: (summary, intl) =>
      summary.totalCount > 0
        ? intl.formatMessage(
            { id: "qa.overview.rejection.ofStarted" },
            { rejected: summary.rejectedCount, total: summary.totalCount },
          )
        : null,
  },
  AMENDMENT: {
    titleKey: "qa.qi.dashboard.tile.amendment.label",
    captionKey: "qa.overview.amendment.caption",
    detailPath: "/qa/qi/amendment",
    summaryUrl: ({ fromDate, toDate }) =>
      `/rest/reports/amendment/summary?fromDate=${fromDate}&toDate=${toDate}`,
    rate: (summary) => summary.ratePercent,
    secondary: (summary, intl) =>
      summary.releasedCount > 0
        ? intl.formatMessage(
            { id: "qa.overview.amendment.ofReleased" },
            { amended: summary.amendedCount, released: summary.releasedCount },
          )
        : null,
  },
  CALLBACK: {
    titleKey: "qa.overview.tile.criticalCallback",
    captionKey: "qa.overview.callback.caption",
    detailPath: "/qa/qi/callback",
    summaryUrl: ({ fromDate, toDate }) => callbackSummaryUrl(fromDate, toDate),
    rate: (summary) => summary.compliancePercent,
    secondary: (summary, intl) =>
      intl.formatMessage(
        {
          id:
            summary.criticalCount > 0
              ? "qa.overview.callback.ofCritical"
              : "qa.overview.callback.none",
        },
        { confirmed: summary.confirmedCount, critical: summary.criticalCount },
      ),
  },
};

const RateTile = ({ indicator }) => {
  const intl = useIntl();
  const spec = RATES[indicator];
  const { enabled, config } = useQiConfig(indicator);
  const query = useServerData(spec.summaryUrl(lastDays(30)));
  const summary = query.data ?? null;

  if (!enabled) {
    return null;
  }

  const rate = summary ? spec.rate(summary) : null;
  const tone = rateTone(rate, config);

  return (
    <QITile
      testId={`qa-overview-tile-${indicator.toLowerCase()}`}
      titleKey={spec.titleKey}
      accent={tone === "gray" ? "blue" : tone}
      loading={query.isLoading}
      primary={rate != null ? `${rate.toFixed(2)}%` : "—"}
      targetLine={intl.formatMessage({ id: spec.captionKey })}
      secondary={summary ? spec.secondary(summary, intl) : null}
      detailPath={spec.detailPath}
    />
  );
};

export default RateTile;
