/**
 * Create an analyzer via the UI using a profile for auto-fill.
 *
 * Handles the full creation flow:
 * 1. (TCP only) Create mock network to get unique analyzer IP
 * 2. Open dashboard → click Add
 * 3. Select plugin type → select profile → fill name
 * 4. (TCP only) Fill IP address and port
 * 5. Save → verify success
 *
 * Returns the IP assigned to the analyzer (for TCP push destinations).
 */

import { execFileSync } from "child_process";
import { Page, expect } from "@playwright/test";
import { AnalyzerFormPage } from "../fixtures/analyzer-form";
import { AnalyzerListPage } from "../fixtures/analyzer-list";
import { cleanupAnalyzerByName } from "./cleanup-analyzer";
import type { DemoPresentation } from "./demo-presentation";
import type { AnalyzerTestConfig } from "./analyzer-test-config";
import { LONG_TIMEOUT } from "./timeouts";
import { resolveDbContainer } from "./db-container";

const SIMULATOR_URL = "http://localhost:8085";

/**
 * Create a mock analyzer network and return the assigned IP.
 * The mock server creates a Docker network with a unique subnet per analyzer,
 * giving each a stable IP for bridge identification.
 */
async function createMockNetwork(
  page: Page,
  mockName: string,
  template: string,
  port: number,
): Promise<string | null> {
  try {
    const response = await page.request.post(`${SIMULATOR_URL}/analyzers`, {
      data: { name: mockName, template, port },
    });
    if (response.ok()) {
      const body = await response.json();
      return body.ip || null;
    }
    // 409 = already exists, which is fine (idempotent)
    if (response.status() === 409) {
      // Fetch existing
      const listResp = await page.request.get(`${SIMULATOR_URL}/analyzers`);
      if (listResp.ok()) {
        const list = await listResp.json();
        const existing = list.analyzers?.find(
          (a: { name: string }) => a.name === mockName,
        );
        return existing?.ip || null;
      }
    }
    return null;
  } catch {
    return null;
  }
}

/**
 * Remove a mock analyzer network (cleanup).
 */
async function removeMockNetwork(page: Page, mockName: string): Promise<void> {
  try {
    await page.request.delete(`${SIMULATOR_URL}/analyzers/${mockName}`);
  } catch {
    // Best-effort cleanup
  }
}

export async function createAnalyzerFromProfile(
  page: Page,
  config: AnalyzerTestConfig,
  presentation: DemoPresentation,
): Promise<string | null> {
  const list = new AnalyzerListPage(page);
  const form = new AnalyzerFormPage(page);

  // Clean up any leftover from a previous failed run
  await cleanupAnalyzerByName(page, config.name);

  // For TCP analyzers: create mock network to get a unique IP.
  // Delete any leftover network first (from a previous failed run).
  let assignedIp: string | null = null;
  if (config.protocol !== "FILE" && config.mockAnalyzerName) {
    await removeMockNetwork(page, config.mockAnalyzerName);
    const template =
      config.push.protocol === "ASTM" || config.push.protocol === "HL7"
        ? (config.push as { template: string }).template
        : "";
    const port = config.port || 0;
    assignedIp = await createMockNetwork(
      page,
      config.mockAnalyzerName,
      template,
      port,
    );
  }

  await list.goto();
  await list.expectLoaded();
  await presentation.pause(500);

  await list.clickAdd();
  await form.expectOpen();

  // Select plugin type
  await form.selectPluginType(config.pluginType);
  await presentation.pause(500);

  // Select profile (auto-fills fields)
  if (config.profileName) {
    await form.selectDefaultConfig(config.profileName);
    await presentation.pause(1_000);
  }

  // Select analyzer type (may already be set by profile)
  await form.selectType(config.analyzerType);

  // Fill name
  await form.fillName(config.name);
  await presentation.pause(500);

  // Fill IP and port for TCP analyzers
  if (config.protocol !== "FILE") {
    const ip = assignedIp || config.ipAddress;
    if (ip) {
      await form.fillIpAddress(ip);
    }
    if (config.port) {
      await form.fillPort(String(config.port));
    }
    await presentation.pause(500);
  }

  // Save
  await form.save();
  await form.expectSuccessNotification();

  // Wait for modal to close
  await expect(form.modal).not.toBeVisible({ timeout: LONG_TIMEOUT });
  await presentation.pause(1_000);

  return assignedIp;
}

/**
 * Delete an analyzer via the UI dashboard (teardown).
 */
export async function deleteAnalyzerFromDashboard(
  page: Page,
  analyzerName: string,
): Promise<void> {
  await cleanupAnalyzerByName(page, analyzerName);
}

/**
 * Full cleanup: soft-delete via UI (tests production flow) → SQL cleanup
 * (test isolation) → remove mock network.
 */
export async function teardownAnalyzer(
  page: Page,
  config: AnalyzerTestConfig,
): Promise<void> {
  // Step 1: Soft-delete via UI (tests the production user flow)
  await deleteAnalyzerFromDashboard(page, config.name);

  // Step 2: SQL cleanup of the soft-deleted row (test isolation)
  hardDeleteAnalyzerFromDb(config.name);

  // Step 3: Remove mock network
  if (config.mockAnalyzerName) {
    await removeMockNetwork(page, config.mockAnalyzerName);
  }
}

/**
 * Remove a soft-deleted analyzer from the DB so tests leave zero trace.
 * Follows the execFileSync + docker psql pattern from file-import-setup.ts.
 * CASCADE FK on analyzer_test_map handles test mapping cleanup automatically.
 */
function hardDeleteAnalyzerFromDb(analyzerName: string): void {
  const container = resolveDbContainer();
  const sql = `DELETE FROM clinlims.analyzer_results WHERE analyzer_id IN (SELECT id FROM clinlims.analyzer WHERE name = '${analyzerName}'); DELETE FROM clinlims.analyzer WHERE name = '${analyzerName}';`;
  try {
    execFileSync("docker", [
      "exec",
      "-i",
      container,
      "psql",
      "-U",
      "clinlims",
      "-d",
      "clinlims",
      "-c",
      sql,
    ]);
  } catch (e) {
    console.warn(`DB cleanup failed for "${analyzerName}": ${e}`);
  }
}
