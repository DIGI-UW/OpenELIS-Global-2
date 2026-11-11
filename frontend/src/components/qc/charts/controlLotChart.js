/**
 * Shared plumbing for the lot-scoped Levey-Jennings chart.
 *
 * The control lot list, the instrument Control Chart tab and the chart detail
 * page all read the same two endpoints and render the same LeveyJenningsChart,
 * so the transform, the paired fetch and the loading gate live here once.
 */

import { useCallback, useRef, useState } from "react";
import { getFromOpenElisServer } from "../../utils/Utils";

/** Control level filter, shared by the chart detail page and the export modal. */
export const CONTROL_LEVEL_OPTIONS = [
  { id: "ALL", labelKey: "qc.chart.filter.allLevels" },
  { id: "LOW", labelKey: "qc.controlLot.level.low" },
  { id: "NORMAL", labelKey: "qc.controlLot.level.normal" },
  { id: "HIGH", labelKey: "qc.controlLot.level.high" },
];

export const controlLevelItems = (intl) =>
  CONTROL_LEVEL_OPTIONS.map((o) => ({
    id: o.id,
    label: intl.formatMessage({ id: o.labelKey }),
  }));

/** Backend data point shape -> the shape LeveyJenningsChart expects. */
export const transformDataPoints = (dataPoints) =>
  (dataPoints || []).map((pt) => ({
    id: pt.resultId,
    runDateTime: pt.timestamp,
    resultValue: pt.value,
    value: pt.value,
    zScore: pt.zscore ?? pt.zScore,
    controlLevel: pt.controlLevel,
    violated: pt.hasViolation,
    violations: (pt.violatedRules || []).map((rule) => ({ code: rule })),
  }));

/**
 * Loads one control lot's chart points and statistics in parallel, clearing the
 * loading flag only once both calls have answered.
 *
 * Each load supersedes the one before it, so a slow response for a lot the user
 * has already navigated away from is dropped rather than rendered under the new
 * lot's heading.
 *
 * @param {boolean} initialLoading whether the caller is already fetching on mount
 */
export const useControlLotChart = (initialLoading = false) => {
  const [chartData, setChartData] = useState([]);
  const [statistics, setStatistics] = useState(null);
  const [loading, setLoading] = useState(initialLoading);
  const requestSeq = useRef(0);

  const load = useCallback((controlLotId, query = "") => {
    const seq = ++requestSeq.current;
    setChartData([]);
    setStatistics(null);

    if (!controlLotId) {
      setLoading(false);
      return;
    }
    setLoading(true);

    let completedCalls = 0;
    const checkDone = () => {
      if (++completedCalls >= 2) setLoading(false);
    };

    getFromOpenElisServer(
      `/rest/qc/charts/${controlLotId}${query}`,
      (response) => {
        if (seq !== requestSeq.current) return;
        setChartData(
          transformDataPoints(
            response?.dataPoints || response?.data?.dataPoints || [],
          ),
        );
        checkDone();
      },
    );

    getFromOpenElisServer(
      `/rest/qc/charts/${controlLotId}/statistics`,
      (response) => {
        if (seq !== requestSeq.current) return;
        setStatistics(response && response.mean != null ? response : null);
        checkDone();
      },
    );
  }, []);

  return { chartData, statistics, loading, setLoading, load };
};
