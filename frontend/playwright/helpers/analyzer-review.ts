import { expect, type Page } from "@playwright/test";
import { worklistFor, type Analyzer, type WorklistRow } from "./analyzer-api";
import type { createClinicalOrder } from "./analyzer-clinical-order";
import { API } from "./analyzer-profile-api";

export type Order = Awaited<ReturnType<typeof createClinicalOrder>>;

/**
 * What the lab sees saved for one component of an ordered test, read as the
 * Results screen does. The screen names a component after the test.
 */
export async function savedValue(
  page: Page,
  order: Order,
  testId: string,
  component: string,
) {
  const response = await page.request.get(
    `${API}/accession-results?accessionNumber=${encodeURIComponent(order.accession)}`,
  );
  expect(response.ok()).toBeTruthy();
  const data = (await response.json()) as {
    lastName: string;
    testResult: Array<{
      testId: string;
      testName: string;
      resultValue: string;
      resultType: string;
      result?: { id?: string };
      dictionaryResults?: Array<{ id: string; value: string }>;
    }>;
  };
  expect(data.lastName).toBe(order.patientLastName);
  const saved = data.testResult.filter(
    (row) =>
      row.testId === testId &&
      row.testName.endsWith(` — ${component}`) &&
      row.result?.id,
  );
  expect(
    saved.length,
    `Saved ${component} for ${order.accession}`,
  ).toBeGreaterThan(0);
  // A test's own result and an analyte can share a label; they carry the same call.
  const values = saved.map((row) =>
    row.resultType === "D"
      ? row.dictionaryResults?.find((entry) => entry.id === row.resultValue)
          ?.value
      : row.resultValue,
  );
  return [...new Set(values)].join(" | ");
}

/**
 * Accept each result the analyzer staged for these accessions, as the reviewer
 * does, and save. A result open already accepted is left as it is, and Save
 * takes every accepted row on the screen, so the accessions are saved together.
 * A held row has no box to accept and stays on the screen. With
 * `onScreen`, the reviewer is already on the analyzer's results screen with
 * placements entered, which a fresh visit would discard. With `note`, every
 * accepted result carries it.
 */
export async function acceptAll(
  page: Page,
  analyzer: Analyzer,
  accessions: string[],
  { onScreen = false, note }: { onScreen?: boolean; note?: string } = {},
) {
  type Row = WorklistRow & { componentId?: string | null };
  if (!onScreen) {
    await page.goto(`/AnalyzerResults?id=${analyzer.id}`, {
      waitUntil: "domcontentloaded",
    });
  }
  const own: Row[] = [];
  for (const accession of accessions) {
    const staged = await worklistFor<Row>(page, analyzer.id, accession);
    // A test's parts are not rows on the screen; they follow its main row.
    own.push(...staged.filter((row) => !row.componentId));
  }
  const box = (row: Row) =>
    page.locator(`[id="resultList${row.id}.isAccepted"]`);
  const accepting = own.filter((row) => !row.importIssueReason);
  expect(
    accepting.length,
    `Results to accept for ${accessions}`,
  ).toBeGreaterThan(0);
  for (const row of accepting) {
    await expect(box(row)).toBeAttached();
    if (note) await page.locator(`[id="resultList${row.id}.note"]`).fill(note);
    if (!(await box(row).isChecked())) {
      await page.locator(`label[for="resultList${row.id}.isAccepted"]`).click();
    }
    await expect(box(row)).toBeChecked();
  }
  await page.getByRole("button", { name: "Save", exact: true }).click();
  for (const row of accepting) await expect(box(row)).toHaveCount(0);
}
