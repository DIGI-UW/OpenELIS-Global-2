// Data seam for My Cycles (T-13). Phase 2: reads the real cycle API
// (PR #4070 contract, enriched with schemeName/provider/schemeType/progress/
// samples). MOCK_CYCLES remains only as the test fixture. schemeType arrives
// as the enum name (INTERNATIONAL_PT); the view model lower-cases it to match
// i18n keys. perAnalyst/requiresCycleReview/hasNce have no backing schema yet
// (scheme-level config lands with T-24/G3c) — they default false here.
import {
  getFromOpenElisServer,
  patchToOpenElisServerJsonResponse,
} from "../../utils/Utils";

const toViewModel = (dto) => ({
  perAnalyst: false,
  requiresCycleReview: false,
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
