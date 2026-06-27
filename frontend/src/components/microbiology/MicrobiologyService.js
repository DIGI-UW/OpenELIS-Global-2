import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
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

const MicrobiologyService = {
  getCaseDetail,
  recordCaseActivity,
  createIsolate,
};

export default MicrobiologyService;
