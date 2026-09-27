/**
 * API utilities for the S-09 (OGC-580) Sample Acceptance Checklist — the
 * pre-analytical intake-acceptance gate, run per specimen at the Collect
 * (reception) step.
 *
 * Per-specimen evaluation/record/resample are keyed by the backend
 * sample_item(id), matching `sample_acceptance_record.sample_item_id REFERENCES
 * sample_item(id)`. The Collect → Label & Store gate is order-level: it
 * aggregates every specimen of the order (`/sample/{sampleId}/gate`).
 *
 * Distinct from `/rest/qa-checklist` (OGC-356 order-level QA verification).
 */

import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";

const BASE_URL = "/rest/sample-acceptance-checklist";

/** Answer vocabulary the gate recognises (anything else is rejected 400). */
export const ANSWER = { PASS: "PASS", FAIL: "FAIL", NA: "NA" };

/**
 * Resolved acceptance evaluation for one specimen: domain, enforcement mode,
 * resolved checklist items, the latest recorded answers, overall status,
 * blocked flag, and the parent order's resample cross-references.
 * @param {string|number} sampleItemId
 * @returns {Promise<Object>}
 */
export const getSampleItemEvaluation = (sampleItemId) => {
  return new Promise((resolve, reject) => {
    getFromOpenElisServer(`${BASE_URL}/sample-item/${sampleItemId}`, (resp) => {
      if (resp && !resp.error && resp.sampleItemId !== undefined) {
        resolve(resp);
      } else {
        reject(
          new Error(resp?.error || "Failed to load acceptance evaluation"),
        );
      }
    });
  });
};

/**
 * Per-specimen acceptance statuses for a whole order — one entry per non-voided
 * sample_item ({ sampleItemId, overallStatus, blocked, domain }). Backs the
 * QA-step intake-acceptance table; resolves to [] on error.
 * @param {string|number} sampleId the order's sample(id)
 * @returns {Promise<Array<{sampleItemId:number,overallStatus:string,blocked:boolean,domain:string}>>}
 */
export const getOrderItemEvaluations = (sampleId) => {
  return new Promise((resolve) => {
    getFromOpenElisServer(`${BASE_URL}/order/${sampleId}/items`, (resp) =>
      resolve(Array.isArray(resp) ? resp : []),
    );
  });
};

/**
 * Record an acceptance assessment for one specimen (append-only). Returns the
 * refreshed evaluation.
 * @param {string|number} sampleItemId
 * @param {Array<{itemKey:string,label:string,answer:string,note?:string}>} answers
 * @returns {Promise<Object>}
 */
export const recordAssessment = (sampleItemId, answers) => {
  return new Promise((resolve, reject) => {
    postToOpenElisServerJsonResponse(
      `${BASE_URL}/sample-item/${sampleItemId}`,
      JSON.stringify({ answers }),
      (json) => {
        if (json && !json.error && json.sampleItemId !== undefined) {
          resolve(json);
        } else {
          reject(new Error(json?.error || "Failed to record acceptance"));
        }
      },
    );
  });
};

/**
 * FR-08 server-side enforcement gate for a whole order (the Collect → Label &
 * Store backstop). Resolves { blocked: boolean }. The endpoint returns 200
 * {blocked:false} when every specimen may proceed and 409 {blocked:true} when
 * any non-voided, non-rejected specimen is unsatisfied under MANDATORY
 * enforcement; getFromOpenElisServer parses the JSON body for both statuses.
 * @param {string|number} sampleId the order's sample(id)
 */
export const getAcceptanceGate = (sampleId) => {
  return new Promise((resolve) => {
    getFromOpenElisServer(`${BASE_URL}/sample/${sampleId}/gate`, (resp) =>
      resolve({ blocked: !!(resp && resp.blocked) }),
    );
  });
};

/**
 * Per-domain enforcement modes (FR-08). Resolves
 * { clinical, environmental, vector } where each value is
 * MANDATORY | OPTIONAL | OFF. Used to decide whether to render the Intake
 * Acceptance section at all (OFF = hidden for that domain). Resolves an empty
 * object on failure so callers fail open (render the section).
 * @returns {Promise<{clinical?:string,environmental?:string,vector?:string}>}
 */
export const getEnforcement = () => {
  return new Promise((resolve) => {
    getFromOpenElisServer(`${BASE_URL}/enforcement`, (resp) =>
      resolve(resp && !resp.error ? resp : {}),
    );
  });
};

/**
 * Resample one failed specimen (FR-10): atomically record an NCE, reject just
 * that specimen (the order's accepted specimens proceed), and create a
 * pre-populated draft replacement order carrying only this specimen. Returns the
 * new accession + ids.
 * @param {string|number} sampleItemId
 * @param {string} reason
 * @returns {Promise<{originalSampleId:number,newSampleId:number,newAccessionNumber:string,nceId:number}>}
 */
export const resampleSample = (sampleItemId, reason) => {
  return new Promise((resolve, reject) => {
    postToOpenElisServerJsonResponse(
      `${BASE_URL}/sample-item/${sampleItemId}/resample`,
      JSON.stringify({ reason }),
      (json) => {
        if (json && !json.error && json.newSampleId) {
          resolve(json);
        } else {
          reject(new Error(json?.error || "Failed to resample"));
        }
      },
    );
  });
};

/**
 * Accept/record an acceptance assessment for an entire vector pool — the pool is
 * the unit of acceptance for vector. Cascades the same answers to every live
 * member sample_item; returns a representative member's refreshed evaluation
 * (same shape as recordAssessment).
 * @param {string|number} vectorPoolId
 * @param {Array<{itemKey:string,label:string,answer:string,note?:string}>} answers
 * @returns {Promise<Object>}
 */
export const recordPoolAssessment = (vectorPoolId, answers) => {
  return new Promise((resolve, reject) => {
    postToOpenElisServerJsonResponse(
      `${BASE_URL}/pool/${vectorPoolId}`,
      JSON.stringify({ answers }),
      (json) => {
        if (json && !json.error && json.sampleItemId !== undefined) {
          resolve(json);
        } else {
          reject(new Error(json?.error || "Failed to record pool acceptance"));
        }
      },
    );
  });
};

/**
 * Reject an entire vector pool — cascade a plain reject (no replacement order,
 * unlike resample) to every member sample_item. Resolves on { rejected: true }.
 * @param {string|number} vectorPoolId
 * @param {string} reason
 * @returns {Promise<Object>}
 */
export const rejectPool = (vectorPoolId, reason) => {
  return new Promise((resolve, reject) => {
    postToOpenElisServerJsonResponse(
      `${BASE_URL}/pool/${vectorPoolId}/reject`,
      JSON.stringify({ reason }),
      (json) => {
        if (json && !json.error && json.rejected) {
          resolve(json);
        } else {
          reject(new Error(json?.error || "Failed to reject pool"));
        }
      },
    );
  });
};

/**
 * Plainly reject a single (non-pooled vector) specimen — no replacement order.
 * Resolves on { rejected: true }.
 * @param {string|number} sampleItemId
 * @param {string} reason
 * @returns {Promise<Object>}
 */
export const rejectSampleItem = (sampleItemId, reason) => {
  return new Promise((resolve, reject) => {
    postToOpenElisServerJsonResponse(
      `${BASE_URL}/sample-item/${sampleItemId}/reject`,
      JSON.stringify({ reason }),
      (json) => {
        if (json && !json.error && json.rejected) {
          resolve(json);
        } else {
          reject(new Error(json?.error || "Failed to reject specimen"));
        }
      },
    );
  });
};
