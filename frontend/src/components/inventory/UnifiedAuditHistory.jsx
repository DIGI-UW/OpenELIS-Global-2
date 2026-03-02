import React, { useState, useEffect } from "react";
import {
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableExpandHeader,
  TableExpandRow,
  TableExpandedRow,
  Tag,
  SkeletonText,
  InlineNotification,
  DatePicker,
  DatePickerInput,
  Select,
  SelectItem,
  Pagination,
  Grid,
  Column,
  Tile,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { InventoryAuditLogAPI } from "./InventoryService";
import "./AuditLogViewer.css";

const UnifiedAuditHistory = () => {
  const intl = useIntl();
  const [auditLogs, setAuditLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [statistics, setStatistics] = useState(null);

  // Filters
  const [filters, setFilters] = useState({
    startDate: "",
    endDate: "",
    entityType: "",
    activity: "",
    limit: 100,
    offset: 0,
  });

  // Pagination
  const [pagination, setpagination] = useState({
    totalRecords: 0,
    currentPage: 1,
    pageSize: 100,
    hasMore: false,
  });

  useEffect(() => {
    fetchAuditLogs();
    fetchStatistics();
  }, [filters]);

  const fetchAuditLogs = async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await InventoryAuditLogAPI.getAllAuditLogs(filters);
      setAuditLogs(response.logs || []);
      setpagination({
        totalRecords: response.totalRecords || 0,
        currentPage: Math.floor(filters.offset / filters.limit) + 1,
        pageSize: filters.limit,
        hasMore: response.hasMore || false,
      });
    } catch (err) {
      console.error("Error fetching audit logs:", err);
      setError(err.message || "Failed to load audit logs");
      setAuditLogs([]);
    } finally {
      setLoading(false);
    }
  };

  const fetchStatistics = async () => {
    try {
      const stats = await InventoryAuditLogAPI.getStatistics();
      setStatistics(stats);
    } catch (err) {
      console.error("Error fetching statistics:", err);
    }
  };

  const handleFilterChange = (filterName, value) => {
    setFilters((prev) => ({
      ...prev,
      [filterName]: value,
      offset: 0, // Reset to first page when filters change
    }));
  };

  const handlePageChange = ({ page, pageSize }) => {
    setFilters((prev) => ({
      ...prev,
      limit: pageSize,
      offset: (page - 1) * pageSize,
    }));
  };

  const getActivityTag = (activity) => {
    const tagMap = {
      I: { type: "green", label: "Created" },
      U: { type: "blue", label: "Updated" },
      D: { type: "red", label: "Deleted" },
    };

    const config = tagMap[activity] || {
      type: "gray",
      label: activity || "Unknown",
    };
    return <Tag type={config.type}>{config.label}</Tag>;
  };

  const getEntityTypeTag = (entityType) => {
    const tagMap = {
      ITEM: { type: "purple", label: "Item" },
      LOT: { type: "cyan", label: "Lot" },
      LOCATION: { type: "teal", label: "Location" },
      USAGE: { type: "magenta", label: "Usage" },
      TRANSACTION: { type: "blue", label: "Transaction" },
    };

    const config = tagMap[entityType] || {
      type: "gray",
      label: entityType || "Unknown",
    };
    return <Tag type={config.type}>{config.label}</Tag>;
  };

  const formatTimestamp = (timestamp) => {
    try {
      const date = new Date(timestamp);
      return date.toLocaleString();
    } catch (e) {
      return timestamp;
    }
  };

  const formatFieldName = (fieldName) => {
    const words = fieldName.replace(/([A-Z])/g, " $1").trim();
    return words.charAt(0).toUpperCase() + words.slice(1);
  };

  const formatValue = (value, fieldName) => {
    if (!value || value === "") {
      return <em className="empty-value">(empty)</em>;
    }

    if (
      fieldName.toLowerCase().includes("date") ||
      fieldName.toLowerCase().includes("timestamp")
    ) {
      try {
        const date = new Date(value);
        if (!isNaN(date.getTime())) {
          return date.toLocaleString();
        }
      } catch (e) {
        // ignore
      }
    }

    if (value === "true" || value === "false") {
      return value === "true" ? "Yes" : "No";
    }

    return value;
  };

  const getChangeClass = (oldVal, newVal) => {
    if (!oldVal || oldVal === "") return "change-added";
    if (!newVal || newVal === "") return "change-removed";
    return "change-modified";
  };

  const renderChanges = (log) => {
    const changes = log.changes || {};

    if (Object.keys(changes).length === 0) {
      return (
        <p className="no-changes-message">
          <em>No field changes recorded</em>
        </p>
      );
    }

    return (
      <div className="field-changes">
        <table className="changes-table">
          <thead>
            <tr>
              <th>Field</th>
              <th>Previous Value</th>
              <th>New Value</th>
            </tr>
          </thead>
          <tbody>
            {Object.entries(changes).map(([field, values]) => (
              <tr
                key={field}
                className={getChangeClass(values.old, values.new)}
              >
                <td>
                  <strong>{formatFieldName(field)}</strong>
                </td>
                <td className="old-value">{formatValue(values.old, field)}</td>
                <td className="new-value">{formatValue(values.new, field)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  };

  const headers = [
    {
      key: "timestamp",
      header: intl.formatMessage({
        id: "audit.timestamp",
        defaultMessage: "Timestamp",
      }),
    },
    {
      key: "entityType",
      header: intl.formatMessage({
        id: "audit.entityType",
        defaultMessage: "Type",
      }),
    },
    {
      key: "activity",
      header: intl.formatMessage({
        id: "audit.activity",
        defaultMessage: "Activity",
      }),
    },
    {
      key: "performedByUser",
      header: intl.formatMessage({
        id: "audit.user",
        defaultMessage: "User",
      }),
    },
    {
      key: "summary",
      header: intl.formatMessage({
        id: "audit.summary",
        defaultMessage: "Summary",
      }),
    },
  ];

  const formatActivity = (activity) => {
    const activityMap = {
      I: { label: "Created", color: "green" },
      U: { label: "Updated", color: "blue" },
      D: { label: "Deleted", color: "red" },
    };

    const config = activityMap[activity] || {
      label: activity,
      color: "gray",
    };

    return (
      <Tag type={config.color} size="sm">
        {config.label}
      </Tag>
    );
  };

  const rows = auditLogs.map((log, index) => ({
    id: `${log.id}-${index}`,
    timestamp: formatTimestamp(log.timestamp),
    entityType: log.entityType,
    activity: formatActivity(log.activity),
    performedByUser: log.performedByUser,
    summary: log.summary || "—",
    _log: log,
  }));

  return (
    <div className="unified-audit-history">
      {/* Statistics Cards */}
      {statistics && (
        <Grid className="audit-statistics" style={{ marginBottom: "2rem" }}>
          <Column lg={4} md={4} sm={4}>
            <Tile>
              <div className="stat-card">
                <div className="stat-label">
                  <FormattedMessage
                    id="audit.stats.total"
                    defaultMessage="Total Audit Logs"
                  />
                </div>
                <div className="stat-value">{statistics.totalLogs}</div>
              </div>
            </Tile>
          </Column>
          <Column lg={4} md={4} sm={4}>
            <Tile>
              <div className="stat-card">
                <div className="stat-label">
                  <FormattedMessage
                    id="audit.stats.inserts"
                    defaultMessage="Created"
                  />
                </div>
                <div className="stat-value stat-green">
                  {statistics.countByActivity.INSERT}
                </div>
              </div>
            </Tile>
          </Column>
          <Column lg={4} md={4} sm={4}>
            <Tile>
              <div className="stat-card">
                <div className="stat-label">
                  <FormattedMessage
                    id="audit.stats.updates"
                    defaultMessage="Updated"
                  />
                </div>
                <div className="stat-value stat-blue">
                  {statistics.countByActivity.UPDATE}
                </div>
              </div>
            </Tile>
          </Column>
          <Column lg={4} md={4} sm={4}>
            <Tile>
              <div className="stat-card">
                <div className="stat-label">
                  <FormattedMessage
                    id="audit.stats.deletes"
                    defaultMessage="Deleted"
                  />
                </div>
                <div className="stat-value stat-red">
                  {statistics.countByActivity.DELETE}
                </div>
              </div>
            </Tile>
          </Column>
        </Grid>
      )}

      {/* Filters */}
      <Grid className="audit-filters" style={{ marginBottom: "1rem" }}>
        <Column lg={6} md={4} sm={4}>
          <div style={{ display: "flex", gap: "1rem", alignItems: "flex-end" }}>
            <div style={{ flex: 1 }}>
              <DatePicker
                datePickerType="single"
                onChange={(dates) => {
                  const date = dates[0];
                  handleFilterChange(
                    "startDate",
                    date ? date.toISOString().split("T")[0] : "",
                  );
                }}
              >
                <DatePickerInput
                  id="date-picker-input-id-start"
                  placeholder="mm/dd/yyyy"
                  labelText={intl.formatMessage({
                    id: "audit.filter.startDate",
                    defaultMessage: "Start Date",
                  })}
                  size="md"
                />
              </DatePicker>
            </div>
            <div style={{ flex: 1 }}>
              <DatePicker
                datePickerType="single"
                onChange={(dates) => {
                  const date = dates[0];
                  handleFilterChange(
                    "endDate",
                    date ? date.toISOString().split("T")[0] : "",
                  );
                }}
              >
                <DatePickerInput
                  id="date-picker-input-id-end"
                  placeholder="mm/dd/yyyy"
                  labelText={intl.formatMessage({
                    id: "audit.filter.endDate",
                    defaultMessage: "End Date",
                  })}
                  size="md"
                />
              </DatePicker>
            </div>
          </div>
        </Column>

        <Column lg={3} md={4} sm={4}>
          <Select
            id="entity-type-filter"
            labelText={intl.formatMessage({
              id: "audit.filter.entityType",
              defaultMessage: "Entity Type",
            })}
            value={filters.entityType}
            onChange={(e) => handleFilterChange("entityType", e.target.value)}
          >
            <SelectItem value="" text="All Types" />
            <SelectItem value="ITEM" text="Items" />
            <SelectItem value="LOT" text="Lots" />
            <SelectItem value="LOCATION" text="Locations" />
            <SelectItem value="USAGE" text="Usage" />
            <SelectItem value="TRANSACTION" text="Transactions" />
          </Select>
        </Column>

        <Column lg={3} md={4} sm={4}>
          <Select
            id="activity-filter"
            labelText={intl.formatMessage({
              id: "audit.filter.activity",
              defaultMessage: "Activity",
            })}
            value={filters.activity}
            onChange={(e) => handleFilterChange("activity", e.target.value)}
          >
            <SelectItem value="" text="All Activities" />
            <SelectItem value="I" text="Created" />
            <SelectItem value="U" text="Updated" />
            <SelectItem value="D" text="Deleted" />
          </Select>
        </Column>
      </Grid>

      {/* Audit Logs Table */}
      {loading ? (
        <div className="loading-container">
          <SkeletonText paragraph lineCount={10} />
        </div>
      ) : error ? (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({
            id: "audit.error.title",
            defaultMessage: "Error Loading Audit Logs",
          })}
          subtitle={error}
          hideCloseButton
        />
      ) : auditLogs.length === 0 ? (
        <InlineNotification
          kind="info"
          title={intl.formatMessage({
            id: "audit.empty.title",
            defaultMessage: "No Audit Logs",
          })}
          subtitle={intl.formatMessage({
            id: "audit.empty.message",
            defaultMessage:
              "No audit logs found with the current filters. Try adjusting your search criteria.",
          })}
          hideCloseButton
        />
      ) : (
        <>
          <DataTable rows={rows} headers={headers}>
            {({
              rows,
              headers,
              getHeaderProps,
              getRowProps,
              getTableProps,
              getTableContainerProps,
            }) => (
              <TableContainer
                title=""
                description={`Showing ${auditLogs.length} of ${pagination.totalRecords} records`}
                {...getTableContainerProps()}
              >
                <Table {...getTableProps()}>
                  <TableHead>
                    <TableRow>
                      <TableExpandHeader />
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
                    {rows.map((row, rowIndex) => {
                      const log = auditLogs[rowIndex];
                      return (
                        <React.Fragment key={row.id}>
                          <TableExpandRow {...getRowProps({ row })}>
                            {row.cells.map((cell) => {
                              if (cell.info.header === "activity") {
                                return (
                                  <TableCell key={cell.id}>
                                    {getActivityTag(cell.value)}
                                  </TableCell>
                                );
                              }
                              if (cell.info.header === "entityType") {
                                return (
                                  <TableCell key={cell.id}>
                                    {getEntityTypeTag(cell.value)}
                                  </TableCell>
                                );
                              }
                              return (
                                <TableCell key={cell.id}>
                                  {cell.value}
                                </TableCell>
                              );
                            })}
                          </TableExpandRow>
                          <TableExpandedRow colSpan={headers.length + 1}>
                            <div className="expanded-audit-details">
                              <h5>
                                <FormattedMessage
                                  id="audit.details.changes"
                                  defaultMessage="Field Changes"
                                />
                              </h5>
                              {renderChanges(log)}
                            </div>
                          </TableExpandedRow>
                        </React.Fragment>
                      );
                    })}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </DataTable>

          {/* Pagination */}
          <Pagination
            totalItems={pagination.totalRecords}
            pageSize={pagination.pageSize}
            pageSizes={[50, 100, 200, 500]}
            page={pagination.currentPage}
            onChange={handlePageChange}
          />
        </>
      )}
    </div>
  );
};

export default UnifiedAuditHistory;
