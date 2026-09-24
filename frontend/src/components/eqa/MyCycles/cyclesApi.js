// Data seam for My Cycles. Reads the real cycle API (PR #4070 contract,
// enriched with schemeName/provider/schemeType/requiresCycleReview/progress/
// samples). MOCK_CYCLES remains only as the test fixture. schemeType arrives
// as the enum name (INTERNATIONAL_PT); the view model lower-cases it to match
// i18n keys. perAnalyst and hasNce still have no backing schema (per-analyst
// mapping and the NCE link ship separately) — they default false here.
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../../utils/Utils";
import { asList } from "../eqaApi";

const toViewModel = (dto) => ({
  perAnalyst: false,
  hasNce: false,
  ...dto,
  schemeName: dto.schemeName || dto.cycleName || "",
  provider: dto.provider || "",
  schemeType: (dto.schemeType || "").toLowerCase(),
  progress: dto.progress || { entered: 0, total: 0 },
  samples: dto.samples || [],
  // Per-lab derived state wins over the cycle's own machine state.
  status: (dto.participantState || dto.status || "").toLowerCase(),
  deadline: dto.plannedEndDate || "",
});

export const fetchMyCycles = (callback) => {
  getFromOpenElisServer("/rest/eqa/cycles/mine", (data) =>
    callback(asList(data).map(toViewModel)),
  );
};

// Single-click review and submit: no re-auth, no extra sign-off. This posts the
// results and stamps their submission channel, which the plain cycle transition
// endpoint does not do — it would leave the screen claiming a send the provider
// never received. 409 carries the reason the cycle could not go, so the raw
// response is read rather than treating any JSON as success.
export const submitCycle = (cycleId, callback) => {
  postToOpenElisServerFullResponse(
    `/rest/eqa/cycles/${cycleId}/review-submit`,
    "{}",
    (response) => {
      response
        .json()
        .catch(() => ({}))
        .then((payload) =>
          callback({
            ok: response.ok,
            status: payload?.status,
            error: response.ok
              ? null
              : payload?.error || payload?.message || response.statusText,
          }),
        );
    },
  );
};

// Manual fallback, reachable at last. The automatic channel spends a
// finite retry budget and then stops for good, and the alert it raises says
// "submit manually" — this is what that sentence now points at. The provider's
// reference is mandatory server-side: without it a manual submission is a claim
// rather than a record.
export const submitCycleManually = (cycleId, reference, callback) => {
  postToOpenElisServerFullResponse(
    `/rest/eqa/cycles/${cycleId}/submit-manual`,
    JSON.stringify({ manualSubmissionReference: reference }),
    (response) => {
      response
        .json()
        .catch(() => ({}))
        .then((payload) => {
          callback({
            ok: response.ok,
            error: response.ok
              ? null
              : payload?.error || payload?.message || response.statusText,
          });
        });
    },
  );
};

// Programmes this lab has enrolled in (My Programs); the "New cycle" form
// offers exactly these, by name, because the participant-created cycle is
// matched to a local programme of the same name on the server.
export const fetchMyPrograms = (callback) => {
  getFromOpenElisServer("/rest/eqa/my-programs", (data) =>
    callback(Array.isArray(data) ? data : []),
  );
};

// Participant-created cycle for a provider that is not an OpenELIS instance.
// The server answers 4xx with an {error} body when the lab is not enrolled or
// no local programme carries the name, so the raw response is inspected rather
// than trusting any JSON as success.
export const createMyCycle = (body, callback) => {
  postToOpenElisServerFullResponse(
    "/rest/eqa/cycles/mine",
    JSON.stringify(body),
    (response) => {
      response
        .json()
        .catch(() => ({}))
        .then((payload) => {
          if (response.ok) {
            callback({ ok: true, cycle: toViewModel(payload) });
          } else {
            callback({
              ok: false,
              error: payload?.error || payload?.message || response.statusText,
            });
          }
        });
    },
  );
};

// The provider's scores as a file, for a provider that is not an OpenELIS
// instance. 400 means the file is unusable and 409 means nothing is left to
// score, so the raw response is read and neither is mistaken for success.
export const importScoresCsv = (cycleId, csv, callback) => {
  postToOpenElisServerFullResponse(
    `/rest/eqa/cycles/${cycleId}/score-intake/csv`,
    JSON.stringify({ csv }),
    (response) => {
      response
        .json()
        .catch(() => ({}))
        .then((payload) =>
          callback({
            ok: response.ok,
            scored: payload?.scored,
            unmapped: payload?.unmapped || [],
            error: payload?.error || (response.ok ? null : response.statusText),
          }),
        );
    },
  );
};
