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
 */

import React, { useCallback, useEffect, useRef, useState } from "react";
import { Button, Dropdown } from "@carbon/react";
import { Renew } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer, toLocalIsoDate } from "../../utils/Utils";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { AlertDialog } from "../../common/CustomNotification";
import { formatTat, tatDelta } from "../../reports/tat/tatUtils";
import {
  NCE_DRILL_URL,
  countCriticalPending,
  countInCorrectiveAction,
  pulseColor,
} from "../overview/nceOverview";
import QITile from "./QITile";
import useQiEnabled from "./useQiEnabled";
import { rateTone, thresholdParts } from "./qiThresholds";
import "./QIDashboard.css";

// CALLBACK ships disabled (opt-in) — hidden until enabled in QI Configuration.
const GATED_INDICATORS = ["TAT", "REJECTION", "AMENDMENT", "NCE", "CALLBACK"];

const WINDOW_STORAGE_KEY = "qa.qi.dashboard.window";
const REFRESH_COOLDOWN_MS = 30000;
const DAY_MS = 24 * 60 * 60 * 1000;

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

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "sideNav.label.qa", link: "" },
  { label: "sideNav.label.qa.qi", link: "" },
  { label: "sideNav.label.qa.qi.dashboard", link: "" },
];

/** Two-call windowed fetch: current window + equal-length prior (delta). */
function fetchWindowedPair(windowId, query, setState) {
  setState({ loading: true });
  const dates = windowDates(windowId);
  let current;
  let prior;
  let pending = 2;
  const finish = () => {
    if (--pending > 0) return;
    setState({ loading: false, data: current, prior });
  };
  getFromOpenElisServer(query(dates.fromDate, dates.toDate), (res) => {
    current = res;
    finish();
  });
  getFromOpenElisServer(
    query(dates.priorFromDate, dates.priorToDate),
    (res) => {
      prior = res;
      finish();
    },
  );
}

/** Δ vs the prior window for a percent metric; tone follows higherBetter. */
function pctDelta(current, prior, higherBetter) {
  if (current == null || prior == null) {
    return null;
  }
  const diff = current - prior;
  const flat = Math.abs(diff) < 0.005;
  return {
    arrow: flat ? "—" : diff < 0 ? "↓" : "↑",
    text: flat ? "" : `${Math.abs(diff).toFixed(2)}%`,
    tone: flat ? "flat" : (higherBetter ? diff > 0 : diff < 0) ? "good" : "bad",
  };
}

const QIDashboard = () => {
  const intl = useIntl();
  const [windowId, setWindowId] = useState(
    () => localStorage.getItem(WINDOW_STORAGE_KEY) || "30d",
  );
  const [tat, setTat] = useState({ loading: true });
  const [rejection, setRejection] = useState({ loading: true });
  const [amendment, setAmendment] = useState({ loading: true });
  const [nce, setNce] = useState({ loading: true });
  const [callback, setCallback] = useState({ loading: true });
  // OGC-711 disable cascade + OGC-710 thresholds: shared hook resolves each
  // indicator's enabled flag and target/action bands.
  const {
    isEnabled,
    getConfig,
    refetch: refetchConfig,
  } = useQiEnabled(GATED_INDICATORS);
  const [lastRefreshed, setLastRefreshed] = useState(null);
  const [refreshDisabled, setRefreshDisabled] = useState(false);
  const [, setTick] = useState(0); // re-render so "last refreshed" stays fresh
  const cooldownRef = useRef(null);

  const fetchTat = useCallback(() => {
    setTat({ loading: true });
    const dates = windowDates(windowId);
    const query = (from, to) =>
      `/rest/reports/tat/summary?fromDate=${from}&toDate=${to}` +
      `&segment=RECEIPT_TO_VALIDATION&calculationMode=CALENDAR&breakdownBy=LAB_UNIT`;
    let current;
    let prior;
    let pending = 2;
    const finish = () => {
      if (--pending > 0) return;
      setTat({ loading: false, data: current, prior });
      setLastRefreshed(new Date());
    };
    getFromOpenElisServer(query(dates.fromDate, dates.toDate), (res) => {
      current = res;
      finish();
    });
    getFromOpenElisServer(
      query(dates.priorFromDate, dates.priorToDate),
      (res) => {
        prior = res;
        finish();
      },
    );
  }, [windowId]);

  const fetchAmendment = useCallback(
    () =>
      fetchWindowedPair(
        windowId,
        (from, to) =>
          `/rest/reports/amendment/summary?fromDate=${from}&toDate=${to}`,
        setAmendment,
      ),
    [windowId],
  );

  const fetchCallback = useCallback(
    () =>
      fetchWindowedPair(
        windowId,
        (from, to) =>
          `/rest/critical-callback/summary?fromDate=${from}&toDate=${to}`,
        setCallback,
      ),
    [windowId],
  );

  const fetchRejection = useCallback(() => {
    setRejection({ loading: true });
    const dates = windowDates(windowId);
    const query = (from, to) =>
      `/rest/reports/rejection/summary?fromDate=${from}&toDate=${to}`;
    let current;
    let prior;
    let pending = 2;
    const finish = () => {
      if (--pending > 0) return;
      setRejection({ loading: false, data: current, prior });
    };
    getFromOpenElisServer(query(dates.fromDate, dates.toDate), (res) => {
      current = res;
      finish();
    });
    getFromOpenElisServer(
      query(dates.priorFromDate, dates.priorToDate),
      (res) => {
        prior = res;
        finish();
      },
    );
  }, [windowId]);

  // NCE Pulse is a current-state count, not a windowed trend — fetched once
  // on mount (and on refresh), independent of the reporting window.
  const fetchNce = useCallback(() => {
    setNce({ loading: true });
    getFromOpenElisServer("/rest/nce/dashboard", (data) => {
      const list = data && Array.isArray(data.nceList) ? data.nceList : null;
      setNce({ loading: false, list });
    });
  }, []);

  useEffect(() => {
    fetchTat();
    fetchAmendment();
    fetchRejection();
    fetchCallback();
  }, [fetchTat, fetchAmendment, fetchRejection, fetchCallback]);

  useEffect(() => {
    fetchNce();
  }, [fetchNce]);

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
    fetchTat();
    fetchAmendment();
    fetchRejection();
    fetchCallback();
    fetchNce();
    refetchConfig();
    setRefreshDisabled(true);
    cooldownRef.current = setTimeout(
      () => setRefreshDisabled(false),
      REFRESH_COOLDOWN_MS,
    );
  };

  const win = WINDOWS.find((w) => w.id === windowId);

  // ---- TAT tile derivations ----
  const tatData = tat.data;
  let tatMessage = null;
  if (!tat.loading && !tatData) {
    tatMessage = intl.formatMessage({ id: "qa.qi.dashboard.tile.tat.error" });
  } else if (!tat.loading && tatData.totalCount === 0) {
    tatMessage = intl.formatMessage({ id: "qa.qi.dashboard.tile.tat.empty" });
  }

  const delta = tatDelta(tatData, tat.prior);

  let tatSecondary = null;
  if (tatData?.breakdown?.length) {
    const slowest = tatData.breakdown.reduce((a, b) =>
      (b.mean ?? 0) > (a.mean ?? 0) ? b : a,
    );
    tatSecondary = intl.formatMessage(
      { id: "qa.qi.dashboard.tile.tat.secondary" },
      {
        count: tatData.breakdown.length,
        unit: slowest.dimensionValue,
        tat: formatTat(slowest.mean),
      },
    );
  }

  // ---- Amendment tile derivations ----
  const amendmentData = amendment.data;
  let amendmentMessage = null;
  if (!amendment.loading && !amendmentData) {
    amendmentMessage = intl.formatMessage({
      id: "qa.qi.dashboard.tile.amendment.error",
    });
  } else if (!amendment.loading && amendmentData.releasedCount === 0) {
    amendmentMessage = intl.formatMessage({
      id: "qa.qi.dashboard.tile.amendment.empty",
    });
  }

  // fewer amendments = better
  const amendmentDelta = pctDelta(
    amendmentData?.ratePercent,
    amendment.prior?.ratePercent,
    false,
  );

  let amendmentSecondary = null;
  if (amendmentData?.releasedCount > 0) {
    amendmentSecondary = intl.formatMessage(
      { id: "qa.qi.dashboard.tile.amendment.secondary" },
      {
        amended: amendmentData.amendedCount,
        released: amendmentData.releasedCount,
      },
    );
  }

  // ---- Rejection tile derivations ----
  const rejectionData = rejection.data;
  let rejectionMessage = null;
  if (!rejection.loading && !rejectionData) {
    rejectionMessage = intl.formatMessage({
      id: "qa.qi.dashboard.tile.rejection.error",
    });
  } else if (!rejection.loading && rejectionData.totalCount === 0) {
    rejectionMessage = intl.formatMessage({
      id: "qa.qi.dashboard.tile.rejection.empty",
    });
  }

  let rejectionDelta = null;
  if (
    rejectionData?.ratePercent != null &&
    rejection.prior?.ratePercent != null
  ) {
    const diff = rejectionData.ratePercent - rejection.prior.ratePercent;
    const flat = Math.abs(diff) < 0.005;
    rejectionDelta = {
      arrow: flat ? "—" : diff < 0 ? "↓" : "↑",
      text: flat ? "" : `${Math.abs(diff).toFixed(2)}%`,
      // fewer rejections = better
      tone: flat ? "flat" : diff < 0 ? "good" : "bad",
    };
  }

  let rejectionSecondary = null;
  if (rejectionData?.totalCount > 0) {
    rejectionSecondary = intl.formatMessage(
      { id: "qa.qi.dashboard.tile.rejection.secondary" },
      {
        rejected: rejectionData.rejectedCount,
        total: rejectionData.totalCount,
      },
    );
  }

  // ---- Callback tile derivations ----
  const callbackData = callback.data;
  let callbackMessage = null;
  if (!callback.loading && !callbackData) {
    callbackMessage = intl.formatMessage({
      id: "qa.qi.dashboard.tile.callback.error",
    });
  } else if (!callback.loading && callbackData.enabled === false) {
    // config-gated off between the resolve and summary fetches
    callbackMessage = intl.formatMessage({
      id: "qa.qi.dashboard.tile.callback.disabled",
    });
  } else if (!callback.loading && callbackData.criticalCount === 0) {
    callbackMessage = intl.formatMessage({
      id: "qa.qi.dashboard.tile.callback.empty",
    });
  }

  // higher compliance = better
  const callbackDelta = pctDelta(
    callbackData?.compliancePercent,
    callback.prior?.compliancePercent,
    true,
  );

  let callbackSecondary = null;
  if (callbackData?.criticalCount > 0) {
    callbackSecondary = intl.formatMessage(
      { id: "qa.qi.dashboard.tile.callback.secondary" },
      {
        confirmed: callbackData.confirmedCount,
        critical: callbackData.criticalCount,
      },
    );
  }

  // ---- OGC-710 config-driven accents + threshold captions ----
  // rateTone falls back to "gray" when unjudgeable; tiles keep their blue
  // identity in that case rather than going washed-out.
  const accentFor = (value, key) => {
    const tone = rateTone(value, getConfig(key));
    return tone === "gray" ? "blue" : tone;
  };
  const thresholdLineFor = (key) => {
    const parts = thresholdParts(getConfig(key), key);
    return parts
      ? intl.formatMessage(
          { id: "qa.qi.dashboard.tile.thresholds" },
          { target: parts.target, action: parts.action },
        )
      : null;
  };

  // ---- NCE Pulse tile derivations (current-state count, not a trend) ----
  const nceCount = nce.list ? countCriticalPending(nce.list) : null;
  const nceMessage =
    !nce.loading && !nce.list
      ? intl.formatMessage({ id: "qa.qi.dashboard.tile.ncePulse.error" })
      : null;
  const nceSecondary = nce.list
    ? intl.formatMessage(
        { id: "qa.qi.dashboard.tile.ncePulse.inCorrectiveAction" },
        { count: countInCorrectiveAction(nce.list) },
      )
    : null;

  return (
    <div className="pageContent qi-dashboard" data-testid="qi-dashboard">
      <AlertDialog />
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <h2>
        <FormattedMessage id="qa.qi.dashboard.title" />
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
          {lastRefreshed && (
            <span
              className="qi-dashboard__refreshed"
              title={lastRefreshed.toLocaleString()}
            >
              {intl.formatMessage(
                { id: "qa.qi.dashboard.lastRecomputed" },
                {
                  time: intl.formatRelativeTime(
                    -Math.round((Date.now() - lastRefreshed) / 60000),
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
            <FormattedMessage id="qa.qi.dashboard.refresh" />
          </Button>
        </div>
      </div>
      <div className="qi-dashboard__tiles">
        {isEnabled("TAT") && (
          <QITile
            testId="qi-tile-tat"
            titleKey="qa.qi.dashboard.tile.tat.label"
            tooltipKey="qa.qi.dashboard.tile.tat.tooltip"
            accent={accentFor(tatData?.mean, "TAT")}
            loading={tat.loading}
            primary={formatTat(tatData?.mean)}
            delta={delta}
            targetLine={
              windowId === "ytd"
                ? intl.formatMessage({
                    id: "qa.qi.dashboard.tile.tat.vsPriorPeriod",
                  })
                : intl.formatMessage(
                    { id: "qa.qi.dashboard.tile.tat.vsPriorDays" },
                    { days: win.days },
                  )
            }
            thresholdLine={thresholdLineFor("TAT")}
            secondary={tatSecondary}
            message={tatMessage}
            detailPath="/qa/qi/tat"
          />
        )}
        {isEnabled("REJECTION") && (
          <QITile
            testId="qi-tile-rejection"
            titleKey="qa.qi.dashboard.tile.rejection.label"
            tooltipKey="qa.qi.dashboard.tile.rejection.tooltip"
            accent={accentFor(rejectionData?.ratePercent, "REJECTION")}
            loading={rejection.loading}
            primary={
              rejectionData?.ratePercent != null
                ? `${rejectionData.ratePercent.toFixed(2)}%`
                : ""
            }
            delta={rejectionDelta}
            targetLine={
              windowId === "ytd"
                ? intl.formatMessage({
                    id: "qa.qi.dashboard.tile.rejection.vsPriorPeriod",
                  })
                : intl.formatMessage(
                    { id: "qa.qi.dashboard.tile.rejection.vsPriorDays" },
                    { days: win.days },
                  )
            }
            thresholdLine={thresholdLineFor("REJECTION")}
            secondary={rejectionSecondary}
            message={rejectionMessage}
            detailPath="/qa/qi/rejection"
          />
        )}
        {isEnabled("AMENDMENT") && (
          <QITile
            testId="qi-tile-amendment"
            titleKey="qa.qi.dashboard.tile.amendment.label"
            tooltipKey="qa.qi.dashboard.tile.amendment.tooltip"
            accent={accentFor(amendmentData?.ratePercent, "AMENDMENT")}
            loading={amendment.loading}
            primary={
              amendmentData?.ratePercent != null
                ? `${amendmentData.ratePercent.toFixed(2)}%`
                : ""
            }
            delta={amendmentDelta}
            targetLine={
              windowId === "ytd"
                ? intl.formatMessage({
                    id: "qa.qi.dashboard.tile.amendment.vsPriorPeriod",
                  })
                : intl.formatMessage(
                    { id: "qa.qi.dashboard.tile.amendment.vsPriorDays" },
                    { days: win.days },
                  )
            }
            thresholdLine={thresholdLineFor("AMENDMENT")}
            secondary={amendmentSecondary}
            message={amendmentMessage}
            detailPath="/qa/qi/amendment"
          />
        )}
        {isEnabled("NCE") && (
          <QITile
            testId="qi-tile-nce-pulse"
            titleKey="qa.qi.dashboard.tile.ncePulse.label"
            tooltipKey="qa.qi.dashboard.tile.ncePulse.tooltip"
            accent={nceCount != null ? pulseColor(nceCount) : "blue"}
            loading={nce.loading}
            primary={nceCount != null ? String(nceCount) : ""}
            targetLine={intl.formatMessage({
              id: "qa.qi.dashboard.tile.ncePulse.criticalPending",
            })}
            secondary={nceSecondary}
            message={nceMessage}
            detailPath={NCE_DRILL_URL}
          />
        )}
        {isEnabled("CALLBACK") && (
          <QITile
            testId="qi-tile-callback"
            titleKey="qa.qi.dashboard.tile.callback.label"
            tooltipKey="qa.qi.dashboard.tile.callback.tooltip"
            accent={accentFor(callbackData?.compliancePercent, "CALLBACK")}
            loading={callback.loading}
            primary={
              callbackData?.compliancePercent != null
                ? `${callbackData.compliancePercent.toFixed(2)}%`
                : ""
            }
            delta={callbackDelta}
            targetLine={
              windowId === "ytd"
                ? intl.formatMessage({
                    id: "qa.qi.dashboard.tile.callback.vsPriorPeriod",
                  })
                : intl.formatMessage(
                    { id: "qa.qi.dashboard.tile.callback.vsPriorDays" },
                    { days: win.days },
                  )
            }
            thresholdLine={thresholdLineFor("CALLBACK")}
            secondary={callbackSecondary}
            message={callbackMessage}
            detailPath="/qa/qi/callback"
          />
        )}
      </div>
    </div>
  );
};

export default QIDashboard;
