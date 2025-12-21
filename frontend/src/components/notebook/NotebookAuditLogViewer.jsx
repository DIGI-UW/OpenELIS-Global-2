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
import { NotebookAuditLogAPI } from "./NotebookService";
import "../inventory/AuditLogViewer.css";

const NotebookAuditLogViewer = ({ entityType, entityId }) => {
  const intl = useIntl();
  const [auditLogs, setAuditLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (entityType && entityId) {
      fetchAuditLogs();
    }
  }, [entityType, entityId]);

  const fetchAuditLogs = async () => {
    setLoading(true);
    setError(null);

    try {
      let logs;
      if (entityType === "NOTEBOOK") {
        logs = await NotebookAuditLogAPI.getNotebookAuditTrail(entityId);
      } else if (entityType === "INSTANCE") {
        logs = await NotebookAuditLogAPI.getInstanceAuditTrail(entityId);
      } else {
        console.error("Unknown entity type:", entityType);
        setError("Unsupported entity type: " + entityType);
        setLoading(false);
        return;
      }
      setAuditLogs(logs || []);
    } catch (err) {
      console.error("Error fetching audit logs:", err);
      setError(err.message || "Failed to load audit logs");
    } finally {
      setLoading(false);
    }
  };

  const getActivityTag = (activity) => {
    const tagMap = {
      INSERT: { type: "green", label: "Created" },
      UPDATE: { type: "blue", label: "Updated" },
      DELETE: { type: "red", label: "Deleted" },
    };

    const config = tagMap[activity] || {
      type: "gray",
      label: activity || "Unknown",
    };
    return <Tag type={config.type}>{config.label}</Tag>;
  };

  const formatTimestamp = (timestamp) => {
    try {
      const date = new Date(timestamp);
      return date.toLocaleString("en-GB", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
        hour12: false,
      });
    } catch (e) {
      return timestamp;
    }
  };

  const formatFieldName = (fieldName) => {
    // Convert camelCase or snake_case to Title Case
    return fieldName
      .replace(/([A-Z])/g, " $1")
      .replace(/_/g, " ")
      .replace(/^./, (str) => str.toUpperCase())
      .trim();
  };

  const renderChanges = (log) => {
    const changes = log.changes || {};

    if (Object.keys(changes).length === 0) {
      return (
        <p>
          <FormattedMessage
            id="audit.changes.none"
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
                  id="audit.changes.field"
                  defaultMessage="Field"
                />
              </th>
              <th>
                <FormattedMessage
                  id="audit.changes.old"
                  defaultMessage="Old Value"
                />
              </th>
              <th>
                <FormattedMessage
                  id="audit.changes.new"
                  defaultMessage="New Value"
                />
              </th>
            </tr>
          </thead>
          <tbody>
            {Object.entries(changes).map(([field, values]) => (
              <tr key={field}>
                <td>
                  <strong>{formatFieldName(field)}</strong>
                </td>
                <td className="old-value">{values.old || <em>—</em>}</td>
                <td className="new-value">{values.new || <em>—</em>}</td>
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
        id: "notebook.auditTrail.timestamp",
        defaultMessage: "Timestamp",
      }),
    },
    {
      key: "activity",
      header: intl.formatMessage({
        id: "notebook.auditTrail.activity",
        defaultMessage: "Activity",
      }),
    },
    {
      key: "performedByUser",
      header: intl.formatMessage({
        id: "notebook.auditTrail.user",
        defaultMessage: "User",
      }),
    },
    {
      key: "summary",
      header: intl.formatMessage({
        id: "notebook.auditTrail.summary",
        defaultMessage: "Summary",
      }),
    },
  ];

  const rows = auditLogs.map((log, index) => ({
    id: `${log.id}-${index}`,
    timestamp: formatTimestamp(log.timestamp),
    activity: log.activity,
    performedByUser: log.performedByUser,
    summary: log.summary || "—",
    _log: log, // Store full log for expanded row
  }));

  if (loading) {
    return (
      <Column lg={16} md={8} sm={4}>
        <div className="loading-container">
          <SkeletonText paragraph lineCount={5} />
        </div>
      </Column>
    );
  }

  if (error) {
    return (
      <Column lg={16} md={8} sm={4}>
        <InlineNotification
          kind="error"
          title={intl.formatMessage({
            id: "notebook.auditTrail.error.title",
            defaultMessage: "Error Loading Audit Logs",
          })}
          subtitle={error}
          lowContrast
        />
      </Column>
    );
  }

  if (auditLogs.length === 0) {
    return (
      <Column lg={16} md={8} sm={4}>
        <InlineNotification
          kind="info"
          title={intl.formatMessage({
            id: "notebook.auditTrail.empty.title",
            defaultMessage: "No Audit Logs",
          })}
          subtitle={intl.formatMessage({
            id: "notebook.auditTrail.empty.message",
            defaultMessage:
              "No audit trail found for this entry. Changes will be recorded here once you start editing.",
          })}
          lowContrast
        />
      </Column>
    );
  }

  return (
    <Column lg={16} md={8} sm={4}>
      <Grid fullWidth={true} className="gridBoundary">
        <Column lg={16} md={8} sm={4}>
          <h5>
            <FormattedMessage id="notebook.auditTrail.title" />
          </h5>
        </Column>
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
                title=""
                description=""
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
                                  id="notebook.auditTrail.details.changes"
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
        </Column>
      </Grid>
    </Column>
  );
};

export default NotebookAuditLogViewer;
