/**
 * Create an analyzer via the UI using a profile for auto-fill.
 *
 * Handles the full creation flow:
 * 1. (TCP only) Create mock network to get unique analyzer IP
 * 2. Open dashboard → click Add
 * 3. Select the reusable Analyzer Type/profile → fill the instance name
 * 4. (TCP only) Fill IP address and port
 * 5. Save → verify success
 *
 * Returns the IP assigned to the analyzer (for TCP push destinations).
 */

import { Page, expect } from "@playwright/test";
import { AnalyzerFormPage } from "../fixtures/analyzer-form";
import { AnalyzerListPage } from "../fixtures/analyzer-list";
import { cleanupAnalyzerByName } from "./cleanup-analyzer";
import {
  resolveMockSimulatorUrl,
  type AnalyzerTestConfig,
} from "./analyzer-test-config";
import { LONG_TIMEOUT } from "./timeouts";

const SIMULATOR_URL = resolveMockSimulatorUrl();

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
  const form = new AnalyzerFormPage(page);

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

  await list.goto();
  await list.expectLoaded();

  await list.clickAdd();
  // A transient fetch failure on the lazy AnalyzerForm chunk renders the
  // route error boundary, and the browser caches the failed dynamic import
  // for the page's lifetime — only a reload recovers. Retry once.
  try {
    await form.expectOpen();
  } catch {
    await page.reload();
    await list.goto();
    await list.expectLoaded();
    await list.clickAdd();
    await form.expectOpen();
  }

  await form.selectProfile(config.profileName);

  // Fill name
  await form.fillName(config.name);

  // Fill IP and port for TCP analyzers
  if (config.protocol !== "FILE") {
    const ip = assignedIp || config.ipAddress;
    if (ip) {
      await form.fillIpAddress(ip);
    }
    if (config.port) {
      await form.fillPort(String(config.port));
    }
  }

  // Fill required import directory for FILE analyzers. The UI intentionally
  // does NOT auto-generate this (per product decision) — tests must set it
  // explicitly. Mirror the mock server's targetDir so the analyzer watches
  // where the mock drops fixtures.
  if (config.protocol === "FILE") {
    const importDir =
      config.push.targetDir ||
      `/data/analyzer-imports/${config.name.toLowerCase().replace(/[^a-z0-9]+/g, "-")}/incoming`;
    await form.fillImportDirectory(importDir);
  }

  // Save
  await form.save();
  await form.expectSuccessNotification();

  // Creation returns to the analyzer list.
  await expect(form.surface).not.toBeVisible({ timeout: LONG_TIMEOUT });

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
