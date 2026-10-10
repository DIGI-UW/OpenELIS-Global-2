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
const caseUrl = (id) => `/rest/microbiology/cases/${encodeURIComponent(id)}`;
const mutate = (url, body) =>
  new Promise((resolve, reject) =>
    postToOpenElisServerJsonResponse(url, JSON.stringify(body), (data) =>
      data && typeof data.status !== "number" && !data.error
        ? resolve(data)
        : reject(
            Object.assign(new Error("request failed"), { response: data }),
          ),
    ),
  );
export const getTests = (id) => read(`${caseUrl(id)}/analyses`);
export const addTests = (id, body) => mutate(`${caseUrl(id)}/analyses`, body);
export const saveResults = (id, analysisId, body) =>
  mutate(
    `${caseUrl(id)}/analyses/${encodeURIComponent(analysisId)}/results`,
    body,
  );
export const setTestedElsewhere = (id, analysisId, body) =>
  mutate(
    `${caseUrl(id)}/analyses/${encodeURIComponent(analysisId)}/tested-elsewhere`,
    body,
  );
export const validateResult = (id, analysisId, version) =>
  mutate(`${caseUrl(id)}/analyses/${encodeURIComponent(analysisId)}/validate`, {
    version,
  });
export const getTimeline = (id) => read(`${caseUrl(id)}/timeline`);
export const addNote = (id, text) => mutate(`${caseUrl(id)}/notes`, { text });
export const getCultures = (id) => read(`${caseUrl(id)}/cultures`);
export const getCultureOptions = (id) =>
  read(`${caseUrl(id)}/cultures/options`);
export const inoculateCulture = (id, body) =>
  mutate(`${caseUrl(id)}/cultures`, body);
export const cultureAction = (id, rowId, action, body) =>
  mutate(
    `${caseUrl(id)}/cultures/${encodeURIComponent(rowId)}/${encodeURIComponent(action)}`,
    body,
  );
export default {
  getCultures,
  getCultureOptions,
  inoculateCulture,
  cultureAction,
  searchCases,
  getCase,
  transferCase,
  getTests,
  addTests,
  saveResults,
  setTestedElsewhere,
  validateResult,
  getTimeline,
  addNote,
};
