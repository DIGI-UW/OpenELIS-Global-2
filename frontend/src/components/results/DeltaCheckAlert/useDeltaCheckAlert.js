import { useState, useEffect, useCallback, useRef } from "react";
import {
  getDeltaCheckAlerts,
  dismissDeltaCheckAlert,
  escalateDeltaCheckAlert,
} from "../../../services/NCEIntegrationService";

/**
 * Hook for managing delta check alerts for a set of analysis IDs.
 * @param {string[]} analysisIds - Analysis IDs to fetch alerts for
 */
const useDeltaCheckAlert = (analysisIds) => {
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [dismissModalOpen, setDismissModalOpen] = useState(false);
  const [escalateModalOpen, setEscalateModalOpen] = useState(false);
  const [selectedAlert, setSelectedAlert] = useState(null);

  // Stabilize analysisIds by value to prevent infinite re-fetch loops
  // when callers pass inline .slice().map() arrays
  const idsKey = JSON.stringify(analysisIds ?? []);
  const stableIds = useRef(analysisIds);
  useEffect(() => {
    stableIds.current = analysisIds;
  }, [idsKey]); // eslint-disable-line react-hooks/exhaustive-deps

  const fetchAlerts = useCallback(() => {
    const ids = stableIds.current;
    if (!ids?.length) return;

    setLoading(true);
    getDeltaCheckAlerts({ analysisIds: ids, status: "ACTIVE" }, (data) => {
      setAlerts(Array.isArray(data) ? data : []);
      setLoading(false);
    });
  }, [idsKey]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    fetchAlerts();
  }, [fetchAlerts]);

  const openDismissModal = useCallback((alert) => {
    setSelectedAlert(alert);
    setDismissModalOpen(true);
  }, []);

  const closeDismissModal = useCallback(() => {
    setDismissModalOpen(false);
    setSelectedAlert(null);
  }, []);

  const openEscalateModal = useCallback((alert) => {
    setSelectedAlert(alert);
    setEscalateModalOpen(true);
  }, []);

  const closeEscalateModal = useCallback(() => {
    setEscalateModalOpen(false);
    setSelectedAlert(null);
  }, []);

  const dismissAlert = useCallback(
    (alertId, reason, callback) => {
      dismissDeltaCheckAlert(alertId, { reason }, (status) => {
        // putToOpenElisServer returns HTTP status code
        if (status >= 200 && status < 300) {
          fetchAlerts();
          closeDismissModal();
        }
        callback?.(status, status < 200 || status >= 300);
      });
    },
    [fetchAlerts, closeDismissModal],
  );

  const escalateAlert = useCallback(
    (alertId, escalateData, callback) => {
      escalateDeltaCheckAlert(alertId, escalateData, (response) => {
        // postToOpenElisServerJsonResponse returns parsed JSON
        if (!response?.error) {
          fetchAlerts();
          closeEscalateModal();
        }
        callback?.(response, !!response?.error);
      });
    },
    [fetchAlerts, closeEscalateModal],
  );

  return {
    alerts,
    loading,
    selectedAlert,
    dismissModalOpen,
    escalateModalOpen,
    openDismissModal,
    closeDismissModal,
    openEscalateModal,
    closeEscalateModal,
    dismissAlert,
    escalateAlert,
    refreshAlerts: fetchAlerts,
  };
};

export default useDeltaCheckAlert;
