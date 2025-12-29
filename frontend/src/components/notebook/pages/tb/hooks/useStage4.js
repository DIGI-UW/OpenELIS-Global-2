import { useState, useEffect, useCallback } from "react";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../../utils/Utils";

/**
 * Hook for fetching and managing media batches.
 */
export function useMediaBatches() {
  const [mediaBatches, setMediaBatches] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchMediaBatches = useCallback(() => {
    setLoading(true);
    setError(null);
    getFromOpenElisServer("/rest/tb/processing/media", (response) => {
      if (response && Array.isArray(response)) {
        setMediaBatches(response);
      } else {
        setError("Failed to load media batches");
      }
      setLoading(false);
    });
  }, []);

  const fetchAvailableMedia = useCallback((mediaType) => {
    setLoading(true);
    setError(null);
    const url = mediaType
      ? `/rest/tb/processing/media/available?mediaType=${mediaType}`
      : "/rest/tb/processing/media/available";
    getFromOpenElisServer(url, (response) => {
      if (response && Array.isArray(response)) {
        setMediaBatches(response);
      }
      setLoading(false);
    });
  }, []);

  const createMediaBatch = useCallback(
    (batchData, onSuccess, onError) => {
      postToOpenElisServer(
        "/rest/tb/processing/media",
        JSON.stringify(batchData),
        (response) => {
          if (response && !response.error) {
            fetchMediaBatches();
            if (onSuccess) onSuccess(response);
          } else {
            if (onError) onError(response?.error || "Failed to create batch");
          }
        },
      );
    },
    [fetchMediaBatches],
  );

  const updateQcStatus = useCallback(
    (id, status, notes, onSuccess, onError) => {
      postToOpenElisServer(
        `/rest/tb/processing/media/${id}/qc-status`,
        JSON.stringify({ status, notes }),
        (response) => {
          if (response && !response.error) {
            fetchMediaBatches();
            if (onSuccess) onSuccess(response);
          } else {
            if (onError)
              onError(response?.error || "Failed to update QC status");
          }
        },
        "PUT",
      );
    },
    [fetchMediaBatches],
  );

  useEffect(() => {
    fetchMediaBatches();
  }, [fetchMediaBatches]);

  return {
    mediaBatches,
    loading,
    error,
    fetchMediaBatches,
    fetchAvailableMedia,
    createMediaBatch,
    updateQcStatus,
  };
}

/**
 * Hook for fetching and managing sample processing records.
 */
export function useProcessedSamples() {
  const [samples, setSamples] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchByStatus = useCallback((status) => {
    setLoading(true);
    setError(null);
    getFromOpenElisServer(
      `/rest/tb/processing/sample/by-status/${status}`,
      (response) => {
        if (response && Array.isArray(response)) {
          setSamples(response);
        } else {
          setError("Failed to load samples");
        }
        setLoading(false);
      },
    );
  }, []);

  const fetchReadyForInoculation = useCallback(() => {
    setLoading(true);
    setError(null);
    getFromOpenElisServer(
      "/rest/tb/processing/sample/ready-for-inoculation",
      (response) => {
        if (response && Array.isArray(response)) {
          setSamples(response);
        }
        setLoading(false);
      },
    );
  }, []);

  const processSample = useCallback((processingData, onSuccess, onError) => {
    postToOpenElisServer(
      "/rest/tb/processing/sample",
      JSON.stringify(processingData),
      (response) => {
        if (response && !response.error) {
          if (onSuccess) onSuccess(response);
        } else {
          if (onError) onError(response?.error || "Failed to process sample");
        }
      },
    );
  }, []);

  const batchProcess = useCallback(
    (sampleItemIds, method, onSuccess, onError) => {
      postToOpenElisServer(
        "/rest/tb/processing/sample/batch",
        JSON.stringify({ sampleItemIds, method }),
        (response) => {
          if (response && !response.error) {
            if (onSuccess) onSuccess(response);
          } else {
            if (onError) onError(response?.error || "Failed to batch process");
          }
        },
      );
    },
    [],
  );

  const markReadyForInoculation = useCallback((id, onSuccess, onError) => {
    postToOpenElisServer(
      `/rest/tb/processing/sample/${id}/ready`,
      JSON.stringify({}),
      (response) => {
        if (response && !response.error) {
          if (onSuccess) onSuccess(response);
        } else {
          if (onError) onError(response?.error || "Failed to update status");
        }
      },
      "PUT",
    );
  }, []);

  return {
    samples,
    loading,
    error,
    fetchByStatus,
    fetchReadyForInoculation,
    processSample,
    batchProcess,
    markReadyForInoculation,
  };
}

/**
 * Hook for inoculation operations.
 */
export function useInoculation() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const inoculateSample = useCallback(
    (sampleItemId, mediaBatchId, processingId, onSuccess, onError) => {
      setLoading(true);
      setError(null);
      postToOpenElisServer(
        "/rest/tb/processing/inoculate",
        JSON.stringify({ sampleItemId, mediaBatchId, processingId }),
        (response) => {
          setLoading(false);
          if (response && !response.error) {
            if (onSuccess) onSuccess(response);
          } else {
            const errMsg = response?.error || "Failed to inoculate sample";
            setError(errMsg);
            if (onError) onError(errMsg);
          }
        },
      );
    },
    [],
  );

  const getInoculatedSamples = useCallback((callback) => {
    getFromOpenElisServer("/rest/tb/processing/inoculated", callback);
  }, []);

  return {
    loading,
    error,
    inoculateSample,
    getInoculatedSamples,
  };
}

/**
 * Hook for incubation monitoring (weekly readings).
 */
export function useIncubationMonitoring() {
  const [incubatingSamples, setIncubatingSamples] = useState([]);
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchIncubatingSamples = useCallback(() => {
    setLoading(true);
    setError(null);
    getFromOpenElisServer("/rest/tb/incubation/samples", (response) => {
      if (response && Array.isArray(response)) {
        setIncubatingSamples(response);
      } else {
        setError("Failed to load incubating samples");
      }
      setLoading(false);
    });
  }, []);

  const fetchSummary = useCallback(() => {
    getFromOpenElisServer("/rest/tb/incubation/summary", (response) => {
      if (response) {
        setSummary(response);
      }
    });
  }, []);

  const fetchReadingsForSample = useCallback((sampleItemId, callback) => {
    getFromOpenElisServer(
      `/rest/tb/incubation/samples/${sampleItemId}/readings`,
      callback,
    );
  }, []);

  const fetchByCultureResult = useCallback((result, callback) => {
    getFromOpenElisServer(
      `/rest/tb/incubation/samples/by-result/${result}`,
      callback,
    );
  }, []);

  const fetchCulturePositiveSamples = useCallback((callback) => {
    getFromOpenElisServer("/rest/tb/incubation/samples/positive", callback);
  }, []);

  const recordReading = useCallback(
    (cultureReadingId, weekNumber, observation, notes, onSuccess, onError) => {
      postToOpenElisServer(
        "/rest/tb/incubation/reading",
        JSON.stringify({ cultureReadingId, weekNumber, observation, notes }),
        (response) => {
          if (response && !response.error) {
            fetchIncubatingSamples();
            fetchSummary();
            if (onSuccess) onSuccess(response);
          } else {
            if (onError) onError(response?.error || "Failed to record reading");
          }
        },
      );
    },
    [fetchIncubatingSamples, fetchSummary],
  );

  const markPositive = useCallback(
    (id, positiveWeek, onSuccess, onError) => {
      postToOpenElisServer(
        `/rest/tb/incubation/result/${id}/positive`,
        JSON.stringify({ positiveWeek }),
        (response) => {
          if (response && !response.error) {
            fetchIncubatingSamples();
            fetchSummary();
            if (onSuccess) onSuccess(response);
          } else {
            if (onError) onError(response?.error || "Failed to mark positive");
          }
        },
        "PUT",
      );
    },
    [fetchIncubatingSamples, fetchSummary],
  );

  const markNegative = useCallback(
    (id, onSuccess, onError) => {
      postToOpenElisServer(
        `/rest/tb/incubation/result/${id}/negative`,
        JSON.stringify({}),
        (response) => {
          if (response && !response.error) {
            fetchIncubatingSamples();
            fetchSummary();
            if (onSuccess) onSuccess(response);
          } else {
            if (onError) onError(response?.error || "Failed to mark negative");
          }
        },
        "PUT",
      );
    },
    [fetchIncubatingSamples, fetchSummary],
  );

  const markContaminated = useCallback(
    (id, onSuccess, onError) => {
      postToOpenElisServer(
        `/rest/tb/incubation/result/${id}/contaminated`,
        JSON.stringify({}),
        (response) => {
          if (response && !response.error) {
            fetchIncubatingSamples();
            fetchSummary();
            if (onSuccess) onSuccess(response);
          } else {
            if (onError)
              onError(response?.error || "Failed to mark contaminated");
          }
        },
        "PUT",
      );
    },
    [fetchIncubatingSamples, fetchSummary],
  );

  useEffect(() => {
    fetchIncubatingSamples();
    fetchSummary();
  }, [fetchIncubatingSamples, fetchSummary]);

  return {
    incubatingSamples,
    summary,
    loading,
    error,
    fetchIncubatingSamples,
    fetchSummary,
    fetchReadingsForSample,
    fetchByCultureResult,
    fetchCulturePositiveSamples,
    recordReading,
    markPositive,
    markNegative,
    markContaminated,
  };
}

/**
 * Hook for processing statistics.
 */
export function useProcessingStatistics() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(false);

  const fetchStats = useCallback(() => {
    setLoading(true);
    getFromOpenElisServer("/rest/tb/processing/statistics", (response) => {
      if (response) {
        setStats(response);
      }
      setLoading(false);
    });
  }, []);

  useEffect(() => {
    fetchStats();
  }, [fetchStats]);

  return {
    stats,
    loading,
    fetchStats,
  };
}
