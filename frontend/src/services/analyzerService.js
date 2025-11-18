/**
 * Analyzer Service API Client
 *
 * Provides methods for CRUD operations on analyzers and analyzer field mappings
 * Follows OpenELIS pattern using getFromOpenElisServer, postToOpenElisServerJsonResponse, and fetch for PUT/DELETE
 *
 * Task Reference: T065
 * Pattern Reference: AGENTS.md Section 5 (Frontend Data Fetching Pattern)
 */

import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../components/utils/Utils";
import config from "../config.json";

/**
 * Get all analyzers with optional filters
 * @param {Object} filters - Optional filters { status, search }
 * @param {Function} callback - Callback function (data) => void
 */
export const getAnalyzers = (filters, callback) => {
  let endpoint = "/rest/analyzer/analyzers";
  const params = new URLSearchParams();

  if (filters) {
    if (filters.status) {
      params.append("status", filters.status);
    }
    if (filters.search) {
      params.append("search", filters.search);
    }
  }

  if (params.toString()) {
    endpoint += "?" + params.toString();
  }

  getFromOpenElisServer(endpoint, callback);
};

/**
 * Get analyzer by ID
 * @param {String} id - Analyzer ID
 * @param {Function} callback - Callback function (data) => void
 */
export const getAnalyzer = (id, callback) => {
  const endpoint = `/rest/analyzer/analyzers/${id}`;
  getFromOpenElisServer(endpoint, callback);
};

/**
 * Create new analyzer
 * @param {Object} analyzerData - Analyzer data { name, analyzerType, ipAddress, port, testUnitIds, active }
 * @param {Function} callback - Callback function (response, extraParams) => void
 * @param {*} extraParams - Optional extra parameters passed to callback
 */
export const createAnalyzer = (analyzerData, callback, extraParams) => {
  const endpoint = "/rest/analyzer/analyzers";
  const payload = JSON.stringify(analyzerData);
  postToOpenElisServerJsonResponse(endpoint, payload, callback, extraParams);
};

/**
 * Update analyzer
 * @param {String} id - Analyzer ID
 * @param {Object} analyzerData - Analyzer data to update
 * @param {Function} callback - Callback function (response, extraParams) => void
 * @param {*} extraParams - Optional extra parameters passed to callback
 */
export const updateAnalyzer = (id, analyzerData, callback, extraParams) => {
  const endpoint = `/rest/analyzer/analyzers/${id}`;
  const payload = JSON.stringify(analyzerData);

  // Use fetch directly to get JSON response (controllers return Map<String, Object>)
  fetch(config.serverBaseUrl + endpoint, {
    credentials: "include",
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      "X-CSRF-Token": localStorage.getItem("CSRF"),
    },
    body: payload,
  })
    .then(async (response) => {
      if (!response.ok) {
        // For error responses, try to parse JSON error message
        const errorJson = await response.json().catch(() => ({
          error: `HTTP ${response.status}: ${response.statusText}`,
        }));
        callback(
          {
            ...errorJson,
            status: response.status,
            statusCode: response.status,
            statusText: response.statusText,
          },
          extraParams,
        );
        return;
      }
      // For successful responses, parse JSON normally
      const json = await response.json();
      callback(json, extraParams);
    })
    .catch((error) => {
      console.error("updateAnalyzer error:", error);
      callback(
        {
          error: error.message || "Network error",
          message: error.message || "Network error",
          status: 0,
        },
        extraParams,
      );
    });
};

/**
 * Delete analyzer (soft delete - sets active=false)
 * @param {String} id - Analyzer ID
 * @param {Function} callback - Callback function (success, error) => void
 */
export const deleteAnalyzer = (id, callback) => {
  const endpoint = `/rest/analyzer/analyzers/${id}`;

  fetch(config.serverBaseUrl + endpoint, {
    credentials: "include",
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
      "X-CSRF-Token": localStorage.getItem("CSRF"),
    },
  })
    .then(async (response) => {
      if (response.ok || response.status === 204) {
        callback(true, null);
      } else {
        const errorData = await response.json().catch(() => ({
          error: `HTTP ${response.status}: ${response.statusText}`,
        }));
        callback(false, errorData);
      }
    })
    .catch((error) => {
      console.error("deleteAnalyzer error:", error);
      callback(false, { error: error.message || "Network error" });
    });
};

/**
 * Test TCP connection to analyzer
 * @param {String} id - Analyzer ID
 * @param {Function} callback - Callback function (response, extraParams) => void
 * @param {*} extraParams - Optional extra parameters passed to callback
 */
export const testConnection = (id, callback, extraParams) => {
  const endpoint = `/rest/analyzer/analyzers/${id}/test-connection`;
  // POST with empty body
  postToOpenElisServerJsonResponse(
    endpoint,
    JSON.stringify({}),
    callback,
    extraParams,
  );
};

/**
 * Query analyzer for available fields (ASTM query)
 * Note: This endpoint is not yet implemented (FR-002), placeholder for future use
 * @param {String} id - Analyzer ID
 * @param {Function} callback - Callback function (response, extraParams) => void
 * @param {*} extraParams - Optional extra parameters passed to callback
 */
export const queryAnalyzer = (id, callback, extraParams) => {
  // TODO: Implement when FR-002 is implemented
  // Endpoint will be: POST /rest/analyzer/analyzers/{id}/query
  const endpoint = `/rest/analyzer/analyzers/${id}/query`;
  postToOpenElisServerJsonResponse(
    endpoint,
    JSON.stringify({}),
    callback,
    extraParams,
  );
};

/**
 * Get all field mappings for an analyzer
 * @param {String} analyzerId - Analyzer ID
 * @param {Function} callback - Callback function (data) => void
 */
export const getMappings = (analyzerId, callback) => {
  const endpoint = `/rest/analyzer/analyzers/${analyzerId}/mappings`;
  getFromOpenElisServer(endpoint, callback);
};

/**
 * Create new field mapping
 * @param {String} analyzerId - Analyzer ID
 * @param {Object} mappingData - Mapping data { analyzerFieldId, openelisFieldId, openelisFieldType, mappingType, isRequired, isActive, specimenTypeConstraint, panelConstraint }
 * @param {Function} callback - Callback function (response, extraParams) => void
 * @param {*} extraParams - Optional extra parameters passed to callback
 */
export const createMapping = (
  analyzerId,
  mappingData,
  callback,
  extraParams,
) => {
  const endpoint = `/rest/analyzer/analyzers/${analyzerId}/mappings`;
  const payload = JSON.stringify(mappingData);
  postToOpenElisServerJsonResponse(endpoint, payload, callback, extraParams);
};

/**
 * Update field mapping
 * @param {String} analyzerId - Analyzer ID
 * @param {String} mappingId - Mapping ID
 * @param {Object} mappingData - Mapping data to update
 * @param {Function} callback - Callback function (response, extraParams) => void
 * @param {*} extraParams - Optional extra parameters passed to callback
 */
export const updateMapping = (
  analyzerId,
  mappingId,
  mappingData,
  callback,
  extraParams,
) => {
  const endpoint = `/rest/analyzer/analyzers/${analyzerId}/mappings/${mappingId}`;
  const payload = JSON.stringify(mappingData);

  // Use fetch directly to get JSON response (controllers return Map<String, Object>)
  fetch(config.serverBaseUrl + endpoint, {
    credentials: "include",
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      "X-CSRF-Token": localStorage.getItem("CSRF"),
    },
    body: payload,
  })
    .then(async (response) => {
      if (!response.ok) {
        // For error responses, try to parse JSON error message
        const errorJson = await response.json().catch(() => ({
          error: `HTTP ${response.status}: ${response.statusText}`,
        }));
        callback(
          {
            ...errorJson,
            status: response.status,
            statusCode: response.status,
            statusText: response.statusText,
          },
          extraParams,
        );
        return;
      }
      // For successful responses, parse JSON normally
      const json = await response.json();
      callback(json, extraParams);
    })
    .catch((error) => {
      console.error("updateMapping error:", error);
      callback(
        {
          error: error.message || "Network error",
          message: error.message || "Network error",
          status: 0,
        },
        extraParams,
      );
    });
};

/**
 * Delete field mapping
 * @param {String} analyzerId - Analyzer ID
 * @param {String} mappingId - Mapping ID
 * @param {Function} callback - Callback function (success, error) => void
 */
export const deleteMapping = (analyzerId, mappingId, callback) => {
  const endpoint = `/rest/analyzer/analyzers/${analyzerId}/mappings/${mappingId}`;

  fetch(config.serverBaseUrl + endpoint, {
    credentials: "include",
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
      "X-CSRF-Token": localStorage.getItem("CSRF"),
    },
  })
    .then(async (response) => {
      if (response.ok || response.status === 204) {
        callback(true, null);
      } else {
        const errorData = await response.json().catch(() => ({
          error: `HTTP ${response.status}: ${response.statusText}`,
        }));
        callback(false, errorData);
      }
    })
    .catch((error) => {
      console.error("deleteMapping error:", error);
      callback(false, { error: error.message || "Network error" });
    });
};
