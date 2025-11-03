import { useState } from "react";
import { postToOpenElisServer } from "../../utils/Utils";

/**
 * Hook for sample storage assignment and movement
 * Following OpenELIS pattern: postToOpenElisServer for mutations
 */
export const useSampleStorage = () => {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const assignSample = async (assignmentData) => {
    setIsSubmitting(true);
    setError(null);

    return new Promise((resolve, reject) => {
      postToOpenElisServer(
        "/rest/storage/samples/assign",
        JSON.stringify(assignmentData),
        (response) => {
          setIsSubmitting(false);
          resolve(response);
        },
        (error) => {
          setIsSubmitting(false);
          setError(error);
          reject(error);
        },
      );
    });
  };

  const moveSample = async (movementData) => {
    setIsSubmitting(true);
    setError(null);

    return new Promise((resolve, reject) => {
      postToOpenElisServer(
        "/rest/storage/samples/move",
        JSON.stringify(movementData),
        (response) => {
          setIsSubmitting(false);
          resolve(response);
        },
        (error) => {
          setIsSubmitting(false);
          setError(error);
          reject(error);
        },
      );
    });
  };

  return { assignSample, moveSample, isSubmitting, error };
};

export default useSampleStorage;
