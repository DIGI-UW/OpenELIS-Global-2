import { useState } from "react";
import {
  postToOpenElisServerJsonResponse,
  patchToOpenElisServerJsonResponse,
} from "../../utils/Utils";

/**
 * Hook for sample item storage assignment and movement
 * Uses postToOpenElisServerJsonResponse to get full JSON response and handle errors
 * Note: Storage tracking operates at SampleItem level (physical specimens), not Sample level (orders)
 */
export const useSampleStorage = () => {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const assignSampleItem = async (assignmentData) => {
    setIsSubmitting(true);
    setError(null);

    return new Promise((resolve, reject) => {
      postToOpenElisServerJsonResponse(
        "/rest/storage/sample-items/assign",
        JSON.stringify(assignmentData),
        (response) => {
          setIsSubmitting(false);
          // Success response has assignmentId or hierarchicalPath, error response has message/error without these
          if (response.assignmentId || response.hierarchicalPath) {
            // Success - response has assignmentId or hierarchicalPath
            setError(null);
            resolve(response);
          } else if (response.error || response.message) {
            // Error - response has error or message but no assignmentId/hierarchicalPath
            const errorMessage =
              response.error || response.message || "Unknown error";
            setError(errorMessage);
            reject(new Error(errorMessage));
          } else if (response.status || response.statusCode) {
            // HTTP error response with status code
            const errorMessage =
              response.message ||
              response.error ||
              `Request failed with status ${response.status || response.statusCode}`;
            setError(errorMessage);
            reject(new Error(errorMessage));
          } else if (typeof response === "string") {
            // Error response is a string
            setError(response);
            reject(new Error(response));
          } else {
            // Unexpected response format - log it for debugging
            console.error("Unexpected response format:", response);
            const errorMessage =
              response.toString() || "Unexpected response format from server";
            setError(errorMessage);
            reject(new Error(errorMessage));
          }
        },
      );
    });
  };

  const moveSampleItem = async (movementData) => {
    setIsSubmitting(true);
    setError(null);

    return new Promise((resolve, reject) => {
      postToOpenElisServerJsonResponse(
        "/rest/storage/sample-items/move",
        JSON.stringify(movementData),
        (response) => {
          setIsSubmitting(false);
          // Success response has movementId, error response has message/error without movementId
          if (response.movementId) {
            // Success - response has movementId
            setError(null);
            resolve(response);
          } else if (response.error || response.message) {
            // Error - response has error or message but no movementId
            const errorMessage =
              response.error || response.message || "Unknown error";
            setError(errorMessage);
            reject(new Error(errorMessage));
          } else if (response.status || response.statusCode) {
            // HTTP error response with status code
            const errorMessage =
              response.message ||
              response.error ||
              `Request failed with status ${response.status || response.statusCode}`;
            setError(errorMessage);
            reject(new Error(errorMessage));
          } else if (typeof response === "string") {
            // Error response is a string
            setError(response);
            reject(new Error(response));
          } else {
            // Unexpected response format - log it for debugging
            console.error("Unexpected response format:", response);
            const errorMessage =
              response.toString() || "Unexpected response format from server";
            setError(errorMessage);
            reject(new Error(errorMessage));
          }
        },
      );
    });
  };

  /**
   * Update position and/or notes for an existing assignment without changing location
   * @param {string} sampleItemId - The sample item ID
   * @param {Object} updates - Object with positionCoordinate and/or notes
   * @returns {Promise} - Resolves with updated assignment data
   */
  const updateSampleItemMetadata = async (sampleItemId, updates) => {
    setIsSubmitting(true);
    setError(null);

    return new Promise((resolve, reject) => {
      patchToOpenElisServerJsonResponse(
        `/rest/storage/sample-items/${sampleItemId}`,
        JSON.stringify(updates),
        (response) => {
          setIsSubmitting(false);
          if (response.assignmentId || response.hierarchicalPath) {
            setError(null);
            resolve(response);
          } else if (response.error || response.message) {
            const errorMessage =
              response.error || response.message || "Unknown error";
            setError(errorMessage);
            reject(new Error(errorMessage));
          } else {
            console.error("Unexpected response format:", response);
            const errorMessage = "Unexpected response format from server";
            setError(errorMessage);
            reject(new Error(errorMessage));
          }
        },
      );
    });
  };

  return {
    assignSampleItem,
    moveSampleItem,
    updateSampleItemMetadata,
    isSubmitting,
    error,
  };
};

export default useSampleStorage;
