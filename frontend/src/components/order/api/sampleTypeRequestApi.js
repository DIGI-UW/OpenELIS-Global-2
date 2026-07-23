/**
 * API utilities for SampleTypeRequest operations.
 *
 * SampleTypeRequests represent requested sample types during Step 1 (Enter Order).
 * They are fulfilled in Step 2 (Collect Sample) when actual sample_item records are created.
 */

import {
  getFromOpenElisServer,
  postToOpenElisServer,
  putToOpenElisServerFullResponse,
} from "../../utils/Utils";

const BASE_URL = "/rest/sample-type-requests";

/**
 * Get all sample type requests for a sample.
 * @param {string} sampleId - The sample ID
 * @returns {Promise<Array>} - Array of SampleTypeRequestDTO objects
 */
export const getRequestsBySample = (sampleId) => {
  return new Promise((resolve, reject) => {
    getFromOpenElisServer(`${BASE_URL}/sample/${sampleId}`, (response) => {
      // Check if response is an array (success) or string/error
      if (response && Array.isArray(response)) {
        resolve(response);
      } else if (response && typeof response === "string") {
        // Error message returned
        reject(new Error(response));
      } else {
        reject(new Error("Failed to fetch sample type requests"));
      }
    });
  });
};

/**
 * Get pending (not yet collected) requests for a sample.
 * @param {string} sampleId - The sample ID
 * @returns {Promise<Array>} - Array of pending SampleTypeRequestDTO objects
 */
export const getPendingRequests = (sampleId) => {
  return new Promise((resolve, reject) => {
    getFromOpenElisServer(
      `${BASE_URL}/sample/${sampleId}/pending`,
      (response) => {
        // Check if response is an array (success) or string/error
        if (response && Array.isArray(response)) {
          resolve(response);
        } else if (response && typeof response === "string") {
          // Error message returned
          reject(new Error(response));
        } else {
          reject(new Error("Failed to fetch pending requests"));
        }
      },
    );
  });
};

/**
 * Create a new sample type request (Step 1: Enter Order).
 * @param {Object} request - The request data
 * @param {string} request.sampleId - The sample ID
 * @param {string} request.typeOfSampleId - The type of sample ID
 * @param {number} [request.sortOrder] - Sort order (default 0)
 * @param {number} [request.requestedQuantity] - Quantity (default 1)
 * @param {string} [request.unitOfMeasureId] - Unit of measure ID
 * @param {string} [request.requestedTests] - Comma-separated test IDs
 * @param {string} [request.requestedPanels] - Comma-separated panel IDs
 * @returns {Promise<Object>} - Created SampleTypeRequestDTO
 */
export const createRequest = (request) => {
  return new Promise((resolve, reject) => {
    postToOpenElisServer(BASE_URL, JSON.stringify(request), (status, body) => {
      if (status === 200 || status === 201) {
        try {
          const data = typeof body === "string" ? JSON.parse(body) : body;
          resolve(data);
        } catch (e) {
          resolve(body);
        }
      } else {
        reject(new Error(body || "Failed to create sample type request"));
      }
    });
  });
};

/**
 * Create multiple sample type requests at once.
 * @param {string} sampleId - The sample ID
 * @param {Array} sampleTypes - Array of sample type selections
 * @returns {Promise<Array>} - Array of created SampleTypeRequestDTO objects
 */
export const createRequestsForSamples = async (sampleId, sampleTypes) => {
  const results = [];
  for (let i = 0; i < sampleTypes.length; i++) {
    const sample = sampleTypes[i];
    const request = {
      sampleId: sampleId,
      typeOfSampleId: sample.sampleTypeId,
      sortOrder: i,
      requestedQuantity: parseFloat(sample.quantity) || 1,
      unitOfMeasureId: sample.quantityUnit || null,
      requestedTests: sample.tests?.map((t) => t.id || t).join(",") || "",
      requestedPanels: sample.panels?.map((p) => p.id || p).join(",") || "",
    };
    const created = await createRequest(request);
    results.push(created);
  }
  return results;
};

/**
 * Fulfill a request by linking it to a collected sample_item (Step 2: Collect Sample).
 * @param {string} requestId - The request ID
 * @param {string} sampleItemId - The created sample_item ID
 * @returns {Promise<Object>} - Updated SampleTypeRequestDTO
 */
export const fulfillRequest = (requestId, sampleItemId) => {
  return new Promise((resolve, reject) => {
    putToOpenElisServerFullResponse(
      `${BASE_URL}/${requestId}/fulfill?sampleItemId=${sampleItemId}`,
      null,
      (response) => {
        if (response && response.ok) {
          response.json().then(resolve).catch(reject);
        } else {
          reject(new Error("Failed to fulfill request"));
        }
      },
    );
  });
};

/**
 * Cancel a pending request.
 * @param {string} requestId - The request ID
 * @returns {Promise<Object>} - Updated SampleTypeRequestDTO
 */
export const cancelRequest = (requestId) => {
  return new Promise((resolve, reject) => {
    putToOpenElisServerFullResponse(
      `${BASE_URL}/${requestId}/cancel`,
      null,
      (response) => {
        if (response && response.ok) {
          response.json().then(resolve).catch(reject);
        } else {
          reject(new Error("Failed to cancel request"));
        }
      },
    );
  });
};

/**
 * Convert pending requests to samples array format for Step 2 UI.
 * @param {Array} pendingRequests - Array of SampleTypeRequestDTO objects
 * @returns {Array} - Array of sample objects for the UI
 */
export const convertRequestsToSamples = (pendingRequests) => {
  if (!pendingRequests || !Array.isArray(pendingRequests)) {
    console.error("convertRequestsToSamples: invalid input", pendingRequests);
    return [];
  }

  // Helper to zip IDs and Names arrays
  const zipIdsAndNames = (idsString, namesString) => {
    if (!idsString) return [];
    const ids = idsString.split(",").filter(Boolean);
    const names = namesString ? namesString.split(",") : [];
    return ids.map((id, idx) => ({
      id: id.trim(),
      name: names[idx] ? names[idx].trim() : "",
    }));
  };

  return pendingRequests.map((request, index) => ({
    index: index,
    sampleTypeRequestId: request.id, // Track the original request
    sampleItemId: "", // Will be populated when collected
    sampleRejected: false,
    rejectionReason: "",
    sampleTypeId: request.typeOfSampleId,
    sampleTypeName: request.typeOfSampleName,
    // Panels need to be objects with id and name properties for the UI
    panels: zipIdsAndNames(
      request.requestedPanels,
      request.requestedPanelNames,
    ),
    // Tests need to be objects with id and name properties for the UI
    tests: zipIdsAndNames(request.requestedTests, request.requestedTestNames),
    requestReferralEnabled: false,
    referralItems: [],
    quantity: request.requestedQuantity?.toString() || "1",
    quantityUnit: request.unitOfMeasureId || "",
    collectionConditions: "",
    collectionDate: "",
    collectionTime: "",
    collectorId: "",
    receivedDate: "",
    receivedTime: "",
    receivedBy: "",
    hasNCE: false,
    nceId: "",
    // Status from request
    status: request.status,
  }));
};
