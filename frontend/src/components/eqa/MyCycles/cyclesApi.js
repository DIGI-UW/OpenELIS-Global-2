// Data seam for My Cycles (T-13). Reads the real cycle API (PR #4070 contract,
// enriched with schemeName/provider/schemeType/requiresCycleReview/progress/
// samples). MOCK_CYCLES remains only as the test fixture. schemeType arrives
// as the enum name (INTERNATIONAL_PT); the view model lower-cases it to match
// i18n keys. perAnalyst and hasNce still have no backing schema (per-analyst
// mapping is T-19, the NCE link is T-17) — they default false here.
import {
  getFromOpenElisServer,
  patchToOpenElisServerJsonResponse,
  postToOpenElisServerFullResponse,
} from "../../utils/Utils";

const toViewModel = (dto) => ({
  perAnalyst: false,
  hasNce: false,
  ...dto,
  schemeName: dto.schemeName || dto.cycleName || "",
  provider: dto.provider || "",
  schemeType: (dto.schemeType || "").toLowerCase(),
  progress: dto.progress || { entered: 0, total: 0 },
  samples: dto.samples || [],
  // Per-lab derived state wins over the cycle's own machine state (FR-V2.1-18).
  status: (dto.participantState || dto.status || "").toLowerCase(),
  deadline: dto.plannedEndDate || "",
});

export const fetchMyCycles = (callback) => {
  getFromOpenElisServer("/rest/eqa/cycles/mine", (data) =>
    callback((data || []).map(toViewModel)),
  );
};

// Single-click Review & Submit (FR-V2.2-07): no re-auth, no extra sign-off.
// The transition endpoint records every HTTP call as a MANUAL override and
// requires a reason, so a fixed one is sent. 409 = illegal edge, 422 =
// missing reason; the helper reports both as an undefined response.
export const submitCycle = (cycleId, callback) => {
  patchToOpenElisServerJsonResponse(
    `/rest/eqa/cycles/${cycleId}/transition`,
    JSON.stringify({
      newState: "SUBMITTED",
      stateMachine: "PARTICIPANT",
      reason: "Participant review & submit from My Cycles",
    }),
    (response) => callback(response ? toViewModel(response) : null),
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
