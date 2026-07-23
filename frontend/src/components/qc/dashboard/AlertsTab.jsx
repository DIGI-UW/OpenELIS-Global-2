/**
 * AlertsTab Component
 *
 * Displays QC violations split into active (unacknowledged) and
 * acknowledged sections, with time period filtering.
 *
 * Features:
 * - Time period dropdown filter (24h, 72h, 7d, 30d, all)
 * - Active violations cards with Acknowledge button
 * - Acknowledged violations DataTable
 */

import React, {
  useState,
  useEffect,
  useCallback,
  useMemo,
  useRef,
} from "react";
import {
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Dropdown,
  Tag,
  Tile,
  Button,
  Loading,
  InlineNotification,
} from "@carbon/react";
import { CheckmarkFilled } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../../utils/Utils";
import {
  getSeverityTagType,
  formatTimestamp,
  filterByTimePeriod,
} from "./qcDashboardUtils";
import "./AlertsTab.css";

const AlertsTab = () => {
  const intl = useIntl();
  const intlRef = useRef(intl);
  intlRef.current = intl;

  console.log("Rendered alerts tab");
  const [violations, setViolations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [timePeriod, setTimePeriod] = useState("72h");

  const timePeriodOptions = [
    {
      id: "24h",
      label: intl.formatMessage({ id: "qc.dashboard.alerts.timePeriod.24h" }),
    },
    {
      id: "72h",
      label: intl.formatMessage({ id: "qc.dashboard.alerts.timePeriod.72h" }),
    },
    {
      id: "7d",
      label: intl.formatMessage({ id: "qc.dashboard.alerts.timePeriod.7d" }),
    },
    {
      id: "30d",
      label: intl.formatMessage({ id: "qc.dashboard.alerts.timePeriod.30d" }),
    },
    {
      id: "all",
      label: intl.formatMessage({ id: "qc.dashboard.alerts.timePeriod.all" }),
    },
  ];

  const loadViolations = useCallback(() => {
    setLoading(true);
    setError(null);

    getFromOpenElisServer("/rest/qc/violations", (response) => {
      if (response && response.data) {
        setViolations(response.data.violations || response.data || []);
      } else if (Array.isArray(response)) {
        setViolations(response);
      } else {
        setError(
          intlRef.current.formatMessage({
            id: "qc.dashboard.error.loadFailed",
          }),
        );
      }
      setLoading(false);
    });
  }, []);

  useEffect(() => {
    loadViolations();
  }, [loadViolations]);

  // Filter by time period
  const filteredViolations = useMemo(() => {
    return filterByTimePeriod(violations, "violationDateTime", timePeriod);
  }, [violations, timePeriod]);

  // Split into active (unresolved+unacknowledged) and acknowledged
  const activeViolations = useMemo(() => {
    return filteredViolations.filter((v) => {
      const status = v.resolutionStatus || v.status;
      return status === "UNRESOLVED";
    });
  }, [filteredViolations]);

  const acknowledgedViolations = useMemo(() => {
    return filteredViolations.filter((v) => {
      const status = v.resolutionStatus || v.status;
      return (
        status === "ACKNOWLEDGED" || status === "CORRECTIVE_ACTION_PENDING"
      );
    });
  }, [filteredViolations]);

  const handleAcknowledge = (violationId) => {
    const endpoint = `/rest/qc/violations/${violationId}/acknowledge`;
    postToOpenElisServerFullResponse(
      endpoint,
      JSON.stringify({}),
      (response) => {
        if (response.ok) {
          loadViolations();
        } else {
          setError(
            intl.formatMessage({ id: "qc.violations.error.acknowledgeFailed" }),
          );
        }
      },
    );
  };

  // Acknowledged violations table headers
  const acknowledgedHeaders = [
    {
      key: "severity",
      header: intl.formatMessage({ id: "qc.dashboard.alerts.col.severity" }),
    },
    {
      key: "instrument",
      header: intl.formatMessage({ id: "qc.dashboard.alerts.col.instrument" }),
    },
    {
      key: "analyte",
      header: intl.formatMessage({ id: "qc.dashboard.alerts.col.analyte" }),
    },
    {
      key: "rule",
      header: intl.formatMessage({ id: "qc.dashboard.alerts.col.rule" }),
    },
    {
      key: "acknowledgedBy",
      header: intl.formatMessage({
        id: "qc.dashboard.alerts.col.acknowledgedBy",
      }),
    },
    {
      key: "acknowledgedDate",
      header: intl.formatMessage({
        id: "qc.dashboard.alerts.col.acknowledgedDate",
      }),
    },
  ];

  const acknowledgedRows = acknowledgedViolations.map((v) => ({
    id: String(v.id),
    severity: v.severity || "WARNING",
    instrument: v.instrumentName || "-",
    analyte: v.testName || "-",
    rule: v.ruleCode || "-",
    acknowledgedBy: v.resolvedByUserName || "-",
    acknowledgedDate: formatTimestamp(v.acknowledgedDate),
  }));

  if (loading && violations.length === 0) {
    return (
      <div className="alerts-tab__loading" data-testid="alerts-tab-loading">
        <Loading
          description={intl.formatMessage({ id: "qc.dashboard.loading" })}
          withOverlay={false}
        />
      </div>
    );
  }

  return (
    <div className="alerts-tab" data-testid="alerts-tab">
      {/* Header with time period filter */}
      <div className="alerts-tab__header">
        <div>
          <h4 className="alerts-tab__title">
            {intl.formatMessage({ id: "qc.dashboard.alerts.title" })}
          </h4>
        </div>
        <Dropdown
          id="alerts-time-period"
          className="alerts-tab__time-filter"
          label={intl.formatMessage({
            id: "qc.dashboard.alerts.timePeriod",
          })}
          titleText={intl.formatMessage({
            id: "qc.dashboard.alerts.timePeriod",
          })}
          items={timePeriodOptions}
          itemToString={(item) => item?.label || ""}
          selectedItem={timePeriodOptions.find((o) => o.id === timePeriod)}
          onChange={({ selectedItem }) =>
            setTimePeriod(selectedItem?.id || "72h")
          }
          data-testid="alerts-time-period-filter"
        />
      </div>

      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({ id: "qc.dashboard.error.title" })}
          subtitle={error}
          onClose={() => setError(null)}
        />
      )}

      {/* Active Violations Section */}
      <div className="alerts-tab__section" data-testid="alerts-active-section">
        <h5 className="alerts-tab__section-title">
          {intl.formatMessage({ id: "qc.dashboard.alerts.active" })}
        </h5>
        <p className="alerts-tab__section-count">
          {intl.formatMessage(
            { id: "qc.dashboard.alerts.active.count" },
            { count: activeViolations.length },
          )}
        </p>

        {activeViolations.length === 0 ? (
          <Tile
            className="alerts-tab__empty-state"
            data-testid="alerts-active-empty"
          >
            <CheckmarkFilled size={24} className="alerts-tab__empty-icon" />
            <span>
              {intl.formatMessage({ id: "qc.dashboard.alerts.active.empty" })}
            </span>
          </Tile>
        ) : (
          <div className="alerts-tab__active-cards">
            {activeViolations.map((violation) => (
              <Tile
                key={violation.id}
                className="alerts-tab__violation-card"
                data-testid={`alert-card-${violation.id}`}
              >
                <div className="alerts-tab__card-header">
                  <Tag type={getSeverityTagType(violation.severity)}>
                    {violation.severity}
                  </Tag>
                  <span className="alerts-tab__card-rule">
                    {violation.ruleCode}
                  </span>
                </div>
                <div className="alerts-tab__card-details">
                  <span>{violation.instrumentName || "-"}</span>
                  <span className="alerts-tab__card-separator">|</span>
                  <span>{violation.testName || "-"}</span>
                </div>
                <div className="alerts-tab__card-footer">
                  <span className="alerts-tab__card-timestamp">
                    {formatTimestamp(violation.violationDateTime)}
                  </span>
                  <Button
                    kind="tertiary"
                    size="sm"
                    onClick={() => handleAcknowledge(violation.id)}
                    data-testid={`alert-acknowledge-${violation.id}`}
                  >
                    {intl.formatMessage({
                      id: "qc.dashboard.alerts.acknowledge",
                    })}
                  </Button>
                </div>
              </Tile>
            ))}
          </div>
        )}
      </div>

      {/* Acknowledged Violations Section */}
      <div
        className="alerts-tab__section"
        data-testid="alerts-acknowledged-section"
      >
        <h5 className="alerts-tab__section-title">
          {intl.formatMessage({ id: "qc.dashboard.alerts.acknowledged" })}
        </h5>
        <p className="alerts-tab__section-count">
          {intl.formatMessage(
            { id: "qc.dashboard.alerts.acknowledged.count" },
            { count: acknowledgedViolations.length },
          )}
        </p>

        {acknowledgedViolations.length === 0 ? (
          <div
            className="alerts-tab__empty-text"
            data-testid="alerts-acknowledged-empty"
          >
            {intl.formatMessage({
              id: "qc.dashboard.alerts.acknowledged.empty",
            })}
          </div>
        ) : (
          <TableContainer data-testid="alerts-acknowledged-table-container">
            <DataTable
              rows={acknowledgedRows}
              headers={acknowledgedHeaders}
              isSortable
            >
              {({
                rows,
                headers,
                getHeaderProps,
                getRowProps,
                getTableProps,
              }) => (
                <Table
                  {...getTableProps()}
                  data-testid="alerts-acknowledged-table"
                >
                  <TableHead>
                    <TableRow>
                      {headers.map((header) => (
                        <TableHeader
                          key={header.key}
                          {...getHeaderProps({ header })}
                        >
                          {header.header}
                        </TableHeader>
                      ))}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {rows.map((row) => (
                      <TableRow key={row.id} {...getRowProps({ row })}>
                        {row.cells.map((cell) => {
                          let cellContent = cell.value;

                          if (cell.info.header === "severity") {
                            cellContent = (
                              <Tag type={getSeverityTagType(cell.value)}>
                                {cell.value}
                              </Tag>
                            );
                          }

                          return (
                            <TableCell key={cell.id}>{cellContent}</TableCell>
                          );
                        })}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </DataTable>
          </TableContainer>
        )}
      </div>
    </div>
  );
};

export default AlertsTab;
