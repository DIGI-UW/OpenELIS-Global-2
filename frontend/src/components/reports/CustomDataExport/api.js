import {
  deleteFromOpenElisServerFullResponse,
  postToOpenElisServerFullResponse,
  putToOpenElisServerFullResponse,
} from "../../utils/Utils";
import config from "../../../config.json";

export const reportingPath = "/rest/reports/data-export";
export const downloadUrl = (id) =>
  `${config.serverBaseUrl}${reportingPath}/jobs/${encodeURIComponent(id)}/download`;
export const submitReport = (request) =>
  new Promise((resolve, reject) => {
    postToOpenElisServerFullResponse(
      `${reportingPath}/jobs`,
      JSON.stringify(request),
      async (response) => {
        if (!response) {
          reject(new Error("reporting.networkError"));
          return;
        }
        const body = await response.json().catch(() => ({}));
        if (!response.ok) {
          reject(new Error(body.code || "reporting.requestError"));
          return;
        }
        resolve(body);
      },
    );
  });

const jsonResponse = (invoke) =>
  new Promise((resolve, reject) => {
    invoke(async (response) => {
      if (!response) {
        reject(new Error("reporting.networkError"));
        return;
      }
      const body =
        response.status === 204 ? {} : await response.json().catch(() => ({}));
      if (!response.ok) {
        reject(new Error(body.code || "reporting.requestError"));
        return;
      }
      resolve(body);
    });
  });

export const createSavedReport = (request) =>
  jsonResponse((done) =>
    postToOpenElisServerFullResponse(
      `${reportingPath}/saved-configs`,
      JSON.stringify(request),
      done,
    ),
  );

export const updateSavedReport = ({ id, ...request }) =>
  jsonResponse((done) =>
    putToOpenElisServerFullResponse(
      `${reportingPath}/saved-configs/${encodeURIComponent(id)}`,
      JSON.stringify(request),
      done,
    ),
  );

export const deleteSavedReport = ({ id, expectedVersion }) =>
  jsonResponse((done) =>
    deleteFromOpenElisServerFullResponse(
      `${reportingPath}/saved-configs/${encodeURIComponent(id)}?expectedVersion=${encodeURIComponent(expectedVersion)}`,
      done,
    ),
  );

export const recoverReport = ({ id, action, clientRequestId }) =>
  jsonResponse((done) =>
    postToOpenElisServerFullResponse(
      `${reportingPath}/jobs/${encodeURIComponent(id)}/${action}`,
      JSON.stringify(action === "retry" ? { clientRequestId } : {}),
      done,
    ),
  );
