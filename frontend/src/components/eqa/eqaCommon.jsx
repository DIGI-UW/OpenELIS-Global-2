import React from "react";
import { Tag } from "@carbon/react";
import { useIntl } from "react-intl";

/**
 * Presentation bits every EQA page repeats. Carbon's own text token rather than a
 * hard-coded grey, so these follow the theme instead of pinning one shade of it.
 */
export const hintStyle = {
  fontSize: "0.75rem",
  color: "var(--cds-text-secondary, #525252)",
};

export const kpiLabelStyle = hintStyle;

export const kpiValueStyle = { fontSize: "1.75rem", fontWeight: 600 };

/**
 * Tag colour per cycle state, across both machines (FR-V2.1-04 participant,
 * FR-V2.1-18 provider). Keyed lower-case and looked up case-insensitively: the
 * participant endpoints answer lower-case, the provider ones the enum name.
 */
const STATUS_TAG = {
  planned: "gray",
  panel_received: "teal",
  testing: "blue",
  ready_to_submit: "purple",
  submitted: "cyan",
  prep_in_progress: "blue",
  ready_to_ship: "purple",
  shipped: "teal",
  delivered: "cyan",
  submissions_open: "cyan",
  submissions_closed: "magenta",
  scoring: "warm-gray",
  scored: "green",
  closed: "gray",
};

/** One cycle-state tag, so no page has to keep its own copy of the palette. */
export const CycleStatusTag = ({ status }) => {
  const intl = useIntl();
  const key = (status || "").toLowerCase();
  return (
    <Tag type={STATUS_TAG[key] || "gray"} size="sm">
      {intl.formatMessage({
        id: `eqa.cycle.status.${key}`,
        defaultMessage: key.replace(/_/g, " "),
      })}
    </Tag>
  );
};
