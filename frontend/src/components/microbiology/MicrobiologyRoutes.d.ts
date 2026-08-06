export type MicrobiologyWorklistState = {
  workflow?: string;
  stage?: string;
  urgency?: string;
  due?: string;
  q?: string;
  sort?: "priority" | "newest" | "workflow";
  page?: number;
  pageSize?: number;
};

export type MicrobiologyCaseState = MicrobiologyWorklistState & {
  section?: string;
  action?: "log-critical" | "";
  targetType?: "CASE" | "ISOLATE" | "";
  targetId?: string;
};

export const MICROBIOLOGY_WORKLIST_PATH: string;
export const MICROBIOLOGY_CASE_PATH: string;
export const MICROBIOLOGY_WORKLIST_PAGE_SIZES: number[];
export const MICROBIOLOGY_CASE_SECTIONS: string[];
export const MICROBIOLOGY_CASE_ACTIONS: string[];
export const MICROBIOLOGY_CRITICAL_TARGET_TYPES: string[];

export function parseMicrobiologyWorklistSearch(
  search?: string,
): Required<MicrobiologyWorklistState>;

export function parseMicrobiologyCaseSearch(
  search?: string,
): Required<MicrobiologyCaseState>;

export function getMicrobiologyWorklistUrl(
  state?: MicrobiologyWorklistState,
): string;

export function getMicrobiologyCaseUrl(
  caseId: string,
  state?: MicrobiologyCaseState,
): string;
