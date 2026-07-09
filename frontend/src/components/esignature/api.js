import {
  getFromOpenElisServerV2,
  postToOpenElisServerJsonResponse,
  deleteFromOpenElisServer,
} from "../utils/Utils";

/**
 * Electronic Signature API module
 * Provides functions for interacting with the e-signature REST endpoints
 * per 21 CFR Part 11 compliance requirements.
 */

/**
 * Check if electronic signatures are enabled in the system.
 * @returns {Promise<{enabled: boolean}>}
 */
export const isEsigEnabled = async () => {
  return getFromOpenElisServerV2("/rest/esig/enabled");
};

/**
 * Check if a user has completed first-use certification.
 * @param {string} username - The username to check
 * @returns {Promise<{username: string, certified: boolean}>}
 */
export const isUserCertified = async (username) => {
  return getFromOpenElisServerV2(
    `/rest/esig/certified/${encodeURIComponent(username)}`,
  );
};

/**
 * Get the current session status for a user.
 * @param {string} username - The username
 * @returns {Promise<{username: string, sessionActive: boolean, signingCount: number}>}
 */
export const getSessionStatus = async (username) => {
  return getFromOpenElisServerV2(
    `/rest/esig/session-status/${encodeURIComponent(username)}`,
  );
};

/**
 * Get all signatures for a specific record.
 * @param {string} recordType - The type of record (e.g., "RESULT", "ANALYSIS")
 * @param {number} recordId - The record ID
 * @returns {Promise<Array>} Array of signature objects
 */
export const getSignaturesForRecord = async (recordType, recordId) => {
  const params = new URLSearchParams({
    recordType,
    recordId: String(recordId),
  });
  return getFromOpenElisServerV2(`/rest/esig/signatures?${params.toString()}`);
};

/**
 * Execute an electronic signature.
 * @param {Object} signatureData - The signature request data
 * @param {string} signatureData.username - The signer's username
 * @param {string} signatureData.password - The signer's password
 * @param {string} signatureData.signatureMeaning - The meaning (AUTHORED, VALIDATED_AND_RELEASED, REJECTED)
 * @param {string} signatureData.recordType - The type of record being signed
 * @param {number} signatureData.recordId - The ID of the record being signed
 * @param {string} [signatureData.rejectionReason] - Required if meaning is REJECTED
 * @returns {Promise<Object>} The created signature record
 */
export const executeSignature = async (signatureData) => {
  return new Promise((resolve, reject) => {
    postToOpenElisServerJsonResponse(
      "/rest/esig/sign",
      JSON.stringify(signatureData),
      (json) => {
        if (
          json &&
          (json.error || json.status >= 400 || json.statusCode >= 400)
        ) {
          const errorCode = json.error || "UNKNOWN_ERROR";
          const errorMessage = json.message || "Failed to execute signature";
          reject(
            new ESignatureError(
              errorCode,
              errorMessage,
              json.status || json.statusCode,
            ),
          );
        } else {
          resolve(json);
        }
      },
      null,
    );
  });
};

/**
 * Complete first-use certification for a user.
 * @param {Object} certificationData - The certification request data
 * @param {string} certificationData.username - The username
 * @param {string} certificationData.password - The user's password
 * @param {string} certificationData.certificationText - The legal certification text acknowledged
 * @returns {Promise<Object>} The created certification record
 */
export const certifyUser = async (certificationData) => {
  return new Promise((resolve, reject) => {
    postToOpenElisServerJsonResponse(
      "/rest/esig/certify",
      JSON.stringify(certificationData),
      (json) => {
        if (
          json &&
          (json.error || json.status >= 400 || json.statusCode >= 400)
        ) {
          const errorCode = json.error || "UNKNOWN_ERROR";
          const errorMessage =
            json.message || "Failed to complete certification";
          reject(
            new ESignatureError(
              errorCode,
              errorMessage,
              json.status || json.statusCode,
            ),
          );
        } else {
          resolve(json);
        }
      },
      null,
    );
  });
};

/**
 * Get all certifications (admin only).
 * @returns {Promise<Array>} Array of certification records
 */
export const getAllCertifications = async () => {
  return getFromOpenElisServerV2("/rest/esig/admin/certifications");
};

/**
 * Revoke a user's certification (admin only).
 * @param {string} username - The username whose certification to revoke
 * @returns {Promise<void>}
 */
export const revokeCertification = async (username) => {
  return new Promise((resolve, reject) => {
    deleteFromOpenElisServer(
      `/rest/esig/admin/certifications/${encodeURIComponent(username)}`,
      (status) => {
        if (status >= 200 && status < 300) {
          resolve();
        } else {
          reject(new Error(`Failed to revoke certification: HTTP ${status}`));
        }
      },
    );
  });
};

/**
 * Custom error class for e-signature operations.
 */
export class ESignatureError extends Error {
  constructor(code, message, statusCode) {
    super(message);
    this.name = "ESignatureError";
    this.code = code;
    this.statusCode = statusCode;
  }
}

/**
 * Signature meaning constants matching backend SignatureMeaning enum.
 */
export const SignatureMeaning = {
  AUTHORED: "AUTHORED",
  VALIDATED_AND_RELEASED: "VALIDATED_AND_RELEASED",
  REJECTED: "REJECTED",
};

/**
 * Authentication method constants matching backend AuthMethod enum.
 */
export const AuthMethod = {
  LOCAL: "LOCAL",
  KEYCLOAK: "KEYCLOAK",
};

/**
 * Default certification text per 21 CFR Part 11 Section 11.100(c).
 * Users must acknowledge this before their first e-signature.
 */
export const DEFAULT_CERTIFICATION_TEXT =
  "I understand that by providing my login credentials, I am creating a legally " +
  "binding electronic signature that carries the same weight as my handwritten " +
  "signature. I certify that I am the sole owner of these credentials and that " +
  "I will not share them with anyone else.";
