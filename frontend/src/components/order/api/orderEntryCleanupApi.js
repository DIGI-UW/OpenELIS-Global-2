/**
 * API utilities for the clinical order entry v4 clean-up (OGC-1424, FRS v0.20
 * slice M14): the possible-match check before Create (FR-B6a), Mark tested
 * elsewhere (FR-B20), and the Handling group's catalog requirement and storage
 * location (FR-C9a).
 */

import {
  deleteFromOpenElisServerFullResponse,
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
  putToOpenElisServerFullResponse,
} from "../../utils/Utils";

export const RECORD_KIND = {
  PATIENT: "patient",
  FACILITY: "facility",
  PROVIDER: "provider",
};

const queryString = (params) =>
  Object.entries(params || {})
    .filter(
      ([, value]) =>
        value !== undefined && value !== null && String(value).trim() !== "",
    )
    .map(([key, value]) => `${key}=${encodeURIComponent(String(value).trim())}`)
    .join("&");

const getJson = (endPoint) =>
  new Promise((resolve, reject) => {
    getFromOpenElisServer(endPoint, (response) => {
      if (response === undefined) {
        reject(new Error(`Request failed: ${endPoint}`));
      } else {
        resolve(response);
      }
    });
  });

const sendJson = (send, endPoint, body) =>
  new Promise((resolve, reject) => {
    const onResponse = (response) => {
      if (!response) {
        reject(new Error(`Request failed: ${endPoint}`));
        return;
      }
      response
        .json()
        .catch(() => ({}))
        .then((json) => {
          if (response.ok) {
            resolve(json);
          } else {
            const error = new Error(json?.message || `HTTP ${response.status}`);
            error.status = response.status;
            reject(error);
          }
        });
    };
    if (body === undefined) {
      send(endPoint, onResponse);
    } else {
      send(endPoint, JSON.stringify(body), onResponse);
    }
  });

/**
 * Up to five existing records that look like the one being created, each
 * with `matchedOn` ("name", "dateOfBirth", "identifier", "code").
 * Rejects when the check could not run, so the caller can say so.
 */
export const findPossibleMatches = (kind, params) =>
  getJson(`/rest/possible-matches/${kind}?${queryString(params)}`).then(
    (response) => (Array.isArray(response?.matches) ? response.matches : []),
  );

/** Records Create new anyway: what was entered and which matches were shown. */
export const recordCreateAnyway = (kind, entered, matches) =>
  sendJson(
    postToOpenElisServerFullResponse,
    `/rest/possible-matches/${kind}/override`,
    { entered, matches },
  );

const TESTED_ELSEWHERE = "/rest/order-tests/tested-elsewhere";

export const listTestedElsewhere = (labNumber) =>
  getJson(`${TESTED_ELSEWHERE}?labNumber=${encodeURIComponent(labNumber)}`);

export const markTestedElsewhere = ({
  labNumber,
  testId,
  performingLabId,
  reportedValue,
}) =>
  sendJson(putToOpenElisServerFullResponse, TESTED_ELSEWHERE, {
    labNumber,
    testId: String(testId),
    performingLabId: performingLabId ? String(performingLabId) : "",
    reportedValue: reportedValue || "",
  });

export const unmarkTestedElsewhere = (labNumber, testId) =>
  sendJson(
    deleteFromOpenElisServerFullResponse,
    `${TESTED_ELSEWHERE}?labNumber=${encodeURIComponent(labNumber)}&testId=${encodeURIComponent(testId)}`,
  );

/** The test catalog's storage condition and holding time per test. */
export const getHandlingRequirements = (testIds) => {
  const ids = (testIds || []).filter(Boolean).join(",");
  if (!ids) {
    return Promise.resolve([]);
  }
  return getJson(
    `/rest/sample-handling/requirements?testIds=${encodeURIComponent(ids)}`,
  ).then((response) => (Array.isArray(response) ? response : []));
};

/** Where a saved sample is stored and its device's temperature setting. */
export const getSampleStorageLocation = (sampleItemId) =>
  getJson(`/rest/storage/sample-items/${encodeURIComponent(sampleItemId)}`);
