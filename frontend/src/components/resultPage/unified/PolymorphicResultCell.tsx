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
 */

export interface DictionaryOption {
  id: string;
  value: string;
}

export interface ResultCellRow {
  id: string;
  analysisId: string;
  resultType: string;
  resultValue?: string;
  multiSelectResultValues?: string;
  dictionaryResults?: DictionaryOption[];
  unitsOfMeasure?: string;
}

interface PolymorphicResultCellProps {
  row: ResultCellRow;
  editable: boolean;
  onValueChange: (
    analysisId: string,
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
          id={`unifiedResultValue-${row.analysisId}`}
          name={`unifiedResultValue-${row.analysisId}`}
          noLabel
          onChange={(e: React.ChangeEvent<HTMLSelectElement>) =>
            onValueChange(row.analysisId, "resultValue", e.target.value)
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
          id={`unifiedMultiResultValue-${row.analysisId}`}
          name={`unifiedMultiResultValue-${row.analysisId}`}
          dictionaryValues={row.dictionaryResults || []}
          value={row.multiSelectResultValues}
          onChange={(e: { target: { value: string } }) =>
            onValueChange(
              row.analysisId,
              "multiSelectResultValues",
              e.target.value,
            )
          }
        />
      );

    case "C":
      return (
        <CascadingMultiSelect
          id={`unifiedCascadingResultValue-${row.analysisId}`}
          name={`unifiedCascadingResultValue-${row.analysisId}`}
          dictionaryValues={row.dictionaryResults || []}
          value={row.multiSelectResultValues}
          onChange={(e: { target: { value: string } }) =>
            onValueChange(
              row.analysisId,
              "multiSelectResultValues",
              e.target.value,
            )
          }
        />
      );

    case "N":
      return (
        <TextInput
          id={`unifiedResultValue-${row.analysisId}`}
          labelText=""
          type="number"
          value={row.resultValue || ""}
          onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
            onValueChange(row.analysisId, "resultValue", e.target.value)
          }
        />
      );

    case "R":
    case "A":
      return (
        <TextArea
          id={`unifiedResultValue-${row.analysisId}`}
          rows={1}
          labelText=""
          value={row.resultValue || ""}
          onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) =>
            onValueChange(row.analysisId, "resultValue", e.target.value)
          }
        />
      );

    default:
      return <span>{row.resultValue || ""}</span>;
  }
};

export default PolymorphicResultCell;
