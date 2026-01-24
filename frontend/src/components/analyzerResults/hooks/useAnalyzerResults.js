import { useState, useCallback, useRef, useEffect } from "react";
import { getFromOpenElisServer } from "../../utils/Utils";

/**
 * Custom hook for managing analyzer results data fetching and state.
 * Implements Promise.all batching for efficient API calls (MAJ-001).
 *
 * @param {string} analyzerType - The type of analyzer to fetch results for
 * @returns {Object} State and methods for analyzer results management
 */
export const useAnalyzerResults = (analyzerType) => {
  const [loading, setLoading] = useState(false);
  const [results, setResults] = useState([]);
  const [qcSamples, setQcSamples] = useState([]);
  const [runSettings, setRunSettings] = useState(null);
  const [qcHistory, setQcHistory] = useState([]);
  const [analyzerInfo, setAnalyzerInfo] = useState(null);
  const [reagentStatus, setReagentStatus] = useState([]);
  const [error, setError] = useState(null);

  const isMountedRef = useRef(true);

  useEffect(() => {
    isMountedRef.current = true;
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  /**
   * Wrapper to convert callback-based API to Promise
   */
  const fetchAsync = (url) => {
    return new Promise((resolve, reject) => {
      getFromOpenElisServer(url, (response, error) => {
        if (error) {
          reject(error);
        } else {
          resolve(response);
        }
      });
    });
  };

  /**
   * Fetch all analyzer data using Promise.all for batched requests (MAJ-001 fix).
   * This replaces 5 sequential API calls with parallel execution.
   */
  const fetchAllData = useCallback(async () => {
    if (!analyzerType) return;

    setLoading(true);
    setError(null);

    try {
      const [
        resultsResponse,
        runSettingsResponse,
        qcHistoryResponse,
        analyzerInfoResponse,
        reagentsResponse,
      ] = await Promise.all([
        fetchAsync(`/rest/AnalyzerResults?type=${analyzerType}`),
        fetchAsync(`/rest/AnalyzerResults/runSettings?type=${analyzerType}`),
        fetchAsync(`/rest/AnalyzerResults/qcHistory?type=${analyzerType}`),
        fetchAsync(`/rest/AnalyzerResults/analyzerInfo?type=${analyzerType}`),
        fetchAsync(`/rest/AnalyzerResults/reagents?type=${analyzerType}`),
      ]);

      if (!isMountedRef.current) return;

      // Process results
      if (resultsResponse) {
        const qcSamplesList =
          resultsResponse.resultList?.filter((r) => r.isControl) || [];
        const patientResults =
          resultsResponse.resultList?.filter((r) => !r.isControl) || [];
        setQcSamples(qcSamplesList);
        setResults(patientResults);
      }

      // Process run settings
      if (runSettingsResponse) {
        setRunSettings(runSettingsResponse);
      }

      // Process QC history
      if (qcHistoryResponse) {
        setQcHistory(qcHistoryResponse.history || []);
      }

      // Process analyzer info
      if (analyzerInfoResponse) {
        setAnalyzerInfo(analyzerInfoResponse);
      }

      // Process reagent status
      if (reagentsResponse) {
        setReagentStatus(reagentsResponse.reagents || []);
      }
    } catch (err) {
      if (isMountedRef.current) {
        console.error("Failed to fetch analyzer data:", err);
        setError(err);
      }
    } finally {
      if (isMountedRef.current) {
        setLoading(false);
      }
    }
  }, [analyzerType]);

  /**
   * Update results state
   */
  const updateResults = useCallback((updater) => {
    setResults((prev) =>
      typeof updater === "function" ? updater(prev) : updater,
    );
  }, []);

  /**
   * Update QC samples state
   */
  const updateQcSamples = useCallback((updater) => {
    setQcSamples((prev) =>
      typeof updater === "function" ? updater(prev) : updater,
    );
  }, []);

  return {
    loading,
    results,
    qcSamples,
    runSettings,
    qcHistory,
    analyzerInfo,
    reagentStatus,
    error,
    fetchAllData,
    updateResults,
    updateQcSamples,
    setRunSettings,
  };
};

export default useAnalyzerResults;
