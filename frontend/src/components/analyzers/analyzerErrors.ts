import type { IntlShape } from "react-intl";

/** The parts of a refused analyzer response that can name its failure. */
export interface AnalyzerRefusal {
  messageKey?: string | null;
  messageArgs?: Record<string, unknown> | null;
  errorKey?: string | null;
  connectionErrorKey?: string | null;
  blockers?: Array<{ code?: string | null }> | null;
  failure?: string | null;
}

// Where a refused analyzer response names its failure. Raw server text is
// never shown: an unnamed or unknown failure gets the screen's own message.
const namedKeys = (response?: AnalyzerRefusal | null) => [
  response?.messageKey,
  response?.errorKey,
  response?.connectionErrorKey,
  response?.blockers?.[0]?.code,
  response?.failure,
];

export const errorKeyFor = (
  response: AnalyzerRefusal | null | undefined,
  messages: Record<string, unknown>,
  fallbackId: string | null,
): string | null =>
  namedKeys(response).find(
    (key): key is string => typeof key === "string" && Boolean(messages[key]),
  ) || fallbackId;

/** The words for a refused response, or "" when it names nothing and there is no fallback. */
export const analyzerErrorText = (
  intl: IntlShape,
  response: AnalyzerRefusal | null | undefined,
  fallbackId: string | null,
): string => {
  const id = errorKeyFor(response, intl.messages, fallbackId);
  if (!id) {
    return "";
  }
  return intl.formatMessage(
    { id },
    id === fallbackId
      ? {}
      : (response?.messageArgs as Record<string, string | number>) || {},
  );
};
