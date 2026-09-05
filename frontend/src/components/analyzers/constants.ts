import type { IntlShape } from "react-intl";

import type { AnalyzerApiError } from "./types";

export type CommunicationMode = "ANALYZER_INITIATED" | "LIS_INITIATED" | "BOTH";

export interface CommunicationModeOption {
  value: CommunicationMode;
  labelId: string;
}

/**
 * Communication mode labels for values supplied by a Bridge profile.
 */
export const COMMUNICATION_MODES: CommunicationModeOption[] = [
  {
    value: "ANALYZER_INITIATED",
    labelId: "analyzer.form.communicationMode.analyzerInitiated",
  },
  {
    value: "LIS_INITIATED",
    labelId: "analyzer.form.communicationMode.lisInitiated",
  },
  { value: "BOTH", labelId: "analyzer.form.communicationMode.both" },
];

// Re-exported from utils so existing analyzer imports keep working while the
// generic implementation lives in one place.
export { resolveApiErrorMessage as resolveAnalyzerApiMessage } from "../utils/Utils";

export type ResolveAnalyzerApiMessage = (
  intl: IntlShape,
  payload: AnalyzerApiError,
  fallbackId: string,
  fallbackValues?: Record<string, unknown>,
) => string;
