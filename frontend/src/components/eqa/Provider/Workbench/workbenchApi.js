// Data seam for the provider prep + shipment workbenches (T-25, OGC-613).
// Every write goes through a FullResponse helper so a 409 (gate refusal, wrong
// box state) or 422 (bad input) reaches the operator verbatim — a JSON-only
// helper would swallow the reason, which is the D-LIVE-1 mistake.
import {
  getFromOpenElisServer,
  patchToOpenElisServerFullResponse,
  postToOpenElisServerFullResponse,
} from "../../../utils/Utils";

export const fetchProviderCycles = (callback) =>
  getFromOpenElisServer("/rest/eqa/provider/cycles", (data) =>
    callback(data || []),
  );

export const fetchPrepStatus = (cycleId, callback) =>
  getFromOpenElisServer(`/rest/eqa/cycles/${cycleId}/prep`, (data) =>
    callback(data || null),
  );

export const fetchShipmentRows = (cycleId, callback) =>
  getFromOpenElisServer(`/rest/eqa/cycles/${cycleId}/shipments`, (data) =>
    callback(data || []),
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

/** Panel samples, for the pack list. Targets are nulled server-side. */
export const fetchPanelSamples = (panelId, callback) =>
  getFromOpenElisServer(`/rest/eqa/panels/${panelId}/samples`, (data) =>
    callback(data || []),
  );
