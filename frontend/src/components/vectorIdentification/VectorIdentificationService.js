/**
 * V-03 API client. Wraps the standard Utils.js helpers (which provide CSRF,
 * Accept-Language, and 403/CSRF-expiry handling) into Promises so components
 * can `await` instead of writing nested callbacks.
 */
import {
  getFromOpenElisServerV2,
  postToOpenElisServerJsonResponse,
  putToOpenElisServerFullResponse,
} from "../utils/Utils";

const ID_BASE = "/rest/vector/identification";
const DEC_BASE = "/rest/vector/deconvolution";
const SPECIES_BASE = "/rest/admin/vector/species";
const SAMPLING_SITE_BASE = "/rest/admin/vector/sampling-sites";
const DICTIONARY_BASE = "/rest/dictionary/categories";

/** Promise-wrap getFromOpenElisServer; rejects on null/undefined response only. */
const get = (path) => getFromOpenElisServerV2(path);

/**
 * Promise-wrap postToOpenElisServerJsonResponse. The util appends
 * status/statusCode/statusText fields to error JSON bodies (HTTP 4xx/5xx);
 * we use those to construct an Error, otherwise resolve with the body.
 */
const postJson = (path, data) =>
  new Promise((resolve, reject) => {
    postToOpenElisServerJsonResponse(
      path,
      JSON.stringify(data ?? {}),
      (json) => {
        if (!json) {
          reject(new Error("No response from server"));
          return;
        }
        const code = json.status ?? json.statusCode;
        if (code != null && code >= 400) {
          const err = new Error(
            json.message ||
              json.error ||
              `Request failed (HTTP ${code}${json.statusText ? " " + json.statusText : ""})`,
          );
          // Attach the full payload so callers can pass it through
          // resolveApiErrorMessage(intl, err.payload, fallbackId) for backend-
          // supplied messageKey / fieldErrors handling.
          err.payload = json;
          err.statusCode = code;
          reject(err);
          return;
        }
        resolve(json);
      },
      null,
    );
  });

/** Promise-wrap PUT. Returns parsed JSON on success, rejects on non-2xx. */
const put = (path, data) =>
  new Promise((resolve, reject) => {
    putToOpenElisServerFullResponse(
      path,
      data ? JSON.stringify(data) : null,
      (response) => {
        if (!response) {
          reject(new Error("No response from server"));
          return;
        }
        if (!response.ok) {
          response
            .clone()
            .json()
            .catch(() => null)
            .then((body) => {
              reject(
                new Error(
                  (body && (body.message || body.error)) ||
                    `Request failed (HTTP ${response.status})`,
                ),
              );
            });
          return;
        }
        if (response.status === 204) {
          resolve(null);
          return;
        }
        response
          .json()
          .then(resolve)
          .catch(() => resolve(null));
      },
    );
  });

export const VectorIdentificationAPI = {
  getWorklist: (status = "pending") =>
    get(`${ID_BASE}/worklist?status=${encodeURIComponent(status)}`),

  getSpecimensForLot: (lotId) => get(`${ID_BASE}/lots/${lotId}/specimens`),

  identify: (specimenId, request) =>
    postJson(`${ID_BASE}/specimens/${specimenId}/identify`, request),

  /** Candidate Results for the molecular Link Result picker. */
  getResultCandidates: (lotId) =>
    get(`${ID_BASE}/lots/${lotId}/result-candidates`),

  /** Dictionary-driven select options, so labs can extend per region. */
  getDictionaryEntries: (categoryName) =>
    get(`${DICTIONARY_BASE}/${encodeURIComponent(categoryName)}/entries`),

  bulkIdentify: (request) =>
    postJson(`${ID_BASE}/specimens/bulk-identify`, request),

  /** Returns 404 if none. */
  getIdentification: (specimenId) =>
    get(`${ID_BASE}/specimens/${specimenId}/identification`),

  addBloodmealPanel: (specimenId) =>
    postJson(`${ID_BASE}/specimens/${specimenId}/bloodmeal-panel`, {}),

  dismissBloodmealSuggestion: (specimenId) =>
    postJson(`${ID_BASE}/specimens/${specimenId}/bloodmeal-dismiss`, {}),
};

export const VectorDeconvolutionAPI = {
  getWorklist: () => get(`${DEC_BASE}/worklist`),

  initiate: (request) => postJson(`${DEC_BASE}/initiate`, request),

  getDeconvolution: (vectorPoolId) => get(`${DEC_BASE}/pool/${vectorPoolId}`),

  /** Read-only preview of copied + reflex test orders. */
  previewReflexes: (vectorPoolId) => get(`${DEC_BASE}/preview/${vectorPoolId}`),

  /** Tech confirms the pool result applies to every member — no split needed. */
  confirmAll: (vectorPoolId) =>
    postJson(`${DEC_BASE}/pool/${vectorPoolId}/confirm-all`, {}),

  /** ADMIN-only. */
  forceComplete: (vectorPoolId) =>
    put(`${DEC_BASE}/pool/${vectorPoolId}/complete`),
};

export const VectorSpeciesAPI = {
  /** Full species catalog (caller filters by active flag) */
  getAll: (sampleTypeId) =>
    get(
      sampleTypeId
        ? `${SPECIES_BASE}?sampleTypeId=${encodeURIComponent(sampleTypeId)}`
        : SPECIES_BASE,
    ),
};

export const VectorSamplingSiteAPI = {
  /** Sampling-site registry — feeds the multi-site decon override picker. */
  getAll: () => get(SAMPLING_SITE_BASE),
};
