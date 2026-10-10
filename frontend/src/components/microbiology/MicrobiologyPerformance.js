export const MICROBIOLOGY_WORKLIST_READY_MARK =
  "openelis:microbiology-worklist-ready";
export const MICROBIOLOGY_CASE_READY_MARK = "openelis:microbiology-case-ready";
export const MICROBIOLOGY_WORKLIST_REFRESH_INTERVAL_MS = 30_000;
export const MICROBIOLOGY_NEW_POSITIVE_HIGHLIGHT_MS = 5_000;

export const markMicrobiologyReady = (
  markName,
  performanceApi = globalThis.performance,
) => {
  if (!performanceApi || typeof performanceApi.mark !== "function") {
    return false;
  }
  if (typeof performanceApi.clearMarks === "function") {
    performanceApi.clearMarks(markName);
  }
  performanceApi.mark(markName);
  return true;
};
