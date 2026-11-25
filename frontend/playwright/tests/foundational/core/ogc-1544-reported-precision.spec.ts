import { test, expect, type Page } from "../../../helpers/test-base";
import { createSampleOrder } from "../../../helpers/seed-tat-data";
import {
  LONG_TIMEOUT,
  NAV_TIMEOUT,
  UI_TIMEOUT,
} from "../../../helpers/timeouts";

/**
 * OGC-1544 — a numeric result on a test reporting no decimal places was cut, not
 * rounded (0.9 showed "0"), and releasing it in Validation stored the cut value
 * over what was entered. Now the value is stored exactly as entered, shown
 * rounded half up everywhere ("1"), and Validation never rewrites it. Entry
 * warns about the extra decimals instead of refusing them.
 *
 * Run with:
 *   cd frontend && npm run pw:test:core-foundational -- ogc-1544-reported-precision
 */

const API = "/api/OpenELIS-Global";
const SERUM_SAMPLE_TYPE_ID = "2";

interface OrderableTest {
  id: string;
  name: string;
}

interface WorklistRow {
  testId?: string;
  testName?: string;
  analysisId?: string;
  resultType?: string;
  significantDigits?: number;
  resultValue?: string;
  rawResultValue?: string;
}

async function getJson<T>(page: Page, path: string): Promise<T> {
  const res = await page.request.get(`${API}${path}`);
  expect(res.status(), `GET ${path}`).toBe(200);
  return (await res.json()) as T;
}

async function worklist(page: Page, accession: string, finished: boolean) {
  const body = await getJson<{ testResult?: WorklistRow[] }>(
    page,
    `/rest/LogbookResults?labNumber=${encodeURIComponent(accession)}&doRange=false&finished=${finished}`,
  );
  return body.testResult ?? [];
}

test.describe("OGC-1544 reported precision", () => {
  test("0.9 on a no-decimals test is stored as entered, shown as 1, and kept through validation", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const catalog = await getJson<{ tests?: OrderableTest[] }>(
      page,
      `/rest/sample-type-tests?sampleType=${SERUM_SAMPLE_TYPE_ID}`,
    );
    const now = new Date();
    const pad = (n: number) => String(n).padStart(2, "0");
    const accession = await createSampleOrder(page, {
      labNo: "",
      receivedDate: `${now.getUTCFullYear()}-${pad(now.getUTCMonth() + 1)}-${pad(now.getUTCDate())}`,
      receivedTime: `${pad(now.getUTCHours())}:${pad(now.getUTCMinutes())}`,
      sampleTypeId: SERUM_SAMPLE_TYPE_ID,
      testIds: (catalog.tests ?? [])
        .slice(0, 8)
        .map((t) => t.id)
        .join(","),
    });
    expect(accession, "the order must save").toBeTruthy();

    const row = (await worklist(page, accession, false)).find(
      (r) => r.resultType === "N" && r.significantDigits === 0,
    );
    expect(row, "a numeric Serum test reporting no decimals").toBeTruthy();
    const field = page.getByRole("textbox", {
      name: `Result for ${row!.testName}`,
      exact: true,
    });

    await test.step("Entry warns about the decimals and saves 0.9 as typed", async () => {
      await page.goto(
        `/Results?accessionNumber=${encodeURIComponent(accession)}`,
        { waitUntil: "domcontentloaded" },
      );
      await expect(field).toBeVisible({ timeout: NAV_TIMEOUT });
      await field.fill("0.9");
      await field.press("Tab");
      await expect(
        page.getByText(/saved as entered and reported rounded/).first(),
      ).toBeVisible({ timeout: UI_TIMEOUT });

      const resultRow = page.getByRole("row").filter({ has: field }).first();
      const save = resultRow.getByRole("button", { name: "Save", exact: true });
      await expect(save).toBeEnabled();
      const saved = page.waitForResponse(
        (r) =>
          r
            .url()
            .includes(
              `/rest/results-entry/analysis/${row!.analysisId}/result`,
            ) && r.request().method() === "POST",
        { timeout: LONG_TIMEOUT },
      );
      await save.click();
      const body = (await (await saved).json()) as WorklistRow;
      expect(body.rawResultValue).toBe("0.9");
      expect(body.resultValue).toBe("1");
    });

    await test.step("Validation shows 1 and releasing it keeps 0.9", async () => {
      await page.goto(
        `/validation?labNumber=${encodeURIComponent(accession)}`,
        {
          waitUntil: "domcontentloaded",
        },
      );
      const validationRow = page
        .getByRole("row")
        .filter({ hasText: row!.testName!.split("(")[0] })
        .first();
      await expect(validationRow).toBeVisible({ timeout: NAV_TIMEOUT });
      await validationRow
        .getByRole("button", { name: /expand/i })
        .first()
        .click();
      const released = page.waitForResponse(
        (r) =>
          r.url().includes(`/analysis/${row!.analysisId}/release`) &&
          r.request().method() === "POST",
        { timeout: LONG_TIMEOUT },
      );
      await page.getByRole("button", { name: "Validate & release" }).click();
      expect((await released).status()).toBe(200);
    });

    await test.step("The released result is still 0.9, reported as 1", async () => {
      await expect
        .poll(
          async () =>
            (await worklist(page, accession, true)).find(
              (r) => r.analysisId === row!.analysisId,
            )?.rawResultValue,
          { timeout: UI_TIMEOUT },
        )
        .toBe("0.9");
      const released = (await worklist(page, accession, true)).find(
        (r) => r.analysisId === row!.analysisId,
      );
      expect(released?.resultValue).toBe("1");
    });
  });
});
