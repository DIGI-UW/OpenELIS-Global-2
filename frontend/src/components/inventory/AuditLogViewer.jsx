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
      } else {
        logs = await InventoryAuditLogAPI.getEntityAuditTrail(
          entityType,
          entityId,
        );
      }
      setAuditLogs(logs || []);
    } catch (err) {
      console.error("Error fetching audit logs:", err);
      setError(err.message || "Failed to load audit logs");
    } finally {
      setLoading(false);
    }
  };

  const getOperationTypeTag = (operationType) => {
    const tagMap = {
      ITEM_CREATE: { type: "green", label: "Created" },
      ITEM_UPDATE: { type: "blue", label: "Updated" },
      ITEM_DEACTIVATE: { type: "red", label: "Deactivated" },
      ITEM_REACTIVATE: { type: "green", label: "Reactivated" },
      LOT_RECEIVE: { type: "green", label: "Received" },
      LOT_OPEN: { type: "blue", label: "Opened" },
      LOT_QC_UPDATE: { type: "purple", label: "QC Updated" },
      LOT_STATUS_UPDATE: { type: "blue", label: "Status Updated" },
      LOT_ADJUST: { type: "cyan", label: "Adjusted" },
      LOT_DISPOSE: { type: "red", label: "Disposed" },
      LOT_UPDATE: { type: "blue", label: "Updated" },
      USAGE_RECORD: { type: "teal", label: "Used" },
      LOCATION_CREATE: { type: "green", label: "Created" },
      LOCATION_UPDATE: { type: "blue", label: "Updated" },
      LOCATION_DEACTIVATE: { type: "red", label: "Deactivated" },
    };

    const config = tagMap[operationType] || {
      type: "gray",
      label: operationType,
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

  const parseOperationDetails = (detailsJson) => {
    if (!detailsJson) return null;
    try {
      return JSON.parse(detailsJson);
    } catch (e) {
      return { raw: detailsJson };
    }
  };

  const formatValue = (key, value) => {
    // Check if value looks like a timestamp (large number)
    if (
      typeof value === "number" &&
      value > 1000000000 &&
      value < 9999999999999
    ) {
      return new Date(value).toLocaleString();
    }
    // Check for common date field names
    if (
      typeof value === "number" &&
      (key.toLowerCase().includes("date") ||
        key.toLowerCase().includes("time") ||
        key === "lastupdated")
    ) {
      return new Date(value).toLocaleString();
    }
    if (typeof value === "object" && value !== null) {
      return JSON.stringify(value);
    }
    return String(value);
  };

  const renderOperationDetails = (log) => {
    const details = parseOperationDetails(log.operationDetails);
    if (!details) return <p>No additional details</p>;

    return (
      <div className="operation-details">
        {Object.entries(details).map(([key, value]) => (
          <div key={key} className="detail-row">
            <strong>{key.replace(/_/g, " ").toUpperCase()}:</strong>{" "}
            {formatValue(key, value)}
          </div>
        ))}
      </div>
    );
  };

  const formatStateForDisplay = (state) => {
    if (!state || typeof state !== "object") return state;

    const formattedState = {};
    for (const [key, value] of Object.entries(state)) {
      // Format timestamp values
      if (
        typeof value === "number" &&
        (key.toLowerCase().includes("date") ||
          key.toLowerCase().includes("time") ||
          key === "lastupdated")
      ) {
        formattedState[key] = `${value} (${new Date(value).toLocaleString()})`;
      } else {
        formattedState[key] = value;
      }
    }
    return formattedState;
  };

  const renderStateComparison = (log) => {
    let beforeState, afterState;
    try {
      beforeState = log.beforeState ? JSON.parse(log.beforeState) : null;
      afterState = log.afterState ? JSON.parse(log.afterState) : null;
    } catch (e) {
      return <p>Unable to parse state data</p>;
    }

    if (!beforeState && !afterState) {
      return <p>No state change recorded</p>;
    }

    return (
      <div className="state-comparison">
        {beforeState && (
          <div className="state-column">
            <h5>Before:</h5>
            <pre>
              {JSON.stringify(formatStateForDisplay(beforeState), null, 2)}
            </pre>
          </div>
        )}
        {afterState && (
          <div className="state-column">
            <h5>After:</h5>
            <pre>
              {JSON.stringify(formatStateForDisplay(afterState), null, 2)}
            </pre>
          </div>
        )}
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
      key: "operationType",
      header: intl.formatMessage({
        id: "audit.operation",
        defaultMessage: "Operation",
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
      key: "details",
      header: intl.formatMessage({
        id: "audit.summary",
        defaultMessage: "Summary",
      }),
    },
  ];

  const rows = auditLogs.map((log, index) => ({
    id: `${log.id}-${index}`,
    timestamp: formatTimestamp(log.timestamp),
    operationType: log.operationType,
    performedByUser: log.performedByUser,
    details: log.operationDetails || "—",
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
                            if (cell.info.header === "operationType") {
                              return (
                                <TableCell key={cell.id}>
                                  {getOperationTypeTag(cell.value)}
                                </TableCell>
                              );
                            }
                            if (cell.info.header === "details") {
                              const details = parseOperationDetails(cell.value);
                              if (details && details.action) {
                                return (
                                  <TableCell key={cell.id}>
                                    {details.action}
                                  </TableCell>
                                );
                              }
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
                                id="audit.details.operation"
                                defaultMessage="Operation Details"
                              />
                            </h5>
                            {renderOperationDetails(log)}

                            <h5>
                              <FormattedMessage
                                id="audit.details.changes"
                                defaultMessage="State Changes"
                              />
                            </h5>
                            {renderStateComparison(log)}
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
