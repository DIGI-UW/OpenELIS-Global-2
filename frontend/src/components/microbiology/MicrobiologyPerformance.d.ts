export const MICROBIOLOGY_WORKLIST_READY_MARK: string;
export const MICROBIOLOGY_CASE_READY_MARK: string;

export function markMicrobiologyReady(
  markName: string,
  performanceApi?: Performance | null,
): boolean;
