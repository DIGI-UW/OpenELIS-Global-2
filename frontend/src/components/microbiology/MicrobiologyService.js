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

export const getReagentLotOverview = (caseId) =>
  new Promise((resolve) => {
    getFromOpenElisServer(
      `/rest/microbiology/cases/${encodeURIComponent(caseId)}/reagent-lots`,
      resolve,
    );
  });

export const getCaseInoculations = (caseId) =>
  new Promise((resolve) => {
    getFromOpenElisServer(
      `/rest/microbiology/cases/${encodeURIComponent(caseId)}/inoculations`,
      resolve,
    );
  });

export const recordCaseInoculation = (caseId, payload) =>
  new Promise((resolve) => {
    postToOpenElisServerJsonResponse(
      `/rest/microbiology/cases/${encodeURIComponent(caseId)}/inoculations`,
      JSON.stringify(payload),
      resolve,
    );
  });

export const getCaseTimeline = (caseId) =>
  new Promise((resolve) => {
    getFromOpenElisServer(
      `/rest/microbiology/cases/${encodeURIComponent(caseId)}/timeline`,
      resolve,
    );
  });

export const addCaseNote = (caseId, text) =>
  new Promise((resolve) => {
    postToOpenElisServerJsonResponse(
      `/rest/microbiology/cases/${encodeURIComponent(caseId)}/notes`,
      JSON.stringify({ text }),
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

export const getAstSetupForIsolate = (isolateId) =>
  new Promise((resolve) => {
    getFromOpenElisServer(
      `/rest/microbiology/ast/setup?isolateId=${encodeURIComponent(isolateId)}`,
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

export const getCultureMethods = (workflowType) =>
  new Promise((resolve) => {
    getFromOpenElisServer(
      `/rest/microbiology/reference/culture-methods?workflowType=${encodeURIComponent(
        workflowType,
      )}`,
      resolve,
    );
  });

export const changeCaseWorkflow = (caseId, payload) =>
  new Promise((resolve) => {
    putToOpenElisServerFullResponse(
      `/rest/microbiology/cases/${encodeURIComponent(caseId)}/workflow`,
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

export const startRepeatAstRun = (sourceRunId, payload) =>
  new Promise((resolve) => {
    postToOpenElisServerJsonResponse(
      `/rest/microbiology/ast/runs/${encodeURIComponent(sourceRunId)}/attempts`,
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

export const selectReportableAstRun = (runId) =>
  new Promise((resolve) => {
    postToOpenElisServerJsonResponse(
      `/rest/microbiology/ast/runs/${encodeURIComponent(runId)}/reportable`,
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

export const getNceCategories = () =>
  new Promise((resolve) => {
    getFromOpenElisServer("/rest/nce/categories", resolve);
  });

export const getNceReportingUnits = () =>
  new Promise((resolve) => {
    getFromOpenElisServer("/rest/displayList/TEST_SECTION_ACTIVE", resolve);
  });

export const reportCaseNonconformance = (caseId, payload) =>
  new Promise((resolve) => {
    postToOpenElisServerJsonResponse(
      `/rest/microbiology/cases/${encodeURIComponent(caseId)}/nonconformances`,
      JSON.stringify(payload),
      resolve,
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

export const getCaseAmendments = (caseId) =>
  new Promise((resolve) => {
    getFromOpenElisServer(
      `/rest/microbiology/cases/${caseId}/amendments`,
      resolve,
    );
  });

export const getCaseReportVersions = (caseId) =>
  new Promise((resolve) => {
    getFromOpenElisServer(
      `/rest/microbiology/cases/${caseId}/amendments/report-versions`,
      resolve,
    );
  });

export const openCaseAmendment = (caseId, payload) =>
  new Promise((resolve) => {
    postToOpenElisServerJsonResponse(
      `/rest/microbiology/cases/${caseId}/amendments`,
      JSON.stringify(payload),
      resolve,
    );
  });

export const cancelCaseAmendment = (caseId, payload) =>
  new Promise((resolve) => {
    postToOpenElisServerJsonResponse(
      `/rest/microbiology/cases/${caseId}/amendments/current/cancel`,
      JSON.stringify(payload),
      resolve,
    );
  });

export const releaseAmendedReport = (caseId) =>
  new Promise((resolve) => {
    postToOpenElisServerJsonResponse(
      `/rest/microbiology/cases/${caseId}/release/amended`,
      JSON.stringify({}),
      resolve,
    );
  });

export const getIdentificationHistory = (isolateId) =>
  new Promise((resolve) => {
    getFromOpenElisServer(
      `/rest/microbiology/isolates/${encodeURIComponent(
        isolateId,
      )}/identification-history`,
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
  getCaseInoculations,
  recordCaseInoculation,
  getCaseTimeline,
  addCaseNote,
  getReagentLotOverview,
  createIsolate,
  updateIsolateIdentification,
  getAstPanels,
  getAstSetupForIsolate,
  getAntibiotics,
  getOrganisms,
  getBreakpointStandards,
  getCultureMethods,
  changeCaseWorkflow,
  getAstRunsForIsolate,
  startAstRun,
  startRepeatAstRun,
  recordAstReading,
  overrideAstReading,
  reviewAstRun,
  selectReportableAstRun,
  getCaseReadiness,
  getWorklistRows,
  getCriticalCommunications,
  logCriticalCommunication,
  acknowledgeCriticalCommunication,
  closeCriticalCommunication,
  getNceCategories,
  getNceReportingUnits,
  reportCaseNonconformance,
  getReportProjection,
  releasePreliminaryReport,
  releaseFinalReport,
  getCaseAmendments,
  getCaseReportVersions,
  openCaseAmendment,
  cancelCaseAmendment,
  releaseAmendedReport,
  getIdentificationHistory,
  saveOrderDetail,
  getWhonetReadiness,
};

export default MicrobiologyService;
