import React from "react";
import { Select, SelectItem, TextArea, TextInput } from "@carbon/react";
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
  multiSelectResultValues?: string;
  dictionaryResults?: DictionaryOption[];
  unitsOfMeasure?: string;
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
  const rowKey = worklistRowKey(row);

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
          noLabel
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

    case "N":
      return (
        <TextInput
          id={`unifiedResultValue-${rowKey}`}
          labelText=""
          type="number"
          value={row.resultValue || ""}
          onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
            onValueChange("resultValue", e.target.value)
          }
        />
      );

    case "R":
    case "A":
      return (
        <TextArea
          id={`unifiedResultValue-${rowKey}`}
          rows={1}
          labelText=""
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
