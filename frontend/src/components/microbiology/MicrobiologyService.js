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

const MicrobiologyService = {
  getCaseDetail,
  recordCaseActivity,
  createIsolate,
  saveOrderDetail,
};

export default MicrobiologyService;
