import React from "react";
import { Tag } from "@carbon/react";
import { useIntl } from "react-intl";
import { formatAgeDays } from "./rangeUtils";

/**
 * OGC-949 M7 / OGC-971 — coverage validation panel.
 *
 * Renders one card per sex from the backend's CoverageReport, showing whether
 * the age axis is fully covered and, when not, the uncovered age windows that
 * the activation gate (OGC-973) will require an acknowledgment for. Read-only:
 * it visualizes the report the ranges API computes; it never recomputes locally.
 */
const STATUS_TAG = {
  COMPLETE: "green",
  GAP: "red",
  OVERLAP: "magenta",
  EMPTY: "gray",
};

const SexCard = ({ labelId, coverage }) => {
  const intl = useIntl();
  if (!coverage) {
    return null;
  }
  const status = coverage.status || "EMPTY";
  return (
    <div
      data-testid={`coverage-card-${coverage.sex}`}
      style={{
        border: "1px solid var(--cds-border-subtle)",
        borderRadius: "4px",
        padding: "0.75rem 1rem",
        minWidth: "12rem",
        flex: "1 1 0",
      }}
    >
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: "0.5rem",
        }}
      >
        <strong>{intl.formatMessage({ id: labelId })}</strong>
        <Tag type={STATUS_TAG[status]} size="sm">
          {intl.formatMessage({
            id: `label.testCatalog.ranges.coverage.status.${status}`,
          })}
        </Tag>
      </div>
      {coverage.gaps && coverage.gaps.length > 0 && (
        <div>
          <span style={{ fontSize: "0.75rem" }}>
            {intl.formatMessage({
              id: "label.testCatalog.ranges.coverage.gapsHeading",
            })}
          </span>
          <ul style={{ marginLeft: "1.25rem", listStyle: "disc" }}>
            {coverage.gaps.map((gap, i) => (
              <li key={i} style={{ fontSize: "0.75rem" }}>
                {intl.formatMessage(
                  { id: "label.testCatalog.ranges.gapRange" },
                  {
                    from: formatAgeDays(gap.fromAge, intl),
                    to: formatAgeDays(gap.toAge, intl),
                  },
                )}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
};

const CoverageValidationPanel = ({ coverage }) => {
  const intl = useIntl();
  if (!coverage) {
    return null;
  }
  return (
    <div>
      <h5 style={{ marginBottom: "0.5rem" }}>
        {intl.formatMessage({
          id: "label.testCatalog.ranges.coverage.title",
        })}
      </h5>
      <div style={{ display: "flex", gap: "1rem", flexWrap: "wrap" }}>
        <SexCard
          labelId="label.testCatalog.ranges.male"
          coverage={coverage.male}
        />
        <SexCard
          labelId="label.testCatalog.ranges.female"
          coverage={coverage.female}
        />
      </div>
    </div>
  );
};

export default CoverageValidationPanel;
