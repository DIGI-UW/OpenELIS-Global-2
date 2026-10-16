import React from "react";
import "./AnalyzerCountChips.css";

export interface AnalyzerCountChip {
  id: string;
  value: number;
  label: string;
  onSelect: () => void;
  /** The filter this chip names is the one in force. Absent for a chip that is not a filter. */
  current?: boolean;
  /** A count that needs someone, such as analyzers needing attention. */
  warning?: boolean;
  testId?: string;
}

/**
 * One line of counts above a list, each the filter it names: the analyzer
 * pages' replacement for KPI tiles.
 */
const AnalyzerCountChips = ({
  chips,
  label,
  testId,
}: {
  chips: AnalyzerCountChip[];
  label: string;
  testId?: string;
}) => (
  <div
    className="analyzer-count-chips"
    role="group"
    aria-label={label}
    data-testid={testId}
  >
    {chips.map((chip) => (
      <button
        key={chip.id}
        type="button"
        className={[
          "analyzer-count-chip",
          chip.current ? "analyzer-count-chip--current" : "",
          chip.warning ? "analyzer-count-chip--warning" : "",
        ]
          .filter(Boolean)
          .join(" ")}
        aria-pressed={chip.current === undefined ? undefined : chip.current}
        data-testid={chip.testId}
        onClick={chip.onSelect}
      >
        <span className="stat-value">{chip.value}</span>
        <span className="stat-label">{chip.label}</span>
      </button>
    ))}
  </div>
);

export default AnalyzerCountChips;
