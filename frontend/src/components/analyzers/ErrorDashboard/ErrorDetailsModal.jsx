/**
 * ErrorDetailsModal Component
 *
 * Displays detailed error information and analyzer logs
 * Task Reference: T097
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
import "./ErrorDetailsModal.css";

const ErrorDetailsModal = ({ error, open, onClose, onAcknowledge }) => {
  const intl = useIntl();
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
  const errorTypeLabel = intl.formatMessage({ id: errorTypeKey }, errorType);

  // Get severity label
  const severityKey = `analyzer.errorDashboard.severity.${severity.toLowerCase()}`;
  const severityLabel = intl.formatMessage({ id: severityKey }, severity);

  // Analyzer logs (placeholder - will be populated from API)
  const analyzerLogs = error.analyzerLogs || [];

  return (
    <ComposedModal
      open={open}
      onClose={onClose}
      data-testid="error-details-modal"
    >
      <ModalHeader
        label={intl.formatMessage(
          { id: "analyzer.errorDetails.subtitle" },
          { id: error.id },
        )}
        title={intl.formatMessage({ id: "analyzer.errorDetails.title" })}
      />
      <ModalBody>
        <Grid>
          {/* Error Information */}
          <Column lg={16}>
            <h3>
              {intl.formatMessage({ id: "analyzer.errorDetails.errorId" })}
            </h3>
            <p>{error.id}</p>
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
                {intl.formatMessage({ id: "analyzer.errorDetails.acknowledged" })}
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
            <Accordion>
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
                        <span className="log-message">{log.message || "-"}</span>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p>No logs available</p>
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
              <li>
                Verify analyzer is powered on and network cable is connected
              </li>
              <li>
                Check IP address and port configuration in analyzer settings
              </li>
              <li>Test connection using the 'Test Connection' feature</li>
            </ul>
          </Column>
        </Grid>
      </ModalBody>
      <ModalFooter>
        <Button kind="secondary" onClick={onClose} data-testid="error-details-close">
          <FormattedMessage id="analyzer.errorDetails.close" />
        </Button>
        {!isAcknowledged && (
          <Button
            kind="primary"
            onClick={() => {
              onAcknowledge(error.id);
              onClose();
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

