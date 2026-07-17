/**
 * Configuration types for unified analyzer E2E demo tests.
 *
 * Each config describes one analyzer's full test flow:
 * create → test connection → push result → verify → accept
 *
 * All protocols (ASTM, HL7, FILE) push through the mock server.
 * The mock returns metadata (sample IDs, results) — tests never
 * hardcode expected values.
 */

export type AnalyzerProtocol = "ASTM" | "HL7" | "FILE";

/**
 * Push config for all protocols. The mock server handles everything:
 * - ASTM: POST /simulate/astm/{template} → pushes via TCP
 * - HL7:  POST /simulate/hl7/{template}  → pushes via MLLP
 * - FILE: POST /simulate/file/{template} → drops fixture into watched folder
 *
 * All return metadata including sample IDs and results.
 */
export interface PushConfig {
  protocol: AnalyzerProtocol;
  simulatorUrl: string;
  /** Mock server template name (e.g., "quantstudio7", "genexpert_astm"). */
  template: string;
  /**
   * Provisioned mock analyzer *instance* name (ASTM/HL7 only). Addressing
   * /simulate by the instance (not the template) makes the mock source the push
   * from the analyzer's own IP so the bridge identifies it by source IP. Falls
   * back to `template` when absent.
   */
  mockAnalyzerName?: string;
  /** TCP/MLLP destination (ASTM/HL7 only). */
  destination?: string;
  /**
   * Container path for file drop (FILE only, legacy watched-directory mode,
   * e.g., "/data/analyzer-imports/quantstudio-7/incoming"). Prefer
   * uploadViaBridge for production-parity.
   */
  targetDir?: string;
  /**
   * FILE only — route the fixture through the bridge's /admin/upload endpoint
   * (matching real lab-tech workflow) instead of dropping into a watched
   * directory. Takes precedence over targetDir.
   */
  uploadViaBridge?: boolean;
  /**
   * FILE only — admin-declared test code for upload. Matches the "Test
   * Code" dropdown in the bridge admin UI. Needed for files whose rows
   * carry no per-row test identity (e.g., FluoroCycler VIH-1 results).
   * Ignored for files whose columns already map to testCode (QuantStudio).
   */
  testCode?: string;
  /**
   * FILE uploadViaBridge only — analyzer name the helper uses to look up
   * the bridge-registered id. Typically equal to AnalyzerTestConfig.name.
   */
  analyzerName?: string;
  /** Explicit sample ID override (optional — mock generates if omitted). */
  sampleId?: string;
}

/** Result metadata returned by the mock server after a push. */
export interface PushResult {
  sampleId: string;
  result: string;
  testCode?: string;
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
  /** How to push a result (all protocols go through mock server). */
  push: PushConfig;
  /** IP address for TCP analyzers (filled in UI form when creating). */
  ipAddress?: string;
  /** Port for TCP analyzers (filled in UI form when creating). */
  port?: number;
  /** Mock analyzer name for dynamic network creation (if different from name). */
  mockAnalyzerName?: string;
}
