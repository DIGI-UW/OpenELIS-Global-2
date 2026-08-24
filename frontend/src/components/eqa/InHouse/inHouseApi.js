// Data seam for the in-house blinding wizard (T-21, OGC-612). Every call here
// is live: T-11/T-22 shipped the panel, seal and label endpoints, and T-21 added
// the cycle/panel creates the wizard needs. No mocks.
import config from "../../../config.json";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
  putToOpenElisServer,
} from "../../utils/Utils";

export const fetchInHouseSchemes = (callback) => {
  getFromOpenElisServer("/rest/eqa/programs", (data) =>
    callback(
      (data || []).filter(
        (scheme) =>
          scheme.schemeType === "IN_HOUSE" && scheme.isActive !== false,
      ),
    ),
  );
};

export const fetchPanelsForScheme = (schemeId, callback) => {
  getFromOpenElisServer(`/rest/eqa/panels?schemeId=${schemeId}`, (data) =>
    callback(data || []),
  );
};

export const fetchAnalysts = (schemeId, callback) => {
  getFromOpenElisServer(`/rest/eqa/programs/${schemeId}/analysts`, (data) =>
    callback(data || []),
  );
};

// The lab's users, for adding someone to a scheme's roster. Reuses the NCE
// assignment autocomplete rather than a second copy of the same query.
export const fetchLabUsers = (callback) => {
  getFromOpenElisServer("/rest/nce/users", (data) => callback(data || []));
};

export const saveAnalystRoster = (schemeId, systemUserIds, callback) => {
  putToOpenElisServer(
    `/rest/eqa/programs/${schemeId}/analysts`,
    JSON.stringify({ systemUserIds }),
    callback,
  );
};

// FR-V2.4-04: seals the panel and creates one blinded order per sample. The
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

// FR-V2.4-13: the label sheet is a GET that answers application/pdf, so it needs
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
    .then((blob) => {
      const link = document.createElement("a");
      link.href = window.URL.createObjectURL(blob);
      link.download = `eqa-panel-${panelId}-labels.pdf`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    })
    .catch((error) => {
      if (onError) {
        onError(error);
      }
    });
};
