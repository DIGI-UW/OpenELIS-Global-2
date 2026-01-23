import React from "react";
import {
  Tile,
  Tag,
  StructuredListWrapper,
  StructuredListHead,
  StructuredListBody,
  StructuredListRow,
  StructuredListCell,
  InlineNotification,
} from "@carbon/react";
import {
  CheckmarkFilled,
  CloseFilled,
  WarningAltFilled,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import "./NotebookWorkflow.css";

/**
 * TraceabilityChecklist - Displays traceability verification results.
 * T137: Shows parent-child relationship, movement history, and storage
 * assignment verification status for archiving workflow.
 *
 * @param {Object} props
 * @param {Object} props.traceabilityResult - Result from verifyTraceability API
 * @param {boolean} props.loading - Whether traceability check is in progress
 */
function TraceabilityChecklist({ traceabilityResult, loading }) {
  const intl = useIntl();

  if (loading) {
    return (
      <Tile className="traceability-checklist loading">
        <p>
          <FormattedMessage
            id="notebook.archive.verifying"
            defaultMessage="Verifying traceability..."
          />
        </p>
      </Tile>
    );
  }

  if (!traceabilityResult) {
    return (
      <Tile className="traceability-checklist empty">
        <p>
          <FormattedMessage
            id="notebook.archive.noVerification"
            defaultMessage="No traceability verification performed yet. Click 'Verify Traceability' to check."
          />
        </p>
      </Tile>
    );
  }

  const { passed, checks, summary } = traceabilityResult;

  const getStatusIcon = (check) => {
    if (check.passed) {
      return <CheckmarkFilled size={20} className="status-icon success" />;
    } else if (check.critical) {
      return <CloseFilled size={20} className="status-icon error" />;
    } else {
      return <WarningAltFilled size={20} className="status-icon warning" />;
    }
  };

  const getStatusTag = (check) => {
    if (check.passed) {
      return (
        <Tag type="green" size="sm">
          <FormattedMessage
            id="notebook.archive.check.passed"
            defaultMessage="Passed"
          />
        </Tag>
      );
    } else if (check.critical) {
      return (
        <Tag type="red" size="sm">
          <FormattedMessage
            id="notebook.archive.check.failed"
            defaultMessage="Failed"
          />
        </Tag>
      );
    } else {
      return (
        <Tag type="orange" size="sm">
          <FormattedMessage
            id="notebook.archive.check.warning"
            defaultMessage="Warning"
          />
        </Tag>
      );
    }
  };

  return (
    <Tile className="traceability-checklist">
      <h4>
        <FormattedMessage
          id="notebook.archive.traceability.title"
          defaultMessage="Traceability Verification"
        />
      </h4>

      {/* Overall Status */}
      <div className="traceability-summary">
        {passed ? (
          <InlineNotification
            kind="success"
            title={intl.formatMessage({
              id: "notebook.archive.traceability.passed",
              defaultMessage: "All Checks Passed",
            })}
            subtitle={summary}
            hideCloseButton
            lowContrast
          />
        ) : traceabilityResult.hasCriticalFailures?.() ||
          checks?.some((c) => c.critical && !c.passed) ? (
          <InlineNotification
            kind="error"
            title={intl.formatMessage({
              id: "notebook.archive.traceability.failed",
              defaultMessage: "Critical Checks Failed",
            })}
            subtitle={summary}
            hideCloseButton
            lowContrast
          />
        ) : (
          <InlineNotification
            kind="warning"
            title={intl.formatMessage({
              id: "notebook.archive.traceability.warnings",
              defaultMessage: "Some Checks Have Warnings",
            })}
            subtitle={summary}
            hideCloseButton
            lowContrast
          />
        )}
      </div>

      {/* Individual Checks */}
      {checks && checks.length > 0 && (
        <StructuredListWrapper className="traceability-checks-list">
          <StructuredListHead>
            <StructuredListRow head>
              <StructuredListCell head>
                <FormattedMessage
                  id="notebook.archive.check.name"
                  defaultMessage="Check"
                />
              </StructuredListCell>
              <StructuredListCell head>
                <FormattedMessage
                  id="notebook.archive.check.description"
                  defaultMessage="Description"
                />
              </StructuredListCell>
              <StructuredListCell head>
                <FormattedMessage
                  id="notebook.archive.check.status"
                  defaultMessage="Status"
                />
              </StructuredListCell>
            </StructuredListRow>
          </StructuredListHead>
          <StructuredListBody>
            {checks.map((check, index) => (
              <StructuredListRow key={index}>
                <StructuredListCell>
                  <div className="check-name">
                    {getStatusIcon(check)}
                    <span>{check.checkName}</span>
                    {check.critical && (
                      <Tag type="high-contrast" size="sm">
                        <FormattedMessage
                          id="notebook.archive.check.critical"
                          defaultMessage="Critical"
                        />
                      </Tag>
                    )}
                  </div>
                </StructuredListCell>
                <StructuredListCell>
                  <div className="check-description">
                    {check.description}
                    {check.issues && check.issues.length > 0 && (
                      <ul className="check-issues">
                        {check.issues.map((issue, i) => (
                          <li key={i}>{issue}</li>
                        ))}
                      </ul>
                    )}
                  </div>
                </StructuredListCell>
                <StructuredListCell>{getStatusTag(check)}</StructuredListCell>
              </StructuredListRow>
            ))}
          </StructuredListBody>
        </StructuredListWrapper>
      )}
    </Tile>
  );
}

export default TraceabilityChecklist;
