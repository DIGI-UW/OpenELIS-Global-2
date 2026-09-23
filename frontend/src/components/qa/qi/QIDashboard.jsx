/**
 * QIDashboard Component
 *
 * Quality Indicators dashboard (OGC-695, thresholds OGC-710) at
 * /qa/qi/dashboard. Five tiles in fixed order: Average TAT (OGC-696),
 * Rejection Rate (OGC-697/710), Amendment Rate (OGC-698), NCE Pulse
 * (OGC-699), Critical Callback Compliance (opt-in, OGC-715). Each metric
 * tile wraps its report summary API: one call for the selected window, one
 * for the equal-length prior window (delta). TAT, Rejection, and Amendment
 * color against their resolved qi_config target/action bands (blue when
 * unjudgeable); NCE Pulse keeps its count-based pulse color — it ships
 * without numeric thresholds by design.
 *
 * The five differ only in the endpoint they read and how they phrase their
 * numbers, so they are one tile driven by the INDICATORS table below.
 */

import React, { useEffect, useRef, useState } from "react";
import { Button, Dropdown } from "@carbon/react";
import { Renew } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { toLocalIsoDate } from "../../utils/Utils";
import {
  useInvalidateServerData,
  useServerData,
} from "../../utils/useServerData";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { formatTat, pctDelta, tatDelta } from "../../reports/tat/tatUtils";
import { DAY_MS } from "../common/qaDates";
import {
  NCE_DRILL_URL,
  NCE_LIST_URL,
  countCriticalPending,
  countInCorrectiveAction,
  pulseColor,
} from "../overview/nceOverview";
import QITile from "./QITile";
import useQiConfig from "./useQiConfig";
import { rateTone, thresholdParts } from "./qiThresholds";
import "../common/QAStyles.css";

const WINDOW_STORAGE_KEY = "qa.qi.dashboard.window";
const REFRESH_COOLDOWN_MS = 30000;

const WINDOWS = [
  { id: "7d", days: 7, labelKey: "qa.qi.dashboard.window.7d" },
  { id: "30d", days: 30, labelKey: "qa.qi.dashboard.window.30d" },
  { id: "90d", days: 90, labelKey: "qa.qi.dashboard.window.90d" },
  { id: "ytd", labelKey: "qa.qi.dashboard.window.ytd" },
];

function windowDates(windowId) {
  const to = new Date();
  const from = new Date();
  const days = WINDOWS.find((w) => w.id === windowId)?.days;
  if (days) {
    from.setDate(from.getDate() - days);
  } else {
    from.setMonth(0, 1); // year to date
  }
  const priorTo = new Date(from.getTime() - DAY_MS);
  const priorFrom = new Date(from.getTime() - DAY_MS - (to - from));
  return {
    fromDate: toLocalIsoDate(from),
    toDate: toLocalIsoDate(to),
    priorFromDate: toLocalIsoDate(priorFrom),
    priorToDate: toLocalIsoDate(priorTo),
  };
}

const tatUrl = (from, to) =>
  `/rest/reports/tat/summary?fromDate=${from}&toDate=${to}` +
  `&segment=RECEIPT_TO_VALIDATION&calculationMode=CALENDAR&breakdownBy=LAB_UNIT`;

const percent = (value) => (value != null ? `${value.toFixed(2)}%` : "");

/**
 * One row per tile: which qi_config indicator gates it, which endpoint it
 * reads, the number its thresholds judge, and how it phrases itself. `name`
 * is the qa.qi.dashboard.tile.<name>.* message family and the tile's testId.
 */
const INDICATORS = [
  {
    key: "TAT",
    name: "tat",
    testId: "qi-tile-tat",
    detailPath: "/qa/qi/tat",
    url: tatUrl,
    value: (data) => data.mean,
    primary: (data) => formatTat(data?.mean),
    delta: (data, prior) => tatDelta(data, prior),
    message: (data) => (data.totalCount === 0 ? "empty" : null),
    secondary: (data, intl) => {
      if (!data.breakdown?.length) {
        return null;
      }
      const slowest = data.breakdown.reduce((a, b) =>
        (b.mean ?? 0) > (a.mean ?? 0) ? b : a,
      );
      return intl.formatMessage(
        { id: "qa.qi.dashboard.tile.tat.secondary" },
        {
          count: data.breakdown.length,
          unit: slowest.dimensionValue,
          tat: formatTat(slowest.mean),
        },
      );
    },
  },
  {
    key: "REJECTION",
    name: "rejection",
    testId: "qi-tile-rejection",
    detailPath: "/qa/qi/rejection",
    url: (from, to) =>
      `/rest/reports/rejection/summary?fromDate=${from}&toDate=${to}`,
    value: (data) => data.ratePercent,
    primary: (data) => percent(data?.ratePercent),
    // fewer rejections = better
    delta: (data, prior) =>
      pctDelta(data?.ratePercent, prior?.ratePercent, false),
    message: (data) => (data.totalCount === 0 ? "empty" : null),
    secondary: (data, intl) =>
      data.totalCount > 0
        ? intl.formatMessage(
            { id: "qa.qi.dashboard.tile.rejection.secondary" },
            { rejected: data.rejectedCount, total: data.totalCount },
          )
        : null,
  },
  {
    key: "AMENDMENT",
    name: "amendment",
    testId: "qi-tile-amendment",
    detailPath: "/qa/qi/amendment",
    url: (from, to) =>
      `/rest/reports/amendment/summary?fromDate=${from}&toDate=${to}`,
    value: (data) => data.ratePercent,
    primary: (data) => percent(data?.ratePercent),
    // fewer amendments = better
    delta: (data, prior) =>
      pctDelta(data?.ratePercent, prior?.ratePercent, false),
    message: (data) => (data.releasedCount === 0 ? "empty" : null),
    secondary: (data, intl) =>
      data.releasedCount > 0
        ? intl.formatMessage(
            { id: "qa.qi.dashboard.tile.amendment.secondary" },
            { amended: data.amendedCount, released: data.releasedCount },
          )
        : null,
  },
  {
    // A current-state count, not a windowed trend: fetched once, whatever the
    // reporting window, and banded by count rather than by qi_config.
    key: "NCE",
    name: "ncePulse",
    testId: "qi-tile-nce-pulse",
    detailPath: NCE_DRILL_URL,
    url: () => NCE_LIST_URL,
    windowed: false,
    value: (data) =>
      Array.isArray(data.nceList) ? countCriticalPending(data.nceList) : null,
    primary: (data, value) => (value != null ? String(value) : ""),
    accent: (value) => (value != null ? pulseColor(value) : "blue"),
    targetLineKey: "qa.qi.dashboard.tile.ncePulse.criticalPending",
    message: (data) => (Array.isArray(data.nceList) ? null : "error"),
    secondary: (data, intl) =>
      Array.isArray(data.nceList)
        ? intl.formatMessage(
            { id: "qa.qi.dashboard.tile.ncePulse.inCorrectiveAction" },
            { count: countInCorrectiveAction(data.nceList) },
          )
        : null,
  },
  {
    key: "CALLBACK",
    name: "callback",
    testId: "qi-tile-callback",
    detailPath: "/qa/qi/callback",
    url: (from, to) =>
      `/rest/critical-callback/summary?fromDate=${from}&toDate=${to}`,
    value: (data) => data.compliancePercent,
    primary: (data) => percent(data?.compliancePercent),
    // higher compliance = better
    delta: (data, prior) =>
      pctDelta(data?.compliancePercent, prior?.compliancePercent, true),
    message: (data) => {
      // config-gated off between the resolve and summary reads
      if (data.enabled === false) {
        return "disabled";
      }
      return data.criticalCount === 0 ? "empty" : null;
    },
    secondary: (data, intl) =>
      data.criticalCount > 0
        ? intl.formatMessage(
            { id: "qa.qi.dashboard.tile.callback.secondary" },
            { confirmed: data.confirmedCount, critical: data.criticalCount },
          )
        : null,
  },
];

const QIIndicatorTile = ({ indicator, win }) => {
  const intl = useIntl();
  const { enabled, config } = useQiConfig(indicator.key);
  const dates = windowDates(win.id);
  const windowed = indicator.windowed !== false;
  const current = useServerData(indicator.url(dates.fromDate, dates.toDate));
  const prior = useServerData(
    windowed ? indicator.url(dates.priorFromDate, dates.priorToDate) : null,
  );

  if (!enabled) {
    return null;
  }

  const loading = current.isLoading || (windowed && prior.isLoading);
  const data = current.data;
  const value = data ? indicator.value(data) : null;

  const messageKey = loading
    ? null
    : (!data && "error") || indicator.message(data);
  const tone = rateTone(value, config);
  const thresholds = thresholdParts(config, indicator.key);

  return (
    <QITile
      testId={indicator.testId}
      titleKey={`qa.qi.dashboard.tile.${indicator.name}.label`}
      tooltipKey={`qa.qi.dashboard.tile.${indicator.name}.tooltip`}
      // rateTone falls back to "gray" when unjudgeable; tiles keep their blue
      // identity in that case rather than going washed-out.
      accent={
        indicator.accent
          ? indicator.accent(value)
          : tone === "gray"
            ? "blue"
            : tone
      }
      loading={loading}
      primary={indicator.primary(data, value)}
      delta={indicator.delta ? indicator.delta(data, prior.data) : undefined}
      targetLine={intl.formatMessage(
        {
          id:
            indicator.targetLineKey ||
            (win.days
              ? `qa.qi.dashboard.tile.${indicator.name}.vsPriorDays`
              : `qa.qi.dashboard.tile.${indicator.name}.vsPriorPeriod`),
        },
        { days: win.days },
      )}
      thresholdLine={
        thresholds
          ? intl.formatMessage(
              { id: "qa.qi.dashboard.tile.thresholds" },
              { target: thresholds.target, action: thresholds.action },
            )
          : null
      }
      secondary={data ? indicator.secondary(data, intl) : null}
      message={
        messageKey
          ? intl.formatMessage({
              id: `qa.qi.dashboard.tile.${indicator.name}.${messageKey}`,
            })
          : null
      }
      detailPath={indicator.detailPath}
    />
  );
};

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "sideNav.label.qa", link: "" },
  { label: "sideNav.label.qa.qi", link: "" },
  { label: "sideNav.label.qa.qi.dashboard", link: "" },
];

const QIDashboard = () => {
  const intl = useIntl();
  const [windowId, setWindowId] = useState(
    () => localStorage.getItem(WINDOW_STORAGE_KEY) || "30d",
  );
  const win = WINDOWS.find((w) => w.id === windowId);
  const invalidateServerData = useInvalidateServerData();
  const [refreshDisabled, setRefreshDisabled] = useState(false);
  const [, setTick] = useState(0); // re-render so "last refreshed" stays fresh
  const cooldownRef = useRef(null);

  // When the numbers on screen were last read from the server. The TAT tile
  // shares this cache entry, so watching it costs no extra request.
  const dates = windowDates(windowId);
  const { dataUpdatedAt } = useServerData(tatUrl(dates.fromDate, dates.toDate));

  useEffect(() => {
    const timer = setInterval(() => setTick((t) => t + 1), 60000);
    return () => {
      clearInterval(timer);
      clearTimeout(cooldownRef.current);
    };
  }, []);

  const handleWindowChange = ({ selectedItem }) => {
    localStorage.setItem(WINDOW_STORAGE_KEY, selectedItem.id);
    setWindowId(selectedItem.id);
  };

  const handleRefresh = () => {
    // Every read, not just this screen's: the tiles, their prior windows and
    // the resolved configs all have to move together or the page reads as
    // half-refreshed.
    invalidateServerData();
    setRefreshDisabled(true);
    cooldownRef.current = setTimeout(
      () => setRefreshDisabled(false),
      REFRESH_COOLDOWN_MS,
    );
  };

  return (
    <div className="pageContent qi-dashboard" data-testid="qi-dashboard">
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <h2>
        <FormattedMessage id="sideNav.label.qa.qi.dashboard" />
      </h2>
      <p className="qi-dashboard__subtitle">
        <FormattedMessage id="qa.qi.dashboard.subtitle" />
      </p>
      <div className="qi-dashboard__controls">
        <Dropdown
          id="qi-dashboard-window"
          size="sm"
          type="inline"
          data-testid="qi-dashboard-window"
          titleText={intl.formatMessage({
            id: "qa.qi.dashboard.window.label",
          })}
          label=""
          items={WINDOWS}
          selectedItem={win}
          itemToString={(item) =>
            item ? intl.formatMessage({ id: item.labelKey }) : ""
          }
          onChange={handleWindowChange}
        />
        <div className="qi-dashboard__refresh">
          {dataUpdatedAt > 0 && (
            <span
              className="qi-dashboard__refreshed"
              title={new Date(dataUpdatedAt).toLocaleString()}
            >
              {intl.formatMessage(
                { id: "qa.qi.dashboard.lastRecomputed" },
                {
                  time: intl.formatRelativeTime(
                    -Math.round((Date.now() - dataUpdatedAt) / 60000),
                    "minute",
                    { numeric: "auto" },
                  ),
                },
              )}
            </span>
          )}
          <Button
            kind="ghost"
            size="sm"
            renderIcon={Renew}
            disabled={refreshDisabled}
            onClick={handleRefresh}
            data-testid="qi-dashboard-refresh"
          >
            <FormattedMessage id="qc.dashboard.refresh" />
          </Button>
        </div>
      </div>
      <div className="qi-dashboard__tiles">
        {INDICATORS.map((indicator) => (
          <QIIndicatorTile
            key={indicator.key}
            indicator={indicator}
            win={win}
          />
        ))}
      </div>
    </div>
  );
};

export default QIDashboard;
