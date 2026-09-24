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
 * Tag colour per cycle state, across both machines (participant and
 * provider). Keyed lower-case and looked up case-insensitively: the
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

/**
 * Props that make a Carbon date picker take its value from the calendar alone.
 * flatpickr never parses what is typed into these fields, so text entry left a
 * date on screen that the save then sent as an empty string. Refusing the
 * keystrokes is honest about it. Modifier combinations pass through, so copy
 * and keyboard navigation are unaffected, and the calendar still opens on focus.
 */
export const calendarOnlyInput = {
  onKeyDown: (e) => {
    if (e.ctrlKey || e.metaKey || e.altKey) return;
    if (e.key.length === 1 || e.key === "Backspace" || e.key === "Delete") {
      e.preventDefault();
    }
  },
  onPaste: (e) => e.preventDefault(),
};

/**
 * A participant's box has left the building. Read from either the box state or
 * the shipment's own date, because the two are written by different steps and a
 * box dispatched before the shipment row was stamped still counts.
 */
export const isDispatched = (row) =>
  row.boxState === "SENT" || !!row.shippedDate;

/** RFC 4180 cell: everything quoted, embedded quotes doubled. */
export const csvCell = (value) =>
  `"${String(value ?? "").replace(/"/g, '""')}"`;

/** Hands the browser a Blob to save under filename. */
export const downloadBlob = (blob, filename) => {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
};

/** Hands the browser a CSV without a round trip to the server. */
export const downloadCsv = (content, filename) =>
  downloadBlob(
    new Blob([content], { type: "text/csv;charset=utf-8;" }),
    filename,
  );

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
