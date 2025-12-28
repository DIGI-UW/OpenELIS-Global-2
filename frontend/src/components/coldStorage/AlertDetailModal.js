import React, { useEffect, useState } from "react";
import {
  Modal,
  Loading,
  InlineNotification,
  Tag,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Section,
  Button,
  TextArea,
} from "@carbon/react";
import { FormattedMessage, injectIntl } from "react-intl";
import PropTypes from "prop-types";
import { fetchAlertDetails, acknowledgeAlert, resolveAlert } from "./api";

const AlertDetailModal = ({
  intl,
  alertId,
  open,
  onClose,
  currentUserId = 1,
}) => {
  const [alert, setAlert] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [actionInProgress, setActionInProgress] = useState(false);
  const [notes, setNotes] = useState("");

  useEffect(() => {
    if (open && alertId) {
      loadAlertDetails();
    }
  }, [open, alertId]);

  const loadAlertDetails = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchAlertDetails(alertId);
      setAlert(data);
    } catch (err) {
      setError(err.message || "Failed to load alert details");
    } finally {
      setLoading(false);
    }
  };

  const formatDateTime = (dateTimeString) => {
    if (!dateTimeString) return "-";
    try {
      // Handle epoch timestamps (in seconds or milliseconds)
      if (typeof dateTimeString === "number") {
        // If timestamp is less than year 2100 in milliseconds, assume it's in seconds
        const timestamp =
          dateTimeString < 4102444800000
            ? dateTimeString * 1000
            : dateTimeString;
        return new Date(timestamp).toLocaleString();
      }
      // Handle ISO 8601 strings (e.g., "2024-01-01T00:00:00Z")
      return new Date(dateTimeString).toLocaleString();
    } catch (error) {
      console.error("Error formatting date:", dateTimeString, error);
      return dateTimeString;
    }
  };

  const handleAcknowledge = async () => {
    setActionInProgress(true);
    setError(null);
    try {
      await acknowledgeAlert(alertId, currentUserId, notes);
      setNotes("");
      onClose(); // Close modal immediately after successful action
    } catch (err) {
      setError(err.message || "Failed to acknowledge alert");
      setActionInProgress(false);
    }
  };

  const handleResolve = async () => {
    setActionInProgress(true);
    setError(null);
    try {
      await resolveAlert(alertId, currentUserId, notes || "Resolved");
      setNotes("");
      onClose(); // Close modal immediately after successful action
    } catch (err) {
      setError(err.message || "Failed to resolve alert");
      setActionInProgress(false);
    }
  };

  const getSeverityTag = (severity) => {
    switch (severity) {
      case "CRITICAL":
        return <Tag type="red">Critical</Tag>;
      case "WARNING":
        return <Tag type="warm-gray">Warning</Tag>;
      default:
        return <Tag>{severity}</Tag>;
    }
  };

  const getStatusTag = (status) => {
    switch (status) {
      case "OPEN":
        return <Tag type="red">Open</Tag>;
      case "ACKNOWLEDGED":
        return <Tag type="blue">Acknowledged</Tag>;
      case "ESCALATED":
        return <Tag type="magenta">Escalated</Tag>;
      case "RESOLVED":
        return <Tag type="green">Resolved</Tag>;
      case "CLOSED":
        return <Tag type="gray">Closed</Tag>;
      default:
        return <Tag>{status}</Tag>;
    }
  };

  return (
    <Modal
      open={open}
      onRequestClose={onClose}
      modalHeading={
        <FormattedMessage
          id="freezer.alert.detail.title"
          defaultMessage="Alert Details"
        />
      }
      size="lg"
      primaryButtonText={
        alert && alert.status === "OPEN" ? (
          <FormattedMessage
            id="freezer.alert.detail.acknowledge"
            defaultMessage="Acknowledge"
          />
        ) : alert && alert.status === "ACKNOWLEDGED" ? (
          <FormattedMessage
            id="freezer.alert.detail.resolve"
            defaultMessage="Resolve"
          />
        ) : undefined
      }
      secondaryButtonText={
        <FormattedMessage
          id="freezer.alert.detail.close"
          defaultMessage="Close"
        />
      }
      onRequestSubmit={
        alert && alert.status === "OPEN"
          ? handleAcknowledge
          : alert && alert.status === "ACKNOWLEDGED"
            ? handleResolve
            : undefined
      }
      onSecondarySubmit={onClose}
      primaryButtonDisabled={actionInProgress || loading}
    >
      {loading && <Loading />}

      {error && (
        <InlineNotification
          kind="error"
          title="Error"
          subtitle={error}
          onCloseButtonClick={() => setError(null)}
        />
      )}

      {alert && !loading && (
        <div style={{ padding: "1rem 0" }}>
          <Section style={{ marginBottom: "1.5rem" }}>
            <h5 style={{ marginBottom: "1rem" }}>
              <FormattedMessage
                id="freezer.alert.detail.overview"
                defaultMessage="Alert Overview"
              />
            </h5>
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "1fr 1fr",
                gap: "1rem",
              }}
            >
              <div>
                <p
                  style={{
                    fontWeight: 600,
                    marginBottom: "0.25rem",
                    fontSize: "0.875rem",
                  }}
                >
                  <FormattedMessage
                    id="freezer.alert.detail.id"
                    defaultMessage="Alert ID"
                  />
                </p>
                <p>{alert.id}</p>
              </div>

              <div>
                <p
                  style={{
                    fontWeight: 600,
                    marginBottom: "0.25rem",
                    fontSize: "0.875rem",
                  }}
                >
                  <FormattedMessage
                    id="freezer.alert.detail.freezer"
                    defaultMessage="Freezer"
                  />
                </p>
                <p>{alert.freezer?.name || alert.freezer?.code || "Unknown"}</p>
              </div>

              <div>
                <p
                  style={{
                    fontWeight: 600,
                    marginBottom: "0.25rem",
                    fontSize: "0.875rem",
                  }}
                >
                  <FormattedMessage
                    id="freezer.alert.detail.severity"
                    defaultMessage="Severity"
                  />
                </p>
                {getSeverityTag(alert.severity)}
              </div>

              <div>
                <p
                  style={{
                    fontWeight: 600,
                    marginBottom: "0.25rem",
                    fontSize: "0.875rem",
                  }}
                >
                  <FormattedMessage
                    id="freezer.alert.detail.status"
                    defaultMessage="Status"
                  />
                </p>
                {getStatusTag(alert.status)}
              </div>

              <div>
                <p
                  style={{
                    fontWeight: 600,
                    marginBottom: "0.25rem",
                    fontSize: "0.875rem",
                  }}
                >
                  <FormattedMessage
                    id="freezer.alert.detail.startTime"
                    defaultMessage="Start Time"
                  />
                </p>
                <p>{formatDateTime(alert.startTime)}</p>
              </div>

              {alert.acknowledgedAt && (
                <>
                  <div>
                    <p
                      style={{
                        fontWeight: 600,
                        marginBottom: "0.25rem",
                        fontSize: "0.875rem",
                      }}
                    >
                      <FormattedMessage
                        id="freezer.alert.detail.acknowledgedAt"
                        defaultMessage="Acknowledged At"
                      />
                    </p>
                    <p>{formatDateTime(alert.acknowledgedAt)}</p>
                  </div>

                  <div>
                    <p
                      style={{
                        fontWeight: 600,
                        marginBottom: "0.25rem",
                        fontSize: "0.875rem",
                      }}
                    >
                      <FormattedMessage
                        id="freezer.alert.detail.acknowledgedBy"
                        defaultMessage="Acknowledged By"
                      />
                    </p>
                    <p>{alert.acknowledgedBy || "-"}</p>
                  </div>
                </>
              )}

              {alert.resolvedAt && (
                <>
                  <div>
                    <p
                      style={{
                        fontWeight: 600,
                        marginBottom: "0.25rem",
                        fontSize: "0.875rem",
                      }}
                    >
                      <FormattedMessage
                        id="freezer.alert.detail.resolvedAt"
                        defaultMessage="Resolved At"
                      />
                    </p>
                    <p>{formatDateTime(alert.resolvedAt)}</p>
                  </div>

                  <div>
                    <p
                      style={{
                        fontWeight: 600,
                        marginBottom: "0.25rem",
                        fontSize: "0.875rem",
                      }}
                    >
                      <FormattedMessage
                        id="freezer.alert.detail.resolvedBy"
                        defaultMessage="Resolved By"
                      />
                    </p>
                    <p>{alert.resolvedBy || "-"}</p>
                  </div>
                </>
              )}
            </div>

            {alert.message && (
              <div style={{ marginTop: "1rem" }}>
                <p
                  style={{
                    fontWeight: 600,
                    marginBottom: "0.25rem",
                    fontSize: "0.875rem",
                  }}
                >
                  <FormattedMessage
                    id="freezer.alert.detail.message"
                    defaultMessage="Message"
                  />
                </p>
                <p>{alert.message}</p>
              </div>
            )}

            {(alert.status === "OPEN" || alert.status === "ACKNOWLEDGED") && (
              <div style={{ marginTop: "1rem" }}>
                <TextArea
                  id="alert-notes"
                  labelText={
                    <FormattedMessage
                      id="freezer.alert.detail.notes"
                      defaultMessage="Notes"
                    />
                  }
                  placeholder={intl.formatMessage({
                    id: "freezer.alert.detail.notesPlaceholder",
                    defaultMessage: "Add notes about this alert...",
                  })}
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  rows={3}
                />
              </div>
            )}

            {alert.resolutionNotes && (
              <div style={{ marginTop: "1rem" }}>
                <p
                  style={{
                    fontWeight: 600,
                    marginBottom: "0.25rem",
                    fontSize: "0.875rem",
                  }}
                >
                  <FormattedMessage
                    id="freezer.alert.detail.resolutionNotes"
                    defaultMessage="Resolution Notes"
                  />
                </p>
                <p>{alert.resolutionNotes}</p>
              </div>
            )}

            {alert.correctiveAction && (
              <div style={{ marginTop: "1rem" }}>
                <p
                  style={{
                    fontWeight: 600,
                    marginBottom: "0.25rem",
                    fontSize: "0.875rem",
                  }}
                >
                  <FormattedMessage
                    id="freezer.alert.detail.correctiveAction"
                    defaultMessage="Corrective Action"
                  />
                </p>
                <p>{alert.correctiveAction}</p>
              </div>
            )}
          </Section>

          {alert.notifications && alert.notifications.length > 0 && (
            <Section style={{ marginBottom: "1.5rem" }}>
              <h5 style={{ marginBottom: "1rem" }}>
                <FormattedMessage
                  id="freezer.alert.detail.notifications"
                  defaultMessage="Notifications Sent"
                />
              </h5>
              <DataTable
                rows={alert.notifications.map((notif, idx) => ({
                  id: idx.toString(),
                  ...notif,
                }))}
                headers={[
                  { key: "recipient", header: "Recipient" },
                  { key: "method", header: "Method" },
                  { key: "sentAt", header: "Sent At" },
                  { key: "status", header: "Status" },
                ]}
              >
                {({
                  rows,
                  headers,
                  getTableProps,
                  getHeaderProps,
                  getRowProps,
                }) => (
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
                            <TableCell key={cell.id}>
                              {cell.info.header === "sentAt"
                                ? formatDateTime(cell.value)
                                : cell.value || "-"}
                            </TableCell>
                          ))}
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                )}
              </DataTable>
            </Section>
          )}

          {alert.actions && alert.actions.length > 0 && (
            <Section>
              <h5 style={{ marginBottom: "1rem" }}>
                <FormattedMessage
                  id="freezer.alert.detail.actions"
                  defaultMessage="Actions Taken"
                />
              </h5>
              <DataTable
                rows={alert.actions.map((action, idx) => ({
                  id: idx.toString(),
                  ...action,
                }))}
                headers={[
                  { key: "summary", header: "Summary" },
                  { key: "takenBy", header: "Taken By" },
                  { key: "takenAt", header: "Taken At" },
                ]}
              >
                {({
                  rows,
                  headers,
                  getTableProps,
                  getHeaderProps,
                  getRowProps,
                }) => (
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
                            <TableCell key={cell.id}>
                              {cell.info.header === "takenAt"
                                ? formatDateTime(cell.value)
                                : cell.value || "-"}
                            </TableCell>
                          ))}
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                )}
              </DataTable>
            </Section>
          )}
        </div>
      )}
    </Modal>
  );
};

AlertDetailModal.propTypes = {
  intl: PropTypes.object.isRequired,
  alertId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  open: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  currentUserId: PropTypes.number,
};

export default injectIntl(AlertDetailModal);
