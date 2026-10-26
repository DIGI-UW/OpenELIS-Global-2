import React, { useContext, useEffect, useRef, useState } from "react";
import { Button, ContentSwitcher, Dropdown, Switch } from "@carbon/react";
import { Renew } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import UserSessionDetailsContext from "../../../UserSessionDetailsContext";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import ServerDataState from "../../utils/ServerDataState";
import {
  useInvalidateServerData,
  useServerData,
} from "../../utils/useServerData";
import KpiStrip from "./KpiStrip";
import { summaryUrl } from "./managementApi";
import WorkloadSection from "./WorkloadSection";
import "./ManagementDashboard.css";

export const SCOPES = [
  { id: "7d", labelKey: "label.dashboard.scope.last7days" },
  { id: "30d", labelKey: "label.dashboard.scope.last30days" },
  { id: "90d", labelKey: "label.dashboard.scope.last90days" },
  { id: "quarter", labelKey: "label.dashboard.scope.quarter" },
  { id: "ytd", labelKey: "label.dashboard.scope.yearToDate" },
  { id: "year", labelKey: "label.dashboard.scope.year" },
];

const SECTIONS = [
  { id: "kpis", Component: KpiStrip },
  { id: "workload", Component: WorkloadSection },
];

const REFRESH_COOLDOWN_MS = 15000;
const SOFT_REFRESH_MS = 5 * 60000;

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "label.mgmt.title", link: "" },
];

const readScope = (key) => {
  try {
    const stored = localStorage.getItem(key);
    return SCOPES.some((s) => s.id === stored) ? stored : "30d";
  } catch {
    return "30d";
  }
};

const ManagementDashboard = () => {
  const intl = useIntl();
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const storageKey = `mgmtDashboard.scope.${userSessionDetails?.userId}`;
  const [scope, setScope] = useState(() => readScope(storageKey));
  const [refreshDisabled, setRefreshDisabled] = useState(false);
  const [, setTick] = useState(0);
  const cooldownRef = useRef(null);
  const invalidateServerData = useInvalidateServerData();
  const summary = useServerData(summaryUrl(scope));

  useEffect(() => {
    const tick = setInterval(() => setTick((t) => t + 1), 60000);
    const soft = setInterval(() => invalidateServerData(), SOFT_REFRESH_MS);
    return () => {
      clearInterval(tick);
      clearInterval(soft);
      clearTimeout(cooldownRef.current);
    };
  }, []);

  const selectScope = ({ name }) => {
    try {
      localStorage.setItem(storageKey, name);
    } catch {
      // storage unavailable: the choice lasts for this visit only
    }
    setScope(name);
  };

  const refresh = () => {
    invalidateServerData();
    setRefreshDisabled(true);
    cooldownRef.current = setTimeout(
      () => setRefreshDisabled(false),
      REFRESH_COOLDOWN_MS,
    );
  };

  const scopeAriaLabel = intl.formatMessage({ id: "label.dashboard.scope" });
  const scopeLabel = intl.formatMessage({
    id: SCOPES.find((s) => s.id === scope).labelKey,
  });
  const data = summary.data;

  return (
    <div className="pageContent" data-testid="mgmt-dashboard">
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <div className="mgmt-dashboard__header">
        <div className="mgmt-dashboard__header-row">
          <div>
            <h2>
              <FormattedMessage id="label.mgmt.title" />
            </h2>
            {data && (
              <p
                className="mgmt-dashboard__subtitle"
                data-testid="mgmt-subtitle"
              >
                {data.labName ? (
                  <FormattedMessage
                    id="label.mgmt.subtitle"
                    values={{
                      0: data.labName,
                      1: scopeLabel,
                      2: intl.formatNumber(data.samplesReceived),
                    }}
                  />
                ) : (
                  <FormattedMessage
                    id="label.mgmt.subtitle.noSiteName"
                    values={{
                      0: scopeLabel,
                      1: intl.formatNumber(data.samplesReceived),
                    }}
                  />
                )}
              </p>
            )}
          </div>
          <div className="mgmt-dashboard__controls">
            <ContentSwitcher
              className="mgmt-dashboard__scope"
              size="sm"
              aria-label={scopeAriaLabel}
              selectedIndex={SCOPES.findIndex((s) => s.id === scope)}
              onChange={selectScope}
            >
              {SCOPES.map((s) => (
                <Switch
                  key={s.id}
                  name={s.id}
                  text={intl.formatMessage({ id: s.labelKey })}
                />
              ))}
            </ContentSwitcher>
            <Dropdown
              id="mgmt-scope-select"
              className="mgmt-dashboard__scope-select"
              size="sm"
              titleText={scopeAriaLabel}
              hideLabel
              label=""
              items={SCOPES}
              selectedItem={SCOPES.find((s) => s.id === scope)}
              itemToString={(s) =>
                s ? intl.formatMessage({ id: s.labelKey }) : ""
              }
              onChange={({ selectedItem }) =>
                selectScope({ name: selectedItem.id })
              }
            />
            <div className="mgmt-dashboard__refresh">
              {summary.dataUpdatedAt > 0 && (
                <span
                  className="mgmt-dashboard__updated"
                  data-testid="mgmt-updated"
                  title={new Date(summary.dataUpdatedAt).toLocaleString()}
                >
                  <FormattedMessage
                    id="label.dashboard.lastUpdated"
                    values={{
                      0: Math.floor(
                        (Date.now() - summary.dataUpdatedAt) / 60000,
                      ),
                    }}
                  />
                </span>
              )}
              <Button
                kind="ghost"
                size="sm"
                renderIcon={Renew}
                disabled={refreshDisabled}
                onClick={refresh}
                data-testid="mgmt-refresh"
              >
                <FormattedMessage id="label.dashboard.refresh" />
              </Button>
            </div>
          </div>
        </div>
      </div>
      {!data && <ServerDataState query={summary} />}
      {data &&
        SECTIONS.map(({ id, Component }) => (
          <Component
            key={id}
            scope={scope}
            scopeLabel={scopeLabel}
            features={data.features}
          />
        ))}
    </div>
  );
};

export default ManagementDashboard;
