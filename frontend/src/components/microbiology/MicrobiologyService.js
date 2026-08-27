import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
  putToOpenElisServerFullResponse,
} from "../utils/Utils";

export const getPatientOrigins = (organizationId) =>
  new Promise((resolve) => {
    const query = organizationId
      ? `?organizationId=${encodeURIComponent(organizationId)}`
      : "";
    getFromOpenElisServer(
      `/rest/microbiology/reference/patient-origins${query}`,
      resolve,
    );
  });

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

export const getAstPanelAntibiotics = (panelId) =>
  new Promise((resolve) => {
    getFromOpenElisServer(
      `/rest/microbiology/ast/panels/${encodeURIComponent(panelId)}/antibiotics`,
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

export const getCaseProtocolOptions = (caseId) =>
  new Promise((resolve) => {
    getFromOpenElisServer(
      `/rest/microbiology/cases/${encodeURIComponent(caseId)}/protocol/options`,
      resolve,
    );
  });

export const changeCaseProtocol = (caseId, payload) =>
  new Promise((resolve) => {
    putToOpenElisServerFullResponse(
      `/rest/microbiology/cases/${encodeURIComponent(caseId)}/protocol`,
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

export const revertAstOverride = (readingId, payload) =>
  new Promise((resolve) => {
    postToOpenElisServerJsonResponse(
      `/rest/microbiology/ast/readings/${readingId}/override/revert`,
      JSON.stringify(payload),
      resolve,
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

const MicrobiologyService = {
  getPatientOrigins,
  getCaseDetail,
  recordCaseActivity,
  getCaseInoculations,
  recordCaseInoculation,
  getCaseTimeline,
  addCaseNote,
  createIsolate,
  updateIsolateIdentification,
  saveOrderDetail,
  getAstPanels,
  getAstSetupForIsolate,
  getAstPanelAntibiotics,
  getAntibiotics,
  getOrganisms,
  getBreakpointStandards,
  getCultureMethods,
  changeCaseWorkflow,
  getCaseProtocolOptions,
  changeCaseProtocol,
  getAstRunsForIsolate,
  startAstRun,
  recordAstReading,
  overrideAstReading,
  revertAstOverride,
  reviewAstRun,
  getCaseReadiness,
  getNceCategories,
  getNceReportingUnits,
  reportCaseNonconformance,
};

export default MicrobiologyService;
