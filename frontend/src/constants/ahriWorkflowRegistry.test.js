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

  it("uses pageKey for stage lookup", () => {
    expect(resolvePageKey({ pageId: "reception", order: 1 })).toBe("reception");
    expect(isActionPermitted("immunology", { pageId: "reception", order: 1 }, "EDIT")).toBe(true);
  });
});
