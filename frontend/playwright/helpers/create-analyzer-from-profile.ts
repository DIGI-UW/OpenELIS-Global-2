/**
 * Create an analyzer via the UI using a profile for auto-fill.
 *
 * Handles the full creation flow (inline lab-facing setup at `/analyzers?add=1`):
 * 1. (TCP only) Create mock network to get unique analyzer IP
 * 2. Open dashboard → click Add
 * 3. Select profile (auto-resolves plugin type + analyzer type) → fill name
 * 4. Save → verify the profile-driven mapping route
 * 5. Open Edit from the analyzer list
 * 6. Enter TCP settings or the bridge-owned FILE import directory
 *
 * The inline form does not expose plugin-type/analyzer-type dropdowns — the
 * profile selection alone drives those fields server-side. Instance connection
 * settings are configured after creation through the visible Edit workflow.
 * For FILE analyzers, OpenELIS stores and registers the directory while the
 * bridge owns watching and transport.
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
const ANALYZER_API_PATH = "/api/OpenELIS-Global/rest/analyzer/analyzers";
const API_READY_TIMEOUT_MS = 15_000;
const API_RETRY_DELAY_MS = 500;

function getAnalyzerApiUrl(): string {
  const baseUrl = (process.env.BASE_URL || "https://localhost").replace(
    /\/$/,
    "",
  );
  return `${baseUrl}${ANALYZER_API_PATH}`;
}

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
      await response.dispose();
      return body.ip || null;
    }
    const status = response.status();
    await response.dispose();
    // 409 = already exists, which is fine (idempotent)
    if (status === 409) {
      // Fetch existing
      const listResp = await page.request.get(`${SIMULATOR_URL}/analyzers`);
      const list = listResp.ok() ? await listResp.json() : null;
      await listResp.dispose();
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
async function removeMockNetwork(page: Page, mockName: string): Promise<void> {
  try {
    const existing = await page.request.get(`${SIMULATOR_URL}/analyzers`);
    const body = existing.ok() ? await existing.json() : null;
    await existing.dispose();
    if (!body) return;

    const exists = Array.isArray(body?.analyzers)
      ? body.analyzers.some((a: { name?: string }) => a?.name === mockName)
      : false;

    if (!exists) return;

    const delResp = await page.request.delete(
      `${SIMULATOR_URL}/analyzers/${mockName}`,
    );
    await delResp.dispose();
  } catch {
    // Best-effort cleanup
  }
}

async function waitForAnalyzerApiReady(page: Page): Promise<void> {
  const analyzerApiUrl = getAnalyzerApiUrl();

  await expect
    .poll(
      async () => {
        try {
          const response = await page.request.get(analyzerApiUrl);
          const status = response.status();
          await response.dispose();
          return status;
        } catch {
          return 0; // Network can flap while docker networks settle
        }
      },
      {
        message: `Analyzer API at ${analyzerApiUrl} did not become ready`,
        timeout: API_READY_TIMEOUT_MS,
        intervals: [API_RETRY_DELAY_MS],
      },
    )
    .toBe(200);
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

    // Creating/attaching docker networks can briefly destabilize connectivity.
    await waitForAnalyzerApiReady(page);
  }

  await list.goto();
  await list.expectLoaded();
  await presentation.pause(500);

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

  // Select profile — auto-resolves plugin type, analyzer type, identifier
  // pattern, and (for FILE) import defaults. The inline form has no separate
  // plugin-type/analyzer-type dropdowns to drive.
  if (!config.profileName) {
    throw new Error(
      `AnalyzerTestConfig "${config.name}" has no profileName; the inline setup flow requires a profile.`,
    );
  }
  await form.selectProfile(config.profileName);
  await presentation.pause(500);

  // Fill name
  await form.fillName(config.name);
  await presentation.pause(500);

  // Create the analyzer identity from the profile. Connection settings belong
  // to the post-create Edit/Connect surface and are intentionally absent here.
  await waitForAnalyzerApiReady(page);
  await form.save();
  await expect(page).toHaveURL(
    /\/analyzers\/\d+\/mappings\?setup=1&step=verify/,
    { timeout: LONG_TIMEOUT },
  );
  const analyzerId = new URL(page.url()).pathname.split("/")[2];
  await expect(page.getByTestId("field-mapping")).toBeVisible({
    timeout: LONG_TIMEOUT,
  });

  // Configure instance connection settings through the visible list → Edit
  // workflow. For FILE, the directory is registered with the bridge; OpenELIS
  // does not watch or poll it.
  await list.goto();
  await list.expectLoaded();
  await list.search(config.name);
  await expect(list.getRow(analyzerId)).toBeVisible({ timeout: LONG_TIMEOUT });
  await list.openOverflowMenu(analyzerId);
  await list.clickAction(analyzerId, "edit");
  await form.expectOpen();

  if (config.protocol === "FILE") {
    const importDir =
      config.push.protocol === "FILE"
        ? config.push.targetDir ||
          `/data/analyzer-imports/${config.name.toLowerCase().replace(/[^a-z0-9]+/g, "-")}/incoming`
        : `/data/analyzer-imports/${config.name.toLowerCase().replace(/[^a-z0-9]+/g, "-")}/incoming`;
    await form.fillImportDirectory(importDir);
  } else {
    const ip = assignedIp || config.ipAddress;
    if (ip) {
      await form.fillIpAddress(ip);
    }
    if (config.port) {
      await form.fillPort(String(config.port));
    }
  }
  await presentation.pause(500);

  await form.save();
  await expect(page).toHaveURL(/\/analyzers(?:\?.*)?$/, {
    timeout: LONG_TIMEOUT,
  });
  await list.expectLoaded();
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

/** Escape a value for use inside a PostgreSQL single-quoted literal. */
function escapePgStringLiteral(value: string): string {
  return `'${value.replace(/'/g, "''")}'`;
}

/**
 * Hard-delete analyzer rows after UI soft-delete (test isolation).
 * CASCADE FK on analyzer_test_map handles test mapping cleanup automatically.
 */
function hardDeleteAnalyzerFromDb(analyzerName: string): void {
  const container = resolveDbContainer();
  const nameLiteral = escapePgStringLiteral(analyzerName);
  const sql = `DELETE FROM clinlims.analyzer_results WHERE analyzer_id IN (SELECT id FROM clinlims.analyzer WHERE name = ${nameLiteral}); DELETE FROM clinlims.analyzer WHERE name = ${nameLiteral};`;
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
