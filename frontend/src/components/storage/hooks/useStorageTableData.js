import { useCallback, useEffect, useState } from "react";
import { getFromOpenElisServer } from "../../utils/Utils";

/**
 * useStorageTableData — shared paginated-table fetch for the per-resource
 * storage pages (SampleItemsPage, RoomsPage, DevicesPage, etc.).
 *
 * Replaces the inline pagination + search plumbing scattered across
 * StorageDashboard.jsx (4,902 LoC). Each page passes:
 *   - listUrl   : GET endpoint for the paginated list (e.g. '/rest/storage/sample-items')
 *   - searchUrl : GET endpoint for a full-text search (e.g. '/rest/storage/sample-items/search')
 *
 * The backend endpoints used by the storage pages return either:
 *   - an array (legacy format) → wrapped as { items, totalItems }
 *   - { items, totalItems|totalElements } (OGC-150 paginated format)
 * Both shapes are normalized here so the callers see a single contract.
 *
 * @returns {Object} { items, totalItems, loading, error, refetch, refreshToken }
 */
export default function useStorageTableData({
  listUrl,
  searchUrl,
  page = 1,
  pageSize = 25,
  searchTerm = "",
  filterStatus,
  locationFilter,
  refreshKey,
}) {
  const [items, setItems] = useState([]);
  const [totalItems, setTotalItems] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const buildUrl = useCallback(() => {
    const trimmed = (searchTerm || "").trim();
    if (trimmed && searchUrl) {
      return `${searchUrl}?q=${encodeURIComponent(trimmed)}`;
    }
    const params = new URLSearchParams();
    params.append("page", String(page - 1)); // backend expects 0-based
    params.append("size", String(pageSize));
    if (filterStatus) params.append("status", filterStatus);
    if (locationFilter && locationFilter.name) {
      params.append("location", locationFilter.name);
    }
    const qs = params.toString();
    return `${listUrl}${qs ? `?${qs}` : ""}`;
  }, [
    listUrl,
    searchUrl,
    page,
    pageSize,
    searchTerm,
    filterStatus,
    locationFilter,
  ]);

  const fetchData = useCallback(() => {
    setLoading(true);
    setError(null);
    const url = buildUrl();
    getFromOpenElisServer(url, (response) => {
      if (Array.isArray(response)) {
        setItems(response);
        setTotalItems(response.length);
      } else if (response && Array.isArray(response.items)) {
        setItems(response.items);
        setTotalItems(
          response.totalItems ??
            response.totalElements ??
            response.items.length,
        );
      } else {
        setItems([]);
        setTotalItems(0);
        if (response && typeof response === "object") {
          setError(
            response.error || response.message || "Unexpected response format",
          );
        }
      }
      setLoading(false);
    });
  }, [buildUrl]);

  useEffect(() => {
    fetchData();
  }, [fetchData, refreshKey]);

  return { items, totalItems, loading, error, refetch: fetchData };
}
