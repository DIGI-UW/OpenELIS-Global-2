/**
 * API client for Site Branding endpoints
 * 
 * Task Reference: T022
 */

import {
  getFromOpenElisServer,
  postToOpenElisServer,
  deleteFromOpenElisServerFullResponse,
} from "../components/utils/Utils";

/**
 * Get current branding configuration
 * @param {Function} callback - Callback function to handle response
 */
export const getBranding = (callback) => {
  getFromOpenElisServer("/rest/site-branding/", callback);
};

/**
 * Update branding configuration
 * @param {Object} formData - Branding configuration data
 * @param {Function} callback - Callback function to handle response
 * @param {Object} extraParams - Additional parameters to pass to callback
 */
export const updateBranding = (formData, callback, extraParams) => {
  const payload = JSON.stringify(formData);
  postToOpenElisServer("/rest/site-branding/", payload, callback, extraParams);
};

/**
 * Remove logo file
 * @param {String} type - Logo type (header, login, favicon)
 * @param {Function} callback - Callback function to handle response
 * @param {Object} extraParams - Additional parameters to pass to callback
 */
export const removeLogo = (type, callback, extraParams) => {
  deleteFromOpenElisServerFullResponse(`/rest/site-branding/logo/${type}`, callback, extraParams);
};

/**
 * Reset all branding to default values
 * @param {Function} callback - Callback function to handle response
 * @param {Object} extraParams - Additional parameters to pass to callback
 */
export const resetBranding = (callback, extraParams) => {
  const payload = JSON.stringify({});
  postToOpenElisServer("/rest/site-branding/reset", payload, callback, extraParams);
};

