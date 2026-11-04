import { test, expect, type Page } from "../../../helpers/test-base";
import { csrfToken } from "../../../helpers/api-session";
import { createSampleOrder } from "../../../helpers/seed-tat-data";
import { LONG_TIMEOUT, NAV_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1569 — /rest/LogbookResults serves a numeric result rounded to its
 * decimal places as resultValue. A client posting a row back with only its
 * note changed sent that rounded value, and 0.9 on a no-decimals test was
 * stored as 1. The rounded form of the stored value now reads as "unchanged"
 * when the client holds no other measurement; typing it on the Results page,
 * which edits from the measurement, and any different value are still saved.
 */

const API = "/api/OpenELIS-Global";
const SERUM_SAMPLE_TYPE_ID = "2";

interface WorklistRow {
  testName?: string;
  analysisId?: string;
  resultType?: string;
  reportingDecimalPlaces?: number;
  resultValue?: string;
  shadowResultValue?: string;
  rawResultValue?: string;
  isModified?: boolean;
  note?: string;
  reportable?: unknown;
  result?: unknown;
}

async function logbook(page: Page, accession: string) {
  const res = await page.request.get(
    `${API}/rest/LogbookResults?labNumber=${encodeURIComponent(accession)}`,
  );
  expect(res.status()).toBe(200);
  return (await res.json()) as { testResult: WorklistRow[] };
}

/** Posts the form back as the existing client does, changing one row. */
async function postBack(
  page: Page,
  accession: string,
  analysisId: string,
  change: Partial<WorklistRow>,
) {
  const form = await logbook(page, accession);
  for (const row of form.testResult) {
    delete row.result;
    row.reportable = row.reportable === true || row.reportable === "Y";
    if (row.analysisId === analysisId) {
      Object.assign(row, { isModified: true }, change);
    }
  }
  const res = await page.request.post(`${API}/rest/LogbookResults`, {
    headers: { "X-CSRF-Token": await csrfToken(page) },
    data: form,
  });
  expect(res.status()).toBe(200);
}

async function stored(page: Page, accession: string, analysisId: string) {
  return (await logbook(page, accession)).testResult.find(
    (row) => row.analysisId === analysisId,
  );
}

test.describe("OGC-1569 a row posted back keeps the entered value", () => {
  test("a note-only save keeps 0.9; a new value is still saved", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const catalog = await (
      await page.request.get(
        `${API}/rest/sample-type-tests?sampleType=${SERUM_SAMPLE_TYPE_ID}`,
      )
    ).json();
    const now = new Date();
    const pad = (n: number) => String(n).padStart(2, "0");
    const accession = await createSampleOrder(page, {
      labNo: "",
      receivedDate: `${now.getUTCFullYear()}-${pad(now.getUTCMonth() + 1)}-${pad(now.getUTCDate())}`,
      receivedTime: `${pad(now.getUTCHours())}:${pad(now.getUTCMinutes())}`,
      sampleTypeId: SERUM_SAMPLE_TYPE_ID,
      testIds: (catalog.tests ?? [])
        .slice(0, 8)
        .map((t: { id: string }) => t.id)
        .join(","),
    });
    const row = (await logbook(page, accession)).testResult.find(
      (r) => r.resultType === "N" && r.reportingDecimalPlaces === 0,
    );
    expect(row, "a numeric Serum test reporting no decimals").toBeTruthy();
    const analysisId = row!.analysisId!;

    await test.step("Enter 0.9 on the Results page", async () => {
      await page.goto(
        `/Results?accessionNumber=${encodeURIComponent(accession)}`,
        { waitUntil: "domcontentloaded" },
      );
      const field = page.getByRole("textbox", {
        name: `Result for ${row!.testName}`,
        exact: true,
      });
      await expect(field).toBeVisible({ timeout: NAV_TIMEOUT });
      await field.fill("0.9");
      const saved = page.waitForResponse(
        (r) =>
          r
            .url()
            .includes(`/rest/results-entry/analysis/${analysisId}/result`) &&
          r.request().method() === "POST",
        { timeout: LONG_TIMEOUT },
      );
      await page
        .getByRole("row")
        .filter({ has: field })
        .first()
        .getByRole("button", { name: "Save", exact: true })
        .click();
      expect((await saved).status()).toBe(200);
      const served = await stored(page, accession, analysisId);
      expect(served?.resultValue, "served rounded").toBe("1");
      expect(served?.rawResultValue).toBe("0.9");
    });

    await test.step("A note-only post of the served row keeps 0.9", async () => {
      await postBack(page, accession, analysisId, {
        note: "OGC-1569 note only",
      });
      expect((await stored(page, accession, analysisId))?.rawResultValue).toBe(
        "0.9",
      );
    });

    await test.step("The same note posted again still saves", async () => {
      await postBack(page, accession, analysisId, {
        note: "OGC-1569 note only",
      });
      expect((await stored(page, accession, analysisId))?.rawResultValue).toBe(
        "0.9",
      );
    });

    await test.step("Typing the reported 1 on the Results page stores 1", async () => {
      await page.goto(
        `/Results?accessionNumber=${encodeURIComponent(accession)}`,
        { waitUntil: "domcontentloaded" },
      );
      const resultRow = page
        .getByRole("row")
        .filter({ hasText: row!.testName })
        .first();
      await resultRow.getByRole("button", { name: "Edit" }).click();
      const field = page.getByRole("textbox", {
        name: `Result for ${row!.testName}`,
        exact: true,
      });
      await expect(field).toHaveValue("0.9");
      await field.fill("1");
      const saved = page.waitForResponse(
        (r) =>
          r
            .url()
            .includes(`/rest/results-entry/analysis/${analysisId}/result`) &&
          r.request().method() === "POST",
        { timeout: LONG_TIMEOUT },
      );
      await page
        .getByRole("row")
        .filter({ has: field })
        .first()
        .getByRole("button", { name: "Save", exact: true })
        .click();
      expect((await saved).status()).toBe(200);
      expect((await stored(page, accession, analysisId))?.rawResultValue).toBe(
        "1",
      );
    });

    await test.step("A different value is what the user entered and is saved", async () => {
      await postBack(page, accession, analysisId, {
        resultValue: "2",
        shadowResultValue: "2",
      });
      expect((await stored(page, accession, analysisId))?.rawResultValue).toBe(
        "2",
      );
    });
  });
});
