/**
 * Unified protocol dispatcher for pushing analyzer results during E2E tests.
 *
 * All protocols go through the mock server:
 * - ASTM: POST /simulate/astm/{template}
 * - HL7:  POST /simulate/hl7/{template}
 * - FILE: POST /simulate/file/{template}
 *
 * The mock server is the single source of truth. It owns the fixture files,
 * knows how to deliver them (TCP, MLLP, or file drop), and returns metadata
 * (sample IDs, results) so tests never hardcode expected values.
 */

import { expect, Page } from "@playwright/test";
import type { DemoPresentation } from "./demo-presentation";
import type { PushConfig, PushResult } from "./analyzer-test-config";

/**
 * Push a result via the mock server and return parsed metadata.
 *
 * For ASTM/HL7: mock generates + sends the message, returns sample_id.
 * For FILE: mock copies the real fixture file to the watched folder,
 *   returns all parsed accessions + results from the file.
 */
export async function pushAnalyzerResult(
  page: Page,
  push: PushConfig,
  presentation: DemoPresentation,
): Promise<PushResult[]> {
  const endpoint = `${push.simulatorUrl}/simulate/${push.protocol.toLowerCase()}/${push.template}`;

  const body: Record<string, unknown> = { count: 1 };

  if (push.destination) {
    body.destination = push.destination;
  }
  if (push.targetDir) {
    body.target_dir = push.targetDir;
  }
  if (push.sampleId) {
    body.sample_id = push.sampleId;
  }

  const response = await page.request.post(endpoint, { data: body });
  expect(
    response.ok(),
    `Mock POST ${endpoint} failed: ${response.status()}`,
  ).toBeTruthy();

  const json = await response.json();
  await response.dispose();
  await presentation.pause(push.protocol === "FILE" ? 2_000 : 1_000);

  // Normalize response into PushResult[] regardless of protocol
  if (json.metadata?.results) {
    // FILE protocol returns parsed fixture metadata
    return json.metadata.results.map(
      (r: { sampleId: string; result: string; testCode?: string }) => ({
        sampleId: r.sampleId,
        result: r.result,
        testCode: r.testCode,
      }),
    );
  }

  if (json.results) {
    // ASTM/HL7 protocol returns results array
    return json.results.map((r: { sample_id?: string; sampleId?: string }) => ({
      sampleId: r.sample_id ?? r.sampleId ?? "",
      result: "",
    }));
  }

  // Fallback: extract sample_id from legacy response shape
  const sampleId = json.sample_id ?? json.sampleId ?? null;
  if (sampleId) {
    return [{ sampleId, result: "" }];
  }

  return [];
}
