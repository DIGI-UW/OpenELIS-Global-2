import React, { useEffect, useState, useContext, useCallback } from "react";
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
  TextArea,
  Dropdown,
} from "@carbon/react";
import { FormattedMessage, injectIntl } from "react-intl";
import PropTypes from "prop-types";
import "./AlertDetailModal.scss";
import {
  fetchAlertDetails,
  acknowledgeAlert,
  resolveAlert,
  deleteAlert,
  createCorrectiveAction,
} from "./api";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { hasRole, Roles } from "../utils/Utils";
import { formatDateTime as formatIsoDateTime } from "./shared/dateUtils";

const CORRECTIVE_ACTION_TYPES = [
  { id: "TEMPERATURE_ADJUSTMENT", label: "Temperature Adjustment" },
  { id: "EQUIPMENT_REPAIR", label: "Equipment Repair" },
  { id: "SAMPLE_RELOCATION", label: "Sample Relocation" },
  { id: "CALIBRATION", label: "Calibration" },
  { id: "ITEM_REORDER", label: "Item Reorder" },
  { id: "MAINTENANCE", label: "Maintenance" },
  { id: "OTHER", label: "Other" },
];

const AlertDetailModal = ({ intl, alertId, open, onClose }) => {
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const currentUserId = userSessionDetails?.userId;
  const isAdminUser = hasRole(userSessionDetails, Roles.GLOBAL_ADMIN);
  const [alert, setAlert] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [actionInProgress, setActionInProgress] = useState(false);
  const [notes, setNotes] = useState("");
  const [showCorrectiveActionForm, setShowCorrectiveActionForm] =
    useState(false);
  const [correctiveActionType, setCorrectiveActionType] = useState(null);
  const [correctiveActionDescription, setCorrectiveActionDescription] =
    useState("");
  const [correctiveActionSubmitting, setCorrectiveActionSubmitting] =
    useState(false);

  const loadAlertDetails = useCallback(async () => {
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
  }, [alertId]);

  useEffect(() => {
    if (open && alertId) {
      loadAlertDetails();
    }
  }, [open, alertId, loadAlertDetails]);

  const formatDateTime = (dateTimeString) =>
    formatIsoDateTime(dateTimeString, "-");

  const handleAcknowledge = async () => {
    if (!currentUserId) {
      setError(
        intl.formatMessage({
          id: "freezer.alert.detail.noUser",
          defaultMessage:
            "Unable to identify current user. Please sign in again.",
        }),
      );
      return;
    }
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
    if (!currentUserId) {
      setError(
        intl.formatMessage({
          id: "freezer.alert.detail.noUser",
          defaultMessage:
            "Unable to identify current user. Please sign in again.",
        }),
      );
      return;
    }
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

  const handleDelete = async () => {
    setActionInProgress(true);
    setError(null);
    try {
      await deleteAlert(alertId);
      onClose();
    } catch (err) {
      const isForbidden = err?.status === 403;
      setError(
        isForbidden
          ? intl.formatMessage({
              id: "freezer.alert.detail.deleteForbidden",
              defaultMessage:
                "You do not have permission to delete this alert.",
            })
          : err.message ||
              intl.formatMessage({
                id: "freezer.alert.detail.deleteFailed",
                defaultMessage: "Failed to delete alert",
              }),
      );
      setActionInProgress(false);
    }
  };

  const handleLogCorrectiveAction = async () => {
    if (!alert?.freezer?.id && !alert?.alertEntityId) {
      return;
    }
    setCorrectiveActionSubmitting(true);
    setError(null);
    try {
      await createCorrectiveAction(
        alert.freezer?.id ?? alert.alertEntityId,
        correctiveActionType?.id,
        correctiveActionDescription,
      );
      setShowCorrectiveActionForm(false);
      setCorrectiveActionType(null);
      setCorrectiveActionDescription("");
      await loadAlertDetails();
    } catch (err) {
      setError(
        err.message ||
          intl.formatMessage({
            id: "freezer.alert.detail.correctiveActionFailed",
            defaultMessage: "Failed to log corrective action",
          }),
      );
    } finally {
      setCorrectiveActionSubmitting(false);
    }
  };

  const getSeverityTag = (severity) => {
    switch (severity) {
      case "CRITICAL":
        return (
          <Tag type="red">
            <FormattedMessage
              id="freezer.alert.severity.critical"
              defaultMessage="Critical"
            />
          </Tag>
        );
      case "WARNING":
        return (
          <Tag type="warm-gray">
            <FormattedMessage
              id="freezer.alert.severity.warning"
              defaultMessage="Warning"
            />
          </Tag>
        );
      default:
        return <Tag>{severity}</Tag>;
    }
  };

  const getStatusTag = (status) => {
    switch (status) {
      case "OPEN":
        return (
          <Tag type="red">
            <FormattedMessage
              id="freezer.alert.status.open"
              defaultMessage="Open"
            />
          </Tag>
        );
      case "ACKNOWLEDGED":
        return (
          <Tag type="blue">
            <FormattedMessage
              id="freezer.alert.status.acknowledged"
              defaultMessage="Acknowledged"
            />
          </Tag>
        );
      case "ESCALATED":
        return (
          <Tag type="magenta">
            <FormattedMessage
              id="freezer.alert.status.escalated"
              defaultMessage="Escalated"
            />
          </Tag>
        );
      case "RESOLVED":
        return (
          <Tag type="green">
            <FormattedMessage
              id="freezer.alert.status.resolved"
              defaultMessage="Resolved"
            />
          </Tag>
        );
      case "CLOSED":
        return (
          <Tag type="gray">
            <FormattedMessage
              id="freezer.alert.status.closed"
              defaultMessage="Closed"
            />
          </Tag>
        );
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
      primaryButtonDisabled={actionInProgress || loading || !currentUserId}
    >
      {loading && <Loading />}

      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({
            id: "error.title",
            defaultMessage: "Error",
          })}
          subtitle={error}
          onCloseButtonClick={() => setError(null)}
        />
      )}

      {alert && !loading && isAdminUser && (
        <div className="oe-coldStorage-alertModalActions">
          <Button
            kind="danger--ghost"
            size="sm"
            disabled={actionInProgress}
            onClick={handleDelete}
          >
            <FormattedMessage
              id="freezer.alert.detail.delete"
              defaultMessage="Delete Alert"
            />
          </Button>
        </div>
      )}

      {alert && !loading && (
        <div className="oe-coldStorage-alertModalBody">
          <Section className="oe-coldStorage-alertModalSection">
            <h5 className="oe-coldStorage-alertModalSectionTitle">
              <FormattedMessage
                id="freezer.alert.detail.overview"
                defaultMessage="Alert Overview"
              />
            </h5>
            <div className="oe-coldStorage-alertModalGrid">
              <div>
                <p className="oe-coldStorage-alertModalFieldLabel">
                  <FormattedMessage
                    id="freezer.alert.detail.id"
                    defaultMessage="Alert ID"
                  />
                </p>
                <p>{alert.id}</p>
              </div>

              <div>
                <p className="oe-coldStorage-alertModalFieldLabel">
                  <FormattedMessage
                    id="freezer.alert.detail.freezer"
                    defaultMessage="Freezer"
                  />
                </p>
                <p>{alert.freezer?.name || alert.freezer?.code || "Unknown"}</p>
              </div>

              <div>
                <p className="oe-coldStorage-alertModalFieldLabel">
                  <FormattedMessage
                    id="freezer.alert.detail.severity"
                    defaultMessage="Severity"
                  />
                </p>
                {getSeverityTag(alert.severity)}
              </div>

              <div>
                <p className="oe-coldStorage-alertModalFieldLabel">
                  <FormattedMessage
                    id="freezer.alert.detail.status"
                    defaultMessage="Status"
                  />
                </p>
                {getStatusTag(alert.status)}
              </div>

              <div>
                <p className="oe-coldStorage-alertModalFieldLabel">
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
                    <p className="oe-coldStorage-alertModalFieldLabel">
                      <FormattedMessage
                        id="freezer.alert.detail.acknowledgedAt"
                        defaultMessage="Acknowledged At"
                      />
                    </p>
                    <p>{formatDateTime(alert.acknowledgedAt)}</p>
                  </div>

                  <div>
                    <p className="oe-coldStorage-alertModalFieldLabel">
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
                    <p className="oe-coldStorage-alertModalFieldLabel">
                      <FormattedMessage
                        id="freezer.alert.detail.resolvedAt"
                        defaultMessage="Resolved At"
                      />
                    </p>
                    <p>{formatDateTime(alert.resolvedAt)}</p>
                  </div>

                  <div>
                    <p className="oe-coldStorage-alertModalFieldLabel">
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
              <div className="oe-coldStorage-alertModalField">
                <p className="oe-coldStorage-alertModalFieldLabel">
                  <FormattedMessage
                    id="freezer.alert.detail.message"
                    defaultMessage="Message"
                  />
                </p>
                <p>{alert.message}</p>
              </div>
            )}

            {(alert.status === "OPEN" || alert.status === "ACKNOWLEDGED") && (
              <div className="oe-coldStorage-alertModalField">
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
              <div className="oe-coldStorage-alertModalField">
                <p className="oe-coldStorage-alertModalFieldLabel">
                  <FormattedMessage
                    id="freezer.alert.detail.resolutionNotes"
                    defaultMessage="Resolution Notes"
                  />
                </p>
                <p>{alert.resolutionNotes}</p>
              </div>
            )}

            {alert.correctiveAction && (
              <div className="oe-coldStorage-alertModalField">
                <p className="oe-coldStorage-alertModalFieldLabel">
                  <FormattedMessage
                    id="freezer.alert.detail.correctiveAction"
                    defaultMessage="Corrective Action"
                  />
                </p>
                <p>{alert.correctiveAction}</p>
              </div>
            )}

            <div className="oe-coldStorage-inlineCorrectiveAction">
              {!showCorrectiveActionForm ? (
                <Button
                  kind="tertiary"
                  size="sm"
                  onClick={() => setShowCorrectiveActionForm(true)}
                >
                  <FormattedMessage
                    id="freezer.alert.detail.logCorrectiveAction"
                    defaultMessage="Log corrective action"
                  />
                </Button>
              ) : (
                <div className="oe-coldStorage-inlineCorrectiveActionForm">
                  <Dropdown
                    id="inline-corrective-action-type"
                    titleText={intl.formatMessage({
                      id: "freezer.alert.detail.correctiveActionType",
                      defaultMessage: "Action Type",
                    })}
                    label={
                      correctiveActionType
                        ? correctiveActionType.label
                        : intl.formatMessage({
                            id: "coldStorage.selectType",
                            defaultMessage: "Select type",
                          })
                    }
                    items={CORRECTIVE_ACTION_TYPES}
                    itemToString={(item) => (item ? item.label : "")}
                    selectedItem={correctiveActionType}
                    onChange={({ selectedItem }) =>
                      setCorrectiveActionType(selectedItem)
                    }
                  />
                  <TextArea
                    id="inline-corrective-action-description"
                    labelText={intl.formatMessage({
                      id: "freezer.alert.detail.correctiveActionDescription",
                      defaultMessage: "Description",
                    })}
                    rows={3}
                    value={correctiveActionDescription}
                    onChange={(e) =>
                      setCorrectiveActionDescription(e.target.value)
                    }
                  />
                  <div className="oe-coldStorage-inlineCorrectiveActionButtons">
                    <Button
                      kind="secondary"
                      size="sm"
                      onClick={() => {
                        setShowCorrectiveActionForm(false);
                        setCorrectiveActionType(null);
                        setCorrectiveActionDescription("");
                      }}
                      disabled={correctiveActionSubmitting}
                    >
                      <FormattedMessage
                        id="label.button.cancel"
                        defaultMessage="Cancel"
                      />
                    </Button>
                    <Button
                      kind="primary"
                      size="sm"
                      onClick={handleLogCorrectiveAction}
                      disabled={
                        correctiveActionSubmitting ||
                        !correctiveActionType ||
                        !correctiveActionDescription.trim()
                      }
                    >
                      <FormattedMessage
                        id="freezer.alert.detail.logCorrectiveAction"
                        defaultMessage="Log corrective action"
                      />
                    </Button>
                  </div>
                </div>
              )}
            </div>
          </Section>

          {alert.notifications && alert.notifications.length > 0 && (
            <Section className="oe-coldStorage-alertModalSection">
              <h5 className="oe-coldStorage-alertModalSectionTitle">
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
                  {
                    key: "recipient",
                    header: intl.formatMessage({
                      id: "freezer.alert.detail.recipient",
                      defaultMessage: "Recipient",
                    }),
                  },
                  {
                    key: "method",
                    header: intl.formatMessage({
                      id: "freezer.alert.detail.method",
                      defaultMessage: "Method",
                    }),
                  },
                  {
                    key: "sentAt",
                    header: intl.formatMessage({
                      id: "freezer.alert.detail.sentAt",
                      defaultMessage: "Sent At",
                    }),
                  },
                  {
                    key: "status",
                    header: intl.formatMessage({
                      id: "coldStorage.status",
                      defaultMessage: "Status",
                    }),
                  },
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
              <h5 className="oe-coldStorage-alertModalSectionTitle">
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
                  {
                    key: "summary",
                    header: intl.formatMessage({
                      id: "freezer.alert.detail.summary",
                      defaultMessage: "Summary",
                    }),
                  },
                  {
                    key: "takenBy",
                    header: intl.formatMessage({
                      id: "freezer.alert.detail.takenBy",
                      defaultMessage: "Taken By",
                    }),
                  },
                  {
                    key: "takenAt",
                    header: intl.formatMessage({
                      id: "freezer.alert.detail.takenAt",
                      defaultMessage: "Taken At",
                    }),
                  },
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
};

export default injectIntl(AlertDetailModal);
