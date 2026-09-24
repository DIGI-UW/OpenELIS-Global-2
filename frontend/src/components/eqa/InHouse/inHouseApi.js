// Data seam for the in-house blinding wizard (OGC-612). Every call here
// is live: the panel, seal and label endpoints shipped first, and the wizard added
// the cycle/panel creates the wizard needs. No mocks.
import config from "../../../config.json";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
  putToOpenElisServer,
} from "../../utils/Utils";
import { asList } from "../eqaApi";
import { downloadBlob } from "../eqaCommon";

export const fetchInHouseSchemes = (callback) => {
  getFromOpenElisServer("/rest/eqa/programs", (data) =>
    callback(
      asList(data).filter(
        (scheme) =>
          scheme.schemeType === "IN_HOUSE" && scheme.isActive !== false,
      ),
    ),
  );
};

export const fetchPanelsForScheme = (schemeId, callback) => {
  getFromOpenElisServer(`/rest/eqa/panels?schemeId=${schemeId}`, (data) =>
    callback(asList(data)),
  );
};

export const fetchAnalysts = (schemeId, callback) => {
  getFromOpenElisServer(`/rest/eqa/programs/${schemeId}/analysts`, (data) =>
    callback(asList(data)),
  );
};

// The lab's users, for adding someone to a scheme's roster. Reuses the NCE
// assignment autocomplete rather than a second copy of the same query.
export const fetchLabUsers = (callback) => {
  getFromOpenElisServer("/rest/nce/users", (data) => callback(asList(data)));
};

export const saveAnalystRoster = (schemeId, systemUserIds, callback) => {
  putToOpenElisServer(
    `/rest/eqa/programs/${schemeId}/analysts`,
    JSON.stringify({ systemUserIds }),
    callback,
  );
};

// Seals the panel and creates one blinded order per sample. The
// response carries orderAccessionNumbers — the blind codes now live in the
// analyst queue and the Workplan.
export const sealAndDistribute = (panelId, orders, callback) => {
  postToOpenElisServerJsonResponse(
    `/rest/eqa/panels/${panelId}/seal-and-distribute`,
    JSON.stringify({ orders }),
    callback,
  );
};

export const unblindPanel = (panelId, callback) => {
  postToOpenElisServerJsonResponse(
    `/rest/eqa/panels/${panelId}/unblind`,
    JSON.stringify({}),
    callback,
  );
};

// The label sheet is a GET that answers application/pdf, so it needs
// a plain fetch — the shared helpers all post.
export const downloadLabelSheet = (panelId, onError) => {
  fetch(`${config.serverBaseUrl}/rest/eqa/panels/${panelId}/labels`, {
    credentials: "include",
  })
    .then((response) => {
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      return response.blob();
    })
    .then((blob) => downloadBlob(blob, `eqa-panel-${panelId}-labels.pdf`))
    .catch((error) => {
      if (onError) {
        onError(error);
      }
    });
};
