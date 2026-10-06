import React from "react";
import { Tag } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import InstrumentReported from "./InstrumentReported";

export const decisionKey = (row) =>
  row.testId ? `${row.sampleGroupingNumber}:${row.testId}` : `row:${row.id}`;

// Held for a decision the reviewer can make on the page, not for a mapping fix.
const reviewableHolds = ["awaiting_specimen", "awaiting_placement"];

const reviewPriority = (row) =>
  !row.importIssueReason
    ? 2
    : reviewableHolds.includes(row.importIssueReason)
      ? 1
      : 0;

/**
 * Splits a page of staged results into tests. A test is headed by its main
 * result, the one the reviewer can act on first, and that row carries its
 * decision; its rows on components are its parts, shown beneath it and decided
 * with it. A re-exported duplicate stays its own row.
 */
export const groupTestParts = (rows) => {
  const rowsByTest = new Map();
  rows.forEach((row) => {
    const key = decisionKey(row);
    rowsByTest.set(key, [...(rowsByTest.get(key) || []), row]);
  });
  const headIdByKey = new Map();
  const partsByHeadId = new Map();
  const partIds = new Set();
  rowsByTest.forEach((testRows, key) => {
    const head = testRows.reduce((best, row) =>
      (!row.componentId && best.componentId) ||
      (!row.componentId === !best.componentId &&
        reviewPriority(row) > reviewPriority(best))
        ? row
        : best,
    );
    const parts = testRows.filter((row) => row !== head && row.componentId);
    headIdByKey.set(key, head.id);
    partsByHeadId.set(head.id, parts);
    parts.forEach((part) => partIds.add(part.id));
  });
  return {
    headIdByKey,
    decisionHeadIds: new Set(headIdByKey.values()),
    partsByHeadId,
    partIds,
  };
};

const partValue = (part) =>
  part.dictionaryResultList?.find((option) => option.id == part.result)
    ?.displayValue ?? part.result;

/** A test's parts beneath its main result, read-only. */
const ResultParts = ({ headId, parts }) => {
  if (parts.length === 0) {
    return null;
  }
  return (
    <ul className="resultParts" data-testid={`result-parts-${headId}`}>
      {parts.map((part) => (
        <li key={part.id}>
          <span className="resultParts__label">
            {part.componentLabel || part.testName}
          </span>{" "}
          <strong>{partValue(part)}</strong>
          {part.importIssueReason && (
            <>
              {" "}
              <Tag type="warm-gray" size="sm">
                <FormattedMessage id="analyzer.results.held.tag" />
              </Tag>
            </>
          )}
          <InstrumentReported row={part} />
        </li>
      ))}
    </ul>
  );
};

export default ResultParts;
