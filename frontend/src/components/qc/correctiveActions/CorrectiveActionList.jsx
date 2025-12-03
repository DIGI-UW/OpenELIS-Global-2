/**
 * CorrectiveActionList Component
 *
 * Displays list of corrective actions with filtering and management
 * Task Reference: T147
 * Specification: FR-039 to FR-045, User Story 4
 *
 * Features:
 * - DataTable with filtering by status, action type
 * - Mark actions as complete
 * - View corrective action details
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
import PageTitle from "../../common/PageTitle/PageTitle";
import "./CorrectiveActionList.css";

const CorrectiveActionList = () => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();

  // State
  const [actions, setActions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [filters, setFilters] = useState({
    status: "",
    actionType: "",
  });

  // Status options
  const statusOptions = [
    { id: "", label: intl.formatMessage({ id: "qc.correctiveActions.filter.allStatuses" }) },
    { id: "PENDING", label: intl.formatMessage({ id: "qc.correctiveAction.status.pending" }) },
    { id: "IN_PROGRESS", label: intl.formatMessage({ id: "qc.correctiveAction.status.inProgress" }) },
    { id: "COMPLETED", label: intl.formatMessage({ id: "qc.correctiveAction.status.completed" }) },
  ];

  // Action type options
  const actionTypeOptions = [
    { id: "", label: intl.formatMessage({ id: "qc.correctiveActions.filter.allTypes" }) },
    { id: "RECALIBRATION", label: intl.formatMessage({ id: "qc.correctiveAction.type.recalibration" }) },
    { id: "MAINTENANCE", label: intl.formatMessage({ id: "qc.correctiveAction.type.maintenance" }) },
    { id: "REPEAT_CONTROL", label: intl.formatMessage({ id: "qc.correctiveAction.type.repeatControl" }) },
    { id: "REAGENT_CHANGE", label: intl.formatMessage({ id: "qc.correctiveAction.type.reagentChange" }) },
    { id: "OTHER", label: intl.formatMessage({ id: "qc.correctiveAction.type.other" }) },
  ];

  // Load corrective actions
  const loadActions = useCallback(() => {
    setLoading(true);
    setError(null);

    const params = new URLSearchParams();
    if (filters.status) params.append("status", filters.status);
    if (filters.actionType) params.append("actionType", filters.actionType);
    if (searchTerm) params.append("search", searchTerm);

    const url = `/rest/qc/corrective-actions${params.toString() ? `?${params.toString()}` : ""}`;

    getFromOpenElisServer(url, (response) => {
      if (response && response.data) {
        setActions(response.data.actions || response.data || []);
      } else if (Array.isArray(response)) {
        setActions(response);
      } else {
        setError(intl.formatMessage({ id: "qc.correctiveActions.error.loadFailed" }));
      }
      setLoading(false);
    });
  }, [filters, searchTerm, intl]);

  // Initial load
  useEffect(() => {
    // Restore filters from URL
    const params = new URLSearchParams(location.search);
    const initialFilters = {
      status: params.get("status") || "",
      actionType: params.get("actionType") || "",
    };
    setFilters(initialFilters);
    setSearchTerm(params.get("search") || "");
  }, [location.search]);

  useEffect(() => {
    loadActions();
  }, [loadActions]);

  // Update URL with filters
  const updateUrlParams = (newFilters, newSearch) => {
    const params = new URLSearchParams();
    if (newFilters.status) params.set("status", newFilters.status);
    if (newFilters.actionType) params.set("actionType", newFilters.actionType);
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

  // Handle mark complete (FR-042)
  const handleMarkComplete = (actionId, resolutionNotes = "") => {
    const endpoint = `/rest/qc/corrective-actions/${actionId}/complete`;
    postToOpenElisServerFullResponse(
      endpoint,
      JSON.stringify({ resolutionNotes }),
      (response) => {
        if (response.ok) {
          loadActions();
        } else {
          setError(intl.formatMessage({ id: "qc.correctiveActions.error.completeFailed" }));
        }
      }
    );
  };

  // Handle start progress
  const handleStartProgress = (actionId) => {
    const endpoint = `/rest/qc/corrective-actions/${actionId}/start`;
    postToOpenElisServerFullResponse(endpoint, JSON.stringify({}), (response) => {
      if (response.ok) {
        loadActions();
      } else {
        setError(intl.formatMessage({ id: "qc.correctiveActions.error.startFailed" }));
      }
    });
  };

  // Navigate to create new action
  const handleCreateNew = () => {
    history.push("/analyzers/qc/violations");
  };

  // Get status tag type
  const getStatusTagType = (status) => {
    switch (status) {
      case "COMPLETED":
        return "green";
      case "IN_PROGRESS":
        return "blue";
      default:
        return "gray";
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
    { key: "createdDate", header: intl.formatMessage({ id: "qc.correctiveActions.table.created" }) },
    { key: "actionType", header: intl.formatMessage({ id: "qc.correctiveActions.table.actionType" }) },
    { key: "analyzer", header: intl.formatMessage({ id: "qc.correctiveActions.table.analyzer" }) },
    { key: "violation", header: intl.formatMessage({ id: "qc.correctiveActions.table.violation" }) },
    { key: "assignedTo", header: intl.formatMessage({ id: "qc.correctiveActions.table.assignedTo" }) },
    { key: "status", header: intl.formatMessage({ id: "qc.correctiveActions.table.status" }) },
    { key: "actions", header: "" },
  ];

  // Format rows
  const rows = actions.map((action) => ({
    id: action.id,
    createdDate: formatTimestamp(action.createdDate),
    actionType: intl.formatMessage({ id: `qc.correctiveAction.type.${action.actionType?.toLowerCase()}` }),
    analyzer: action.analyzerName || action.violation?.analyzerName || "-",
    violation: action.violationRuleCode || action.violation?.ruleCode || "-",
    assignedTo: action.assignedUserName || action.assignedUser?.displayName || "-",
    status: action.status,
    _action: action,
  }));

  if (loading && actions.length === 0) {
    return (
      <div className="corrective-action-list-loading" data-testid="corrective-action-list-loading">
        <Loading
          description={intl.formatMessage({ id: "qc.correctiveActions.loading" })}
          withOverlay={false}
        />
      </div>
    );
  }

  return (
    <div className="corrective-action-list" data-testid="corrective-action-list">
      {/* Header */}
      <div className="corrective-action-list-header" data-testid="corrective-action-list-header">
        <div className="corrective-action-list-header-title">
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
                label: intl.formatMessage({ id: "qc.correctiveActions.title" }),
              },
            ]}
            subtitle={intl.formatMessage({ id: "qc.correctiveActions.subtitle" })}
          />
        </div>
        <Button
          kind="primary"
          renderIcon={Add}
          onClick={handleCreateNew}
          data-testid="corrective-action-create-button"
        >
          {intl.formatMessage({ id: "qc.correctiveActions.createNew" })}
        </Button>
      </div>

      {/* Error notification */}
      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({ id: "qc.correctiveActions.error.title" })}
          subtitle={error}
          onClose={() => setError(null)}
          data-testid="corrective-action-list-error"
        />
      )}

      {/* Filters */}
      <Grid className="corrective-action-list-filters" data-testid="corrective-action-list-filters">
        <Column lg={4} md={4} sm={4}>
          <Search
            placeholder={intl.formatMessage({ id: "qc.correctiveActions.filter.search" })}
            labelText={intl.formatMessage({ id: "qc.correctiveActions.filter.search" })}
            value={searchTerm}
            onChange={(e) => handleSearch(e.target.value)}
            data-testid="corrective-action-search-input"
          />
        </Column>
        <Column lg={3} md={2} sm={4}>
          <Dropdown
            id="status-filter"
            titleText={intl.formatMessage({ id: "qc.correctiveActions.filter.status" })}
            items={statusOptions}
            itemToString={(item) => item?.label || ""}
            selectedItem={statusOptions.find((o) => o.id === filters.status)}
            onChange={({ selectedItem }) => handleFilterChange("status", selectedItem?.id || "")}
            data-testid="corrective-action-status-filter"
          />
        </Column>
        <Column lg={3} md={2} sm={4}>
          <Dropdown
            id="action-type-filter"
            titleText={intl.formatMessage({ id: "qc.correctiveActions.filter.actionType" })}
            items={actionTypeOptions}
            itemToString={(item) => item?.label || ""}
            selectedItem={actionTypeOptions.find((o) => o.id === filters.actionType)}
            onChange={({ selectedItem }) => handleFilterChange("actionType", selectedItem?.id || "")}
            data-testid="corrective-action-type-filter"
          />
        </Column>
      </Grid>

      {/* DataTable */}
      <TableContainer data-testid="corrective-action-table-container">
        <DataTable rows={rows} headers={headers} isSortable>
          {({ rows, headers, getHeaderProps, getRowProps, getTableProps }) => (
            <Table {...getTableProps()} data-testid="corrective-action-table">
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
                  const action = row._action || actions.find((a) => a.id === row.id);
                  const isPending = action?.status === "PENDING";
                  const isInProgress = action?.status === "IN_PROGRESS";

                  return (
                    <TableRow key={row.id} {...getRowProps({ row })} data-testid={`corrective-action-row-${row.id}`}>
                      {row.cells.map((cell) => {
                        let cellContent = cell.value;

                        if (cell.info.header === "status") {
                          cellContent = (
                            <Tag type={getStatusTagType(cell.value)} data-testid={`corrective-action-status-${row.id}`}>
                              {intl.formatMessage({ id: `qc.correctiveAction.status.${cell.value?.toLowerCase()}` })}
                            </Tag>
                          );
                        } else if (cell.info.header === "actions") {
                          cellContent = (
                            <OverflowMenu
                              ariaLabel={intl.formatMessage({ id: "qc.correctiveActions.action.menu" })}
                              data-testid={`corrective-action-actions-${row.id}`}
                            >
                              {isPending && (
                                <OverflowMenuItem
                                  itemText={intl.formatMessage({ id: "qc.correctiveActions.action.start" })}
                                  onClick={() => handleStartProgress(action.id)}
                                />
                              )}
                              {isInProgress && (
                                <OverflowMenuItem
                                  itemText={intl.formatMessage({ id: "qc.correctiveActions.action.complete" })}
                                  onClick={() => handleMarkComplete(action.id)}
                                />
                              )}
                            </OverflowMenu>
                          );
                        }

                        return (
                          <TableCell key={cell.id} data-testid={`corrective-action-${cell.info.header}-${row.id}`}>
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
    </div>
  );
};

export default CorrectiveActionList;
