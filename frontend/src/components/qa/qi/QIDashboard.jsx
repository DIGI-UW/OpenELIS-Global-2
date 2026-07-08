/**
 * QIDashboard Component
 *
 * Quality Indicators dashboard MVP (OGC-695) at /qa/qi/dashboard.
 * Four tiles in fixed order: Average TAT (live, OGC-696), Rejection Rate
 * (OGC-697), Amendment Rate (OGC-698), NCE Pulse (OGC-699). Coming-soon
 * tiles are replaced by their own workstreams — deleted, never reworked.
 *
 * The TAT tile wraps the existing /rest/reports/tat/summary API: one call
 * for the selected window, one for the equal-length prior window (delta).
 * No dedicated tile endpoint — thresholds/compliance arrive in v8 (OGC-709).
 */

import React, { useCallback, useEffect, useRef, useState } from "react";
import { Button, Dropdown } from "@carbon/react";
import { Renew } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { formatTat } from "../../reports/tat/tatUtils";
import QITile from "./QITile";
import "./QIDashboard.css";

const WINDOW_STORAGE_KEY = "qa.qi.dashboard.window";
const REFRESH_COOLDOWN_MS = 30000;
const DAY_MS = 24 * 60 * 60 * 1000;

const WINDOWS = [
  { id: "7d", days: 7, labelKey: "qa.qi.dashboard.window.7d" },
  { id: "30d", days: 30, labelKey: "qa.qi.dashboard.window.30d" },
  { id: "90d", days: 90, labelKey: "qa.qi.dashboard.window.90d" },
  { id: "ytd", labelKey: "qa.qi.dashboard.window.ytd" },
];

function formatDate(d) {
  return d.toISOString().split("T")[0];
}

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
    fromDate: formatDate(from),
    toDate: formatDate(to),
    priorFromDate: formatDate(priorFrom),
    priorToDate: formatDate(priorTo),
  };
}

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
  const [tat, setTat] = useState({ loading: true });
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

  useEffect(() => {
    fetchTat();
  }, [fetchTat]);

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

  let tatDelta = null;
  if (tatData?.totalCount > 0 && tat.prior?.totalCount > 0) {
    const diff = tatData.mean - tat.prior.mean;
    const flat = Math.abs(diff) < 1 / 60; // under a minute
    tatDelta = {
      arrow: flat ? "—" : diff < 0 ? "↓" : "↑",
      text: flat ? "" : formatTat(Math.abs(diff)),
      tone: flat ? "flat" : diff < 0 ? "good" : "bad",
    };
  }

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

  return (
    <div className="adminPageContent qi-dashboard" data-testid="qi-dashboard">
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
        <QITile
          testId="qi-tile-tat"
          titleKey="qa.qi.dashboard.tile.tat.label"
          tooltipKey="qa.qi.dashboard.tile.tat.tooltip"
          accent="blue"
          loading={tat.loading}
          primary={formatTat(tatData?.mean)}
          delta={tatDelta}
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
          secondary={tatSecondary}
          message={tatMessage}
          detailPath="/qa/qi/tat"
        />
        <QITile
          testId="qi-tile-rejection"
          titleKey="qa.qi.dashboard.tile.rejection.label"
          comingSoonTicket="OGC-697"
        />
        <QITile
          testId="qi-tile-amendment"
          titleKey="qa.qi.dashboard.tile.amendment.label"
          comingSoonTicket="OGC-698"
        />
        <QITile
          testId="qi-tile-nce-pulse"
          titleKey="qa.qi.dashboard.tile.ncePulse.label"
          comingSoonTicket="OGC-699"
        />
      </div>
    </div>
  );
};

export default QIDashboard;
