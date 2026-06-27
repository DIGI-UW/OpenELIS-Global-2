import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
  putToOpenElisServerFullResponse,
} from "../utils/Utils";

const DEFAULT_USER_ID = "1";

export const getCaseDetail = (caseId) =>
  new Promise((resolve) => {
    getFromOpenElisServer(`/rest/microbiology/cases/${caseId}`, resolve);
  });

export const recordCaseActivity = (caseId, payload) =>
  new Promise((resolve) => {
    postToOpenElisServerJsonResponse(
      `/rest/microbiology/cases/${caseId}/activities`,
      JSON.stringify({ performedBy: DEFAULT_USER_ID, ...payload }),
      resolve,
    );
  });

export const createIsolate = (payload) =>
  new Promise((resolve) => {
    postToOpenElisServerJsonResponse(
      "/rest/microbiology/isolates",
      JSON.stringify({ performedBy: DEFAULT_USER_ID, ...payload }),
      resolve,
    );
  });

export const getAstPanels = (workflowType) =>
  new Promise((resolve) => {
    getFromOpenElisServer(
      `/rest/microbiology/reference/ast-panels?workflowType=${encodeURIComponent(
        workflowType,
      )}`,
      resolve,
    );
  });

export const getAntibiotics = () =>
  new Promise((resolve) => {
    getFromOpenElisServer("/rest/microbiology/reference/antibiotics", resolve);
  });

export const getAstRunsForIsolate = (isolateId) =>
  new Promise((resolve) => {
    getFromOpenElisServer(
      `/rest/microbiology/ast/runs?isolateId=${encodeURIComponent(isolateId)}`,
      resolve,
    );
  });

export const startAstRun = (payload) =>
  new Promise((resolve) => {
    postToOpenElisServerJsonResponse(
      "/rest/microbiology/ast/runs",
      JSON.stringify({ performedBy: DEFAULT_USER_ID, ...payload }),
      resolve,
    );
  });

export const recordAstReading = (runId, payload) =>
  new Promise((resolve) => {
    postToOpenElisServerJsonResponse(
      `/rest/microbiology/ast/runs/${runId}/readings`,
      JSON.stringify({ performedBy: DEFAULT_USER_ID, ...payload }),
      resolve,
    );
  });

export const overrideAstReading = (readingId, payload) =>
  new Promise((resolve) => {
    putToOpenElisServerFullResponse(
      `/rest/microbiology/ast/readings/${readingId}/override`,
      JSON.stringify({ performedBy: DEFAULT_USER_ID, ...payload }),
      (response) => {
        if (!response) {
          resolve({ status: 0 });
          return;
        }
        response.json().then(resolve);
      },
    );
  });

export const reviewAstRun = (runId) =>
  new Promise((resolve) => {
    postToOpenElisServerJsonResponse(
      `/rest/microbiology/ast/runs/${runId}/review`,
      JSON.stringify({ performedBy: DEFAULT_USER_ID }),
      resolve,
    );
  });

export const getCaseReadiness = (caseId) =>
  new Promise((resolve) => {
    getFromOpenElisServer(
      `/rest/microbiology/cases/${caseId}/readiness`,
      resolve,
    );
  });

export const getWorklistRows = () =>
  new Promise((resolve) => {
    getFromOpenElisServer("/rest/microbiology/worklist", resolve);
  });

export const getCriticalCommunications = (caseId) =>
  new Promise((resolve) => {
    getFromOpenElisServer(
      `/rest/microbiology/cases/${caseId}/critical-communications`,
      resolve,
    );
  });

export const logCriticalCommunication = (caseId, payload) =>
  new Promise((resolve) => {
    postToOpenElisServerJsonResponse(
      `/rest/microbiology/cases/${caseId}/critical-communications`,
      JSON.stringify({ performedBy: DEFAULT_USER_ID, ...payload }),
      resolve,
    );
  });

export const acknowledgeCriticalCommunication = (communicationId) =>
  new Promise((resolve) => {
    putToOpenElisServerFullResponse(
      `/rest/microbiology/critical-communications/${communicationId}/acknowledge`,
      JSON.stringify({ performedBy: DEFAULT_USER_ID }),
      (response) => {
        if (!response) {
          resolve({ status: 0 });
          return;
        }
        response.json().then(resolve);
      },
    );
  });

const MicrobiologyService = {
  getCaseDetail,
  recordCaseActivity,
  createIsolate,
  getAstPanels,
  getAntibiotics,
  getAstRunsForIsolate,
  startAstRun,
  recordAstReading,
  overrideAstReading,
  reviewAstRun,
  getCaseReadiness,
  getWorklistRows,
  getCriticalCommunications,
  logCriticalCommunication,
  acknowledgeCriticalCommunication,
};

export default MicrobiologyService;
