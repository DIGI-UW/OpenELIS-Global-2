import { useState, useCallback, useMemo } from "react";

/**
 * Custom hook for QC evaluation logic.
 * Separates QC evaluation from component state (CRIT-003 compliant).
 *
 * @param {Array} qcSamples - List of QC samples to evaluate
 * @returns {Object} QC evaluation state and methods
 */
export const useQcEvaluation = () => {
  const [overallQcStatus, setOverallQcStatus] = useState("none");
  const [nonConformityId, setNonConformityId] = useState(null);

  /**
   * Parse a range string into min/max values with support for multiple formats.
   */
  const parseRange = useCallback((rangeStr) => {
    if (!rangeStr || typeof rangeStr !== "string") {
      return null;
    }

    const trimmed = rangeStr.trim();

    const rangePattern = /^(\d+\.?\d*)\s*-\s*(\d+\.?\d*)$/;
    const rangeMatch = trimmed.match(rangePattern);
    if (rangeMatch) {
      const min = parseFloat(rangeMatch[1]);
      const max = parseFloat(rangeMatch[2]);
      if (Number.isFinite(min) && Number.isFinite(max) && min <= max) {
        return { min, max };
      }
    }

    const lessThanPattern = /^<?=?\s*(\d+\.?\d*)$/;
    const lessThanMatch = trimmed.match(lessThanPattern);
    if (lessThanMatch) {
      const max = parseFloat(lessThanMatch[1]);
      if (Number.isFinite(max)) {
        return { min: null, max };
      }
    }

    const greaterThanPattern = /^>?=?\s*(\d+\.?\d*)$/;
    const greaterThanMatch = trimmed.match(greaterThanPattern);
    if (greaterThanMatch) {
      const min = parseFloat(greaterThanMatch[1]);
      if (Number.isFinite(min)) {
        return { min, max: null };
      }
    }

    console.warn("Unable to parse normal range:", rangeStr);
    return null;
  }, []);

  /**
   * Evaluate a single QC sample against its expected range.
   */
  const evaluateQcSample = useCallback(
    (qc) => {
      if (!qc.result || !qc.expectedRange) {
        return { qcStatus: "none" };
      }

      const range = parseRange(qc.expectedRange);
      if (!range || range.min === null || range.max === null) {
        return { qcStatus: "none" };
      }

      const resultValue = parseFloat(qc.result);
      if (!Number.isFinite(resultValue)) {
        return { qcStatus: "none" };
      }

      if (resultValue >= range.min && resultValue <= range.max) {
        return { qcStatus: "pass" };
      } else {
        return {
          qcStatus: "fail",
          failureReason: `Result ${resultValue} ${qc.unit || ""} is outside acceptable range ${qc.expectedRange}`,
        };
      }
    },
    [parseRange],
  );

  /**
   * Compute QC evaluation without side effects (CRIT-003 compliant).
   */
  const computeQcEvaluation = useCallback(
    (qcSamplesList) => {
      if (!qcSamplesList || qcSamplesList.length === 0) {
        return {
          evaluatedSamples: [],
          anyFailed: false,
          allPassed: false,
          status: "none",
        };
      }

      const evaluatedSamples = qcSamplesList.map((qc) => {
        const qcResult = evaluateQcSample(qc);
        return { ...qc, ...qcResult };
      });

      const anyFailed = evaluatedSamples.some((qc) => qc.qcStatus === "fail");
      const allPassed = evaluatedSamples.every((qc) => qc.qcStatus === "pass");

      let status = "none";
      if (anyFailed) {
        status = "fail";
      } else if (allPassed) {
        status = "pass";
      }

      return {
        evaluatedSamples,
        anyFailed,
        allPassed,
        status,
      };
    },
    [evaluateQcSample],
  );

  /**
   * Evaluate QC status and update state appropriately.
   */
  const evaluateQcStatus = useCallback(
    (qcSamplesList) => {
      const evaluation = computeQcEvaluation(qcSamplesList);
      setOverallQcStatus(evaluation.status);
      return evaluation;
    },
    [computeQcEvaluation],
  );

  return {
    overallQcStatus,
    nonConformityId,
    setOverallQcStatus,
    setNonConformityId,
    evaluateQcStatus,
    computeQcEvaluation,
    evaluateQcSample,
    parseRange,
  };
};

export default useQcEvaluation;
