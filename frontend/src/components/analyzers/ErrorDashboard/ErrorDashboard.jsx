/**
 * ErrorDashboard Component
 *
 * Displays all unmapped or failed analyzer messages
 * Specification: FR-016
 *
 * Features:
 * - Statistics cards (Total Errors, Unacknowledged, Critical, Last 24 Hours)
 * - Filter bar (search, error type, severity, analyzer)
 * - Data table with error details
 * - Error Details modal
 */

import React, { useState, useEffect, useRef, useCallback } from "react";
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
  Tile,
  Button,
  Tag,
  OverflowMenu,
  OverflowMenuItem,
  Dropdown,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { useHistory, useLocation } from "react-router-dom";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../../../components/utils/Utils";
import ErrorDetailsModal from "./ErrorDetailsModal";
import PageTitle from "../../common/PageTitle/PageTitle";
import "./ErrorDashboard.css";

const ErrorDashboard = () => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const searchTimeoutRef = useRef(null);

  const [errors, setErrors] = useState([]);
  const [filteredErrors, setFilteredErrors] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [filters, setFilters] = useState({
    errorType: "",
    severity: "",
    analyzer: "",
  });
  const [stats, setStats] = useState({
    total: 0,
    unacknowledged: 0,
    critical: 0,
    last24Hours: 0,
  });
  const [selectedError, setSelectedError] = useState(null);
  const [errorDetailsOpen, setErrorDetailsOpen] = useState(false);

  const loadErrors = useCallback((searchFilters = {}) => {
    setLoading(true);
    // TODO: Replace with actual API endpoint when AnalyzerErrorRestController is implemented
    // Endpoint will be: GET /rest/analyzer/errors?errorType=...&severity=...&analyzer=...
    const endpoint = "/rest/analyzer/errors";
    const params = new URLSearchParams();

    if (searchFilters.errorType) {
      params.append("errorType", searchFilters.errorType);
    }
    if (searchFilters.severity) {
      params.append("severity", searchFilters.severity);
    }
    if (searchFilters.analyzer) {
      params.append("analyzer", searchFilters.analyzer);
    }
    if (searchFilters.search) {
      params.append("search", searchFilters.search);
    }

    const url = params.toString()
      ? `${endpoint}?${params.toString()}`
      : endpoint;

    getFromOpenElisServer(url, (data) => {
      // API returns { data: { content: [...], statistics: {...} }, status: "success" }
      let errors = [];
      let statistics = null;

      if (data && data.data) {
        // Response wrapped in data object
        if (Array.isArray(data.data.content)) {
          errors = data.data.content;
        } else if (Array.isArray(data.data)) {
          // Fallback: data.data might be the array directly
          errors = data.data;
        }
        if (data.data.statistics) {
          statistics = data.data.statistics;
        }
      } else if (Array.isArray(data)) {
        // Direct array response (fallback)
        errors = data;
      }

      setErrors(errors);
      setFilteredErrors(errors);

      // Use statistics from API if available, otherwise calculate from errors
      if (statistics) {
        setStats({
          total: statistics.totalErrors || errors.length,
          unacknowledged: statistics.unacknowledged || 0,
          critical: statistics.critical || 0,
          last24Hours: statistics.last24Hours || 0,
        });
      } else {
        const now = new Date();
        const last24Hours = new Date(now.getTime() - 24 * 60 * 60 * 1000);
        const unacknowledgedCount = errors.filter(
          (e) => e.status === "UNACKNOWLEDGED" || e.status === "unacknowledged",
        ).length;
        const criticalCount = errors.filter(
          (e) => e.severity === "CRITICAL" || e.severity === "critical",
        ).length;
        const last24HoursCount = errors.filter((e) => {
          const errorDate = new Date(e.timestamp || e.createdDate);
          return errorDate >= last24Hours;
        }).length;

        setStats({
          total: errors.length,
          unacknowledged: unacknowledgedCount,
          critical: criticalCount,
          last24Hours: last24HoursCount,
        });
      }
      setLoading(false);
    });
  }, []);

  // Initial load + restore state from URL/sessionStorage
  useEffect(() => {
    // Restore from URL query parameters
    const params = new URLSearchParams(location.search);
    const initialSearch = params.get("search") || "";
    const initialErrorType = params.get("errorType") || "";
    const initialSeverity = params.get("severity") || "";
    const initialAnalyzer = params.get("analyzer") || "";

    setSearchTerm(initialSearch);
    const initialFilters = {
      errorType: initialErrorType,
      severity: initialSeverity,
      analyzer: initialAnalyzer,
    };
    setFilters(initialFilters);
    loadErrors({
      ...initialFilters,
      ...(initialSearch ? { search: initialSearch } : {}),
    });

    // Restore scroll position from sessionStorage
    const storedScrollY = sessionStorage.getItem("errorDashboard.scrollY");
    if (storedScrollY) {
      try {
        setTimeout(() => {
          window.scrollTo(0, parseInt(storedScrollY, 10));
        }, 100);
      } catch (_) {
        // ignore
      }
    }

    // Persist scroll position on unload
    const onBeforeUnload = () => {
      sessionStorage.setItem("errorDashboard.scrollY", String(window.scrollY));
    };
    window.addEventListener("beforeunload", onBeforeUnload);
    return () => {
      window.removeEventListener("beforeunload", onBeforeUnload);
      sessionStorage.setItem("errorDashboard.scrollY", String(window.scrollY));
    };
  }, [loadErrors, location.search]);

  const handleSearch = (value) => {
    setSearchTerm(value);

    const params = new URLSearchParams(location.search);
    if (value.trim()) {
      params.set("search", value.trim());
    } else {
      params.delete("search");
    }
    history.replace({
      pathname: location.pathname,
      search: params.toString(),
    });

    if (searchTimeoutRef.current) {
      clearTimeout(searchTimeoutRef.current);
    }

    searchTimeoutRef.current = setTimeout(() => {
      const searchFilters = { ...filters };
      if (value.trim()) {
        searchFilters.search = value.trim();
      }
      loadErrors(searchFilters);
    }, 300);
  };

  const handleFilterChange = (filterName, value) => {
    const newFilters = { ...filters, [filterName]: value };
    setFilters(newFilters);

    const params = new URLSearchParams(location.search);
    if (value) {
      params.set(filterName, value);
    } else {
      params.delete(filterName);
    }
    history.replace({
      pathname: location.pathname,
      search: params.toString(),
    });

    loadErrors(newFilters);
  };

  // Handle acknowledge all
  const handleAcknowledgeAll = () => {
    // Get all unacknowledged error IDs
    const unacknowledgedErrorIds = filteredErrors
      .filter(
        (error) =>
          error.status === "UNACKNOWLEDGED" ||
          error.status === "unacknowledged",
      )
      .map((error) => error.id);

    if (unacknowledgedErrorIds.length === 0) {
      return; // No errors to acknowledge
    }

    // Call batch acknowledge endpoint
    const endpoint = "/rest/analyzer/errors/batch-acknowledge";
    const payload = JSON.stringify({ errorIds: unacknowledgedErrorIds });

    postToOpenElisServerFullResponse(endpoint, payload, (response) => {
      if (response.ok) {
        response.json().then((data) => {
          if (data.status === "success") {
            // Reload errors after acknowledgment
            loadErrors(filters);
          }
        });
      }
    });
  };

  // Handle view error details
  const handleViewDetails = (error) => {
    setSelectedError(error);
    setErrorDetailsOpen(true);
  };

  // Handle acknowledge error
  const handleAcknowledge = (errorId) => {
    // Call acknowledge endpoint
    const endpoint = `/rest/analyzer/errors/${errorId}/acknowledge`;
    const payload = JSON.stringify({});

    postToOpenElisServerFullResponse(endpoint, payload, (response) => {
      if (response.ok) {
        response.json().then((data) => {
          if (data.status === "success") {
            // Reload errors after acknowledgment
            loadErrors(filters);
            // Close error details modal if open
            if (selectedError && selectedError.id === errorId) {
              setErrorDetailsOpen(false);
            }
          }
        });
      }
    });
  };

  // Format timestamp
  const formatTimestamp = (timestamp) => {
    if (!timestamp) return "-";
    const date = new Date(timestamp);
    return intl.formatDate(date, {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
    });
  };

  // Table headers
  const headers = [
    {
      key: "timestamp",
      header: intl.formatMessage({
        id: "analyzer.errorDashboard.table.timestamp",
      }),
    },
    {
      key: "analyzer",
      header: intl.formatMessage({
        id: "analyzer.errorDashboard.table.analyzer",
      }),
    },
    {
      key: "type",
      header: intl.formatMessage({ id: "analyzer.errorDashboard.table.type" }),
    },
    {
      key: "severity",
      header: intl.formatMessage({
        id: "analyzer.errorDashboard.table.severity",
      }),
    },
    {
      key: "message",
      header: intl.formatMessage({
        id: "analyzer.errorDashboard.table.message",
      }),
    },
    {
      key: "status",
      header: intl.formatMessage({
        id: "analyzer.errorDashboard.table.status",
      }),
    },
    {
      key: "actions",
      header: intl.formatMessage({ id: "analyzer.table.actions" }),
    },
  ];

  // Format error data for table rows
  const rows = filteredErrors.map((error, index) => {
    const isAcknowledged =
      error?.status === "ACKNOWLEDGED" || error?.status === "acknowledged";
    const severity = error?.severity || "ERROR";
    const errorType = error?.errorType || "MAPPING";

    // Truncate message
    const message = error?.errorMessage || error?.message || "-";
    const truncatedMessage =
      message.length > 50 ? `${message.substring(0, 50)}...` : message;

    return {
      id: error?.id ?? `temp-${index}`,
      timestamp: formatTimestamp(error.timestamp || error.createdDate),
      analyzer: error.analyzerName || error.analyzer?.name || "-",
      type: errorType,
      severity: severity,
      message: truncatedMessage,
      status: isAcknowledged ? "Acknowledged" : "Unacknowledged",
      _error: error, // Store full error object for actions
    };
  });

  return (
    <div className="error-dashboard" data-testid="error-dashboard">
      {/* Header */}
      <div
        className="error-dashboard-header"
        data-testid="error-dashboard-header"
      >
        <div className="error-dashboard-header-title">
          <PageTitle
            breadcrumbs={[
              {
                label: intl.formatMessage({
                  id: "analyzer.page.hierarchy.root",
                }),
                link: "/analyzers",
              },
              {
                label: intl.formatMessage({
                  id: "analyzer.errorDashboard.title",
                }),
              },
            ]}
            subtitle={intl.formatMessage({
              id: "analyzer.errorDashboard.subtitle",
            })}
          />
        </div>
        <Button
          kind="primary"
          data-testid="acknowledge-all-button"
          onClick={handleAcknowledgeAll}
        >
          {intl.formatMessage({ id: "analyzer.errorDashboard.acknowledgeAll" })}
        </Button>
      </div>

      {/* Statistics Cards */}
      <Grid
        className="error-dashboard-stats"
        data-testid="error-dashboard-stats"
      >
        <Column lg={4} md={4} sm={4}>
          <Tile data-testid="stat-total">
            <div className="stat-label">
              {intl.formatMessage({
                id: "analyzer.errorDashboard.stats.total",
              })}
            </div>
            <div className="stat-value">{stats.total}</div>
          </Tile>
        </Column>
        <Column lg={4} md={4} sm={4}>
          <Tile data-testid="stat-unacknowledged">
            <div className="stat-label">
              {intl.formatMessage({
                id: "analyzer.errorDashboard.stats.unacknowledged",
              })}
            </div>
            <div className="stat-value">{stats.unacknowledged}</div>
          </Tile>
        </Column>
        <Column lg={4} md={4} sm={4}>
          <Tile data-testid="stat-critical">
            <div className="stat-label">
              {intl.formatMessage({
                id: "analyzer.errorDashboard.stats.critical",
              })}
            </div>
            <div className="stat-value">{stats.critical}</div>
          </Tile>
        </Column>
        <Column lg={4} md={4} sm={4}>
          <Tile data-testid="stat-last24hours">
            <div className="stat-label">
              {intl.formatMessage({
                id: "analyzer.errorDashboard.stats.last24Hours",
              })}
            </div>
            <div className="stat-value">{stats.last24Hours}</div>
          </Tile>
        </Column>
      </Grid>

      {/* Filter Bar */}
      <div
        className="error-dashboard-filters"
        data-testid="error-dashboard-filters"
      >
        <Grid>
          <Column lg={5} md={4} sm={4}>
            <Search
              data-testid="error-search-input"
              placeholder={intl.formatMessage({
                id: "analyzer.errorDashboard.filter.search",
              })}
              labelText={intl.formatMessage({
                id: "analyzer.errorDashboard.filter.search",
              })}
              value={searchTerm}
              onChange={(e) => handleSearch(e.target.value)}
              size="lg"
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <Dropdown
              id="error-type-filter"
              data-testid="error-type-filter"
              titleText={intl.formatMessage({
                id: "analyzer.errorDashboard.filter.type",
              })}
              label={intl.formatMessage({
                id: "analyzer.errorDashboard.filter.type.all",
              })}
              items={[
                intl.formatMessage({
                  id: "analyzer.errorDashboard.filter.type.all",
                }),
                intl.formatMessage({
                  id: "analyzer.errorDashboard.errorType.mapping",
                }),
                intl.formatMessage({
                  id: "analyzer.errorDashboard.errorType.validation",
                }),
                intl.formatMessage({
                  id: "analyzer.errorDashboard.errorType.timeout",
                }),
                intl.formatMessage({
                  id: "analyzer.errorDashboard.errorType.protocol",
                }),
                intl.formatMessage({
                  id: "analyzer.errorDashboard.errorType.connection",
                }),
              ]}
              selectedItem={
                filters.errorType ||
                intl.formatMessage({
                  id: "analyzer.errorDashboard.filter.type.all",
                })
              }
              onChange={({ selectedItem }) =>
                handleFilterChange(
                  "errorType",
                  selectedItem ===
                    intl.formatMessage({
                      id: "analyzer.errorDashboard.filter.type.all",
                    })
                    ? ""
                    : selectedItem,
                )
              }
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <Dropdown
              id="severity-filter"
              data-testid="severity-filter"
              titleText={intl.formatMessage({
                id: "analyzer.errorDashboard.filter.severity",
              })}
              label={intl.formatMessage({
                id: "analyzer.errorDashboard.filter.severity.all",
              })}
              items={[
                intl.formatMessage({
                  id: "analyzer.errorDashboard.filter.severity.all",
                }),
                intl.formatMessage({
                  id: "analyzer.errorDashboard.severity.critical",
                }),
                intl.formatMessage({
                  id: "analyzer.errorDashboard.severity.error",
                }),
                intl.formatMessage({
                  id: "analyzer.errorDashboard.severity.warning",
                }),
              ]}
              selectedItem={
                filters.severity ||
                intl.formatMessage({
                  id: "analyzer.errorDashboard.filter.severity.all",
                })
              }
              onChange={({ selectedItem }) =>
                handleFilterChange(
                  "severity",
                  selectedItem ===
                    intl.formatMessage({
                      id: "analyzer.errorDashboard.filter.severity.all",
                    })
                    ? ""
                    : selectedItem,
                )
              }
            />
          </Column>
          <Column lg={3} md={4} sm={4}>
            <Dropdown
              id="analyzer-filter"
              data-testid="analyzer-filter"
              titleText={intl.formatMessage({
                id: "analyzer.errorDashboard.filter.analyzer",
              })}
              label={intl.formatMessage({
                id: "analyzer.errorDashboard.filter.analyzer.all",
              })}
              items={[
                intl.formatMessage({
                  id: "analyzer.errorDashboard.filter.analyzer.all",
                }),
                // TODO: Populate with actual analyzer list from API
                // For now, show empty list - will be populated when AnalyzerErrorRestController is implemented
              ]}
              selectedItem={
                filters.analyzer ||
                intl.formatMessage({
                  id: "analyzer.errorDashboard.filter.analyzer.all",
                })
              }
              onChange={({ selectedItem }) =>
                handleFilterChange(
                  "analyzer",
                  selectedItem ===
                    intl.formatMessage({
                      id: "analyzer.errorDashboard.filter.analyzer.all",
                    })
                    ? ""
                    : selectedItem,
                )
              }
            />
          </Column>
        </Grid>
      </div>

      {/* DataTable */}
      <Grid>
        <Column lg={16} md={8} sm={4}>
          <TableContainer data-testid="error-table-container">
            <DataTable rows={rows} headers={headers} isSortable>
              {({
                rows,
                headers,
                getHeaderProps,
                getRowProps,
                getTableProps,
              }) => (
                <Table {...getTableProps()} data-testid="error-table">
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
                    {rows.map((row) => {
                      const error =
                        row._error ||
                        filteredErrors.find((e) => e?.id === row?.id);
                      const isAcknowledged =
                        error?.status === "ACKNOWLEDGED" ||
                        error?.status === "acknowledged";
                      const severity = error?.severity || "ERROR";
                      const errorType = error?.errorType || "MAPPING";

                      // Get severity color
                      const severityColor =
                        severity === "CRITICAL" || severity === "critical"
                          ? "red"
                          : severity === "ERROR" || severity === "error"
                            ? "magenta"
                            : "blue";

                      // Get error type label
                      const errorTypeKey = `analyzer.errorDashboard.errorType.${errorType.toLowerCase()}`;
                      const errorTypeLabel = intl.formatMessage(
                        { id: errorTypeKey },
                        errorType,
                      );

                      return (
                        <TableRow
                          key={row.id}
                          {...getRowProps({ row })}
                          data-testid={`error-row-${row.id}`}
                        >
                          {row.cells.map((cell) => {
                            const headerKey = cell.info.header;
                            let cellContent = cell.value;
                            let testId = null;

                            if (headerKey === "type") {
                              testId = `error-type-${row.id}`;
                              cellContent = (
                                <Tag type="blue" data-testid={testId}>
                                  {errorTypeLabel}
                                </Tag>
                              );
                            } else if (headerKey === "severity") {
                              testId = `error-severity-${row.id}`;
                              const severityKey = `analyzer.errorDashboard.severity.${severity.toLowerCase()}`;
                              const severityLabel = intl.formatMessage(
                                { id: severityKey },
                                severity,
                              );
                              cellContent = (
                                <Tag type={severityColor} data-testid={testId}>
                                  {severityLabel}
                                </Tag>
                              );
                            } else if (headerKey === "status") {
                              testId = `error-status-${row.id}`;
                              const statusKey = isAcknowledged
                                ? "analyzer.errorDashboard.status.acknowledged"
                                : "analyzer.errorDashboard.status.unacknowledged";
                              const statusLabel = intl.formatMessage({
                                id: statusKey,
                              });
                              cellContent = (
                                <Tag
                                  type={isAcknowledged ? "green" : "red"}
                                  data-testid={testId}
                                >
                                  {statusLabel}
                                </Tag>
                              );
                            } else if (headerKey === "actions") {
                              testId = `error-actions-${row.id}`;
                              cellContent = error ? (
                                <OverflowMenu
                                  ariaLabel={intl.formatMessage({
                                    id: "analyzer.errorDashboard.action.menu",
                                  })}
                                >
                                  <OverflowMenuItem
                                    itemText={intl.formatMessage({
                                      id: "analyzer.errorDashboard.action.viewDetails",
                                    })}
                                    onClick={() => handleViewDetails(error)}
                                    data-testid={`error-action-view-${row.id}`}
                                  />
                                  {!isAcknowledged && error?.id && (
                                    <OverflowMenuItem
                                      itemText={intl.formatMessage({
                                        id: "analyzer.errorDashboard.action.acknowledge",
                                      })}
                                      onClick={() =>
                                        handleAcknowledge(error.id)
                                      }
                                      data-testid={`error-action-acknowledge-${row.id}`}
                                    />
                                  )}
                                </OverflowMenu>
                              ) : null;
                            } else {
                              testId = `error-${headerKey}-${row.id}`;
                            }

                            return (
                              <TableCell key={cell.id} data-testid={testId}>
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
        </Column>
      </Grid>

      {/* Error Details Modal */}
      {errorDetailsOpen && selectedError && (
        <ErrorDetailsModal
          error={selectedError}
          open={errorDetailsOpen}
          onClose={() => {
            setErrorDetailsOpen(false);
            setSelectedError(null);
          }}
          onAcknowledge={handleAcknowledge}
        />
      )}
    </div>
  );
};

export default ErrorDashboard;
