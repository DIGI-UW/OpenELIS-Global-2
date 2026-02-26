/**
 * ErrorDetailsModal Component
 *
 * Displays detailed error information and analyzer logs
 * Specification: FR-016
 */

import React, { useState } from "react";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  Tag,
  Grid,
  Column,
  Accordion,
  AccordionItem,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { useHistory } from "react-router-dom";
import "./ErrorDetailsModal.css";

const ErrorDetailsModal = ({ error, open, onClose, onAcknowledge }) => {
  const intl = useIntl();
  const history = useHistory();
  const [logsExpanded, setLogsExpanded] = useState(false);

  if (!error) return null;

  const isAcknowledged =
    error.status === "ACKNOWLEDGED" || error.status === "acknowledged";
  const severity = error.severity || "ERROR";
  const errorType = error.errorType || "MAPPING";

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

  // Get severity color
  const severityColor =
    severity === "CRITICAL" || severity === "critical"
      ? "red"
      : severity === "ERROR" || severity === "error"
        ? "magenta"
        : "blue";

  // Get error type label
  const errorTypeKey = `analyzer.errorDashboard.errorType.${errorType.toLowerCase()}`;
  const errorTypeLabel = intl.formatMessage({
    id: errorTypeKey,
    defaultMessage: errorType,
  });

  // Get severity label
  const severityKey = `analyzer.errorDashboard.severity.${severity.toLowerCase()}`;
  const severityLabel = intl.formatMessage({
    id: severityKey,
    defaultMessage: severity,
  });

  // Analyzer logs from error object (empty array fallback)
  const analyzerLogs = error.analyzerLogs || [];

  const handleClose = () => {
    // Remove focus from any button before closing to prevent aria-hidden warning
    if (document.activeElement && document.activeElement.blur) {
      document.activeElement.blur();
    }
    onClose && onClose();
  };

  return (
    <ComposedModal
      open={open}
      onClose={handleClose}
      data-testid="error-details-modal"
      preventCloseOnClickOutside={false}
    >
      <ModalHeader
        label={intl.formatMessage(
          { id: "analyzer.errorDetails.subtitle" },
          { id: error?.id ?? "N/A" },
        )}
        title={intl.formatMessage({ id: "analyzer.errorDetails.title" })}
      />
      <ModalBody className="error-details-modal">
        <Grid>
          {/* Error Information */}
          <Column lg={16}>
            <h3>
              {intl.formatMessage({ id: "analyzer.errorDetails.errorId" })}
            </h3>
            <p>{error?.id ?? "N/A"}</p>
          </Column>
          <Column lg={8}>
            <h3>
              {intl.formatMessage({ id: "analyzer.errorDetails.timestamp" })}
            </h3>
            <p>{formatTimestamp(error.timestamp || error.createdDate)}</p>
          </Column>
          <Column lg={8}>
            <h3>
              {intl.formatMessage({ id: "analyzer.errorDetails.analyzer" })}
            </h3>
            <p>{error.analyzerName || error.analyzer?.name || "-"}</p>
          </Column>
          <Column lg={8}>
            <h3>
              {intl.formatMessage({ id: "analyzer.errorDetails.errorType" })}
            </h3>
            <Tag type="blue">{errorTypeLabel}</Tag>
          </Column>
          <Column lg={8}>
            <h3>
              {intl.formatMessage({ id: "analyzer.errorDetails.severity" })}
            </h3>
            <Tag type={severityColor}>{severityLabel}</Tag>
          </Column>
          <Column lg={16}>
            <h3>
              {intl.formatMessage({ id: "analyzer.errorDetails.errorMessage" })}
            </h3>
            <p>{error.errorMessage || error.message || "-"}</p>
          </Column>

          {/* Acknowledgment Status */}
          {isAcknowledged && (
            <Column lg={16}>
              <h3>
                {intl.formatMessage({
                  id: "analyzer.errorDetails.acknowledged",
                })}
              </h3>
              <p>
                {intl.formatMessage(
                  { id: "analyzer.errorDetails.acknowledgedBy" },
                  {
                    user: error.acknowledgedBy || "-",
                    date: formatTimestamp(error.acknowledgedDate),
                  },
                )}
              </p>
            </Column>
          )}

          {/* Analyzer Logs */}
          <Column lg={16}>
            <Accordion data-testid="analyzer-logs-accordion">
              <AccordionItem
                title={intl.formatMessage(
                  { id: "analyzer.errorDetails.logs" },
                  { count: analyzerLogs.length },
                )}
                open={logsExpanded}
                onHeadingClick={() => setLogsExpanded(!logsExpanded)}
              >
                {analyzerLogs.length > 0 ? (
                  <div className="analyzer-logs">
                    {analyzerLogs.map((log, index) => (
                      <div key={index} className="log-entry">
                        <span className="log-timestamp">
                          [{log.timestamp || "-"}]
                        </span>
                        <span className="log-level">{log.level || "INFO"}</span>
                        <span className="log-message">
                          {log.message || "-"}
                        </span>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p>
                    <FormattedMessage
                      id="analyzer.errorDetails.noLogsAvailable"
                      defaultMessage="No logs available"
                    />
                  </p>
                )}
              </AccordionItem>
            </Accordion>
          </Column>

          {/* Recommended Actions */}
          <Column lg={16}>
            <h3>
              {intl.formatMessage({
                id: "analyzer.errorDetails.recommendedActions",
              })}
            </h3>
            <ul>
              {errorType === "MAPPING" && (
                <li>
                  {intl.formatMessage({
                    id: "analyzer.errorDetails.recommendedActions.createMapping",
                  })}
                </li>
              )}
              <li>
                {intl.formatMessage({
                  id: "analyzer.errorDetails.recommendedActions.verifyConnection",
                })}
              </li>
              <li>
                {intl.formatMessage({
                  id: "analyzer.errorDetails.recommendedActions.checkConfiguration",
                })}
              </li>
              <li>
                {intl.formatMessage({
                  id: "analyzer.errorDetails.recommendedActions.testConnection",
                })}
              </li>
            </ul>
          </Column>
        </Grid>
      </ModalBody>
      <ModalFooter>
        <Button
          kind="secondary"
          onClick={handleClose}
          data-testid="error-details-close"
        >
          <FormattedMessage id="analyzer.errorDetails.close" />
        </Button>
        {errorType === "MAPPING" && error.analyzerId && (
          <Button
            kind="tertiary"
            onClick={() => {
              // Navigate to FieldMapping page with analyzer ID
              // Pre-select the unmapped field if available from error context
              const analyzerId = error.analyzerId || error.analyzer?.id;
              if (analyzerId) {
                handleClose();
                history.push(`/analyzers/${analyzerId}/mappings`);
              }
            }}
            data-testid="error-details-create-mapping"
          >
            <FormattedMessage id="analyzer.errorDetails.createMapping" />
          </Button>
        )}
        {!isAcknowledged && (
          <Button
            kind="primary"
            onClick={() => {
              onAcknowledge(error.id);
              handleClose();
            }}
            data-testid="error-details-acknowledge"
          >
            <FormattedMessage id="analyzer.errorDashboard.action.acknowledge" />
          </Button>
        )}
      </ModalFooter>
    </ComposedModal>
  );
};

export default ErrorDetailsModal;
