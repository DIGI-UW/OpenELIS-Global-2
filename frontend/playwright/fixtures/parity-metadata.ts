import type { TestInfo } from "@playwright/test";

export type RiskTier = "P0" | "P1" | "P2";
export type ScenarioDomain =
  | "core"
  | "clinical"
  | "admin"
  | "storage"
  | "analyzer";

export interface ParityMetadata {
  legacyScenarioId: string;
  riskTier: RiskTier;
  domain: ScenarioDomain;
  notes?: string;
}

export function addParityAnnotations(
  testInfo: TestInfo,
  metadata: ParityMetadata,
): void {
  testInfo.annotations.push(
    { type: "legacy-scenario", description: metadata.legacyScenarioId },
    { type: "risk-tier", description: metadata.riskTier },
    { type: "domain", description: metadata.domain },
  );

  if (metadata.notes) {
    testInfo.annotations.push({
      type: "parity-note",
      description: metadata.notes,
    });
  }
}
