import { useState, useEffect } from "react";
import { getFromOpenElisServer } from "../../utils/Utils";

/**
 * Hook for fetching storage location hierarchy
 * Following OpenELIS pattern: getFromOpenElisServer (NOT SWR)
 */
export const useStorageLocations = (type, parentId) => {
  const [data, setData] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!type) {
      return;
    }

    setIsLoading(true);
    setError(null);

    const url = parentId
      ? `/rest/storage/${type}?parentId=${parentId}`
      : `/rest/storage/${type}`;

    getFromOpenElisServer(
      url,
      (response) => {
        setData(response);
        setIsLoading(false);
      },
      (error) => {
        setError(error);
        setIsLoading(false);
      },
    );
  }, [type, parentId]);

  return { data, isLoading, error };
};

export default useStorageLocations;
