/**
 * API utilities for SampleTypeRequest operations.
 *
 * SampleTypeRequests represent requested sample types during Step 1 (Enter Order).
 * They are fulfilled in Step 2 (Collect Sample) when actual sample_item records are created.
 */

import {
  getFromOpenElisServer,
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
 * Shape the entered specimens for the order save. They travel with the order
 * so the two are persisted together.
 */
export const toRequestedSampleTypes = (samples = []) =>
  samples
    .filter((sample) => sample && sample.sampleTypeId)
    .map((sample) => ({
      typeOfSampleId: sample.sampleTypeId,
      requestedQuantity: parseFloat(sample.quantity) || null,
      unitOfMeasureId: sample.quantityUnit || null,
      requestedTests: sample.tests?.map((t) => t.id || t).join(",") || "",
      requestedPanels: sample.panels?.map((p) => p.id || p).join(",") || "",
    }));

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

  return pendingRequests
    .filter((request) => request.status !== "CANCELLED")
    .map((request, index) => ({
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
      // Prefer the complete server projection so reloaded selections retain
      // workflow and linked Method metadata. Keep the legacy shape as fallback.
      tests:
        request.requestedTestDetails?.length > 0
          ? request.requestedTestDetails
          : zipIdsAndNames(request.requestedTests, request.requestedTestNames),
      referralItems: [],
      quantity:
        request.requestedQuantity == null
          ? ""
          : String(request.requestedQuantity),
      quantityUnit: request.unitOfMeasureId || "",
      collectionConditions: "",
      collectionDate: "",
      collectionTime: "",
      collectorId: "",
      labPerformedSampling: false,
      receivedDate: "",
      receivedTime: "",
      receivedBy: "",
      hasNCE: false,
      nceId: "",
      qcMetadata: null,
      // Status from request
      status: request.status,
    }));
};

/**
 * A partially collected order loads its collected specimens as sample_items and
 * leaves the rest as pending sample_type_requests. Rebuilding the entry state
 * from either source alone drops the other, so a resave of the Enter step would
 * cancel the omitted pending specimens. This keeps both.
 * @param {Array} responseSamples - samples from the order response
 * @param {Array} requests - SampleTypeRequestDTO rows for the sample
 * @param {Array|null} fallback - value to use when neither source has rows
 * @returns {Array|null} - merged UI samples
 */
export const mergeCollectedAndPendingSamples = (
  responseSamples,
  requests,
  fallback,
) => {
  const collected = (responseSamples || []).filter(
    (sample) => sample && sample.sampleItemId,
  );
  const pending = convertRequestsToSamples(
    (requests || []).filter(
      (request) => request && request.status === "REQUESTED",
    ),
  );
  const merged = [...collected, ...pending];
  if (merged.length > 0) {
    return merged;
  }
  return responseSamples && responseSamples.length > 0
    ? responseSamples
    : fallback;
};
