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
  Column,
  Grid,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import NotebookAuditLogAPI from "./NotebookAuditLogAPI";

/**
 * Notebook Audit Log Viewer Component
 *
 * Displays audit trail for notebook entities (NoteBook, NotebookEntry, etc.)
 * with expandable rows showing field-level changes.
 */
const NotebookAuditLogViewer = ({ entityType, entityId }) => {
  const intl = useIntl();
  const [auditLogs, setAuditLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    console.log(
      "NotebookAuditLogViewer: entityType=",
      entityType,
      "entityId=",
      entityId,
    );
    if (entityType && entityId) {
      fetchAuditLogs();
    } else {
      setLoading(false);
      if (!entityId) {
        console.warn("NotebookAuditLogViewer: entityId is null or undefined");
      }
    }
  }, [entityType, entityId]);

  const fetchAuditLogs = () => {
    setLoading(true);
    setError(null);

    console.log(
      "Fetching audit logs for entityType:",
      entityType,
      "entityId:",
      entityId,
    );

    const handleSuccess = (data) => {
      console.log("Audit logs fetched:", data);
      // Ensure data is an array and filter out any null/undefined entries
      if (Array.isArray(data)) {
        setAuditLogs(data.filter((log) => log != null));
      } else if (data && data.error) {
        // Handle error response from server
        setError(data.error || "Failed to load audit logs");
        setAuditLogs([]);
      } else {
        // Fallback to empty array
        setAuditLogs([]);
      }
      setLoading(false);
    };

    const handleError = (err) => {
      console.error("Error fetching audit logs:", err);
      setError(err?.message || "Failed to load audit logs");
      setLoading(false);
    };

    try {
      if (entityType === "NOTEBOOK" || entityType === "INSTANCE") {
        NotebookAuditLogAPI.getNotebookAuditTrail(entityId, handleSuccess);
      } else if (entityType === "ENTRY") {
        NotebookAuditLogAPI.getEntryAuditTrail(entityId, handleSuccess);
      } else if (entityType === "PAGE_SAMPLE") {
        NotebookAuditLogAPI.getPageSampleAuditTrail(entityId, handleSuccess);
      } else {
        console.error("Unknown entity type:", entityType);
        setError("Unsupported entity type: " + entityType);
        setLoading(false);
      }
    } catch (err) {
      handleError(err);
    }
  };

  const getActivityTag = (activity) => {
    const tagMap = {
      INSERT: {
        type: "green",
        label: intl.formatMessage({
          id: "notebook.audit.activity.INSERT",
          defaultMessage: "Created",
        }),
      },
      UPDATE: {
        type: "blue",
        label: intl.formatMessage({
          id: "notebook.audit.activity.UPDATE",
          defaultMessage: "Updated",
        }),
      },
      DELETE: {
        type: "red",
        label: intl.formatMessage({
          id: "notebook.audit.activity.DELETE",
          defaultMessage: "Deleted",
        }),
      },
    };

    const config = tagMap[activity] || {
      type: "gray",
      label: activity || "Unknown",
    };
    return <Tag type={config.type}>{config.label}</Tag>;
  };

  const getStatusChangeTag = (log) => {
    if (log.statusOld && log.statusNew && log.statusOld !== log.statusNew) {
      return (
        <span>
          <Tag type="gray" size="sm">
            {log.statusOld}
          </Tag>
          {" → "}
          <Tag type="cyan" size="sm">
            {log.statusNew}
          </Tag>
        </span>
      );
    }
    return null;
  };

  const formatTimestamp = (timestamp) => {
    try {
      const date = new Date(timestamp);
      return date.toLocaleString();
    } catch (e) {
      return timestamp;
    }
  };

  const renderChanges = (log) => {
    const changes = log.changes || {};

    if (Object.keys(changes).length === 0) {
      return (
        <p>
          <FormattedMessage
            id="notebook.audit.noChanges"
            defaultMessage="No field changes recorded"
          />
        </p>
      );
    }

    return (
      <div className="field-changes">
        <table className="changes-table">
          <thead>
            <tr>
              <th>
                <FormattedMessage
                  id="notebook.audit.field"
                  defaultMessage="Field"
                />
              </th>
              <th>
                <FormattedMessage
                  id="notebook.audit.oldValue"
                  defaultMessage="Old Value"
                />
              </th>
              <th>
                <FormattedMessage
                  id="notebook.audit.newValue"
                  defaultMessage="New Value"
                />
              </th>
            </tr>
          </thead>
          <tbody>
            {Object.entries(changes).map(([field, values]) => (
              <tr key={field}>
                <td>
                  <strong>{field}</strong>
                </td>
                <td>{values.old || <em>—</em>}</td>
                <td>{values.new || <em>—</em>}</td>
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
        id: "notebook.audit.timestamp",
        defaultMessage: "Timestamp",
      }),
    },
    {
      key: "activity",
      header: intl.formatMessage({
        id: "notebook.audit.activity",
        defaultMessage: "Activity",
      }),
    },
    {
      key: "performedByUser",
      header: intl.formatMessage({
        id: "notebook.audit.user",
        defaultMessage: "User",
      }),
    },
    {
      key: "summary",
      header: intl.formatMessage({
        id: "notebook.audit.summary",
        defaultMessage: "Summary",
      }),
    },
  ];

  // Create a lookup map for original logs by ID
  const logLookup = {};
  const rows = auditLogs
    .filter((log) => log != null) // Filter out null/undefined entries
    .map((log, index) => {
      const rowId = log.id || `log-${index}`;
      logLookup[rowId] = log; // Store original log for later lookup
      return {
        id: rowId,
        timestamp: formatTimestamp(log.timestamp),
        activity: log.activityDisplay || log.activity,
        activityRaw: log.activityDisplay || log.activity || "Unknown", // Store for tag rendering
        performedByUser: log.performedByUser || "Unknown",
        summary: log.summary || "No summary available",
        statusChange: getStatusChangeTag(log),
        _originalLog: log,
      };
    });

  if (loading) {
    return (
      <Grid fullWidth={true} className="gridBoundary">
        <Column lg={16} md={8} sm={4}>
          <SkeletonText paragraph lineCount={5} />
        </Column>
      </Grid>
    );
  }

  if (error) {
    return (
      <Grid fullWidth={true} className="gridBoundary">
        <Column lg={16} md={8} sm={4}>
          <InlineNotification
            kind="error"
            title={intl.formatMessage({
              id: "notebook.audit.error.title",
              defaultMessage: "Error",
            })}
            subtitle={error}
          />
        </Column>
      </Grid>
    );
  }

  // If no entityId, show message that entity must be saved first
  if (!entityId) {
    return (
      <Grid fullWidth={true} className="gridBoundary">
        <Column lg={16} md={8} sm={4}>
          <InlineNotification
            kind="info"
            title={intl.formatMessage({
              id: "notebook.audit.notSaved.title",
              defaultMessage: "Not Saved Yet",
            })}
            subtitle={intl.formatMessage({
              id: "notebook.audit.notSaved.subtitle",
              defaultMessage:
                "Save this notebook entry first to view its audit trail.",
            })}
            hideCloseButton
          />
        </Column>
      </Grid>
    );
  }

  if (auditLogs.length === 0 && !loading) {
    return (
      <Grid fullWidth={true} className="gridBoundary">
        <Column lg={16} md={8} sm={4}>
          <InlineNotification
            kind="info"
            title={intl.formatMessage({
              id: "notebook.audit.empty.title",
              defaultMessage: "No Audit Logs",
            })}
            subtitle={intl.formatMessage({
              id: "notebook.audit.empty.subtitle",
              defaultMessage:
                "No audit trail found for this entry. Changes will be recorded here once you start editing.",
            })}
            hideCloseButton
          />
        </Column>
      </Grid>
    );
  }

  return (
    <Grid fullWidth={true} className="gridBoundary">
      <Column lg={16} md={8} sm={4}>
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
              {...getTableContainerProps()}
              title={intl.formatMessage({
                id: "notebook.audit.title",
                defaultMessage: "Audit Trail",
              })}
              description={intl.formatMessage({
                id: "notebook.audit.description",
                defaultMessage:
                  "Complete history of changes made to this notebook",
              })}
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
                  {rows.map((row) => (
                    <React.Fragment key={row.id}>
                      <TableExpandRow {...getRowProps({ row })}>
                        {row.cells.map((cell) => {
                          if (cell.info.header === "activity") {
                            // Use cell.value which contains the activity from the row data
                            const originalLog = logLookup[row.id];
                            const activityValue =
                              cell.value ||
                              originalLog?.activityDisplay ||
                              "Unknown";
                            return (
                              <TableCell key={cell.id}>
                                {getActivityTag(activityValue)}
                                {row.statusChange && (
                                  <div style={{ marginTop: "0.5rem" }}>
                                    {row.statusChange}
                                  </div>
                                )}
                              </TableCell>
                            );
                          }
                          return (
                            <TableCell key={cell.id}>{cell.value}</TableCell>
                          );
                        })}
                      </TableExpandRow>
                      <TableExpandedRow colSpan={headers.length + 1}>
                        {logLookup[row.id] ? (
                          renderChanges(logLookup[row.id])
                        ) : (
                          <p>
                            <FormattedMessage
                              id="notebook.audit.noData"
                              defaultMessage="No data available"
                            />
                          </p>
                        )}
                      </TableExpandedRow>
                    </React.Fragment>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </DataTable>
      </Column>
    </Grid>
  );
};

export default NotebookAuditLogViewer;
