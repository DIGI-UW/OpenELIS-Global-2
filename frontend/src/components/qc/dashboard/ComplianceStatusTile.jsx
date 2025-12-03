/**
 * ComplianceStatusTile Component
 *
 * Displays compliance status for a single analyzer with color-coded indicator
 * Task Reference: T122
 * Specification: FR-047, FR-048, FR-049
 *
 * Features:
 * - Color-coded status (green=compliant, yellow=warning, red=non-compliant)
 * - Shows triggered rules with severity
 * - Displays last QC result timestamp
 * - Clickable to navigate to detail view
 */

import React from "react";
import { ClickableTile, Tag } from "@carbon/react";
import {
  CheckmarkFilled,
  WarningFilled,
  ErrorFilled,
} from "@carbon/icons-react";
import { useIntl } from "react-intl";
import PropTypes from "prop-types";
import "./ComplianceStatusTile.css";

const ComplianceStatusTile = ({
  analyzerId,
  analyzerName,
  status,
  triggeredRules,
  lastResultTime,
  unresolvedViolationCount,
  onClick,
}) => {
  const intl = useIntl();

  // Determine status color and icon
  const getStatusConfig = () => {
    switch (status) {
      case "COMPLIANT":
      case "compliant":
      case "green":
        return {
          color: "green",
          icon: CheckmarkFilled,
          label: intl.formatMessage({ id: "qc.status.compliant" }),
          className: "compliance-tile--compliant",
        };
      case "WARNING":
      case "warning":
      case "yellow":
        return {
          color: "yellow",
          icon: WarningFilled,
          label: intl.formatMessage({ id: "qc.status.warning" }),
          className: "compliance-tile--warning",
        };
      case "NON_COMPLIANT":
      case "non_compliant":
      case "red":
      default:
        return {
          color: "red",
          icon: ErrorFilled,
          label: intl.formatMessage({ id: "qc.status.nonCompliant" }),
          className: "compliance-tile--non-compliant",
        };
    }
  };

  const statusConfig = getStatusConfig();
  const StatusIcon = statusConfig.icon;

  // Format last result time
  const formatLastResultTime = () => {
    if (!lastResultTime) {
      return intl.formatMessage({ id: "qc.dashboard.noResults" });
    }
    const date = new Date(lastResultTime);
    return intl.formatDate(date, {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  // Get rule severity tag type
  const getRuleSeverityTagType = (severity) => {
    switch (severity?.toUpperCase()) {
      case "REJECTION":
        return "red";
      case "WARNING":
        return "yellow";
      default:
        return "gray";
    }
  };

  return (
    <ClickableTile
      className={`compliance-tile ${statusConfig.className}`}
      onClick={onClick}
      data-testid={`compliance-tile-${analyzerId}`}
    >
      {/* Status indicator */}
      <div className="compliance-tile-status" data-testid={`compliance-status-${analyzerId}`}>
        <StatusIcon
          className={`compliance-tile-status-icon compliance-tile-status-icon--${statusConfig.color}`}
          size={24}
        />
        <span className="compliance-tile-status-label">{statusConfig.label}</span>
      </div>

      {/* Analyzer name */}
      <h4 className="compliance-tile-name" data-testid={`compliance-analyzer-name-${analyzerId}`}>
        {analyzerName}
      </h4>

      {/* Triggered rules */}
      {triggeredRules && triggeredRules.length > 0 && (
        <div className="compliance-tile-rules" data-testid={`compliance-rules-${analyzerId}`}>
          <span className="compliance-tile-rules-label">
            {intl.formatMessage({ id: "qc.dashboard.triggeredRules" })}:
          </span>
          <div className="compliance-tile-rules-list">
            {triggeredRules.slice(0, 3).map((rule, index) => (
              <Tag
                key={index}
                type={getRuleSeverityTagType(rule.severity)}
                size="sm"
              >
                {rule.code || rule.name}
              </Tag>
            ))}
            {triggeredRules.length > 3 && (
              <Tag type="gray" size="sm">
                +{triggeredRules.length - 3}
              </Tag>
            )}
          </div>
        </div>
      )}

      {/* Unresolved violations count */}
      {unresolvedViolationCount > 0 && (
        <div className="compliance-tile-violations" data-testid={`compliance-violations-${analyzerId}`}>
          <Tag type="red" size="sm">
            {intl.formatMessage(
              { id: "qc.dashboard.unresolvedViolations" },
              { count: unresolvedViolationCount }
            )}
          </Tag>
        </div>
      )}

      {/* Last result time (FR-048) */}
      <div className="compliance-tile-last-result" data-testid={`compliance-last-result-${analyzerId}`}>
        <span className="compliance-tile-last-result-label">
          {intl.formatMessage({ id: "qc.dashboard.lastResult" })}:
        </span>
        <span className="compliance-tile-last-result-time">
          {formatLastResultTime()}
        </span>
      </div>
    </ClickableTile>
  );
};

ComplianceStatusTile.propTypes = {
  analyzerId: PropTypes.string.isRequired,
  analyzerName: PropTypes.string.isRequired,
  status: PropTypes.oneOf([
    "COMPLIANT",
    "compliant",
    "green",
    "WARNING",
    "warning",
    "yellow",
    "NON_COMPLIANT",
    "non_compliant",
    "red",
  ]).isRequired,
  triggeredRules: PropTypes.arrayOf(
    PropTypes.shape({
      code: PropTypes.string,
      name: PropTypes.string,
      severity: PropTypes.string,
    })
  ),
  lastResultTime: PropTypes.string,
  unresolvedViolationCount: PropTypes.number,
  onClick: PropTypes.func,
};

ComplianceStatusTile.defaultProps = {
  triggeredRules: [],
  lastResultTime: null,
  unresolvedViolationCount: 0,
  onClick: () => {},
};

export default ComplianceStatusTile;
