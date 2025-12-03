/**
 * ViolationDetailModal Component
 *
 * Modal showing detailed information about a QC rule violation
 * Task Reference: T111
 * Specification: FR-033 to FR-038
 *
 * Features:
 * - Display violation details (rule, severity, affected results)
 * - Show corrective action history
 * - Actions: Acknowledge (warnings), Create corrective action
 */

import React from "react";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  Tag,
  StructuredListWrapper,
  StructuredListHead,
  StructuredListRow,
  StructuredListCell,
  StructuredListBody,
} from "@carbon/react";
import { useIntl } from "react-intl";
import PropTypes from "prop-types";
import "./ViolationDetailModal.css";

const ViolationDetailModal = ({
  violation,
  open,
  onClose,
  onAcknowledge,
  onCreateCorrectiveAction,
}) => {
  const intl = useIntl();

  if (!violation) return null;

  const isUnresolved =
    violation.resolutionStatus === "UNRESOLVED" ||
    violation.status === "UNRESOLVED";
  const isWarning = violation.severity === "WARNING";

  // Get severity tag type
  const getSeverityTagType = () => {
    return violation.severity === "REJECTION" ? "red" : "yellow";
  };

  // Get status tag type
  const getStatusTagType = () => {
    switch (violation.resolutionStatus || violation.status) {
      case "RESOLVED":
        return "green";
      case "CORRECTIVE_ACTION_PENDING":
        return "blue";
      case "ACKNOWLEDGED":
        return "gray";
      default:
        return "red";
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
      second: "2-digit",
    });
  };

  // Get rule description based on code
  const getRuleDescription = (ruleCode) => {
    const descriptions = {
      "1_2s": intl.formatMessage({ id: "qc.rules.1_2s.description" }),
      "1_3s": intl.formatMessage({ id: "qc.rules.1_3s.description" }),
      "2_2s": intl.formatMessage({ id: "qc.rules.2_2s.description" }),
      R_4s: intl.formatMessage({ id: "qc.rules.R_4s.description" }),
      "4_1s": intl.formatMessage({ id: "qc.rules.4_1s.description" }),
      "10_x": intl.formatMessage({ id: "qc.rules.10_x.description" }),
      "3_1s": intl.formatMessage({ id: "qc.rules.3_1s.description" }),
      "7_t": intl.formatMessage({ id: "qc.rules.7_t.description" }),
    };
    return descriptions[ruleCode] || ruleCode;
  };

  return (
    <ComposedModal
      open={open}
      onClose={onClose}
      size="lg"
      data-testid="violation-detail-modal"
    >
      <ModalHeader
        title={intl.formatMessage({ id: "qc.violations.detail.title" })}
        label={violation.ruleCode}
        data-testid="violation-detail-modal-header"
      />
      <ModalBody data-testid="violation-detail-modal-body">
        {/* Status and Severity */}
        <div className="violation-detail-tags">
          <Tag
            type={getSeverityTagType()}
            data-testid="violation-detail-severity"
          >
            {intl.formatMessage({
              id: `qc.violations.severity.${violation.severity?.toLowerCase()}`,
            })}
          </Tag>
          <Tag
            type={getStatusTagType()}
            data-testid="violation-detail-status"
          >
            {intl.formatMessage({
              id: `qc.violations.status.${(violation.resolutionStatus || violation.status)?.toLowerCase()}`,
            })}
          </Tag>
        </div>

        {/* Violation Details */}
        <div className="violation-detail-section">
          <h4>{intl.formatMessage({ id: "qc.violations.detail.information" })}</h4>
          <StructuredListWrapper data-testid="violation-detail-info">
            <StructuredListBody>
              <StructuredListRow>
                <StructuredListCell>
                  {intl.formatMessage({ id: "qc.violations.detail.rule" })}
                </StructuredListCell>
                <StructuredListCell>
                  <strong>{violation.ruleCode}</strong> -{" "}
                  {getRuleDescription(violation.ruleCode)}
                </StructuredListCell>
              </StructuredListRow>
              <StructuredListRow>
                <StructuredListCell>
                  {intl.formatMessage({ id: "qc.violations.detail.analyzer" })}
                </StructuredListCell>
                <StructuredListCell>
                  {violation.analyzerName || violation.analyzer?.name || "-"}
                </StructuredListCell>
              </StructuredListRow>
              <StructuredListRow>
                <StructuredListCell>
                  {intl.formatMessage({ id: "qc.violations.detail.test" })}
                </StructuredListCell>
                <StructuredListCell>
                  {violation.testName || violation.test?.name || "-"}
                </StructuredListCell>
              </StructuredListRow>
              <StructuredListRow>
                <StructuredListCell>
                  {intl.formatMessage({ id: "qc.violations.detail.controlLot" })}
                </StructuredListCell>
                <StructuredListCell>
                  {violation.controlLotNumber || violation.controlLot?.lotNumber || "-"}
                </StructuredListCell>
              </StructuredListRow>
              <StructuredListRow>
                <StructuredListCell>
                  {intl.formatMessage({ id: "qc.violations.detail.timestamp" })}
                </StructuredListCell>
                <StructuredListCell>
                  {formatTimestamp(violation.violationTimestamp || violation.createdDate)}
                </StructuredListCell>
              </StructuredListRow>
            </StructuredListBody>
          </StructuredListWrapper>
        </div>

        {/* Affected Results */}
        {violation.affectedResults && violation.affectedResults.length > 0 && (
          <div className="violation-detail-section">
            <h4>{intl.formatMessage({ id: "qc.violations.detail.affectedResults" })}</h4>
            <StructuredListWrapper data-testid="violation-detail-results">
              <StructuredListHead>
                <StructuredListRow head>
                  <StructuredListCell head>
                    {intl.formatMessage({ id: "qc.violations.detail.result.value" })}
                  </StructuredListCell>
                  <StructuredListCell head>
                    {intl.formatMessage({ id: "qc.violations.detail.result.zScore" })}
                  </StructuredListCell>
                  <StructuredListCell head>
                    {intl.formatMessage({ id: "qc.violations.detail.result.timestamp" })}
                  </StructuredListCell>
                </StructuredListRow>
              </StructuredListHead>
              <StructuredListBody>
                {violation.affectedResults.map((result, index) => (
                  <StructuredListRow key={result.id || index}>
                    <StructuredListCell>
                      {result.resultValue?.toFixed(2) || result.value?.toFixed(2) || "-"}
                    </StructuredListCell>
                    <StructuredListCell>
                      {result.zScore?.toFixed(2) || "-"}
                    </StructuredListCell>
                    <StructuredListCell>
                      {formatTimestamp(result.runDateTime || result.timestamp)}
                    </StructuredListCell>
                  </StructuredListRow>
                ))}
              </StructuredListBody>
            </StructuredListWrapper>
          </div>
        )}

        {/* Corrective Actions */}
        {violation.correctiveActions && violation.correctiveActions.length > 0 && (
          <div className="violation-detail-section">
            <h4>{intl.formatMessage({ id: "qc.violations.detail.correctiveActions" })}</h4>
            <StructuredListWrapper data-testid="violation-detail-corrective-actions">
              <StructuredListHead>
                <StructuredListRow head>
                  <StructuredListCell head>
                    {intl.formatMessage({ id: "qc.violations.detail.action.type" })}
                  </StructuredListCell>
                  <StructuredListCell head>
                    {intl.formatMessage({ id: "qc.violations.detail.action.status" })}
                  </StructuredListCell>
                  <StructuredListCell head>
                    {intl.formatMessage({ id: "qc.violations.detail.action.assignedTo" })}
                  </StructuredListCell>
                </StructuredListRow>
              </StructuredListHead>
              <StructuredListBody>
                {violation.correctiveActions.map((action, index) => (
                  <StructuredListRow key={action.id || index}>
                    <StructuredListCell>
                      {intl.formatMessage({ id: `qc.correctiveAction.type.${action.actionType?.toLowerCase()}` })}
                    </StructuredListCell>
                    <StructuredListCell>
                      <Tag
                        type={action.status === "COMPLETED" ? "green" : action.status === "IN_PROGRESS" ? "blue" : "gray"}
                        size="sm"
                      >
                        {intl.formatMessage({ id: `qc.correctiveAction.status.${action.status?.toLowerCase()}` })}
                      </Tag>
                    </StructuredListCell>
                    <StructuredListCell>
                      {action.assignedUserName || action.assignedUser?.name || "-"}
                    </StructuredListCell>
                  </StructuredListRow>
                ))}
              </StructuredListBody>
            </StructuredListWrapper>
          </div>
        )}

        {/* Resolution Details (if resolved) */}
        {violation.resolutionStatus === "RESOLVED" && violation.resolutionDetails && (
          <div className="violation-detail-section">
            <h4>{intl.formatMessage({ id: "qc.violations.detail.resolution" })}</h4>
            <StructuredListWrapper data-testid="violation-detail-resolution">
              <StructuredListBody>
                <StructuredListRow>
                  <StructuredListCell>
                    {intl.formatMessage({ id: "qc.violations.detail.resolution.date" })}
                  </StructuredListCell>
                  <StructuredListCell>
                    {formatTimestamp(violation.resolutionTimestamp)}
                  </StructuredListCell>
                </StructuredListRow>
                <StructuredListRow>
                  <StructuredListCell>
                    {intl.formatMessage({ id: "qc.violations.detail.resolution.notes" })}
                  </StructuredListCell>
                  <StructuredListCell>
                    {violation.resolutionDetails}
                  </StructuredListCell>
                </StructuredListRow>
              </StructuredListBody>
            </StructuredListWrapper>
          </div>
        )}
      </ModalBody>
      <ModalFooter data-testid="violation-detail-modal-footer">
        <Button kind="secondary" onClick={onClose}>
          {intl.formatMessage({ id: "button.close" })}
        </Button>
        {isUnresolved && isWarning && (
          <Button
            kind="primary"
            onClick={() => onAcknowledge(violation.id)}
            data-testid="violation-detail-acknowledge-button"
          >
            {intl.formatMessage({ id: "qc.violations.action.acknowledge" })}
          </Button>
        )}
        {isUnresolved && !isWarning && (
          <Button
            kind="primary"
            onClick={() => onCreateCorrectiveAction(violation.id)}
            data-testid="violation-detail-corrective-action-button"
          >
            {intl.formatMessage({ id: "qc.violations.action.createCorrectiveAction" })}
          </Button>
        )}
      </ModalFooter>
    </ComposedModal>
  );
};

ViolationDetailModal.propTypes = {
  violation: PropTypes.shape({
    id: PropTypes.string,
    ruleCode: PropTypes.string,
    severity: PropTypes.string,
    resolutionStatus: PropTypes.string,
    status: PropTypes.string,
    analyzerName: PropTypes.string,
    analyzer: PropTypes.object,
    testName: PropTypes.string,
    test: PropTypes.object,
    controlLotNumber: PropTypes.string,
    controlLot: PropTypes.object,
    violationTimestamp: PropTypes.string,
    createdDate: PropTypes.string,
    affectedResults: PropTypes.array,
    correctiveActions: PropTypes.array,
    resolutionTimestamp: PropTypes.string,
    resolutionDetails: PropTypes.string,
  }),
  open: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  onAcknowledge: PropTypes.func.isRequired,
  onCreateCorrectiveAction: PropTypes.func.isRequired,
};

export default ViolationDetailModal;
