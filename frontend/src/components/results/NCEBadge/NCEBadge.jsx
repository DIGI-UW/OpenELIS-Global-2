import React from "react";
import { useIntl } from "react-intl";
import { useHistory } from "react-router-dom";
import { Tag, Tooltip } from "@carbon/react";
import { WarningAlt } from "@carbon/react/icons";
import useNCEBadge from "./useNCEBadge";
import "./NCEBadge.scss";

const SEVERITY_TAG_TYPE = {
  Minor: "teal",
  Major: "magenta",
  Critical: "red",
};

const SEVERITY_I18N = {
  Minor: "nce.badge.severity.minor",
  Major: "nce.badge.severity.major",
  Critical: "nce.badge.severity.critical",
};

/**
 * NCEBadge displays a warning indicator on result rows that have
 * associated Non-Conformity Events. Clicking navigates to NCE details.
 */
const NCEBadge = ({ resultId, onClick }) => {
  const intl = useIntl();
  const history = useHistory();
  const { hasNCE, nceCount, highestSeverity, nceNumbers, loading } =
    useNCEBadge(resultId);

  if (loading || !hasNCE) {
    return null;
  }

  const tagType = SEVERITY_TAG_TYPE[highestSeverity] || "green";
  const tooltipText =
    nceCount === 1
      ? intl.formatMessage({ id: "nce.badge.tooltip.single" })
      : intl.formatMessage(
          { id: "nce.badge.tooltip.multiple" },
          { count: nceCount },
        );

  const severityText = highestSeverity
    ? intl.formatMessage({ id: SEVERITY_I18N[highestSeverity] })
    : "";

  const handleClick = () => {
    if (onClick) {
      onClick(nceNumbers);
    } else if (nceNumbers.length > 0) {
      history.push(
        `/ViewNonConformingEvent?nceNumber=${encodeURIComponent(nceNumbers[0])}`,
      );
    }
  };

  return (
    <Tooltip
      label={`${tooltipText}${severityText ? ` - ${severityText}` : ""}`}
    >
      <button
        type="button"
        className="nce-badge"
        onClick={handleClick}
        aria-label={tooltipText}
      >
        <Tag type={tagType} size="md" renderIcon={WarningAlt}>
          {intl.formatMessage({ id: "nce.badge.label" })}
          {nceCount > 1 ? ` (${nceCount})` : ""}
        </Tag>
      </button>
    </Tooltip>
  );
};

export default NCEBadge;
