import React, { useState, useEffect } from "react";
import {
  Modal,
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
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { InventoryAuditLogAPI } from "./InventoryService";
import "./AuditLogViewer.css";

const AuditLogViewer = ({
  open,
  onClose,
  entityType,
  entityId,
  entityName,
}) => {
  const intl = useIntl();
  const [auditLogs, setAuditLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (open && entityType && entityId) {
      fetchAuditLogs();
    }
  }, [open, entityType, entityId]);

  const fetchAuditLogs = async () => {
    setLoading(true);
    setError(null);

    try {
      let logs;
      if (entityType === "ITEM") {
        logs = await InventoryAuditLogAPI.getItemAuditTrail(entityId);
      } else if (entityType === "LOT") {
        logs = await InventoryAuditLogAPI.getLotAuditTrail(entityId);
      } else if (entityType === "LOCATION") {
        logs = await InventoryAuditLogAPI.getLocationAuditTrail(entityId);
      } else {
        console.error("Unknown entity type:", entityType);
        setError("Unsupported entity type: " + entityType);
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
      return date.toLocaleString();
    } catch (e) {
      return timestamp;
    }
  };

  const renderChanges = (log) => {
    const changes = log.changes || {};

    if (Object.keys(changes).length === 0) {
      return <p>No field changes recorded</p>;
    }

    return (
      <div className="field-changes">
        <table className="changes-table">
          <thead>
            <tr>
              <th>Field</th>
              <th>Old Value</th>
              <th>New Value</th>
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
        id: "audit.timestamp",
        defaultMessage: "Timestamp",
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

  const rows = auditLogs.map((log, index) => ({
    id: `${log.id}-${index}`,
    timestamp: formatTimestamp(log.timestamp),
    activity: log.activity,
    performedByUser: log.performedByUser,
    summary: log.summary || "—",
    _log: log, // Store full log for expanded row
  }));

  return (
    <Modal
      open={open}
      onRequestClose={onClose}
      modalHeading={
        <FormattedMessage
          id="audit.log.title"
          defaultMessage="Audit Trail: {name}"
          values={{ name: entityName || `${entityType} ${entityId}` }}
        />
      }
      passiveModal
      size="lg"
      className="audit-log-viewer"
    >
      {loading ? (
        <div className="loading-container">
          <SkeletonText paragraph lineCount={5} />
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
              "No audit trail found for this entity. Audit logging may not be enabled yet.",
          })}
          hideCloseButton
        />
      ) : (
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
                              <TableCell key={cell.id}>{cell.value}</TableCell>
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
      )}
    </Modal>
  );
};

export default AuditLogViewer;
