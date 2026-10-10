import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../utils/Utils";
const read = (url) =>
  new Promise((resolve, reject) =>
    getFromOpenElisServer(url, (data) =>
      data && typeof data.status !== "number" && !data.error
        ? resolve(data)
        : reject(new Error("request failed")),
    ),
  );
export const searchCases = (query) =>
  read(`/rest/microbiology/cases/search?${query}`);
export const getCase = (id) =>
  read(`/rest/microbiology/cases/${encodeURIComponent(id)}/shell`);
export const transferCase = (id, labUnitId) =>
  new Promise((resolve, reject) =>
    postToOpenElisServerJsonResponse(
      `/rest/microbiology/cases/${encodeURIComponent(id)}/transfer`,
      JSON.stringify({ labUnitId }),
      (data) =>
        data && typeof data.status !== "number" && !data.error
          ? resolve(data)
          : reject(new Error("request failed")),
    ),
  );
export default { searchCases, getCase, transferCase };
