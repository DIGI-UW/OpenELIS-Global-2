/**
 * ViolationList Component
 *
 * Displays list of QC rule violations with filtering and management
 * Task Reference: T111
 * Specification: FR-033 to FR-038, User Story 3
 *
 * Features:
 * - DataTable with filtering by severity, status, analyzer
 * - Violation detail modal
 * - Acknowledge and resolve actions
 * - Navigation to corrective action creation
 */

import React, { useState, useEffect, useCallback } from "react";
import {
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Search,
  Grid,
  Column,
  Dropdown,
  Tag,
  OverflowMenu,
  OverflowMenuItem,
  Button,
  Loading,
  InlineNotification,
} from "@carbon/react";
import { Add } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import { useHistory, useLocation } from "react-router-dom";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../../utils/Utils";
import ViolationDetailModal from "./ViolationDetailModal";
import PageTitle from "../../common/PageTitle/PageTitle";
import "./ViolationList.css";

const ViolationList = () => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();

  // State
  const [violations, setViolations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [filters, setFilters] = useState({
    severity: "",
    status: "",
    analyzer: "",
  });
  const [selectedViolation, setSelectedViolation] = useState(null);
  const [detailModalOpen, setDetailModalOpen] = useState(false);
  const [analyzers, setAnalyzers] = useState([]);

  // Severity options
  const severityOptions = [
    { id: "", label: intl.formatMessage({ id: "qc.violations.filter.allSeverities" }) },
    { id: "REJECTION", label: intl.formatMessage({ id: "qc.violations.severity.rejection" }) },
    { id: "WARNING", label: intl.formatMessage({ id: "qc.violations.severity.warning" }) },
  ];

  // Status options
  const statusOptions = [
    { id: "", label: intl.formatMessage({ id: "qc.violations.filter.allStatuses" }) },
    { id: "UNRESOLVED", label: intl.formatMessage({ id: "qc.violations.status.unresolved" }) },
    { id: "ACKNOWLEDGED", label: intl.formatMessage({ id: "qc.violations.status.acknowledged" }) },
    { id: "CORRECTIVE_ACTION_PENDING", label: intl.formatMessage({ id: "qc.violations.status.correctiveActionPending" }) },
    { id: "RESOLVED", label: intl.formatMessage({ id: "qc.violations.status.resolved" }) },
  ];

  // Load violations
  const loadViolations = useCallback(() => {
    setLoading(true);
    setError(null);

    const params = new URLSearchParams();
    if (filters.severity) params.append("severity", filters.severity);
    if (filters.status) params.append("status", filters.status);
    if (filters.analyzer) params.append("analyzerId", filters.analyzer);
    if (searchTerm) params.append("search", searchTerm);

    const url = `/rest/qc/violations${params.toString() ? `?${params.toString()}` : ""}`;

    getFromOpenElisServer(url, (response) => {
      if (response && response.data) {
        setViolations(response.data.violations || response.data || []);
        if (response.data.analyzers) {
          setAnalyzers(response.data.analyzers);
        }
      } else if (Array.isArray(response)) {
        setViolations(response);
      } else {
        setError(intl.formatMessage({ id: "qc.violations.error.loadFailed" }));
      }
      setLoading(false);
    });
  }, [filters, searchTerm, intl]);

  // Load analyzers for filter
  const loadAnalyzers = useCallback(() => {
    getFromOpenElisServer("/rest/analyzers", (response) => {
      if (response && Array.isArray(response.data)) {
        setAnalyzers(response.data);
      } else if (Array.isArray(response)) {
        setAnalyzers(response);
      }
    });
  }, []);

  // Initial load
  useEffect(() => {
    // Restore filters from URL
    const params = new URLSearchParams(location.search);
    const initialFilters = {
      severity: params.get("severity") || "",
      status: params.get("status") || "",
      analyzer: params.get("analyzer") || "",
    };
    setFilters(initialFilters);
    setSearchTerm(params.get("search") || "");

    loadAnalyzers();
  }, [location.search, loadAnalyzers]);

  useEffect(() => {
    loadViolations();
  }, [loadViolations]);

  // Update URL with filters
  const updateUrlParams = (newFilters, newSearch) => {
    const params = new URLSearchParams();
    if (newFilters.severity) params.set("severity", newFilters.severity);
    if (newFilters.status) params.set("status", newFilters.status);
    if (newFilters.analyzer) params.set("analyzer", newFilters.analyzer);
    if (newSearch) params.set("search", newSearch);

    history.replace({
      pathname: location.pathname,
      search: params.toString(),
    });
  };

  // Handle filter change
  const handleFilterChange = (filterName, value) => {
    const newFilters = { ...filters, [filterName]: value };
    setFilters(newFilters);
    updateUrlParams(newFilters, searchTerm);
  };

  // Handle search
  const handleSearch = (value) => {
    setSearchTerm(value);
    updateUrlParams(filters, value);
  };

  // Handle acknowledge violation (FR-036)
  const handleAcknowledge = (violationId) => {
    const endpoint = `/rest/qc/violations/${violationId}/acknowledge`;
    postToOpenElisServerFullResponse(endpoint, JSON.stringify({}), (response) => {
      if (response.ok) {
        loadViolations();
        if (selectedViolation?.id === violationId) {
          setDetailModalOpen(false);
        }
      } else {
        setError(intl.formatMessage({ id: "qc.violations.error.acknowledgeFailed" }));
      }
    });
  };

  // Handle view details
  const handleViewDetails = (violation) => {
    setSelectedViolation(violation);
    setDetailModalOpen(true);
  };

  // Handle create corrective action
  const handleCreateCorrectiveAction = (violationId) => {
    history.push(`/analyzers/qc/corrective-actions/new?violationId=${violationId}`);
  };

  // Get severity tag type
  const getSeverityTagType = (severity) => {
    return severity === "REJECTION" ? "red" : "yellow";
  };

  // Get status tag type
  const getStatusTagType = (status) => {
    switch (status) {
      case "RESOLVED":
        return "green";
      case "CORRECTIVE_ACTION_PENDING":
        return "blue";
      case "ACKNOWLEDGED":
        return "gray";
      default:
        return "red";
    }
  };

  // Format timestamp
  const formatTimestamp = (timestamp) => {
    if (!timestamp) return "-";
    return intl.formatDate(new Date(timestamp), {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  // Table headers
  const headers = [
    { key: "timestamp", header: intl.formatMessage({ id: "qc.violations.table.timestamp" }) },
    { key: "analyzer", header: intl.formatMessage({ id: "qc.violations.table.analyzer" }) },
    { key: "ruleCode", header: intl.formatMessage({ id: "qc.violations.table.rule" }) },
    { key: "severity", header: intl.formatMessage({ id: "qc.violations.table.severity" }) },
    { key: "status", header: intl.formatMessage({ id: "qc.violations.table.status" }) },
    { key: "actions", header: "" },
  ];

  // Format rows
  const rows = violations.map((violation) => ({
    id: violation.id,
    timestamp: formatTimestamp(violation.violationTimestamp || violation.createdDate),
    analyzer: violation.analyzerName || violation.analyzer?.name || "-",
    ruleCode: violation.ruleCode || "-",
    severity: violation.severity,
    status: violation.resolutionStatus || violation.status,
    _violation: violation,
  }));

  if (loading && violations.length === 0) {
    return (
      <div className="violation-list-loading" data-testid="violation-list-loading">
        <Loading
          description={intl.formatMessage({ id: "qc.violations.loading" })}
          withOverlay={false}
        />
      </div>
    );
  }

  return (
    <div className="violation-list" data-testid="violation-list">
      {/* Header */}
      <div className="violation-list-header" data-testid="violation-list-header">
        <PageTitle
          breadcrumbs={[
            {
              label: intl.formatMessage({ id: "analyzer.page.hierarchy.root" }),
              link: "/analyzers",
            },
            {
              label: intl.formatMessage({ id: "qc.dashboard.title" }),
              link: "/analyzers/qc",
            },
            {
              label: intl.formatMessage({ id: "qc.violations.title" }),
            },
          ]}
          subtitle={intl.formatMessage({ id: "qc.violations.subtitle" })}
        />
      </div>

      {/* Error notification */}
      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({ id: "qc.violations.error.title" })}
          subtitle={error}
          onClose={() => setError(null)}
          data-testid="violation-list-error"
        />
      )}

      {/* Filters */}
      <Grid className="violation-list-filters" data-testid="violation-list-filters">
        <Column lg={4} md={4} sm={4}>
          <Search
            placeholder={intl.formatMessage({ id: "qc.violations.filter.search" })}
            labelText={intl.formatMessage({ id: "qc.violations.filter.search" })}
            value={searchTerm}
            onChange={(e) => handleSearch(e.target.value)}
            data-testid="violation-search-input"
          />
        </Column>
        <Column lg={3} md={2} sm={4}>
          <Dropdown
            id="severity-filter"
            titleText={intl.formatMessage({ id: "qc.violations.filter.severity" })}
            items={severityOptions}
            itemToString={(item) => item?.label || ""}
            selectedItem={severityOptions.find((o) => o.id === filters.severity)}
            onChange={({ selectedItem }) => handleFilterChange("severity", selectedItem?.id || "")}
            data-testid="violation-severity-filter"
          />
        </Column>
        <Column lg={3} md={2} sm={4}>
          <Dropdown
            id="status-filter"
            titleText={intl.formatMessage({ id: "qc.violations.filter.status" })}
            items={statusOptions}
            itemToString={(item) => item?.label || ""}
            selectedItem={statusOptions.find((o) => o.id === filters.status)}
            onChange={({ selectedItem }) => handleFilterChange("status", selectedItem?.id || "")}
            data-testid="violation-status-filter"
          />
        </Column>
        <Column lg={3} md={2} sm={4}>
          <Dropdown
            id="analyzer-filter"
            titleText={intl.formatMessage({ id: "qc.violations.filter.analyzer" })}
            items={[
              { id: "", name: intl.formatMessage({ id: "qc.violations.filter.allAnalyzers" }) },
              ...analyzers,
            ]}
            itemToString={(item) => item?.name || ""}
            selectedItem={analyzers.find((a) => a.id === filters.analyzer) || { id: "", name: intl.formatMessage({ id: "qc.violations.filter.allAnalyzers" }) }}
            onChange={({ selectedItem }) => handleFilterChange("analyzer", selectedItem?.id || "")}
            data-testid="violation-analyzer-filter"
          />
        </Column>
      </Grid>

      {/* DataTable */}
      <TableContainer data-testid="violation-table-container">
        <DataTable rows={rows} headers={headers} isSortable>
          {({ rows, headers, getHeaderProps, getRowProps, getTableProps }) => (
            <Table {...getTableProps()} data-testid="violation-table">
              <TableHead>
                <TableRow>
                  {headers.map((header) => (
                    <TableHeader key={header.key} {...getHeaderProps({ header })}>
                      {header.header}
                    </TableHeader>
                  ))}
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map((row) => {
                  const violation = row._violation || violations.find((v) => v.id === row.id);
                  const isUnresolved = violation?.resolutionStatus === "UNRESOLVED" || violation?.status === "UNRESOLVED";
                  const isWarning = violation?.severity === "WARNING";

                  return (
                    <TableRow key={row.id} {...getRowProps({ row })} data-testid={`violation-row-${row.id}`}>
                      {row.cells.map((cell) => {
                        let cellContent = cell.value;

                        if (cell.info.header === "severity") {
                          cellContent = (
                            <Tag type={getSeverityTagType(cell.value)} data-testid={`violation-severity-${row.id}`}>
                              {intl.formatMessage({ id: `qc.violations.severity.${cell.value?.toLowerCase()}` })}
                            </Tag>
                          );
                        } else if (cell.info.header === "status") {
                          cellContent = (
                            <Tag type={getStatusTagType(cell.value)} data-testid={`violation-status-${row.id}`}>
                              {intl.formatMessage({ id: `qc.violations.status.${cell.value?.toLowerCase()}` })}
                            </Tag>
                          );
                        } else if (cell.info.header === "actions") {
                          cellContent = (
                            <OverflowMenu
                              ariaLabel={intl.formatMessage({ id: "qc.violations.action.menu" })}
                              data-testid={`violation-actions-${row.id}`}
                            >
                              <OverflowMenuItem
                                itemText={intl.formatMessage({ id: "qc.violations.action.viewDetails" })}
                                onClick={() => handleViewDetails(violation)}
                              />
                              {isUnresolved && isWarning && (
                                <OverflowMenuItem
                                  itemText={intl.formatMessage({ id: "qc.violations.action.acknowledge" })}
                                  onClick={() => handleAcknowledge(violation.id)}
                                />
                              )}
                              {isUnresolved && !isWarning && (
                                <OverflowMenuItem
                                  itemText={intl.formatMessage({ id: "qc.violations.action.createCorrectiveAction" })}
                                  onClick={() => handleCreateCorrectiveAction(violation.id)}
                                />
                              )}
                            </OverflowMenu>
                          );
                        }

                        return (
                          <TableCell key={cell.id} data-testid={`violation-${cell.info.header}-${row.id}`}>
                            {cellContent}
                          </TableCell>
                        );
                      })}
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          )}
        </DataTable>
      </TableContainer>

      {/* Violation Detail Modal */}
      {detailModalOpen && selectedViolation && (
        <ViolationDetailModal
          violation={selectedViolation}
          open={detailModalOpen}
          onClose={() => {
            setDetailModalOpen(false);
            setSelectedViolation(null);
          }}
          onAcknowledge={handleAcknowledge}
          onCreateCorrectiveAction={handleCreateCorrectiveAction}
        />
      )}
    </div>
  );
};

export default ViolationList;
