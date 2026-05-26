import { execSync } from "child_process";
import path from "path";
import {
  enrichPagesWithRegistryRoles,
  getRegistryStages,
  isActionPermitted,
  resolvePageAllowedRoles,
  resolvePageKey,
} from "./ahriWorkflowRegistry";

const repoRoot = path.resolve(__dirname, "../../..");

describe("ahriWorkflowRegistry", () => {
  it("JS registry matches ahri-workflows.csv", () => {
    execSync("node scripts/sync-ahri-workflow-registry.mjs --check", {
      cwd: repoRoot,
      stdio: "pipe",
    });
  });

  it("resolves biorepository intake personas", () => {
    const roles = resolvePageAllowedRoles("biorepository", { order: 1 });
    expect(roles).toContain("Sample Collector");
    expect(roles).not.toContain("Storage Manager");
  });

  it("enriches pages without explicit allowedRoles", () => {
    const pages = enrichPagesWithRegistryRoles("immunology", [{ order: 2, title: "Initial Processing" }]);
    expect(pages[0].allowedRoles.length).toBeGreaterThan(0);
  });

  it("lists stages for tuberculosis workflow", () => {
    const stages = getRegistryStages("tuberculosis");
    expect(stages.length).toBeGreaterThanOrEqual(8);
  });

  it("keeps viral vaccine aligned to the 14-page workflow", () => {
    const stages = getRegistryStages("viral_vaccine");
    expect(stages).toHaveLength(14);
    expect(stages[10].stageTitle).toBe("Titer Measurement");
    expect(stages[13].stageTitle).toBe("Preclinical & Clinical Trials");
  });

  it("keeps virology aligned to the 10-stage lab workflow", () => {
    const stages = getRegistryStages("virology");
    expect(stages).toHaveLength(10);
    expect(stages[0].stageTitle).toBe("Sample Intake & Registration");
    expect(stages[9].stageTitle).toBe("Storage & Environmental Monitoring");
  });

  it("keeps genomics and gbd aligned to the 10-stage genomics workflow", () => {
    const gbdStages = getRegistryStages("gbd");
    const genomicsStages = getRegistryStages("genomics");
    expect(gbdStages).toHaveLength(10);
    expect(genomicsStages).toHaveLength(10);
    expect(genomicsStages[8].stageTitle).toBe(
      "Bioinformatics Analysis & Data Submission",
    );
    expect(genomicsStages[9].stageTitle).toBe(
      "Storage & Environmental Monitoring",
    );
  });

  it("prefers registry personas over stale explicit page roles", () => {
    const roles = resolvePageAllowedRoles(
      "genomics",
      { order: 1, allowedRoles: ["Old Role"] },
      "VIEW",
    );
    expect(roles).toContain("Sample Collector");
    expect(roles).not.toContain("Old Role");
  });

  it("uses pageKey for stage lookup", () => {
    expect(resolvePageKey({ pageId: "reception", order: 1 })).toBe("reception");
    expect(isActionPermitted("immunology", { pageId: "reception", order: 1 }, "EDIT")).toBe(true);
  });
});
