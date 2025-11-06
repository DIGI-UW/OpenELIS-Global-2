import { useState } from "react";
import { postToOpenElisServerJsonResponse } from "../../utils/Utils";

/**
 * Hook for sample storage assignment and movement
 * Uses postToOpenElisServerJsonResponse to get full JSON response and handle errors
 */
export const useSampleStorage = () => {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const assignSample = async (assignmentData) => {
    setIsSubmitting(true);
    setError(null);

    return new Promise((resolve, reject) => {
      postToOpenElisServerJsonResponse(
        "/rest/storage/samples/assign",
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
          } else {
            // Unexpected response format
            setError("Unexpected response format");
            reject(new Error("Unexpected response format"));
          }
        },
      );
    });
  };

  const moveSample = async (movementData) => {
    setIsSubmitting(true);
    setError(null);

    return new Promise((resolve, reject) => {
      postToOpenElisServerJsonResponse(
        "/rest/storage/samples/move",
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
          } else {
            // Unexpected response format
            setError("Unexpected response format");
            reject(new Error("Unexpected response format"));
          }
        },
      );
    });
  };

  return { assignSample, moveSample, isSubmitting, error };
};

export default useSampleStorage;
