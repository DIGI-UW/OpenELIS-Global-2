import { useState, useEffect, useCallback } from "react";
import { getResultNCEBadge } from "../../../services/NCEIntegrationService";

/**
 * Hook for fetching and managing NCE badge data for a result.
 * @param {string} resultId - Result ID to fetch badge info for
 */
const useNCEBadge = (resultId) => {
  const [badgeData, setBadgeData] = useState(null);
  const [loading, setLoading] = useState(false);

  const fetchBadge = useCallback(() => {
    if (!resultId) return;

    setLoading(true);
    getResultNCEBadge(resultId, (data) => {
      setBadgeData(data?.error ? null : data || null);
      setLoading(false);
    });
  }, [resultId]);

  useEffect(() => {
    fetchBadge();
  }, [fetchBadge]);

  return {
    hasNCE: badgeData?.hasNCE || false,
    nceCount: badgeData?.nceCount || 0,
    highestSeverity: badgeData?.highestSeverity || null,
    nceNumbers: badgeData?.nceNumbers || [],
    loading,
    refreshBadge: fetchBadge,
  };
};

export default useNCEBadge;
