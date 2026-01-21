import React, { useState, useEffect } from "react";
import {
  Grid,
  Column,
  Button,
  RadioButtonGroup,
  RadioButton,
  InlineNotification,
  InlineLoading,
  Search,
  Select,
  SelectItem,
  DatePicker,
  DatePickerInput,
  Accordion,
  AccordionItem,
} from "@carbon/react";
import { Download, Reset } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import PropTypes from "prop-types";
import config from "../../../../../config.json";

/**
 * ExportReportsTab - Multi-format report export
 *
 * Features:
 * - Dashboard metrics export (CSV/Excel/JSON/PDF)
 * - Audit trail export with search/filter capability
 * - Download progress indicators
 * - Success/error notifications
 */
function ExportReportsTab({ entryId, notebookId, pageData }) {
  const intl = useIntl();

  // Dashboard export state
  const [dashboardFormat, setDashboardFormat] = useState("csv");
  const [dashboardLoading, setDashboardLoading] = useState(false);
  const [dashboardError, setDashboardError] = useState(null);
  const [dashboardSuccess, setDashboardSuccess] = useState(false);

  // Audit trail export state
  const [auditFormat, setAuditFormat] = useState("csv");
  const [auditLoading, setAuditLoading] = useState(false);
  const [auditError, setAuditError] = useState(null);
  const [auditSuccess, setAuditSuccess] = useState(false);

  // Audit trail filter state
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedAction, setSelectedAction] = useState("ALL");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [actions, setActions] = useState([]);

  // Helper function to format Date to YYYY-MM-DD
  const formatDateToISO = (date) => {
    if (!date) return "";
    const d = new Date(date);
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, "0");
    const day = String(d.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
  };

  // Fetch custody action types for filter
  useEffect(() => {
    fetch(`${config.serverBaseUrl}/rest/biorepository/custody/actions`, {
      credentials: "include",
    })
      .then((r) => r.json())
      .then((data) => setActions(data))
      .catch((err) => console.error("Failed to load action types:", err));
  }, []);

  // Generic download handler
  const downloadFile = (blob, filename, format) => {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `${filename}.${format}`;
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
    document.body.removeChild(a);
  };

  // Dashboard metrics export handler
  const handleDashboardExport = () => {
    setDashboardLoading(true);
    setDashboardError(null);
    setDashboardSuccess(false);

    const timestamp = new Date().toISOString().split("T")[0];
    const endpoint = `/rest/biorepository/dashboard/export/${dashboardFormat}`;

    fetch(`${config.serverBaseUrl}${endpoint}`, {
      credentials: "include",
      method: "GET",
    })
      .then((response) => {
        if (!response.ok)
          throw new Error(
            `Export failed: ${response.status} ${response.statusText}`,
          );
        return response.blob();
      })
      .then((blob) => {
        downloadFile(
          blob,
          `biorepository_dashboard_${timestamp}`,
          dashboardFormat,
        );
        setDashboardSuccess(true);
        setDashboardLoading(false);
        setTimeout(() => setDashboardSuccess(false), 5000);
      })
      .catch((err) => {
        setDashboardError(err.message);
        setDashboardLoading(false);
      });
  };

  // Audit trail export handler
  const handleAuditExport = () => {
    setAuditLoading(true);
    setAuditError(null);
    setAuditSuccess(false);

    const timestamp = new Date().toISOString().split("T")[0];
    const params = new URLSearchParams();

    if (searchQuery) params.append("sampleExternalId", searchQuery);
    if (selectedAction && selectedAction !== "ALL")
      params.append("action", selectedAction);
    if (startDate) {
      const formattedStart =
        typeof startDate === "string" ? startDate : formatDateToISO(startDate);
      if (formattedStart) params.append("startDate", formattedStart);
    }
    if (endDate) {
      const formattedEnd =
        typeof endDate === "string" ? endDate : formatDateToISO(endDate);
      if (formattedEnd) params.append("endDate", formattedEnd);
    }

    const queryString = params.toString();
    const endpoint = `/rest/biorepository/custody/export/${auditFormat}${queryString ? `?${queryString}` : ""}`;

    fetch(`${config.serverBaseUrl}${endpoint}`, {
      credentials: "include",
      method: "GET",
    })
      .then((response) => {
        if (!response.ok)
          throw new Error(
            `Export failed: ${response.status} ${response.statusText}`,
          );
        return response.blob();
      })
      .then((blob) => {
        const filename = searchQuery
          ? `audit_trail_${searchQuery}_${timestamp}`
          : `audit_trail_${timestamp}`;
        downloadFile(blob, filename, auditFormat);
        setAuditSuccess(true);
        setAuditLoading(false);
        setTimeout(() => setAuditSuccess(false), 5000);
      })
      .catch((err) => {
        setAuditError(err.message);
        setAuditLoading(false);
      });
  };

  const handleResetAuditFilters = () => {
    setSearchQuery("");
    setSelectedAction("ALL");
    setStartDate("");
    setEndDate("");
  };

  return (
    <div className="export-reports-tab" style={{ padding: "1.5rem" }}>
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <h4 style={{ marginBottom: "1rem" }}>
            <FormattedMessage
              id="biorepository.reporting.export.title"
              defaultMessage="Export Reports"
            />
          </h4>
          <p
            style={{
              fontSize: "0.875rem",
              color: "#525252",
              marginBottom: "1.5rem",
            }}
          >
            <FormattedMessage
              id="biorepository.reporting.export.description"
              defaultMessage="Export dashboard metrics and audit trail data in multiple formats (CSV, Excel, JSON, PDF)."
            />
          </p>
        </Column>

        {/* Dashboard Metrics Export Section */}
        <Column lg={16} md={8} sm={4} style={{ marginBottom: "2rem" }}>
          <Accordion>
            <AccordionItem
              title={intl.formatMessage({
                id: "biorepository.reporting.export.dashboard.title",
                defaultMessage: "Dashboard Metrics Export",
              })}
              open
            >
              <div style={{ padding: "1rem" }}>
                <p
                  style={{
                    fontSize: "0.875rem",
                    color: "#525252",
                    marginBottom: "1rem",
                  }}
                >
                  <FormattedMessage
                    id="biorepository.reporting.export.dashboard.subtitle"
                    defaultMessage="Export all dashboard metrics including storage capacity, sample aging, QC compliance, and retrieval statistics."
                  />
                </p>

                <RadioButtonGroup
                  legendText={intl.formatMessage({
                    id: "biorepository.reporting.export.formatLabel",
                    defaultMessage: "Export Format",
                  })}
                  name="dashboard-format"
                  valueSelected={dashboardFormat}
                  onChange={setDashboardFormat}
                  style={{ marginBottom: "1rem" }}
                >
                  <RadioButton labelText="CSV" value="csv" id="dashboard-csv" />
                  <RadioButton
                    labelText="Excel"
                    value="excel"
                    id="dashboard-excel"
                  />
                  <RadioButton
                    labelText="JSON"
                    value="json"
                    id="dashboard-json"
                  />
                  <RadioButton labelText="PDF" value="pdf" id="dashboard-pdf" />
                </RadioButtonGroup>

                {dashboardSuccess && (
                  <InlineNotification
                    kind="success"
                    title={intl.formatMessage({
                      id: "biorepository.reporting.export.success",
                      defaultMessage: "Export Successful",
                    })}
                    subtitle={intl.formatMessage({
                      id: "biorepository.reporting.export.successMessage",
                      defaultMessage: "File downloaded successfully",
                    })}
                    lowContrast
                    hideCloseButton
                    style={{ marginBottom: "1rem" }}
                  />
                )}

                {dashboardError && (
                  <InlineNotification
                    kind="error"
                    title={intl.formatMessage({
                      id: "biorepository.reporting.export.error",
                      defaultMessage: "Export Failed",
                    })}
                    subtitle={dashboardError}
                    lowContrast
                    onClose={() => setDashboardError(null)}
                    style={{ marginBottom: "1rem" }}
                  />
                )}

                {dashboardLoading ? (
                  <InlineLoading
                    description={intl.formatMessage({
                      id: "biorepository.reporting.export.loading",
                      defaultMessage: "Generating export...",
                    })}
                  />
                ) : (
                  <Button
                    kind="primary"
                    renderIcon={Download}
                    onClick={handleDashboardExport}
                  >
                    <FormattedMessage
                      id="biorepository.reporting.export.downloadButton"
                      defaultMessage="Download Dashboard Metrics"
                    />
                  </Button>
                )}
              </div>
            </AccordionItem>

            {/* Audit Trail Export Section */}
            <AccordionItem
              title={intl.formatMessage({
                id: "biorepository.reporting.export.audit.title",
                defaultMessage: "Audit Trail Export",
              })}
            >
              <div style={{ padding: "1rem" }}>
                <p
                  style={{
                    fontSize: "0.875rem",
                    color: "#525252",
                    marginBottom: "1rem",
                  }}
                >
                  <FormattedMessage
                    id="biorepository.reporting.export.audit.subtitle"
                    defaultMessage="Export custody logs with optional filters. Leave filters empty to export all records."
                  />
                </p>

                {/* Filters */}
                <div
                  style={{
                    display: "flex",
                    gap: "1rem",
                    flexWrap: "wrap",
                    marginBottom: "1rem",
                  }}
                >
                  <Search
                    id="audit-export-search"
                    labelText={intl.formatMessage({
                      id: "biorepository.reporting.export.audit.search",
                      defaultMessage: "Search by sample barcode",
                    })}
                    placeholder={intl.formatMessage({
                      id: "biorepository.reporting.export.audit.searchPlaceholder",
                      defaultMessage: "Enter barcode...",
                    })}
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    size="md"
                    style={{ minWidth: "250px" }}
                  />

                  <Select
                    id="audit-export-action"
                    labelText={intl.formatMessage({
                      id: "biorepository.reporting.export.audit.action",
                      defaultMessage: "Custody Action",
                    })}
                    value={selectedAction}
                    onChange={(e) => setSelectedAction(e.target.value)}
                    size="md"
                    style={{ minWidth: "200px" }}
                  >
                    <SelectItem value="ALL" text="All Actions" />
                    {actions.map((action) => (
                      <SelectItem
                        key={action.value}
                        value={action.value}
                        text={action.label}
                      />
                    ))}
                  </Select>

                  <DatePicker
                    datePickerType="single"
                    value={startDate}
                    onChange={(dates) => setStartDate(dates[0] || "")}
                  >
                    <DatePickerInput
                      id="audit-export-start-date"
                      placeholder="mm/dd/yyyy"
                      labelText={intl.formatMessage({
                        id: "biorepository.reporting.export.audit.startDate",
                        defaultMessage: "Start Date",
                      })}
                      size="md"
                    />
                  </DatePicker>

                  <DatePicker
                    datePickerType="single"
                    value={endDate}
                    onChange={(dates) => setEndDate(dates[0] || "")}
                  >
                    <DatePickerInput
                      id="audit-export-end-date"
                      placeholder="mm/dd/yyyy"
                      labelText={intl.formatMessage({
                        id: "biorepository.reporting.export.audit.endDate",
                        defaultMessage: "End Date",
                      })}
                      size="md"
                    />
                  </DatePicker>

                  <Button
                    kind="secondary"
                    onClick={handleResetAuditFilters}
                    renderIcon={Reset}
                    size="md"
                    style={{ alignSelf: "flex-end" }}
                  >
                    <FormattedMessage
                      id="biorepository.reporting.export.audit.reset"
                      defaultMessage="Reset"
                    />
                  </Button>
                </div>

                <RadioButtonGroup
                  legendText={intl.formatMessage({
                    id: "biorepository.reporting.export.formatLabel",
                    defaultMessage: "Export Format",
                  })}
                  name="audit-format"
                  valueSelected={auditFormat}
                  onChange={setAuditFormat}
                  style={{ marginBottom: "1rem" }}
                >
                  <RadioButton labelText="CSV" value="csv" id="audit-csv" />
                  <RadioButton
                    labelText="Excel"
                    value="excel"
                    id="audit-excel"
                  />
                  <RadioButton labelText="JSON" value="json" id="audit-json" />
                  <RadioButton labelText="PDF" value="pdf" id="audit-pdf" />
                </RadioButtonGroup>

                {auditSuccess && (
                  <InlineNotification
                    kind="success"
                    title={intl.formatMessage({
                      id: "biorepository.reporting.export.success",
                      defaultMessage: "Export Successful",
                    })}
                    subtitle={intl.formatMessage({
                      id: "biorepository.reporting.export.successMessage",
                      defaultMessage: "File downloaded successfully",
                    })}
                    lowContrast
                    hideCloseButton
                    style={{ marginBottom: "1rem" }}
                  />
                )}

                {auditError && (
                  <InlineNotification
                    kind="error"
                    title={intl.formatMessage({
                      id: "biorepository.reporting.export.error",
                      defaultMessage: "Export Failed",
                    })}
                    subtitle={auditError}
                    lowContrast
                    onClose={() => setAuditError(null)}
                    style={{ marginBottom: "1rem" }}
                  />
                )}

                {auditLoading ? (
                  <InlineLoading
                    description={intl.formatMessage({
                      id: "biorepository.reporting.export.loading",
                      defaultMessage: "Generating export...",
                    })}
                  />
                ) : (
                  <Button
                    kind="primary"
                    renderIcon={Download}
                    onClick={handleAuditExport}
                  >
                    <FormattedMessage
                      id="biorepository.reporting.export.audit.downloadButton"
                      defaultMessage="Download Audit Trail"
                    />
                  </Button>
                )}
              </div>
            </AccordionItem>
          </Accordion>
        </Column>
      </Grid>
    </div>
  );
}

ExportReportsTab.propTypes = {
  entryId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  notebookId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  pageData: PropTypes.object,
};

export default ExportReportsTab;
