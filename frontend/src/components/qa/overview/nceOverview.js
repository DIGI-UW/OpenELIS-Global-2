import { useServerData } from "../../utils/useServerData";

/**
 * Shared NCE client-filter util for the QA Overview (OGC-699).
 *
 * There is no acknowledgment column on nce_event: acknowledging an NCE
 * transitions status "Pending" -> "Under Investigation" (NceEnhancement
 * REST controller), so "critical pending acknowledgment" is exactly
 * severity CRITICAL + status Pending. The overview aggregators reuse these predicates for
 * the QMS pillar chip and This-Week counters.
 */

export const NCE_DRILL_URL = "/NceDashboard?severity=CRITICAL&status=Pending";

export const NCE_LIST_URL = "/rest/nce/dashboard";

// Overview slots mounting together share this one cached request.
export const useNceList = () => {
  const query = useServerData(NCE_LIST_URL);
  return {
    loading: query.isLoading,
    nceList: Array.isArray(query.data?.nceList) ? query.data.nceList : null,
  };
};

export const countCriticalPending = (list) =>
  list.filter((nce) => nce.severity === "CRITICAL" && nce.status === "Pending")
    .length;

// NCEs in the corrective-action stage. The backend workflow uses status
// "CAPA" for this stage (there is no "Corrective Action" status), so this is
// the superset that "effectiveness reviews due" (CAPA awaiting a verdict) is
// drawn from.
export const countInCorrectiveAction = (list) =>
  list.filter((nce) => nce.status === "CAPA").length;

// A CAPA needs its effectiveness verdict once corrective action is recorded
// (status "CAPA") and no Yes/No has been given yet (nc_event.effective null).
export const countEffectivenessReviewsDue = (list) =>
  list.filter((nce) => nce.status === "CAPA" && !nce.effective).length;

// Thresholds from OGC-699: 0 green, 1-4 amber, 5+ red. Make them
// configurable in QI config if labs ever need their own.
export const pulseColor = (count) =>
  count === 0 ? "green" : count < 5 ? "amber" : "red";
