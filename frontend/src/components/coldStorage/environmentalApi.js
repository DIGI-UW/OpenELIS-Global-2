import {
  getFromOpenElisServerV2,
  postToOpenElisServerJsonResponse,
} from "../utils/Utils";

// Environmental Monitoring API functions following Cold Storage patterns

export const fetchEnvironmentalLogs = async (filters = {}) => {
  const params = new URLSearchParams();
  if (filters.storageUnitType) {
    params.append("storageUnitType", filters.storageUnitType);
  }
  if (filters.limit) {
    params.append("limit", filters.limit);
  }
  if (filters.offset) {
    params.append("offset", filters.offset);
  }
  const queryString = params.toString() ? `?${params.toString()}` : "";

  try {
    const response = await getFromOpenElisServerV2(
      `/rest/environmental-monitoring/logs${queryString}`,
    );
    // getFromOpenElisServerV2 returns the direct response, not wrapped in {success: true}
    return {
      success: true,
      logs: Array.isArray(response) ? response : response?.logs || [],
      count: Array.isArray(response) ? response.length : response?.count || 0,
    };
  } catch (error) {
    console.error("Error fetching environmental logs:", error);
    return {
      success: false,
      logs: [],
      count: 0,
      error: error.message || "Failed to fetch environmental logs",
    };
  }
};

export const fetchDashboardStatistics = async (storageUnitType = null) => {
  try {
    const endpoint = storageUnitType
      ? `/rest/environmental-monitoring/dashboard-stats?storageUnitType=${encodeURIComponent(storageUnitType)}`
      : `/rest/environmental-monitoring/dashboard-stats`;

    const response = await getFromOpenElisServerV2(endpoint);
    return {
      success: true,
      ...response,
    };
  } catch (error) {
    console.error("Error fetching dashboard statistics:", error);
    return {
      success: false,
      error: error.message || "Failed to fetch dashboard statistics",
    };
  }
};

export const createEnvironmentalLog = async (logData) => {
  return new Promise((resolve, reject) => {
    postToOpenElisServerJsonResponse(
      "/rest/environmental-monitoring/log",
      JSON.stringify(logData),
      (response) => {
        if (
          response &&
          (response.status >= 400 || response.statusCode >= 400)
        ) {
          reject(
            new Error(
              response.message ||
                response.error ||
                `Request failed with status ${response.status || response.statusCode}`,
            ),
          );
        } else {
          resolve({
            success: true,
            data: response,
          });
        }
      },
      (error) => {
        reject(new Error(error || "Failed to create environmental log"));
      },
    );
  });
};

export const searchEnvironmentalLogs = async (filters = {}) => {
  const params = new URLSearchParams();

  if (filters.storageUnitType) {
    params.append("storageUnitType", filters.storageUnitType);
  }
  if (filters.storageUnitId) {
    params.append("storageUnitId", filters.storageUnitId);
  }
  if (filters.startDate) {
    params.append("startDate", filters.startDate);
  }
  if (filters.endDate) {
    params.append("endDate", filters.endDate);
  }
  if (filters.checkedBy) {
    params.append("checkedBy", filters.checkedBy);
  }
  if (filters.limit) {
    params.append("limit", filters.limit);
  }
  if (filters.offset) {
    params.append("offset", filters.offset);
  }

  const queryString = params.toString() ? `?${params.toString()}` : "";

  try {
    const response = await getFromOpenElisServerV2(
      `/rest/environmental-monitoring/search${queryString}`,
    );
    return {
      success: true,
      logs: Array.isArray(response) ? response : response?.logs || [],
      count: Array.isArray(response) ? response.length : response?.count || 0,
    };
  } catch (error) {
    console.error("Error searching environmental logs:", error);
    return {
      success: false,
      logs: [],
      count: 0,
      error: error.message || "Failed to search environmental logs",
    };
  }
};

export const fetchTemperatureRanges = async () => {
  try {
    const response = await getFromOpenElisServerV2(
      "/rest/environmental-monitoring/temperature-ranges",
    );
    return {
      success: true,
      ranges: response?.ranges || response || {},
    };
  } catch (error) {
    console.error("Error fetching temperature ranges:", error);
    return {
      success: false,
      ranges: {},
      error: error.message || "Failed to fetch temperature ranges",
    };
  }
};

export const fetchStorageUnitTypes = async () => {
  try {
    const response = await getFromOpenElisServerV2(
      "/rest/environmental-monitoring/storage-unit-types",
    );
    return {
      success: true,
      storageUnitTypes: Array.isArray(response)
        ? response
        : response?.storageUnitTypes || [],
    };
  } catch (error) {
    console.error("Error fetching storage unit types:", error);
    return {
      success: false,
      storageUnitTypes: [],
      error: error.message || "Failed to fetch storage unit types",
    };
  }
};

export const fetchSampleTypes = async () => {
  try {
    const response = await getFromOpenElisServerV2(
      "/rest/displayList/SAMPLE_TYPE_ACTIVE",
    );
    return {
      success: true,
      sampleTypes: Array.isArray(response) ? response : [],
    };
  } catch (error) {
    console.error("Error fetching sample types:", error);
    return {
      success: false,
      sampleTypes: [],
      error: error.message || "Failed to fetch sample types",
    };
  }
};

export const fetchProjects = async () => {
  try {
    const response = await getFromOpenElisServerV2(
      "/rest/notebook/dashboard/notebooks",
    );
    return {
      success: true,
      projects: Array.isArray(response) ? response : [],
    };
  } catch (error) {
    console.error("Error fetching projects:", error);
    return {
      success: false,
      projects: [],
      error: error.message || "Failed to fetch projects",
    };
  }
};

export const searchSampleIds = async (filters = {}) => {
  const params = new URLSearchParams();

  if (filters.projectName) {
    params.append("projectName", filters.projectName);
  }
  if (filters.sampleType) {
    params.append("sampleType", filters.sampleType);
  }
  if (filters.query) {
    params.append("query", filters.query);
  }
  if (filters.limit) {
    params.append("limit", filters.limit);
  }

  const queryString = params.toString() ? `?${params.toString()}` : "";

  try {
    // TODO: This endpoint doesn't exist yet - need to implement or use existing sample search
    const response = await getFromOpenElisServerV2(
      `/rest/environmental-monitoring/sample-id-lookup${queryString}`,
    );
    return {
      success: true,
      sampleIds: Array.isArray(response) ? response : response?.sampleIds || [],
      count: Array.isArray(response) ? response.length : response?.count || 0,
    };
  } catch (error) {
    console.error("Error searching sample IDs:", error);
    return {
      success: false,
      sampleIds: [],
      count: 0,
      error: error.message || "Failed to search sample IDs",
    };
  }
};
