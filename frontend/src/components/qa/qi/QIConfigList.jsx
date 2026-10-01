import React, { useState } from "react";
import { Button, DataTableSkeleton, Tag } from "@carbon/react";
import { Edit } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  useInvalidateServerData,
  useServerData,
} from "../../utils/useServerData";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import ServerDataState from "../../utils/ServerDataState";
import QASimpleTable from "../common/QASimpleTable";
import QIConfigEditor from "./QIConfigEditor";
import { thresholdParts } from "./qiThresholds";
import "../common/QAStyles.css";

/**
 * OGC-709 — QI Configuration admin page at /qa/qi/config. Lists the four quality
 * indicators with their default thresholds + enabled flag and a count of
 * per-test-section overrides; each row opens the two-level editor. Route is
 * gated on qa.manage.qi (SecureRoute in App.jsx); no add/delete of indicators —
 * the set is fixed, you disable rather than remove.
 */

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "sideNav.label.qa", link: "" },
  { label: "sideNav.label.qa.qi", link: "" },
  { label: "sideNav.label.qa.qi.config", link: "" },
];

const HEADERS = [
  { key: "indicator", labelKey: "reports.label.indicator" },
  { key: "enabled", labelKey: "qc.westgard.rule.enabled" },
  { key: "target", labelKey: "qa.qiConfig.field.target" },
  { key: "action", labelKey: "qa.qiConfig.field.action" },
  { key: "overrides", labelKey: "qa.qiConfig.column.overrides" },
  { key: "actions", labelKey: "common.actions" },
];

const CONFIG_ENDPOINT = "/rest/qi-config";

const bands = (cfg) =>
  thresholdParts({ ...cfg, enabled: true }, cfg.indicatorKey);

const QIConfigList = () => {
  const intl = useIntl();
  const [editing, setEditing] = useState(null);
  const query = useServerData(CONFIG_ENDPOINT);
  const invalidateServerData = useInvalidateServerData();
  const configs = Array.isArray(query.data) ? query.data : null;

  const rows = (configs || []).map((cfg) => ({
    id: cfg.indicatorKey,
    indicator: intl.formatMessage({
      id: `qa.qiConfig.indicator.${cfg.indicatorKey.toLowerCase()}`,
    }),
    enabled: (
      <Tag type={cfg.enabled ? "green" : "gray"} size="sm">
        <FormattedMessage id={cfg.enabled ? "label.yes" : "label.no"} />
      </Tag>
    ),
    // Target shows the good side ("≤ 2%" when lower is better), action the
    // breach side ("≥ 5%") — the same strings the dashboard tiles caption
    // themselves with. Read as enabled: this table shows what an indicator is
    // configured to, and the Enabled column next to it says whether it is on.
    target: bands(cfg)?.target ?? "—",
    action: bands(cfg)?.action ?? "—",
    overrides: cfg.overrides ? cfg.overrides.length : 0,
    actions: (
      <Button
        kind="ghost"
        size="sm"
        renderIcon={Edit}
        iconDescription={intl.formatMessage({ id: "label.button.edit" })}
        hasIconOnly
        onClick={() => setEditing(cfg)}
        data-testid={`qi-config-edit-${cfg.indicatorKey}`}
      />
    ),
  }));

  return (
    <div className="pageContent qi-dashboard" data-testid="qi-config">
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <h2>
        <FormattedMessage id="sideNav.label.qa.qi.config" />
      </h2>
      <p className="qi-dashboard__subtitle">
        <FormattedMessage id="qa.qiConfig.subtitle" />
      </p>

      {query.isFetching ? (
        <DataTableSkeleton columnCount={HEADERS.length} rowCount={4} />
      ) : !configs ? (
        // A failed read of the only thing this page shows: the shared state
        // says so and offers the retry, rather than a dead sentence.
        <ServerDataState query={query} />
      ) : (
        <QASimpleTable rows={rows} headers={HEADERS} size="lg" />
      )}

      {editing && (
        <QIConfigEditor
          indicator={editing}
          onClose={(saved) => {
            setEditing(null);
            if (saved) {
              // The resolved configs the tiles and detail pages read change
              // with it, so retire every server read rather than this list.
              invalidateServerData();
            }
          }}
        />
      )}
    </div>
  );
};

export default QIConfigList;
