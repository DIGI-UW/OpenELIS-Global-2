import React, { useEffect, useState } from "react";
import {
  Button,
  DataTable,
  DataTableSkeleton,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
} from "@carbon/react";
import { Edit } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import QIConfigEditor from "./QIConfigEditor";
import "./QIDashboard.css";

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
  { label: "qa.qiConfig.title", link: "" },
];

const HEADERS = [
  { key: "indicator", labelKey: "qa.qiConfig.column.indicator" },
  { key: "enabled", labelKey: "qa.qiConfig.column.enabled" },
  { key: "target", labelKey: "qa.qiConfig.column.target" },
  { key: "action", labelKey: "qa.qiConfig.column.action" },
  { key: "overrides", labelKey: "qa.qiConfig.column.overrides" },
  { key: "actions", labelKey: "qa.qiConfig.column.actions" },
];

// higher-is-better shows "≥ target", lower-is-better shows "≤ target".
function thresholdText(value, direction) {
  if (value === null || value === undefined) {
    return "—";
  }
  const op = direction === "HIGHER_BETTER" ? "≥" : "≤";
  return `${op} ${value}`;
}

const QIConfigList = () => {
  const intl = useIntl();
  // undefined = loading, null = fetch failed
  const [configs, setConfigs] = useState();
  const [editing, setEditing] = useState(null);

  const load = () => {
    setConfigs(undefined);
    getFromOpenElisServer("/rest/qi-config", (res) =>
      setConfigs(Array.isArray(res) ? res : null),
    );
  };

  useEffect(load, []);

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
    target: thresholdText(cfg.target, cfg.direction),
    action: thresholdText(cfg.action, cfg.direction),
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
    <div className="adminPageContent qi-dashboard" data-testid="qi-config">
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <h2>
        <FormattedMessage id="qa.qiConfig.title" />
      </h2>
      <p className="qi-dashboard__subtitle">
        <FormattedMessage id="qa.qiConfig.subtitle" />
      </p>

      {configs === undefined ? (
        <DataTableSkeleton columnCount={HEADERS.length} rowCount={4} />
      ) : configs === null ? (
        <p className="qi-tile__message">
          <FormattedMessage id="qa.qiConfig.error" />
        </p>
      ) : (
        <DataTable
          rows={rows}
          headers={HEADERS.map((h) => ({
            key: h.key,
            header: intl.formatMessage({ id: h.labelKey }),
          }))}
        >
          {({ rows: tableRows, headers, getHeaderProps, getRowProps }) => (
            <TableContainer>
              <Table size="lg">
                <TableHead>
                  <TableRow>
                    {headers.map((header) => (
                      <TableHeader
                        {...getHeaderProps({ header })}
                        key={header.key}
                      >
                        {header.header}
                      </TableHeader>
                    ))}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {tableRows.map((row) => (
                    <TableRow {...getRowProps({ row })} key={row.id}>
                      {row.cells.map((cell) => (
                        <TableCell key={cell.id}>{cell.value}</TableCell>
                      ))}
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </DataTable>
      )}

      {editing && (
        <QIConfigEditor
          indicator={editing}
          onClose={(saved) => {
            setEditing(null);
            if (saved) {
              load();
            }
          }}
        />
      )}
    </div>
  );
};

export default QIConfigList;
