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
  FILE: "ASTM_LIS2_A2", // FILE is transport, default message format is ASTM
};

/** Default protocol version for new analyzers. */
export const DEFAULT_PROTOCOL_VERSION = "ASTM_LIS2_A2";
