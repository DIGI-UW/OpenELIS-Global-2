/**
 * Serial Port Service API Client
 *
 * Provides methods for CRUD operations on serial port configurations
 * Follows OpenELIS pattern using getFromOpenElisServer, postToOpenElisServerJsonResponse
 */

import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../components/utils/Utils";

/**
 * Get all serial port configurations
 * @param {Function} callback - Callback function (data) => void
 */
export const getSerialPortConfigurations = (callback) => {
  const endpoint = "/rest/analyzer/serial-port/configurations";
  getFromOpenElisServer(endpoint, callback);
};

/**
 * Get serial port configuration by ID
 * @param {String} id - Configuration ID
 * @param {Function} callback - Callback function (data) => void
 */
export const getSerialPortConfiguration = (id, callback) => {
  const endpoint = `/rest/analyzer/serial-port/configurations/${id}`;
  getFromOpenElisServer(endpoint, callback);
};

/**
 * Get serial port configuration by analyzer ID
 * @param {Number} analyzerId - Analyzer ID
 * @param {Function} callback - Callback function (data) => void
 */
export const getSerialPortConfigurationByAnalyzerId = (
  analyzerId,
  callback,
) => {
  const endpoint = `/rest/analyzer/serial-port/configurations/analyzer/${analyzerId}`;
  getFromOpenElisServer(endpoint, callback);
};

/**
 * Create new serial port configuration
 * @param {Object} configData - Configuration data
 * @param {Function} callback - Callback function (response, extraParams) => void
 * @param {*} extraParams - Optional extra parameters passed to callback
 */
export const createSerialPortConfiguration = (
  configData,
  callback,
  extraParams,
) => {
  const endpoint = "/rest/analyzer/serial-port/configurations";
  const payload = JSON.stringify(configData);
  postToOpenElisServerJsonResponse(endpoint, payload, callback, extraParams);
};

/**
 * Update serial port configuration
 * @param {String} id - Configuration ID
 * @param {Object} configData - Configuration data
 * @param {Function} callback - Callback function (response, extraParams) => void
 * @param {*} extraParams - Optional extra parameters passed to callback
 */
export const updateSerialPortConfiguration = (
  id,
  configData,
  callback,
  extraParams,
) => {
  const endpoint = `/rest/analyzer/serial-port/configurations/${id}`;
  const payload = JSON.stringify(configData);

  // Use fetch for PUT request
  fetch(endpoint, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    body: payload,
  })
    .then((response) => response.json())
    .then((data) => {
      if (callback) {
        callback(data, extraParams);
      }
    })
    .catch((error) => {
      if (callback) {
        callback({ error: error.message }, extraParams);
      }
    });
};

/**
 * Delete serial port configuration
 * @param {String} id - Configuration ID
 * @param {Function} callback - Callback function (response, extraParams) => void
 * @param {*} extraParams - Optional extra parameters passed to callback
 */
export const deleteSerialPortConfiguration = (id, callback, extraParams) => {
  const endpoint = `/rest/analyzer/serial-port/configurations/${id}`;

  fetch(endpoint, {
    method: "DELETE",
  })
    .then((response) => response.json())
    .then((data) => {
      if (callback) {
        callback(data, extraParams);
      }
    })
    .catch((error) => {
      if (callback) {
        callback({ error: error.message }, extraParams);
      }
    });
};

/**
 * Open serial port connection
 * @param {String} id - Configuration ID
 * @param {Function} callback - Callback function (response, extraParams) => void
 * @param {*} extraParams - Optional extra parameters passed to callback
 */
export const connectSerialPort = (id, callback, extraParams) => {
  const endpoint = `/rest/analyzer/serial-port/configurations/${id}/connect`;
  postToOpenElisServerJsonResponse(endpoint, "{}", callback, extraParams);
};

/**
 * Close serial port connection
 * @param {String} id - Configuration ID
 * @param {Function} callback - Callback function (response, extraParams) => void
 * @param {*} extraParams - Optional extra parameters passed to callback
 */
export const disconnectSerialPort = (id, callback, extraParams) => {
  const endpoint = `/rest/analyzer/serial-port/configurations/${id}/disconnect`;
  postToOpenElisServerJsonResponse(endpoint, "{}", callback, extraParams);
};

/**
 * Get connection status
 * @param {String} id - Configuration ID
 * @param {Function} callback - Callback function (data) => void
 */
export const getSerialPortStatus = (id, callback) => {
  const endpoint = `/rest/analyzer/serial-port/configurations/${id}/status`;
  getFromOpenElisServer(endpoint, callback);
};
