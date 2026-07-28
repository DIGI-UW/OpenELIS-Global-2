import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
  putToOpenElisServerFullResponse,
} from "../utils/Utils";

export const getCaseDetail = (caseId) =>
  new Promise((resolve) => {
    getFromOpenElisServer(`/rest/microbiology/cases/${caseId}`, resolve);
  });

export const recordCaseActivity = (caseId, payload) =>
  new Promise((resolve) => {
    postToOpenElisServerJsonResponse(
      `/rest/microbiology/cases/${caseId}/activities`,
      JSON.stringify(payload),
      resolve,
    );
  });

export const createIsolate = (payload) =>
  new Promise((resolve) => {
    postToOpenElisServerJsonResponse(
      "/rest/microbiology/isolates",
      JSON.stringify(payload),
      resolve,
    );
  });

export const updateIsolateIdentification = (isolateId, payload) =>
  new Promise((resolve) => {
    putToOpenElisServerFullResponse(
      `/rest/microbiology/isolates/${encodeURIComponent(isolateId)}/identification`,
      JSON.stringify(payload),
      (response) => {
        if (!response) {
          resolve({ status: 0 });
          return;
        }
        response.json().then(resolve);
      },
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

export const getOrganisms = () =>
  new Promise((resolve) => {
    getFromOpenElisServer("/rest/microbiology/reference/organisms", resolve);
  });

export const getBreakpointStandards = () =>
  new Promise((resolve) => {
    getFromOpenElisServer(
      "/rest/microbiology/reference/breakpoint-standards",
      resolve,
    );
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
      JSON.stringify(payload),
      resolve,
    );
  });

export const recordAstReading = (runId, payload) =>
  new Promise((resolve) => {
    postToOpenElisServerJsonResponse(
      `/rest/microbiology/ast/runs/${runId}/readings`,
      JSON.stringify(payload),
      resolve,
    );
  });

export const overrideAstReading = (readingId, payload) =>
  new Promise((resolve) => {
    putToOpenElisServerFullResponse(
      `/rest/microbiology/ast/readings/${readingId}/override`,
      JSON.stringify(payload),
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
      JSON.stringify({}),
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

export const getWorklistRows = (query = {}) =>
  new Promise((resolve) => {
    const params = new URLSearchParams();
    [
      "workflow",
      "stage",
      "urgency",
      "due",
      "q",
      "sort",
      "page",
      "pageSize",
    ].forEach((key) => {
      if (query[key]) {
        params.set(key, query[key]);
      }
    });
    const search = params.toString();
    getFromOpenElisServer(
      `/rest/microbiology/worklist${search ? `?${search}` : ""}`,
      resolve,
    );
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
      JSON.stringify(payload),
      resolve,
    );
  });

export const acknowledgeCriticalCommunication = (communicationId) =>
  new Promise((resolve) => {
    putToOpenElisServerFullResponse(
      `/rest/microbiology/critical-communications/${communicationId}/acknowledge`,
      JSON.stringify({}),
      (response) => {
        if (!response) {
          resolve({ status: 0 });
          return;
        }
        response.json().then(resolve);
      },
    );
  });

export const closeCriticalCommunication = (communicationId, payload) =>
  new Promise((resolve) => {
    putToOpenElisServerFullResponse(
      `/rest/microbiology/critical-communications/${communicationId}/close`,
      JSON.stringify(payload),
      (response) => {
        if (!response) {
          resolve({ status: 0 });
          return;
        }
        response.json().then(resolve);
      },
    );
  });

export const getReportProjection = (caseId) =>
  new Promise((resolve) => {
    getFromOpenElisServer(
      `/rest/microbiology/cases/${caseId}/release/preview`,
      resolve,
    );
  });

export const releasePreliminaryReport = (caseId) =>
  new Promise((resolve) => {
    postToOpenElisServerJsonResponse(
      `/rest/microbiology/cases/${caseId}/release/preliminary`,
      JSON.stringify({}),
      resolve,
    );
  });

export const releaseFinalReport = (caseId) =>
  new Promise((resolve) => {
    postToOpenElisServerJsonResponse(
      `/rest/microbiology/cases/${caseId}/release/final`,
      JSON.stringify({}),
      resolve,
    );
  });

export const saveOrderDetail = (caseId, payload) =>
  new Promise((resolve) => {
    putToOpenElisServerFullResponse(
      `/rest/microbiology/cases/${caseId}/order-detail`,
      JSON.stringify(payload),
      (response) => {
        if (!response) {
          resolve({ status: 0 });
          return;
        }
        response.json().then(resolve);
      },
    );
  });

export const getWhonetReadiness = (caseId) =>
  new Promise((resolve) => {
    getFromOpenElisServer(
      `/rest/microbiology/cases/${caseId}/whonet-readiness`,
      resolve,
    );
  });

const MicrobiologyService = {
  getCaseDetail,
  recordCaseActivity,
  createIsolate,
  updateIsolateIdentification,
  getAstPanels,
  getAntibiotics,
  getOrganisms,
  getBreakpointStandards,
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
  closeCriticalCommunication,
  getReportProjection,
  releasePreliminaryReport,
  releaseFinalReport,
  saveOrderDetail,
  getWhonetReadiness,
};

export default MicrobiologyService;
