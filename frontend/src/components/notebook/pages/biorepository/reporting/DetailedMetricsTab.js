import React, { useState, useEffect } from "react";
import {
  Grid,
  Column,
  Loading,
  InlineNotification,
  Accordion,
  AccordionItem,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  DatePicker,
  DatePickerInput,
  Button,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { Search, Reset } from "@carbon/icons-react";
import PropTypes from "prop-types";
import config from "../../../../../config.json";

/**
 * DetailedMetricsTab - Drill-down metrics with advanced filtering
 *
 * Displays detailed breakdowns of:
 * - Storage capacity by status
 * - Sample aging by time range
 * - QC inspection checkpoint statistics
 * - Retrieval and disposal details
 */
function DetailedMetricsTab({ entryId, notebookId, pageData }) {
  const intl = useIntl();
  const [isLoading, setIsLoading] = useState(true);
  const [metricsData, setMetricsData] = useState(null);
  const [error, setError] = useState(null);
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");

  // Helper function to format Date to YYYY-MM-DD
  const formatDateToISO = (date) => {
    if (!date) return "";
    const d = new Date(date);
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, "0");
    const day = String(d.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
  };

  const fetchMetrics = (start = null, end = null) => {
    setIsLoading(true);
    setError(null);

    const dateParams = new URLSearchParams();
    if (start) dateParams.append("startDate", start);
    if (end) dateParams.append("endDate", end);
    const dateQuery = dateParams.toString() ? `?${dateParams.toString()}` : "";

    Promise.all([
      fetch(
        `${config.serverBaseUrl}/rest/biorepository/dashboard/storage-capacity`,
        {
          credentials: "include",
        },
      ).then((r) => r.json()),
      fetch(
        `${config.serverBaseUrl}/rest/biorepository/dashboard/sample-aging`,
        {
          credentials: "include",
        },
      ).then((r) => r.json()),
      fetch(`${config.serverBaseUrl}/rest/biorepository/dashboard/qc-metrics`, {
        credentials: "include",
      }).then((r) => r.json()),
      fetch(
        `${config.serverBaseUrl}/rest/biorepository/dashboard/qc-discrepancies`,
        {
          credentials: "include",
        },
      ).then((r) => r.json()),
      fetch(
        `${config.serverBaseUrl}/rest/biorepository/dashboard/retrieval-stats${dateQuery}`,
        {
          credentials: "include",
        },
      ).then((r) => r.json()),
      fetch(
        `${config.serverBaseUrl}/rest/biorepository/dashboard/disposal-stats${dateQuery}`,
        {
          credentials: "include",
        },
      ).then((r) => r.json()),
    ])
      .then(([capacity, aging, qc, discrepancies, retrieval, disposal]) => {
        setMetricsData({
          capacity,
          aging,
          qc,
          discrepancies,
          retrieval,
          disposal,
        });
        setIsLoading(false);
      })
      .catch((err) => {
        setError(err.message);
        setIsLoading(false);
      });
  };

  useEffect(() => {
    fetchMetrics();
  }, []);

  const handleApplyFilters = () => {
    // Convert dates to ISO format strings before sending to API
    const startISO = startDate ? formatDateToISO(startDate) : null;
    const endISO = endDate ? formatDateToISO(endDate) : null;
    fetchMetrics(startISO, endISO);
  };

  const handleResetFilters = () => {
    setStartDate("");
    setEndDate("");
    fetchMetrics();
  };

  if (isLoading) {
    return (
      <div style={{ padding: "2rem" }}>
        <Loading
          description={intl.formatMessage({
            id: "biorepository.reporting.metrics.loading",
            defaultMessage: "Loading detailed metrics...",
          })}
        />
      </div>
    );
  }

  if (error) {
    return (
      <div style={{ padding: "2rem" }}>
        <InlineNotification
          kind="error"
          title={intl.formatMessage({
            id: "biorepository.reporting.metrics.error",
            defaultMessage: "Error Loading Metrics",
          })}
          subtitle={error}
          lowContrast
        />
      </div>
    );
  }

  if (!metricsData) {
    return (
      <div style={{ padding: "2rem" }}>
        <InlineNotification
          kind="warning"
          title={intl.formatMessage({
            id: "biorepository.reporting.metrics.noData",
            defaultMessage: "No Data Available",
          })}
          lowContrast
        />
      </div>
    );
  }

  const { capacity, aging, qc, discrepancies, retrieval, disposal } =
    metricsData;

  // Storage capacity rows
  const storageRows = [
    {
      id: "1",
      status: "Stored",
      count: capacity.totalSamplesStored || 0,
      percentage: capacity.totalSamplesStored ? "N/A" : "0%",
    },
    {
      id: "2",
      status: "Pending Storage",
      count: capacity.pendingStorage || 0,
      percentage: "N/A",
    },
  ];

  const storageHeaders = [
    { key: "status", header: "Status" },
    { key: "count", header: "Sample Count" },
    { key: "percentage", header: "Percentage" },
  ];

  // Sample aging rows
  const agingRows = [
    {
      id: "1",
      category: "Expired",
      count: aging.expired || 0,
      description: "Past retention expiry date",
    },
    {
      id: "2",
      category: "Expiring 0-30 days",
      count: aging.expiring30Days || 0,
      description: "Urgent attention needed",
    },
    {
      id: "3",
      category: "Expiring 31-60 days",
      count: aging.expiring60Days || 0,
      description: "Plan for disposal/extension",
    },
    {
      id: "4",
      category: "Expiring 61-90 days",
      count: aging.expiring90Days || 0,
      description: "Monitor closely",
    },
    {
      id: "5",
      category: "Total Active",
      count: aging.total || 0,
      description: "All samples in repository",
    },
  ];

  const agingHeaders = [
    { key: "category", header: "Time Range" },
    { key: "count", header: "Sample Count" },
    { key: "description", header: "Description" },
  ];

  // QC checkpoint rows
  const qcCheckpointRows = [
    {
      id: "1",
      checkpoint: "Sample Present",
      passRate: qc.samplePresentPassRate
        ? `${qc.samplePresentPassRate.toFixed(1)}%`
        : "N/A",
    },
    {
      id: "2",
      checkpoint: "Label Integrity",
      passRate: qc.labelIntegrityPassRate
        ? `${qc.labelIntegrityPassRate.toFixed(1)}%`
        : "N/A",
    },
    {
      id: "3",
      checkpoint: "Container Integrity",
      passRate: qc.containerIntegrityPassRate
        ? `${qc.containerIntegrityPassRate.toFixed(1)}%`
        : "N/A",
    },
    {
      id: "4",
      checkpoint: "Volume Appearance",
      passRate: qc.volumeAppearancePassRate
        ? `${qc.volumeAppearancePassRate.toFixed(1)}%`
        : "N/A",
    },
    {
      id: "5",
      checkpoint: "Correct Position",
      passRate: qc.correctPositionPassRate
        ? `${qc.correctPositionPassRate.toFixed(1)}%`
        : "N/A",
    },
    {
      id: "6",
      checkpoint: "Overall Compliance",
      passRate: qc.complianceRate ? `${qc.complianceRate.toFixed(1)}%` : "N/A",
    },
  ];

  const qcHeaders = [
    { key: "checkpoint", header: "Checkpoint" },
    { key: "passRate", header: "Pass Rate" },
  ];

  // Retrieval stats rows
  const retrievalRows = [
    {
      id: "1",
      metric: "Total Requests",
      value: retrieval.totalRequests || 0,
    },
    {
      id: "2",
      metric: "Completed",
      value: retrieval.completedRequests || 0,
    },
    {
      id: "3",
      metric: "Pending/In Progress",
      value: retrieval.pendingRequests || 0,
    },
    {
      id: "4",
      metric: "Rejected",
      value: retrieval.rejectedRequests || 0,
    },
    {
      id: "5",
      metric: "Total Items Retrieved",
      value: retrieval.totalItemsRetrieved || 0,
    },
    {
      id: "6",
      metric: "Items Returned",
      value: retrieval.returnedItems || 0,
    },
    {
      id: "7",
      metric: "Items Consumed",
      value: retrieval.consumedItems || 0,
    },
    {
      id: "8",
      metric: "Overdue Returns",
      value: retrieval.overdueReturns || 0,
    },
  ];

  const retrievalHeaders = [
    { key: "metric", header: "Metric" },
    { key: "value", header: "Count" },
  ];

  // Disposal stats rows
  const disposalsByProject = disposal.disposalsByProject || {};
  const disposalRows = [
    {
      id: "total",
      project: "Total Disposed",
      count: disposal.totalDisposed || 0,
    },
    ...Object.entries(disposalsByProject).map(([project, count], idx) => ({
      id: `project-${idx}`,
      project,
      count,
    })),
  ];

  const disposalHeaders = [
    { key: "project", header: "Project" },
    { key: "count", header: "Disposed Count" },
  ];

  return (
    <div className="detailed-metrics-tab" style={{ padding: "1.5rem" }}>
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <h4 style={{ marginBottom: "1rem" }}>
            <FormattedMessage
              id="biorepository.reporting.metrics.title"
              defaultMessage="Detailed Metrics"
            />
          </h4>
        </Column>

        {/* Date Range Filters */}
        <Column lg={16} md={8} sm={4} style={{ marginBottom: "1rem" }}>
          <div
            style={{
              display: "flex",
              gap: "1rem",
              alignItems: "flex-end",
              flexWrap: "wrap",
            }}
          >
            <DatePicker
              datePickerType="single"
              value={startDate}
              onChange={(dates) => setStartDate(dates[0] || "")}
            >
              <DatePickerInput
                id="start-date"
                placeholder="mm/dd/yyyy"
                labelText={intl.formatMessage({
                  id: "biorepository.reporting.metrics.startDate",
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
                id="end-date"
                placeholder="mm/dd/yyyy"
                labelText={intl.formatMessage({
                  id: "biorepository.reporting.metrics.endDate",
                  defaultMessage: "End Date",
                })}
                size="md"
              />
            </DatePicker>

            <Button
              kind="primary"
              onClick={handleApplyFilters}
              renderIcon={Search}
              size="md"
            >
              <FormattedMessage
                id="biorepository.reporting.metrics.apply"
                defaultMessage="Apply Filters"
              />
            </Button>

            <Button
              kind="secondary"
              onClick={handleResetFilters}
              renderIcon={Reset}
              size="md"
            >
              <FormattedMessage
                id="biorepository.reporting.metrics.reset"
                defaultMessage="Reset"
              />
            </Button>
          </div>
        </Column>

        {/* Accordion Sections */}
        <Column lg={16} md={8} sm={4}>
          <Accordion>
            {/* Storage Capacity Details */}
            <AccordionItem
              title={intl.formatMessage({
                id: "biorepository.reporting.metrics.storage.title",
                defaultMessage: "Storage Capacity Details",
              })}
            >
              <DataTable rows={storageRows} headers={storageHeaders}>
                {({
                  rows,
                  headers,
                  getTableProps,
                  getHeaderProps,
                  getRowProps,
                }) => (
                  <TableContainer
                    title={intl.formatMessage({
                      id: "biorepository.reporting.metrics.storage.subtitle",
                      defaultMessage: "Sample counts by storage status",
                    })}
                  >
                    <Table {...getTableProps()}>
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
            </AccordionItem>

            {/* Sample Aging Breakdown */}
            <AccordionItem
              title={intl.formatMessage({
                id: "biorepository.reporting.metrics.aging.title",
                defaultMessage: "Sample Aging Breakdown",
              })}
            >
              <DataTable rows={agingRows} headers={agingHeaders}>
                {({
                  rows,
                  headers,
                  getTableProps,
                  getHeaderProps,
                  getRowProps,
                }) => (
                  <TableContainer
                    title={intl.formatMessage({
                      id: "biorepository.reporting.metrics.aging.subtitle",
                      defaultMessage:
                        "Samples categorized by retention expiry timeline",
                    })}
                  >
                    <Table {...getTableProps()}>
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
            </AccordionItem>

            {/* QC Checkpoint Statistics */}
            <AccordionItem
              title={intl.formatMessage({
                id: "biorepository.reporting.metrics.qc.title",
                defaultMessage: "QC Checkpoint Statistics",
              })}
            >
              <DataTable rows={qcCheckpointRows} headers={qcHeaders}>
                {({
                  rows,
                  headers,
                  getTableProps,
                  getHeaderProps,
                  getRowProps,
                }) => (
                  <TableContainer
                    title={intl.formatMessage({
                      id: "biorepository.reporting.metrics.qc.subtitle",
                      defaultMessage:
                        "Pass rates for individual QC checkpoints",
                    })}
                    description={intl.formatMessage(
                      {
                        id: "biorepository.reporting.metrics.qc.description",
                        defaultMessage:
                          "Total inspections: {total} | Passed: {passed} | Failed: {failed}",
                      },
                      {
                        total: qc.totalInspections || 0,
                        passed: qc.passedInspections || 0,
                        failed: qc.failedInspections || 0,
                      },
                    )}
                  >
                    <Table {...getTableProps()}>
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
            </AccordionItem>

            {/* Retrieval Statistics */}
            <AccordionItem
              title={intl.formatMessage({
                id: "biorepository.reporting.metrics.retrieval.title",
                defaultMessage: "Retrieval Statistics",
              })}
            >
              <DataTable rows={retrievalRows} headers={retrievalHeaders}>
                {({
                  rows,
                  headers,
                  getTableProps,
                  getHeaderProps,
                  getRowProps,
                }) => (
                  <TableContainer
                    title={intl.formatMessage({
                      id: "biorepository.reporting.metrics.retrieval.subtitle",
                      defaultMessage:
                        "Sample retrieval request and item metrics",
                    })}
                    description={
                      startDate || endDate
                        ? intl.formatMessage(
                            {
                              id: "biorepository.reporting.metrics.retrieval.filtered",
                              defaultMessage: "Filtered: {start} to {end}",
                            },
                            {
                              start: startDate
                                ? formatDateToISO(startDate)
                                : "All time",
                              end: endDate ? formatDateToISO(endDate) : "Today",
                            },
                          )
                        : intl.formatMessage({
                            id: "biorepository.reporting.metrics.retrieval.default",
                            defaultMessage: "Last 30 days",
                          })
                    }
                  >
                    <Table {...getTableProps()}>
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
            </AccordionItem>

            {/* Disposal Statistics */}
            <AccordionItem
              title={intl.formatMessage({
                id: "biorepository.reporting.metrics.disposal.title",
                defaultMessage: "Disposal Statistics",
              })}
            >
              <DataTable rows={disposalRows} headers={disposalHeaders}>
                {({
                  rows,
                  headers,
                  getTableProps,
                  getHeaderProps,
                  getRowProps,
                }) => (
                  <TableContainer
                    title={intl.formatMessage({
                      id: "biorepository.reporting.metrics.disposal.subtitle",
                      defaultMessage: "Disposed samples by project",
                    })}
                    description={
                      startDate || endDate
                        ? intl.formatMessage(
                            {
                              id: "biorepository.reporting.metrics.disposal.filtered",
                              defaultMessage: "Filtered: {start} to {end}",
                            },
                            {
                              start: startDate
                                ? formatDateToISO(startDate)
                                : "All time",
                              end: endDate ? formatDateToISO(endDate) : "Today",
                            },
                          )
                        : intl.formatMessage({
                            id: "biorepository.reporting.metrics.disposal.default",
                            defaultMessage: "All time",
                          })
                    }
                  >
                    <Table {...getTableProps()}>
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
            </AccordionItem>
          </Accordion>
        </Column>
      </Grid>
    </div>
  );
}

DetailedMetricsTab.propTypes = {
  entryId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  notebookId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  pageData: PropTypes.object,
};

export default DetailedMetricsTab;
