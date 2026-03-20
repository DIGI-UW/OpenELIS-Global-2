import React, { useState, useEffect, useCallback } from "react";
import { Grid, Column, Select, SelectItem, Search } from "@carbon/react";
import { useIntl } from "react-intl";
import { getFromOpenElisServer, putToOpenElisServer } from "../utils/Utils";
import AlertSummaryTiles from "./AlertSummaryTiles";
import AlertsTable from "./AlertsTable";
import AlertAcknowledgeModal from "./AlertAcknowledgeModal";
import EQADeadlineSummary from "./EQADeadlineSummary";

const AUTO_REFRESH_INTERVAL = 60000;

const AlertsDashboard = () => {
  const intl = useIntl();
  const [summary, setSummary] = useState({});
  const [alerts, setAlerts] = useState([]);
  const [totalCount, setTotalCount] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(25);
  const [typeFilter, setTypeFilter] = useState("");
  const [severityFilter, setSeverityFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [searchText, setSearchText] = useState("");
  const [modalOpen, setModalOpen] = useState(false);
  const [selectedAlert, setSelectedAlert] = useState(null);

  const fetchSummary = useCallback(() => {
    getFromOpenElisServer("/rest/alerts/dashboard/summary", (data) => {
      if (data) {
        setSummary(data);
      }
    });
  }, []);

  const fetchAlerts = useCallback(() => {
    let url = `/rest/alerts/dashboard?page=${page}&pageSize=${pageSize}`;
    if (typeFilter) url += `&type=${typeFilter}`;
    if (severityFilter) url += `&severity=${severityFilter}`;
    if (statusFilter) url += `&status=${statusFilter}`;
    if (searchText) url += `&search=${encodeURIComponent(searchText)}`;

    getFromOpenElisServer(url, (data) => {
      if (data) {
        setAlerts(data.alerts || []);
        setTotalCount(data.totalCount || 0);
      }
    });
  }, [page, pageSize, typeFilter, severityFilter, statusFilter, searchText]);

  useEffect(() => {
    fetchSummary();
    fetchAlerts();
  }, [fetchSummary, fetchAlerts]);

  useEffect(() => {
    const interval = setInterval(() => {
      fetchSummary();
      fetchAlerts();
    }, AUTO_REFRESH_INTERVAL);
    return () => clearInterval(interval);
  }, [fetchSummary, fetchAlerts]);

  const handleAcknowledge = (alert) => {
    setSelectedAlert(alert);
    setModalOpen(true);
  };

  const handleAcknowledgeSubmit = (alertId, comment) => {
    const payload = comment ? JSON.stringify({ comment }) : "{}";
    putToOpenElisServer(
      `/rest/alerts/${alertId}/acknowledge`,
      payload,
      (status) => {
        if (status === 200) {
          setModalOpen(false);
          setSelectedAlert(null);
          fetchSummary();
          fetchAlerts();
        }
      },
    );
  };

  const handlePageChange = (newPage, newPageSize) => {
    setPage(newPage);
    setPageSize(newPageSize);
  };

  return (
    <div className="alerts-dashboard">
      <h2>{intl.formatMessage({ id: "alerts.dashboard.title" })}</h2>

      <AlertSummaryTiles summary={summary} />

      <Grid condensed style={{ marginTop: "1rem", marginBottom: "1rem" }}>
        <Column lg={4} md={4} sm={4}>
          <Select
            id="alert-type-filter"
            labelText={intl.formatMessage({ id: "alerts.filter.type" })}
            value={typeFilter}
            onChange={(e) => {
              setTypeFilter(e.target.value);
              setPage(0);
            }}
          >
            <SelectItem value="" text="" />
            <SelectItem
              value="EQA_DEADLINE"
              text={intl.formatMessage({ id: "alerts.type.eqa_deadline" })}
            />
            <SelectItem
              value="SAMPLE_EXPIRATION"
              text={intl.formatMessage({ id: "alerts.type.sample_expiration" })}
            />
            <SelectItem
              value="STAT_OVERDUE"
              text={intl.formatMessage({ id: "alerts.type.stat_overdue" })}
            />
            <SelectItem
              value="CRITICAL_UNACKNOWLEDGED"
              text={intl.formatMessage({
                id: "alerts.type.critical_unacknowledged",
              })}
            />
          </Select>
        </Column>
        <Column lg={4} md={4} sm={4}>
          <Select
            id="alert-severity-filter"
            labelText={intl.formatMessage({ id: "alerts.filter.severity" })}
            value={severityFilter}
            onChange={(e) => {
              setSeverityFilter(e.target.value);
              setPage(0);
            }}
          >
            <SelectItem value="" text="" />
            <SelectItem
              value="WARNING"
              text={intl.formatMessage({ id: "alerts.severity.warning" })}
            />
            <SelectItem
              value="CRITICAL"
              text={intl.formatMessage({ id: "alerts.severity.critical" })}
            />
          </Select>
        </Column>
        <Column lg={4} md={4} sm={4}>
          <Select
            id="alert-status-filter"
            labelText={intl.formatMessage({ id: "alerts.filter.status" })}
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setPage(0);
            }}
          >
            <SelectItem value="" text="" />
            <SelectItem
              value="OPEN"
              text={intl.formatMessage({ id: "alerts.status.open" })}
            />
            <SelectItem
              value="ACKNOWLEDGED"
              text={intl.formatMessage({ id: "alerts.status.acknowledged" })}
            />
            <SelectItem
              value="RESOLVED"
              text={intl.formatMessage({ id: "alerts.status.resolved" })}
            />
          </Select>
        </Column>
        <Column lg={4} md={4} sm={4}>
          <Search
            id="alert-search"
            labelText={intl.formatMessage({ id: "alerts.filter.search" })}
            placeholder={intl.formatMessage({ id: "alerts.filter.search" })}
            value={searchText}
            onChange={(e) => {
              setSearchText(e.target.value);
              setPage(0);
            }}
          />
        </Column>
      </Grid>

      <AlertsTable
        alerts={alerts}
        totalCount={totalCount}
        page={page}
        pageSize={pageSize}
        onPageChange={handlePageChange}
        onAcknowledge={handleAcknowledge}
      />

      <EQADeadlineSummary
        alerts={alerts}
        onFilterByProvider={(provider) => {
          setTypeFilter("EQA_DEADLINE");
          setSearchText(provider);
          setPage(0);
        }}
      />

      <AlertAcknowledgeModal
        open={modalOpen}
        alert={selectedAlert}
        onClose={() => {
          setModalOpen(false);
          setSelectedAlert(null);
        }}
        onSubmit={handleAcknowledgeSubmit}
      />
    </div>
  );
};

export default AlertsDashboard;
