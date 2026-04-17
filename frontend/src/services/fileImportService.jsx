/**
 * File Import Service API Client
 *
 * Provides methods for CRUD operations on file import configurations
 * Follows OpenELIS pattern using getFromOpenElisServer, postToOpenElisServerJsonResponse, and fetch for PUT/DELETE
 *
 * Pattern Reference: AGENTS.md Section 5 (Frontend Data Fetching Pattern)
 */

import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../components/utils/Utils";
import config from "../config.json";

/**
 * Get all file import configurations
 * @param {Boolean} active - Optional filter for active configurations only
 * @param {Function} callback - Callback function (data) => void
 */
export const getAllConfigurations = (active, callback) => {
  let endpoint = "/rest/analyzer/file-import/configurations";
  const params = new URLSearchParams();

  if (active !== undefined && active !== null) {
    params.append("active", active);
  }

  if (params.toString()) {
    endpoint += "?" + params.toString();
  }

  getFromOpenElisServer(endpoint, callback);
};

/**
 * Get file import configuration by ID
 * @param {String} id - Configuration ID
 * @param {Function} callback - Callback function (data) => void
 */
export const getConfiguration = (id, callback) => {
  const endpoint = `/rest/analyzer/file-import/configurations/${id}`;
  getFromOpenElisServer(endpoint, callback);
};

/**
 * Get file import configuration by analyzer ID
 * @param {Number} analyzerId - Analyzer ID
 * @param {Function} callback - Callback function (data) => void
 */
export const getConfigurationByAnalyzerId = (analyzerId, callback) => {
  const endpoint = `/rest/analyzer/file-import/configurations/analyzer/${analyzerId}`;
  getFromOpenElisServer(endpoint, callback);
};

/**
 * Create new file import configuration
 * @param {Object} configuration - Configuration data
 * @param {Function} callback - Callback function (response, extraParams) => void
 * @param {*} extraParams - Optional extra parameters passed to callback
 */
export const createConfiguration = (configuration, callback, extraParams) => {
  const endpoint = "/rest/analyzer/file-import/configurations";
  const payload = JSON.stringify(configuration);
  postToOpenElisServerJsonResponse(endpoint, payload, callback, extraParams);
};

/**
 * Update file import configuration
 * @param {String} id - Configuration ID
 * @param {Object} configuration - Configuration data
 * @param {Function} callback - Callback function (response, extraParams) => void
 * @param {*} extraParams - Optional extra parameters passed to callback
 */
export const updateConfiguration = (
  id,
  configuration,
  callback,
  extraParams,
) => {
  const endpoint = `/rest/analyzer/file-import/configurations/${id}`;
  const payload = JSON.stringify(configuration);

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
 * Delete file import configuration
 * @param {String} id - Configuration ID
 * @param {Function} callback - Callback function (response, extraParams) => void
 * @param {*} extraParams - Optional extra parameters passed to callback
 */
export const deleteConfiguration = (id, callback, extraParams) => {
  const endpoint = `/rest/analyzer/file-import/configurations/${id}`;
  const csrfToken = localStorage.getItem("CSRF");

  fetch(config.serverBaseUrl + endpoint, {
    credentials: "include",
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
      "X-CSRF-Token": csrfToken,
    },
  })
    .then(async (response) => {
      // Read response body if present
      let responseData = null;
      try {
        const contentType = response.headers.get("content-type");
        if (contentType && contentType.indexOf("application/json") !== -1) {
          responseData = await response.json();
        } else if (response.status !== 204) {
          await response.text();
        }
      } catch (e) {
        // Ignore errors reading response
      }

      if (!response.ok) {
        callback(
          {
            ...responseData,
            status: response.status,
            statusCode: response.status,
            statusText: response.statusText,
          },
          extraParams,
        );
        return;
      }

      callback(
        responseData || { message: "Configuration deleted successfully" },
        extraParams,
      );
    })
    .catch((error) => {
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
