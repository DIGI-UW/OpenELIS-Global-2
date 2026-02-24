import React from "react";
import { useIntl } from "react-intl";
import { Tag, Tooltip } from "@carbon/react";
import { Activity } from "@carbon/react/icons";
import "./DeltaCheckBadge.scss";

/**
 * DeltaCheckBadge displays a purple indicator on result rows that have
 * an active delta check alert. Shows change percentage in tooltip.
 */
const DeltaCheckBadge = ({ alert }) => {
  const intl = useIntl();

  if (!alert) {
    return null;
  }

  const tooltipText = intl.formatMessage(
    { id: "deltacheck.badge.tooltip" },
    { changePercent: alert.changePercent?.toFixed(1) },
  );

  return (
    <Tooltip label={tooltipText}>
      <span className="delta-check-badge">
        <Tag type="purple" size="sm" renderIcon={Activity}>
          {intl.formatMessage({ id: "deltacheck.badge.label" })}
        </Tag>
      </span>
    </Tooltip>
  );
};

export default DeltaCheckBadge;
