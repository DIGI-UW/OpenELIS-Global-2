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
export const addNote = (id, text, sendWithResult = false) =>
  mutate(`${caseUrl(id)}/notes`, { text, sendWithResult });
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
export const getIsolates = (id) => read(`${caseUrl(id)}/isolates`);
export const getIsolateOptions = (id) =>
  read(`${caseUrl(id)}/isolates/options`);
export const pickIsolate = (id, body) =>
  mutate(`${caseUrl(id)}/isolates`, body);
export const referSpecimen = (id, body) =>
  mutate(`${caseUrl(id)}/isolates/referrals`, body);
export const isolateAction = (id, isolateId, action, body) =>
  mutate(
    `${caseUrl(id)}/isolates/${encodeURIComponent(isolateId)}/${encodeURIComponent(action)}`,
    body,
  );
export const getAstWorkspace = (id) => read(`${caseUrl(id)}/ast`);
export const astAction = (id, body, validation = false) =>
  mutate(`${caseUrl(id)}/ast/${validation ? "validation" : "entry"}`, body);
export const resolveDst = (id, body) =>
  mutate(`${caseUrl(id)}/dst/resolve`, body);
export const getReport = (id) => read(`${caseUrl(id)}/report`);
export const getReportVersions = (id) => read(`${caseUrl(id)}/report/versions`);
export const getReadiness = (id) => read(`${caseUrl(id)}/readiness`);
export const selectReportResult = (id, analysisId, selected) =>
  mutate(
    `${caseUrl(id)}/analyses/${encodeURIComponent(analysisId)}/report-selection`,
    { selected },
  );
export const releaseReport = (id, type) =>
  mutate(`${caseUrl(id)}/release/${encodeURIComponent(type)}`, {});
export default {
  getAstWorkspace,
  astAction,
  resolveDst,
  getReport,
  getReportVersions,
  getReadiness,
  selectReportResult,
  releaseReport,
  getIsolates,
  getIsolateOptions,
  pickIsolate,
  referSpecimen,
  isolateAction,
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
