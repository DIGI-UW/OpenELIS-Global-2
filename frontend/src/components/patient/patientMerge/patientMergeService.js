import config from "../../../config.json";

/**
 * Patient Merge API Service
 * Handles all API calls for the patient merge feature
 */

/**
 * Get patient details for merge preview
 * @param {string} patientId - The patient ID
 * @returns {Promise<Object>} Patient merge details
 */
export const getPatientMergeDetails = async (patientId) => {
  const response = await fetch(
    `${config.serverBaseUrl}/rest/patient/merge/details/${patientId}`,
    {
      credentials: "include",
      method: "GET",
    },
  );

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw {
      status: response.status,
      message: errorData.message || `Failed to get patient details`,
      ...errorData,
    };
  }

  return response.json();
};

/**
 * Validate patient merge eligibility
 * @param {Object} request - Merge validation request
 * @param {string} request.patient1Id - First patient ID
 * @param {string} request.patient2Id - Second patient ID
 * @param {string} request.primaryPatientId - ID of patient to keep
 * @returns {Promise<Object>} Validation result with errors/warnings
 */
export const validatePatientMerge = async (request) => {
  const response = await fetch(
    `${config.serverBaseUrl}/rest/patient/merge/validate`,
    {
      credentials: "include",
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-CSRF-Token": localStorage.getItem("CSRF"),
      },
      body: JSON.stringify({
        patient1Id: request.patient1Id,
        patient2Id: request.patient2Id,
        primaryPatientId: request.primaryPatientId,
        reason: request.reason || "",
        confirmed: false,
      }),
    },
  );

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw {
      status: response.status,
      message: errorData.message || `Validation failed`,
      ...errorData,
    };
  }

  return response.json();
};

/**
 * Execute patient merge
 * @param {Object} request - Merge execution request
 * @param {string} request.patient1Id - First patient ID
 * @param {string} request.patient2Id - Second patient ID
 * @param {string} request.primaryPatientId - ID of patient to keep
 * @param {string} request.reason - Reason for merge
 * @returns {Promise<Object>} Execution result with audit ID
 */
export const executePatientMerge = async (request) => {
  const response = await fetch(
    `${config.serverBaseUrl}/rest/patient/merge/execute`,
    {
      credentials: "include",
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-CSRF-Token": localStorage.getItem("CSRF"),
      },
      body: JSON.stringify({
        patient1Id: request.patient1Id,
        patient2Id: request.patient2Id,
        primaryPatientId: request.primaryPatientId,
        reason: request.reason,
        confirmed: true,
      }),
    },
  );

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw {
      status: response.status,
      message: errorData.message || `Merge execution failed`,
      ...errorData,
    };
  }

  return response.json();
};

/**
 * Search patients (using existing patient search endpoint)
 * @param {Object} searchParams - Search parameters
 * @returns {Promise<Object>} Search results
 */
export const searchPatients = async (searchParams) => {
  const queryParams = new URLSearchParams({
    lastName: searchParams.lastName || "",
    firstName: searchParams.firstName || "",
    STNumber: searchParams.patientId || "",
    subjectNumber: searchParams.patientId || "",
    nationalID: searchParams.patientId || "",
    labNumber: searchParams.labNumber || "",
    guid: searchParams.guid || "",
    dateOfBirth: searchParams.dateOfBirth || "",
    gender: searchParams.gender || "",
    suppressExternalSearch: searchParams.suppressExternalSearch || "true",
  });

  const response = await fetch(
    `${config.serverBaseUrl}/rest/patient-search-results?${queryParams}`,
    {
      credentials: "include",
      method: "GET",
    },
  );

  if (!response.ok) {
    throw new Error("Failed to search patients");
  }

  return response.json();
};

/**
 * Handle API errors and return user-friendly messages
 * @param {Object} error - Error object with status
 * @param {Function} intl - React Intl formatMessage function
 * @returns {string} User-friendly error message
 */
export const getErrorMessage = (error, intl) => {
  switch (error.status) {
    case 401:
      return intl.formatMessage({ id: "accessDenied.message" });
    case 403:
      return intl.formatMessage({ id: "patient.merge.error.noPermission" });
    case 404:
      return intl.formatMessage({ id: "patient.merge.error.patientNotFound" });
    case 400:
      return (
        error.message ||
        intl.formatMessage({ id: "patient.merge.error.validationFailed" })
      );
    default:
      return (
        error.message ||
        intl.formatMessage({ id: "patient.merge.error.executionFailed" })
      );
  }
};
