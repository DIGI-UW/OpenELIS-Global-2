/**
 * Configuration types for unified analyzer E2E demo tests.
 *
 * Each config describes one analyzer's full test flow:
 * create → test connection → push result → verify → accept
 *
 * Protocol-specific details (how to push: MLLP, ASTM TCP, file drop)
 * are encapsulated in the push config variant.
 */

export type AnalyzerProtocol = "ASTM" | "HL7" | "FILE";

export interface AstmPush {
  protocol: "ASTM";
  simulatorUrl: string;
  template: string;
  destination: string; // e.g., "tcp://openelis-analyzer-bridge:12001"
  sampleId?: string;
}

export interface Hl7Push {
  protocol: "HL7";
  simulatorUrl: string;
  template: string;
  destination: string; // e.g., "mllp://openelis-analyzer-bridge:2575"
  sampleId?: string;
}

export interface FilePush {
  protocol: "FILE";
  fixtureFile: string; // path relative to e2e-fixtures/
  importDir: string; // host path for file drop
  filePrefix: string;
}

export type PushConfig = AstmPush | Hl7Push | FilePush;

export interface ExpectedResult {
  /** Optional accession/sample ID tied to this expected result. */
  sampleId?: string;
  /** Expected result value to verify on AnalyzerResults page. */
  result: string;
  /** Optional test name for display. */
  testName?: string;
}

export interface AnalyzerTestConfig {
  /** Analyzer name as it appears in the list (must match seeded name). */
  name: string;
  /** Display name for demo title cards. */
  displayName: string;
  /** Analyzer category: HEMATOLOGY, CHEMISTRY, MOLECULAR, etc. */
  analyzerType: string;
  /** Plugin type label for the dropdown: "Generic HL7", "Generic ASTM", "Generic File". */
  pluginType: string;
  /** Profile name for the default config dropdown (e.g., "QuantStudio", "Mindray"). */
  profileName?: string;
  /** Protocol family. */
  protocol: AnalyzerProtocol;
  /** How to push a result to the analyzer (protocol-specific). */
  push: PushConfig;
  /** Expected results to verify on the AnalyzerResults page. */
  expectedResults: ExpectedResult[];
  /** For FILE protocol: known accession/sample ID from the fixture file. */
  fileSampleId?: string;
  /** IP address for TCP analyzers (filled in UI form when creating). */
  ipAddress?: string;
  /** Port for TCP analyzers (filled in UI form when creating). */
  port?: number;
  /** Mock analyzer name for dynamic network creation (if different from name). */
  mockAnalyzerName?: string;
}
