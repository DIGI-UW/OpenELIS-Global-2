import config from "../config.json";
import { getFromOpenElisServer, postToOpenElisServerFormData, postToOpenElisServerJsonResponse } from "../components/utils/Utils";

/**
 * Document service for patient ID document management.
 * Uses existing OpenELIS server communication patterns.
 */
export const documentService = {
  /**
   * Upload a document for a patient.
   * @param {string} patientId - Patient ID
   * @param {File} file - File to upload
   * @param {string} documentType - Document type (NATIONAL_ID, INSURANCE_CARD, OTHER)
   * @param {string} description - Optional description
   * @param {Function} callback - Callback function (success, error)
   */
  uploadDocument: (patientId, file, documentType, description, callback) => {
    const formData = new FormData();
    formData.append("file", file);
    formData.append("documentType", documentType);
    if (description) {
      formData.append("description", description);
    }

    const endpoint = `/rest/patients/${patientId}/documents`;

    postToOpenElisServerFormData(
      endpoint,
      formData,
      (status, extraParams) => {
        if (status === 201) {
          // Fetch the created document to return full metadata
          documentService.getDocuments(patientId, (docs) => {
            const created = docs.find(d => d.documentId === extraParams?.documentId);
            callback(created || { documentId: extraParams?.documentId });
          });
        } else {
          callback(null, new Error(`Upload failed with status ${status}`));
        }
      },
      { documentId: null }
    );
  },

  /**
   * Get all documents for a patient.
   * @param {string} patientId - Patient ID
   * @param {Function} callback - Callback function (documents)
   */
  getDocuments: (patientId, callback) => {
    const endpoint = `/rest/patients/${patientId}/documents`;
    getFromOpenElisServer(endpoint, callback);
  },

  /**
   * Get document metadata.
   * @param {string} patientId - Patient ID
   * @param {string} documentId - Document ID
   * @param {Function} callback - Callback function (document)
   */
  getDocument: (patientId, documentId, callback) => {
    const endpoint = `/rest/patients/${patientId}/documents/${documentId}`;
    getFromOpenElisServer(endpoint, callback);
  },

  /**
   * Download document content.
   * @param {string} patientId - Patient ID
   * @param {string} documentId - Document ID
   * @param {string} versionId - Version ID
   * @returns {Promise<Blob>} Document blob
   */
  downloadDocument: async (patientId, documentId, versionId) => {
    const endpoint = `/rest/patients/${patientId}/documents/${documentId}/versions/${versionId}/content`;
    const response = await fetch(config.serverBaseUrl + endpoint, {
      credentials: "include",
      method: "GET",
    });
    if (!response.ok) {
      throw new Error(`Download failed: ${response.statusText}`);
    }
    return response.blob();
  },

  /**
   * Get document versions.
   * @param {string} patientId - Patient ID
   * @param {string} documentId - Document ID
   * @param {Function} callback - Callback function (versions)
   */
  getVersions: (patientId, documentId, callback) => {
    const endpoint = `/rest/patients/${patientId}/documents/${documentId}/versions`;
    getFromOpenElisServer(endpoint, callback);
  },

  /**
   * Soft delete a document.
   * @param {string} patientId - Patient ID
   * @param {string} documentId - Document ID
   * @param {Function} callback - Callback function (success)
   */
  deleteDocument: (patientId, documentId, callback) => {
    const endpoint = `/rest/patients/${patientId}/documents/${documentId}`;
    fetch(config.serverBaseUrl + endpoint, {
      credentials: "include",
      method: "DELETE",
      headers: {
        "X-CSRF-Token": localStorage.getItem("CSRF"),
      },
    })
      .then((response) => {
        if (response.status === 204) {
          callback(true);
        } else {
          callback(false, new Error(`Delete failed: ${response.statusText}`));
        }
      })
      .catch((error) => {
        callback(false, error);
      });
  },
};

export default documentService;

