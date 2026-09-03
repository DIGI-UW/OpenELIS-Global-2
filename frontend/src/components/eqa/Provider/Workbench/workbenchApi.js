// Data seam for the provider lane: the scheme list and cycle wizard (T-24) and
// the prep + shipment workbenches (T-25), OGC-613.
// Every write goes through a FullResponse helper so a 409 (gate refusal, wrong
// box state) or 422 (bad input) reaches the operator verbatim — a JSON-only
// helper would swallow the reason, which is the D-LIVE-1 mistake.
import {
  deleteFromOpenElisServerFullResponse,
  getFromOpenElisServer,
  patchToOpenElisServerFullResponse,
  postToOpenElisServerFullResponse,
} from "../../../utils/Utils";
import config from "../../../../config.json";

/**
 * A list read must answer a list. `data || []` is not enough: a failed read
 * hands back the server's error object, which is truthy, so the page reaches
 * `.map` on an object and white-screens. Every list endpoint here goes through
 * this.
 */
const asList = (callback) => (data) =>
  callback(Array.isArray(data) ? data : []);

/**
 * FR-V2.5-01. One read draws the whole board: `schemes` (each carrying its
 * cycles) plus the `kpis` tile counts, so the tiles and the table cannot
 * disagree. The same list-shape guard as asList, applied to the board's parts.
 */
export const fetchProviderSchemes = (callback) =>
  getFromOpenElisServer("/rest/eqa/provider/schemes", (data) =>
    callback({
      kpis: data && !Array.isArray(data) && data.kpis ? data.kpis : {},
      schemes: data && Array.isArray(data.schemes) ? data.schemes : [],
    }),
  );

export const fetchPrepStatus = (cycleId, callback) =>
  getFromOpenElisServer(`/rest/eqa/cycles/${cycleId}/prep`, (data) =>
    callback(data || null),
  );

export const fetchShipmentRows = (cycleId, callback) =>
  getFromOpenElisServer(
    `/rest/eqa/cycles/${cycleId}/shipments`,
    asList(callback),
  );

/**
 * Every write answers with {ok, body}: on failure the body is the server's
 * own {error: "..."} so the operator reads the actual refusal, not a guess.
 */
const withBody = (callback) => (response) => {
  if (!response) {
    callback({ ok: false, body: null });
    return;
  }
  response
    .json()
    .then((body) => callback({ ok: response.ok, body }))
    .catch(() => callback({ ok: response.ok, body: null }));
};

/**
 * FR-V2.5-02. One POST writes the cycle, its panel, its samples and its
 * participant roster, and leaves the cycle in prep — so a wizard the server
 * refuses leaves nothing half-created for the scheme list to show.
 */
export const createProviderCycle = (payload, callback) =>
  postToOpenElisServerFullResponse(
    "/rest/eqa/provider/cycles",
    JSON.stringify(payload),
    withBody(callback),
  );

export const savePrep = (panelId, fields, callback) =>
  patchToOpenElisServerFullResponse(
    `/rest/eqa/panels/${panelId}/prep`,
    JSON.stringify(fields),
    withBody(callback),
  );

export const saveShipmentDetails = (cycleId, fields, callback) =>
  postToOpenElisServerFullResponse(
    `/rest/eqa/cycles/${cycleId}/shipments`,
    JSON.stringify(fields),
    withBody(callback),
  );

export const markShipped = (cycleId, organizationIds, callback) =>
  postToOpenElisServerFullResponse(
    `/rest/eqa/cycles/${cycleId}/shipments/ship`,
    JSON.stringify({ organizationIds }),
    withBody(callback),
  );

/**
 * Clearing a cycle to ship is the ordinary provider transition — the inventory
 * and QC gate lives there (T-10 + T-25), so the workbench does not get a
 * second, weaker gate of its own.
 */
export const requestReadyToShip = (cycleId, callback) =>
  patchToOpenElisServerFullResponse(
    `/rest/eqa/cycles/${cycleId}/transition`,
    JSON.stringify({
      newState: "READY_TO_SHIP",
      stateMachine: "PROVIDER",
      reason: "Prep complete — cleared to ship from the prep workbench",
    }),
    withBody(callback),
  );

/**
 * Panel samples, for the pack list. Targets are nulled server-side.
 *
 * Deliberately NOT defaulted to []: a failed read answers undefined, and the
 * pack list has to tell that apart from a panel that genuinely holds no samples
 * — otherwise a courier gets a manifest missing whole panels.
 */
export const fetchPanelSamples = (panelId, callback) =>
  getFromOpenElisServer(`/rest/eqa/panels/${panelId}/samples`, callback);

// --- T-26 receipt monitoring, reprovisioning and scoring (FR-V2.5-14/15/03/04) ---

export const fetchReceiptRows = (cycleId, callback) =>
  getFromOpenElisServer(`/rest/eqa/cycles/${cycleId}/receipts`, (data) =>
    callback(data || []),
  );

export const fetchScoreRows = (cycleId, callback) =>
  getFromOpenElisServer(`/rest/eqa/cycles/${cycleId}/scores`, (data) =>
    callback(data || []),
  );

export const markDelivered = (cycleId, organizationId, callback) =>
  postToOpenElisServerFullResponse(
    `/rest/eqa/cycles/${cycleId}/receipts/${organizationId}/delivered`,
    "{}",
    withBody(callback),
  );

/** The override note is only required when the panel's reserve is short. */
export const sendRepeat = (cycleId, organizationId, overrideNote, callback) =>
  postToOpenElisServerFullResponse(
    `/rest/eqa/cycles/${cycleId}/receipts/${organizationId}/repeat`,
    JSON.stringify({ overrideNote }),
    withBody(callback),
  );

/**
 * T-46: the operator's audited override — open submissions while part of the
 * roster is still undelivered. MANUAL with a written reason by construction:
 * it rides the generic transition endpoint, which refuses a blank reason.
 */
export const openSubmissions = (cycleId, reason, callback) =>
  patchToOpenElisServerFullResponse(
    `/rest/eqa/cycles/${cycleId}/transition`,
    JSON.stringify({
      newState: "SUBMISSIONS_OPEN",
      stateMachine: "PROVIDER",
      reason,
    }),
    withBody(callback),
  );

export const scoreCycle = (cycleId, callback) =>
  postToOpenElisServerFullResponse(
    `/rest/eqa/cycles/${cycleId}/score`,
    "{}",
    withBody(callback),
  );

export const distributeScores = (cycleId, organizationId, callback) =>
  postToOpenElisServerFullResponse(
    `/rest/eqa/cycles/${cycleId}/scores/${organizationId}/distribute`,
    "{}",
    withBody(callback),
  );

/**
 * Server-rendered CSV, so the browser downloads it rather than rebuilding it.
 * The base URL is required: the SPA is served from a different origin than the
 * API, so a root-relative path 404s (found driving the page, 2026-08-24) — the
 * same reason the performance report opens through config.serverBaseUrl.
 */
export const scoresCsvUrl = (cycleId, organizationId) =>
  `${config.serverBaseUrl}/rest/eqa/cycles/${cycleId}/scores/${organizationId}/csv`;

// --- FR-V2.5-03 provider-side result intake (phoned/emailed results, export bundles) ---

/** The scheme's tests with what this participant has reported so far. */
export const fetchIntake = (cycleId, organizationId, callback) =>
  getFromOpenElisServer(
    `/rest/eqa/cycles/${cycleId}/results?organizationId=${organizationId}`,
    (data) => callback(data || null),
  );

export const saveIntake = (cycleId, organizationId, results, callback) =>
  postToOpenElisServerFullResponse(
    `/rest/eqa/cycles/${cycleId}/results`,
    JSON.stringify({ organizationId, results }),
    withBody(callback),
  );

export const importIntakeCsv = (cycleId, organizationId, csv, callback) =>
  postToOpenElisServerFullResponse(
    `/rest/eqa/cycles/${cycleId}/results/import`,
    JSON.stringify({ organizationId, csv }),
    withBody(callback),
  );

// --- OGC-934 report comments ---

/** The pre-approved library the picker offers. */
export const fetchCommentLibrary = (callback) =>
  getFromOpenElisServer("/rest/eqa/report-comments", (data) =>
    callback(data || []),
  );

export const fetchCycleComments = (cycleId, callback) =>
  getFromOpenElisServer(`/rest/eqa/cycles/${cycleId}/report-comments`, (data) =>
    callback(data || []),
  );

/** Ids only: the endpoint has no text field, so nothing unapproved can be sent. */
export const attachComments = (cycleId, commentIds, callback) =>
  postToOpenElisServerFullResponse(
    `/rest/eqa/cycles/${cycleId}/report-comments`,
    JSON.stringify({ commentIds }),
    withBody(callback),
  );

export const detachComment = (cycleId, commentId, callback) =>
  deleteFromOpenElisServerFullResponse(
    `/rest/eqa/cycles/${cycleId}/report-comments/${commentId}`,
    withBody(callback),
  );
