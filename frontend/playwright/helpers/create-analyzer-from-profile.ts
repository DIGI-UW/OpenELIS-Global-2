/**
 * Create an analyzer via the UI using a profile for auto-fill.
 *
 * Handles the full creation flow:
 * 1. (TCP only) Create mock network to get unique analyzer IP
 * 2. Open dashboard → click Add
 * 3. Select the reusable Analyzer Type/profile, lab unit, and instance name
 * 4. Review the currently confirmed profile mappings
 * 5. Configure and visibly test the connection
 *
 * Returns the IP assigned to the analyzer (for TCP push destinations).
 */

import { Page, expect } from "@playwright/test";
import { AnalyzerSetupPage } from "../fixtures/analyzer-setup";
import { AnalyzerListPage } from "../fixtures/analyzer-list";
import { cleanupAnalyzerByName } from "./cleanup-analyzer";
import {
  resolveMockSimulatorUrl,
  type AnalyzerTestConfig,
} from "./analyzer-test-config";
import { LONG_TIMEOUT } from "./timeouts";

const SIMULATOR_URL = resolveMockSimulatorUrl();

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

/**
 * Create a mock analyzer network and return the assigned IP.
 * The mock server creates a Docker network with a unique subnet per analyzer,
 * giving each a stable IP for bridge identification.
 */
async function createMockNetwork(
  mockName: string,
  template: string,
  port: number,
): Promise<string | null> {
  try {
    const response = await fetch(`${SIMULATOR_URL}/analyzers`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name: mockName, template, port }),
    });
    if (response.ok) {
      const body = await response.json();
      return body.ip || null;
    }
    const status = response.status;
    // 409 = already exists, which is fine (idempotent)
    if (status === 409) {
      // Fetch existing
      const listResp = await fetch(`${SIMULATOR_URL}/analyzers`);
      const list = listResp.ok ? await listResp.json() : null;
      if (list) {
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
export async function removeMockNetwork(mockName: string): Promise<void> {
  const existing = await fetch(`${SIMULATOR_URL}/analyzers`);
  const body = existing.ok ? await existing.json() : null;
  if (!body) return;

  const exists = Array.isArray(body?.analyzers)
    ? body.analyzers.some((a: { name?: string }) => a?.name === mockName)
    : false;

  if (!exists) return;

  const removal = await fetch(`${SIMULATOR_URL}/analyzers/${mockName}`, {
    method: "DELETE",
  });
  if (!removal.ok && removal.status !== 404) {
    throw new Error(`Mock analyzer removal failed with HTTP ${removal.status}`);
  }

  const deadline = Date.now() + 10_000;
  while (Date.now() < deadline) {
    try {
      const current = await fetch(`${SIMULATOR_URL}/analyzers`);
      const currentBody = current.ok ? await current.json() : null;
      const stillExists = Array.isArray(currentBody?.analyzers)
        ? currentBody.analyzers.some(
            (a: { name?: string }) => a?.name === mockName,
          )
        : true;
      if (!stillExists) return;
    } catch {
      // Docker briefly interrupts the mock control port while disconnecting
      // the mock container from the analyzer's temporary network.
    }
    await new Promise((resolve) => setTimeout(resolve, 100));
  }
  throw new Error(`Timed out removing mock analyzer "${mockName}"`);
}

export async function createAnalyzerFromProfile(
  page: Page,
  config: AnalyzerTestConfig,
): Promise<string | null> {
  const list = new AnalyzerListPage(page);
  const setup = new AnalyzerSetupPage(page);

  // Clean up any leftover from a previous failed run
  await cleanupAnalyzerByName(page, config.name);

  // For TCP analyzers: create mock network to get a unique IP.
  // Delete any leftover network first (from a previous failed run).
  let assignedIp: string | null = null;
  if (config.protocol !== "FILE" && config.mockAnalyzerName) {
    await removeMockNetwork(config.mockAnalyzerName);
    const template =
      config.push.protocol === "ASTM" || config.push.protocol === "HL7"
        ? (config.push as { template: string }).template
        : "";
    const port = config.port || 0;
    assignedIp = await createMockNetwork(
      config.mockAnalyzerName,
      template,
      port,
    );
  }

  if (new URL(page.url()).pathname !== "/analyzers") {
    await list.goto();
  }
  await list.expectLoaded();

  await list.clickAdd();
  await setup.expectOpen();
  await setup.selectProfile(config.profileName);
  await setup.fillName(config.name);
  await setup.selectFirstLabUnit();
  await setup.continueToVerify();
  await setup.continueToConnect();

  if (config.protocol !== "FILE") {
    const ip = assignedIp || config.ipAddress;
    if (ip) {
      await setup.fillNetworkAddress(ip);
    }
    if (config.port) {
      await setup.fillPort(String(config.port));
    }
  }

  if (config.protocol === "FILE") {
    const importDir =
      config.push.targetDir ||
      `/data/analyzer-imports/${config.name.toLowerCase().replace(/[^a-z0-9]+/g, "-")}/incoming`;
    await setup.fillImportDirectory(importDir);
  }

  await setup.testConnection();
  await setup.close();
  const createdRow = page.locator("tbody tr", {
    hasText: new RegExp(escapeRegExp(config.name), "i"),
  });
  await expect(createdRow.first()).toContainText(config.profileName, {
    timeout: LONG_TIMEOUT,
  });

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
 * Cleanup uses the visible delete workflow, then removes the mock transport.
 */
export async function teardownAnalyzer(
  page: Page,
  config: AnalyzerTestConfig,
): Promise<void> {
  // Step 1: Soft-delete via UI (tests the production user flow)
  await deleteAnalyzerFromDashboard(page, config.name);

  // Step 2: Remove mock network
  if (config.mockAnalyzerName) {
    await removeMockNetwork(config.mockAnalyzerName);
  }
}
