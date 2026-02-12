/**
 * Analyzer Service API Client
 *
 * Provides methods for CRUD operations on analyzers and analyzer field mappings
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
 * Preview mapping for analyzer
 * @param {String} analyzerId - Analyzer ID
 * @param {Object} previewData - Preview data { astmMessage, includeDetailedParsing, validateAllMappings }
 * @param {Function} callback - Callback function (response, extraParams) => void
 * @param {*} extraParams - Optional extra parameters passed to callback
 */
export const previewMapping = (
  analyzerId,
  previewData,
  callback,
  extraParams,
) => {
  const endpoint = `/rest/analyzer/analyzers/${analyzerId}/preview-mapping`;
  const payload = JSON.stringify(previewData);
  postToOpenElisServerJsonResponse(endpoint, payload, callback, extraParams);
};

/**
 * Copy mappings from source analyzer to target analyzer
 * @param {String} targetAnalyzerId - Target analyzer ID
 * @param {Object} copyData - Copy data { sourceAnalyzerId, overwriteExisting, skipIncompatible }
 * @param {Function} callback - Callback function (response, extraParams) => void
 * @param {*} extraParams - Optional extra parameters passed to callback
 */
export const copyMappings = (
  targetAnalyzerId,
  copyData,
  callback,
  extraParams,
) => {
  const endpoint = `/rest/analyzer/analyzers/${targetAnalyzerId}/copy-mappings`;
  const payload = JSON.stringify(copyData);
  postToOpenElisServerJsonResponse(endpoint, payload, callback, extraParams);
};

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
 *
 * Note: Uses POST /delete endpoint instead of DELETE HTTP method due to Spring
 * Security 6 CSRF protection issues with DELETE requests.
 *
 * @param {String} id - Analyzer ID
 * @param {Function} callback - Callback function (success, error) => void
 */
export const deleteAnalyzer = (id, callback) => {
  const endpoint = `/rest/analyzer/analyzers/${id}/delete`;
  const csrfToken = localStorage.getItem("CSRF");

  fetch(config.serverBaseUrl + endpoint, {
    credentials: "include",
    method: "POST",
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
        // Response body could not be parsed
      }

      if (response.ok || response.status === 204 || response.status === 200) {
        callback(true, null);
      } else {
        // Parse error response
        let errorData;
        try {
          const contentType = response.headers.get("content-type");
          if (contentType && contentType.indexOf("application/json") !== -1) {
            errorData = responseData || {
              error: `HTTP ${response.status}: ${response.statusText}`,
              status: response.status,
              statusText: response.statusText,
            };
          } else {
            errorData = {
              error: `HTTP ${response.status}: ${response.statusText}`,
              status: response.status,
              statusText: response.statusText,
            };
          }
        } catch (e) {
          errorData = {
            error: `HTTP ${response.status}: ${response.statusText}`,
            status: response.status,
            statusText: response.statusText,
          };
        }
        callback(false, errorData);
      }
    })
    .catch((error) => {
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
 * @param {String} id - Analyzer ID
 * @param {Function} callback - Callback function (response, extraParams) => void
 * @param {*} extraParams - Optional extra parameters passed to callback
 */
export const queryAnalyzer = (id, callback, extraParams) => {
  const endpoint = `/rest/analyzer/analyzers/${id}/query`;
  postToOpenElisServerJsonResponse(
    endpoint,
    JSON.stringify({}),
    callback,
    extraParams,
  );
};

/**
 * Get query job status
 * @param {String} analyzerId
 * @param {String} jobId
 * @param {Function} callback - Callback (data) => void
 */
export const getQueryStatus = (analyzerId, jobId, callback) => {
  const endpoint = `/rest/analyzer/analyzers/${analyzerId}/query/${jobId}/status`;
  getFromOpenElisServer(endpoint, callback);
};

/**
 * Get all analyzer fields for an analyzer
 * @param {String} analyzerId - Analyzer ID
 * @param {Function} callback - Callback function (data) => void
 */
export const getFields = (analyzerId, callback) => {
  const endpoint = `/rest/analyzer/analyzers/${analyzerId}/fields`;
  getFromOpenElisServer(endpoint, callback);
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
      callback(false, { error: error.message || "Network error" });
    });
};

/**
 * Create a new OpenELIS field
 * @param {Object} fieldData - Field data { fieldName, entityType, loincCode, description, fieldType, acceptedUnits }
 * @param {Function} callback - Callback function (response, extraParams) => void
 * @param {*} extraParams - Optional extra parameters passed to callback
 */
export const createField = (fieldData, callback, extraParams) => {
  const endpoint = "/rest/analyzer/openelis-fields";
  const payload = JSON.stringify(fieldData);
  postToOpenElisServerJsonResponse(endpoint, payload, callback, extraParams);
};

/**
 * Get all custom field types
 * @param {Function} callback - Callback function (response, extraParams) => void
 * @param {*} extraParams - Optional extra parameters passed to callback
 */
export const getCustomFieldTypes = (callback, extraParams) => {
  const endpoint = "/rest/analyzer/custom-field-types";
  getFromOpenElisServer(endpoint, callback, extraParams);
};

/**
 * Get active custom field types
 * @param {Function} callback - Callback function (response, extraParams) => void
 * @param {*} extraParams - Optional extra parameters passed to callback
 */
export const getActiveCustomFieldTypes = (callback, extraParams) => {
  const endpoint = "/rest/analyzer/custom-field-types/active";
  getFromOpenElisServer(endpoint, callback, extraParams);
};

/**
 * Get a specific custom field type by ID
 * @param {String} id - Custom field type ID
 * @param {Function} callback - Callback function (response, extraParams) => void
 * @param {*} extraParams - Optional extra parameters passed to callback
 */
export const getCustomFieldType = (id, callback, extraParams) => {
  const endpoint = `/rest/analyzer/custom-field-types/${id}`;
  getFromOpenElisServer(endpoint, callback, extraParams);
};

/**
 * Create a new custom field type
 * @param {Object} fieldTypeData - Field type data { typeName, displayName, validationPattern, valueRangeMin, valueRangeMax, allowedCharacters, isActive }
 * @param {Function} callback - Callback function (response, extraParams) => void
 * @param {*} extraParams - Optional extra parameters passed to callback
 */
export const createCustomFieldType = (fieldTypeData, callback, extraParams) => {
  const endpoint = "/rest/analyzer/custom-field-types";
  const payload = JSON.stringify(fieldTypeData);
  postToOpenElisServerJsonResponse(endpoint, payload, callback, extraParams);
};

/**
 * Update an existing custom field type
 * @param {String} id - Custom field type ID
 * @param {Object} fieldTypeData - Field type data { typeName, displayName, validationPattern, valueRangeMin, valueRangeMax, allowedCharacters, isActive }
 * @param {Function} callback - Callback function (response, extraParams) => void
 * @param {*} extraParams - Optional extra parameters passed to callback
 */
export const updateCustomFieldType = (
  id,
  fieldTypeData,
  callback,
  extraParams,
) => {
  const endpoint = `/rest/analyzer/custom-field-types/${id}`;
  const payload = JSON.stringify(fieldTypeData);
  fetch(`${config.serverBaseUrl}${endpoint}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      "X-CSRF-Token": localStorage.getItem("CSRF"),
    },
    body: payload,
  })
    .then(async (response) => {
      const data = await response.json().catch(() => ({}));
      if (response.ok) {
        callback(data, extraParams);
      } else {
        callback(null, {
          ...extraParams,
          error: data.error || `HTTP ${response.status}`,
        });
      }
    })
    .catch((error) => {
      callback(null, {
        ...extraParams,
        error: error.message || "Network error",
      });
    });
};

/**
 * Delete a custom field type
 * @param {String} id - Custom field type ID
 * @param {Function} callback - Callback function (response, extraParams) => void
 * @param {*} extraParams - Optional extra parameters passed to callback
 */
export const deleteCustomFieldType = (id, callback, extraParams) => {
  const endpoint = `/rest/analyzer/custom-field-types/${id}`;
  fetch(`${config.serverBaseUrl}${endpoint}`, {
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
      callback(false, { error: error.message || "Network error" });
    });
};

/**
 * Get validation rules for a custom field type
 * @param {String} customFieldTypeId - Custom field type ID
 * @param {Function} callback - Callback function (response, extraParams) => void
 * @param {*} extraParams - Optional extra parameters passed to callback
 */
export const getValidationRules = (
  customFieldTypeId,
  callback,
  extraParams,
) => {
  const endpoint = `/rest/analyzer/custom-field-types/${customFieldTypeId}/validation-rules`;
  getFromOpenElisServer(endpoint, callback, extraParams);
};

/**
 * Create a validation rule for a custom field type
 * @param {String} customFieldTypeId - Custom field type ID
 * @param {Object} ruleData - Rule data { ruleName, ruleType, ruleExpression, errorMessage, isActive }
 * @param {Function} callback - Callback function (response, extraParams) => void
 * @param {*} extraParams - Optional extra parameters passed to callback
 */
export const createValidationRule = (
  customFieldTypeId,
  ruleData,
  callback,
  extraParams,
) => {
  const endpoint = `/rest/analyzer/custom-field-types/${customFieldTypeId}/validation-rules`;
  const payload = JSON.stringify(ruleData);
  postToOpenElisServerJsonResponse(endpoint, payload, callback, extraParams);
};

/**
 * Update a validation rule
 * @param {String} customFieldTypeId - Custom field type ID
 * @param {String} ruleId - Validation rule ID
 * @param {Object} ruleData - Rule data { ruleName, ruleType, ruleExpression, errorMessage, isActive }
 * @param {Function} callback - Callback function (response, extraParams) => void
 * @param {*} extraParams - Optional extra parameters passed to callback
 */
export const updateValidationRule = (
  customFieldTypeId,
  ruleId,
  ruleData,
  callback,
  extraParams,
) => {
  const endpoint = `/rest/analyzer/custom-field-types/${customFieldTypeId}/validation-rules/${ruleId}`;
  const payload = JSON.stringify(ruleData);
  fetch(`${config.serverBaseUrl}${endpoint}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      "X-CSRF-Token": localStorage.getItem("CSRF"),
    },
    body: payload,
  })
    .then(async (response) => {
      const data = await response.json().catch(() => ({}));
      if (response.ok) {
        callback(data, extraParams);
      } else {
        callback(null, {
          ...extraParams,
          error: data.error || `HTTP ${response.status}`,
        });
      }
    })
    .catch((error) => {
      callback(null, {
        ...extraParams,
        error: error.message || "Network error",
      });
    });
};

/**
 * Delete a validation rule
 * @param {String} customFieldTypeId - Custom field type ID
 * @param {String} ruleId - Validation rule ID
 * @param {Function} callback - Callback function (response, extraParams) => void
 * @param {*} extraParams - Optional extra parameters passed to callback
 */
/**
 * Validate a field value against custom field type validation rules
 * @param {String} analyzerId - Analyzer ID
 * @param {String} fieldId - Analyzer field ID
 * @param {String} value - Value to validate
 * @param {Function} callback - Callback function (response, extraParams) => void
 * @param {*} extraParams - Optional extra parameters passed to callback
 */
export const validateFieldValue = (
  analyzerId,
  fieldId,
  value,
  callback,
  extraParams,
) => {
  const endpoint = `/rest/analyzer/analyzers/${analyzerId}/fields/${fieldId}/validate-value`;
  const payload = JSON.stringify({ value });
  postToOpenElisServerJsonResponse(endpoint, payload, callback, extraParams);
};

export const deleteValidationRule = (
  customFieldTypeId,
  ruleId,
  callback,
  extraParams,
) => {
  const endpoint = `/rest/analyzer/custom-field-types/${customFieldTypeId}/validation-rules/${ruleId}`;
  fetch(`${config.serverBaseUrl}${endpoint}`, {
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
      callback(false, { error: error.message || "Network error" });
    });
};

/**
 * Get all analyzer plugin types from the analyzer_type table.
 *
 * <p>Returns plugin type definitions including:
 * - id: Database ID
 * - name: Human-readable name (e.g., "Generic ASTM", "Horiba Pentra 60")
 * - protocol: Communication protocol (ASTM, HL7, FILE)
 * - isGenericPlugin: Whether this is a dashboard-configurable generic plugin
 * - identifierPattern: Regex pattern for generic plugins
 *
 *
 * @param {Object} filters - Optional filters { active, genericOnly, search }
 * @param {Function} callback - Callback function (data) => void
 */
export const getAnalyzerTypes = (filters, callback) => {
  let endpoint = "/rest/analyzer-types";
  const params = new URLSearchParams();

  if (filters) {
    if (filters.active !== undefined) {
      params.append("active", filters.active);
    }
    if (filters.genericOnly !== undefined) {
      params.append("genericOnly", filters.genericOnly);
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
 * Get list of available default analyzer configurations.
 *
 * <p>Returns minimal metadata for each template:
 * - id (e.g., "astm/mindray-ba88a")
 * - protocol ("ASTM" or "HL7")
 * - analyzerName (from JSON)
 *
 *
 * @param {Function} callback - Callback function (data) => void
 */
export const getDefaultConfigs = (callback) => {
  const endpoint = "/rest/analyzer/defaults";
  getFromOpenElisServer(endpoint, callback);
};

/**
 * Get specific default analyzer configuration template.
 *
 * <p>Loads JSON template from filesystem for the specified protocol and name.
 *
 *
 * @param {String} protocol - Protocol type ("astm" or "hl7")
 * @param {String} name - Template name (without .json extension)
 * @param {Function} callback - Callback function (data) => void
 */
export const getDefaultConfig = (protocol, name, callback) => {
  const endpoint = `/rest/analyzer/defaults/${protocol}/${name}`;
  getFromOpenElisServer(endpoint, callback);
};
