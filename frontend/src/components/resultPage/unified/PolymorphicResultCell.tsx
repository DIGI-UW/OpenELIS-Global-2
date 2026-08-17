import React from "react";
import { Select, SelectItem, TextArea, TextInput } from "@carbon/react";
import { useIntl } from "react-intl";
import ResultMultiSelect from "../../common/multiSelect";
import CascadingMultiSelect from "../../common/cascadingMultiSelect";

/**
 * OGC-1020 (R1) — FR-A1 polymorphic result cell.
 *
 * Renders the result input by the test's result type — numeric (N),
 * dictionary (D), multi-checkbox (M) — matching the legacy widgets one for
 * one so stored values stay compatible. Cascading (C), remark (R) and
 * alphanumeric (A) reuse the legacy behavior. When the row is read-only
 * (FR-A2: saved until Edit) the stored display value renders as plain text.
 *
 * A multi-component test yields one row PER COMPONENT sharing an analysisId
 * (FR-A′1), so widget identity and change events are keyed by the composite
 * row key — never by analysisId alone.
 */

export interface DictionaryOption {
  id: string;
  value: string;
}

export interface ResultCellRow {
  id: string;
  analysisId: string;
  testResultComponentId?: string;
  resultType: string;
  resultValue?: string;
  /** the value as stored, where resultValue is the value as reported */
  rawResultValue?: string;
  multiSelectResultValues?: string;
  dictionaryResults?: DictionaryOption[];
  unitsOfMeasure?: string;
  testName?: string;
  /** decimal places this test is configured to report to */
  significantDigits?: number;
}

/**
 * The decimal places entered, or 0 when the value carries no fraction.
 *
 * <p>A value entered to more places than the test reports to is stored in full
 * and shown rounded, so the record and the screen stop agreeing and the next
 * edit saves the rounded form over the stored one (OGC-1179). The entry field
 * says so rather than accepting it silently.
 */
export function enteredDecimalPlaces(value: string): number {
  const dot = value.indexOf(".");
  return dot === -1 ? 0 : value.length - dot - 1;
}

export function exceedsConfiguredPrecision(
  value: string | undefined,
  significantDigits: number | undefined,
): boolean {
  if (!value || significantDigits === undefined || significantDigits < 0) {
    return false;
  }
  return (
    Number.isFinite(Number(value)) &&
    enteredDecimalPlaces(value) > significantDigits
  );
}

/**
 * Whether the row is holding a newly typed value finer than the test reports
 * to.
 *
 * <p>A value already stored at that precision is left alone: it is a record
 * that exists, and refusing to save the row would strand it — the reader could
 * not so much as add a note to say so. What is refused is entering a new one.
 */
export function blocksSaveOnPrecision(row: {
  resultType?: string;
  resultValue?: string;
  rawResultValue?: string;
  significantDigits?: number;
}): boolean {
  return (
    row.resultType === "N" &&
    row.resultValue !== row.rawResultValue &&
    exceedsConfiguredPrecision(row.resultValue, row.significantDigits)
  );
}

/** The `step` a number input of this precision accepts. */
export function precisionStep(significantDigits: number | undefined): string {
  if (significantDigits === undefined || significantDigits < 0) {
    return "any";
  }
  return significantDigits === 0
    ? "1"
    : `0.${"0".repeat(significantDigits - 1)}1`;
}

/** Unique identity for a worklist row: one analysis may render N component rows. */
export function worklistRowKey(row: {
  analysisId: string;
  testResultComponentId?: string;
}): string {
  return `${row.analysisId}-${row.testResultComponentId || "primary"}`;
}

interface PolymorphicResultCellProps {
  row: ResultCellRow;
  editable: boolean;
  onValueChange: (
    field: "resultValue" | "multiSelectResultValues",
    value: string,
  ) => void;
}

function readOnlyDisplay(row: ResultCellRow): string {
  if (row.resultType === "D") {
    const match = (row.dictionaryResults || []).find(
      (option) => option.id === row.resultValue,
    );
    return match ? match.value : row.resultValue || "";
  }
  if (row.resultType === "M" || row.resultType === "C") {
    try {
      const parsed = JSON.parse(row.multiSelectResultValues || "{}");
      const ids = Object.values(parsed)
        .flatMap((group) => String(group).split(","))
        .filter(Boolean);
      return (row.dictionaryResults || [])
        .filter((option) => ids.includes(String(option.id)))
        .map((option) => option.value)
        .join(", ");
    } catch {
      return "";
    }
  }
  return row.resultValue || "";
}

const PolymorphicResultCell: React.FC<PolymorphicResultCellProps> = ({
  row,
  editable,
  onValueChange,
}) => {
  const intl = useIntl();
  const rowKey = worklistRowKey(row);
  // The cell is the page's primary control and carries no visible label — the
  // column header names it for a sighted reader, but a screen reader lands on
  // an unnamed control, so each variant gets a hidden one (OGC-1179).
  const accessibleName = intl.formatMessage(
    { id: "label.results.resultFor" },
    { 0: row.testName || "" },
  );

  if (!editable) {
    return (
      <span className="unifiedResultsReadOnlyValue">
        {readOnlyDisplay(row)}
      </span>
    );
  }

  switch (row.resultType) {
    case "D":
      return (
        <Select
          id={`unifiedResultValue-${rowKey}`}
          name={`unifiedResultValue-${rowKey}`}
          labelText={accessibleName}
          hideLabel
          onChange={(e: React.ChangeEvent<HTMLSelectElement>) =>
            onValueChange("resultValue", e.target.value)
          }
          value={row.resultValue || ""}
        >
          <SelectItem text="" value="" />
          {(row.dictionaryResults || []).map((option) => (
            <SelectItem text={option.value} value={option.id} key={option.id} />
          ))}
        </Select>
      );

    case "M":
      return (
        <ResultMultiSelect
          id={`unifiedMultiResultValue-${rowKey}`}
          name={`unifiedMultiResultValue-${rowKey}`}
          dictionaryValues={row.dictionaryResults || []}
          value={row.multiSelectResultValues}
          onChange={(e: { target: { value: string } }) =>
            onValueChange("multiSelectResultValues", e.target.value)
          }
        />
      );

    case "C":
      return (
        <CascadingMultiSelect
          id={`unifiedCascadingResultValue-${rowKey}`}
          name={`unifiedCascadingResultValue-${rowKey}`}
          dictionaryValues={row.dictionaryResults || []}
          value={row.multiSelectResultValues}
          onChange={(e: { target: { value: string } }) =>
            onValueChange("multiSelectResultValues", e.target.value)
          }
        />
      );

    case "N": {
      const tooPrecise = blocksSaveOnPrecision(row);
      return (
        <TextInput
          id={`unifiedResultValue-${rowKey}`}
          labelText={accessibleName}
          hideLabel
          type="number"
          step={precisionStep(row.significantDigits)}
          invalid={tooPrecise}
          invalidText={intl.formatMessage(
            { id: "error.results.precision" },
            { 0: row.significantDigits ?? 0 },
          )}
          value={row.resultValue || ""}
          onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
            onValueChange("resultValue", e.target.value)
          }
        />
      );
    }

    case "R":
    case "A":
      return (
        <TextArea
          id={`unifiedResultValue-${rowKey}`}
          rows={1}
          labelText={accessibleName}
          hideLabel
          value={row.resultValue || ""}
          onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) =>
            onValueChange("resultValue", e.target.value)
          }
        />
      );

    default:
      return <span>{row.resultValue || ""}</span>;
  }
};

export default PolymorphicResultCell;
