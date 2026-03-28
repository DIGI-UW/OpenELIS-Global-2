/**
 * Canonical protocol version values — matches the ProtocolVersion Java enum.
 * These represent message formats (how to parse content), NOT transport
 * mechanisms (TCP, serial, file).
 */
export const PROTOCOL_VERSIONS = [
  { value: "ASTM_LIS2_A2", label: "ASTM LIS2-A2" },
  { value: "HL7_V2_3_1", label: "HL7 v2.3.1" },
  { value: "HL7_V2_5", label: "HL7 v2.5" },
];

/**
 * Default protocol version for each plugin protocol family.
 * Maps the protocol string from AnalyzerType (ASTM, HL7, FILE) to the
 * corresponding ProtocolVersion enum value.
 */
export const PLUGIN_PROTOCOL_DEFAULTS = {
  ASTM: "ASTM_LIS2_A2",
  HL7: "HL7_V2_3_1",
};

/** Default protocol version for new analyzers. */
export const DEFAULT_PROTOCOL_VERSION = "ASTM_LIS2_A2";

/**
 * Communication mode values — matches the CommunicationMode Java enum.
 * Describes who initiates communication between LIS and analyzer.
 * MVP: all analyzers use ANALYZER_INITIATED. LIS_INITIATED and BOTH
 * are planned post-MVP capabilities per vendor specs.
 */
export const COMMUNICATION_MODES = [
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

/** Default communication mode for new analyzers. */
export const DEFAULT_COMMUNICATION_MODE = "ANALYZER_INITIATED";

/**
 * Resolve analyzer API message payloads to localized UI text.
 * Backend may return message/error directly or messageKey/errorKey + args.
 */
export const resolveAnalyzerApiMessage = (
  intl,
  payload,
  fallbackId,
  fallbackValues = {},
) => {
  const key = payload?.messageKey || payload?.errorKey;
  const keyArgs = payload?.messageArgs || payload?.errorArgs || {};
  if (key) {
    return intl.formatMessage({ id: key }, keyArgs);
  }

  const keyedMessage =
    payload?.message && typeof payload.message === "string"
      ? payload.message
      : payload?.error && typeof payload.error === "string"
        ? payload.error
        : null;
  if (keyedMessage && keyedMessage.startsWith("analyzer.")) {
    return intl.formatMessage({ id: keyedMessage }, keyArgs);
  }
  if (keyedMessage) {
    return keyedMessage;
  }
  return intl.formatMessage({ id: fallbackId }, fallbackValues);
};
