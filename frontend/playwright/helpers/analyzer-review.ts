import { expect, type Page } from "@playwright/test";
import { worklistFor, type Analyzer, type WorklistRow } from "./analyzer-api";
import type { createClinicalOrder } from "./analyzer-clinical-order";
import type { DemoPresentation } from "./demo-presentation";
import { API } from "./analyzer-profile-api";

export type Order = Awaited<ReturnType<typeof createClinicalOrder>>;

/**
 * Brings one sample into view on the review page: the page lists the
 * analyzer's samples on the left and shows one at a time.
 */
export async function showSample(page: Page, accession: string) {
  const tab = page
    .locator("[data-testid^='sample-tab-']", { hasText: accession })
    .first();
  await expect(tab).toBeVisible();
  if ((await tab.getAttribute("aria-current")) !== "true") await tab.click();
  const detail = page.getByTestId("sample-detail");
  await expect(detail).toContainText(accession);
  return detail;
}

/**
 * What the lab sees saved for one component of an ordered test, or for the
 * test itself when no component is named, read as the Results screen does.
 * The screen names a component after the test.
 */
export async function savedValue(
  page: Page,
  order: Order,
  testId: string,
  component?: string,
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
      (component
        ? row.testName.endsWith(` — ${component}`)
        : !row.testName.includes(" — ")) &&
      row.result?.id,
  );
  expect(
    saved.length,
    `Saved ${component ?? `test ${testId}`} for ${order.accession}`,
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
 * A held row has no decision to accept and stays on the screen. Each
 * accession's sample is brought into view in turn. With
 * `onScreen`, the reviewer is already on the analyzer's results screen with
 * placements entered, which a fresh visit would discard. With `note`, every
 * accepted result carries it. With `demo`, a recording dwells on the screen
 * before and after.
 */
export async function acceptAll(
  page: Page,
  analyzer: Analyzer,
  accessions: string[],
  {
    onScreen = false,
    note,
    demo,
  }: { onScreen?: boolean; note?: string; demo?: DemoPresentation } = {},
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
  await demo?.pause(2000);
  for (const accession of accessions) {
    const rows = accepting.filter((row) => row.accessionNumber === accession);
    if (rows.length === 0) continue;
    await showSample(page, accession);
    for (const row of rows) await expect(box(row)).toBeAttached();
    for (const row of rows) {
      if (note) {
        // The note sits in the row's details.
        await page
          .getByTestId(`result-row-${row.id}`)
          .getByRole("button", { name: "Details" })
          .click();
        await page.locator(`[id="resultList${row.id}.note"]`).fill(note);
      }
      if (!(await box(row).isChecked())) {
        await page
          .locator(`label[for="resultList${row.id}.isAccepted"]`)
          .click();
      }
      await expect(box(row)).toBeChecked();
    }
  }
  await demo?.pause(1000);
  await page.getByRole("button", { name: "Save", exact: true }).click();
  for (const row of accepting) await expect(box(row)).toHaveCount(0);
  await demo?.pause(1500);
}
