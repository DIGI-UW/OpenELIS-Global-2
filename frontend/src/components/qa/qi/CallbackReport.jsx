import React from "react";
import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { toLocalIsoDateTime } from "../../utils/Utils";
import { useServerData } from "../../utils/useServerData";
import { formatMinutes } from "../common/qaDates";
import { rateTone } from "./qiThresholds";
import QIReportPage, { QIRateHeader } from "./QIReportPage";

/**
 * Critical Callback Compliance detail page (OGC-715) at /qa/qi/callback:
 * compliance header colored against qi_config thresholds, the design's
 * time-to-acknowledge histogram + failures-by-reason table (qa-final-preview
 * §callback), and the released critical results list with each result's
 * latest callback attempt. Never-logged criticals sort first — they are the
 * actionable gap list.
 */

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "sideNav.label.qa", link: "" },
  { label: "sideNav.label.qa.qi.dashboard", link: "/qa/qi/dashboard" },
  { label: "qa.qi.dashboard.tile.callback.label", link: "" },
];

const HEADERS = [
  { key: "releasedAt", labelKey: "microbiology.enum.RELEASED" },
  { key: "labNumber", labelKey: "common.labNumber" },
  { key: "testName", labelKey: "common.test" },
  { key: "resultValue", labelKey: "common.result" },
  { key: "criticalRange", labelKey: "label.critical.range" },
  { key: "status", labelKey: "qa.qi.callback.column.status" },
  { key: "timeToCallback", labelKey: "qa.qi.callback.column.timeToCallback" },
  { key: "recipientName", labelKey: "qa.qi.callback.field.recipientName" },
  { key: "loggedBy", labelKey: "qa.qi.callback.column.loggedBy" },
];

// CONFIRMED green, REACHED_NO_READBACK amber (gray Tag + class — Carbon has
// no amber), UNABLE_TO_REACH red, never-logged gray.
const STATUS_TAG_TYPE = {
  CONFIRMED: "green",
  REACHED_NO_READBACK: "gray",
  UNABLE_TO_REACH: "red",
};

// Histogram buckets (backend ackDistribution keys); red = non-compliant.
const DISTRIBUTION_BUCKETS = [
  { key: "0-5" },
  { key: "5-15" },
  { key: "15-30" },
  { key: "30-60" },
  { key: "over60", red: true },
  { key: "noAck", red: true },
];

const FAILURE_REASONS = [
  "overTarget",
  "unableToReach",
  "noReadback",
  "noCallback",
];

const toRows = (detail, intl) =>
  (detail.items || []).map((item, index) => ({
    id: `${item.analysisId}-${index}`,
    releasedAt: toLocalIsoDateTime(item.releasedAt),
    labNumber: item.labNumber || "—",
    testName: item.testName || "—",
    resultValue: item.resultValue ?? "—",
    criticalRange: item.criticalRange ?? "—",
    status: (
      <Tag
        type={STATUS_TAG_TYPE[item.status] || "gray"}
        size="sm"
        className={
          item.status === "REACHED_NO_READBACK" ? "qi-rate-tag--amber" : ""
        }
      >
        {intl.formatMessage({
          id: item.status
            ? `qa.qi.callback.status.${item.status}`
            : "qa.qi.callback.status.notLogged",
        })}
      </Tag>
    ),
    timeToCallback: formatMinutes(item.minutesToCallback),
    recipientName: item.recipientName || "—",
    loggedBy: item.loggedBy || "—",
  }));

/** Compliance header, time-to-acknowledge histogram and failure reasons. */
const CallbackSections = ({ range, config, detail }) => {
  const intl = useIntl();
  const summaryQuery = useServerData(
    `/rest/critical-callback/summary?fromDate=${range.fromDate}` +
      `&toDate=${range.toDate}`,
  );
  const summary = summaryQuery.data;

  const distribution = detail?.ackDistribution;
  const maxBucket = distribution
    ? Math.max(1, ...DISTRIBUTION_BUCKETS.map((b) => distribution[b.key] || 0))
    : 1;
  const failureTotal = detail?.failureCounts
    ? FAILURE_REASONS.reduce(
        (sum, reason) => sum + (detail.failureCounts[reason] || 0),
        0,
      )
    : 0;
  // The sections below describe the list; with no rows the empty state speaks
  // for the window instead.
  const hasRows = (detail?.items || []).length > 0;

  return (
    <>
      {summary && (
        <QIRateHeader
          label={
            <FormattedMessage
              id="qa.qi.callback.rate.label"
              values={{ minutes: summary.slaMinutes }}
            />
          }
          value={summary.compliancePercent}
          tone={rateTone(summary.compliancePercent, config)}
          testId="callback-rate-tag"
          secondary={
            <FormattedMessage
              id="qa.qi.dashboard.tile.callback.secondary"
              values={{
                confirmed: summary.confirmedCount,
                critical: summary.criticalCount,
              }}
            />
          }
        />
      )}

      {hasRows && distribution && (
        <>
          <h4 className="amendment-section__title">
            <FormattedMessage id="qa.qi.callback.distribution.title" />
          </h4>
          <div className="qi-barlist" data-testid="callback-distribution">
            {DISTRIBUTION_BUCKETS.map((bucket) => (
              <div className="qi-barlist__row" key={bucket.key}>
                <span className="qi-barlist__label">
                  {intl.formatMessage({
                    id: `qa.qi.callback.distribution.${bucket.key}`,
                  })}
                </span>
                <span className="qi-barlist__track">
                  <span
                    className={`qi-barlist__fill${
                      bucket.red ? " qi-barlist__fill--red" : ""
                    }`}
                    style={{
                      width: `${((distribution[bucket.key] || 0) / maxBucket) * 100}%`,
                    }}
                  />
                </span>
                <span className="qi-barlist__count">
                  {distribution[bucket.key] || 0}
                </span>
              </div>
            ))}
          </div>
        </>
      )}

      {hasRows && failureTotal > 0 && (
        <>
          <h4 className="amendment-section__title">
            <FormattedMessage
              id="qa.qi.callback.failures.title"
              values={{ count: failureTotal }}
            />
          </h4>
          <TableContainer>
            <Table size="sm" data-testid="callback-failures">
              <TableHead>
                <TableRow>
                  <TableHeader>
                    {intl.formatMessage({
                      id: "storage.audit.reason",
                    })}
                  </TableHeader>
                  <TableHeader>
                    {intl.formatMessage({
                      id: "reports.tat.column.count",
                    })}
                  </TableHeader>
                </TableRow>
              </TableHead>
              <TableBody>
                {FAILURE_REASONS.filter(
                  (reason) => (detail.failureCounts[reason] || 0) > 0,
                ).map((reason) => (
                  <TableRow key={reason}>
                    <TableCell>
                      {intl.formatMessage({
                        id: `qa.qi.callback.failures.${reason}`,
                      })}
                    </TableCell>
                    <TableCell>{detail.failureCounts[reason]}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </>
      )}
    </>
  );
};

const CallbackReport = () => {
  const intl = useIntl();
  return (
    <QIReportPage
      indicator="CALLBACK"
      breadcrumbs={breadcrumbs}
      titleKey="qa.qi.callback.title"
      subtitleKey="qa.qi.callback.subtitle"
      errorKey="qa.qi.dashboard.tile.callback.error"
      idPrefix="callback"
      // The callback page has never had filter labels of its own; the
      // amendment pair is the same "From"/"To".
      fromLabelKey="common.from"
      toLabelKey="to.title"
      className="adminPageContent qi-dashboard"
      detailUrl={(range, page, pageSize) =>
        `/rest/critical-callback/detail?fromDate=${range.fromDate}` +
        `&toDate=${range.toDate}&page=${page}&pageSize=${pageSize}`
      }
      headers={HEADERS}
      toRows={(detail) => toRows(detail, intl)}
      listTitleKey="qa.qi.callback.list.title"
      emptyTitleKey="qa.empty.callback.title"
      emptySubheadKey="qa.empty.callback.subhead"
    >
      {({ range, config, detail }) => (
        <CallbackSections range={range} config={config} detail={detail} />
      )}
    </QIReportPage>
  );
};

export default CallbackReport;
