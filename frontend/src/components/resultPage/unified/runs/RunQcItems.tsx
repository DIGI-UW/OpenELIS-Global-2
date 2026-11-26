import React from "react";
import { Tag } from "@carbon/react";
import { useIntl } from "react-intl";
import { runVerdictTagType } from "./runFormat";

export interface RunQcItemLine {
  id: number;
  kind: "BLANK" | "DUPLICATE";
  observed?: string | null;
  parentAccession?: string | null;
  evaluation: "PASS" | "FAIL" | "NOT_EVALUATED" | "BELOW_LOQ";
  detail?: string | null;
  operator?: string | null;
  recordedAt?: string | null;
}

interface RunQcItemsProps {
  items?: RunQcItemLine[] | null;
  testIdPrefix?: string;
}

/**
 * OGC-1200 FR-C10, FR-C11: the blanks and duplicates a run recorded for a
 * test, each with what it came to, the same on the run, on the row and in
 * Validation.
 */
const RunQcItems: React.FC<RunQcItemsProps> = ({
  items,
  testIdPrefix = "run-qc-item",
}) => {
  const intl = useIntl();
  if (!items || items.length === 0) {
    return null;
  }
  return (
    <ul className="runQcControls">
      {items.map((item) => (
        <li key={item.id} data-testid={`${testIdPrefix}-${item.id}`}>
          <Tag
            type={runVerdictTagType(
              item.evaluation === "PASS" || item.evaluation === "FAIL"
                ? item.evaluation
                : null,
            )}
            size="sm"
          >
            {intl.formatMessage({
              id: `run.qc.item.evaluation.${item.evaluation}`,
            })}
          </Tag>{" "}
          {[
            item.kind === "DUPLICATE" && item.parentAccession
              ? intl.formatMessage(
                  { id: "run.qc.item.duplicateOf" },
                  { accession: item.parentAccession },
                )
              : intl.formatMessage({ id: `run.qc.item.${item.kind}` }),
            item.observed,
            item.detail,
            item.operator,
          ]
            .filter(Boolean)
            .join(" · ")}
        </li>
      ))}
    </ul>
  );
};

export default RunQcItems;
