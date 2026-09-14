import { test, expect, Page } from "../../../helpers/test-base";
import { SiteInformationPage } from "../../../fixtures/esig-admin";
import { createSampleOrder } from "../../../helpers/seed-tat-data";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1186 — on a test with several result components, saving a multi-select
 * value wrote it to the sibling dictionary component: that component's row
 * changed type, the multi-select row stayed empty, and a re-save added a
 * phantom row. The two components offered the same dictionary entries, and the
 * save bound the value by test and entry alone.
 *
 * The walk is the ticket's: order a three-component test, tick one option on
 * the Multi-Select row, save, and read the worklist back.
 */

const API = "/api/OpenELIS-Global";
const CATALOG = `${API}/rest/test-catalog`;
const SERUM_SAMPLE_TYPE_ID = "2";
const DETECTED = "Detected";
const NOT_DETECTED = "Not detected";

interface MultiComponentTest {
  testId: string;
  name: string;
}

/**
 * Creates an active Serum test with a numeric primary, a dictionary component
 * and a multi-select component. The two selectable components offer the same
 * two entries, which is what routed the value onto the wrong component.
 */
async function createMultiComponentTest(
  page: Page,
): Promise<MultiComponentTest> {
  await page.goto("/", { waitUntil: "domcontentloaded" });
  const stamp = Date.now().toString().slice(-8);
  const name = `OGC1186 MC ${stamp}`;
  const result = await page.evaluate(
    async ({ catalog, name, stamp, sampleTypeId, options }) => {
      const csrf = localStorage.getItem("CSRF") || "";
      const headers = {
        "Content-Type": "application/json",
        "X-CSRF-Token": csrf,
      };
      const labUnitsRes = await fetch(`${catalog}/lab-units`, {
        credentials: "include",
        headers,
      });
      const labUnits = await labUnitsRes.json();
      const labUnitId = Array.isArray(labUnits) && labUnits[0]?.id;
      const created = await fetch(`${catalog}/tests`, {
        method: "POST",
        credentials: "include",
        headers,
        body: JSON.stringify({
          name,
          reportingName: name,
          code: `M1186${stamp}`,
          labUnitId,
          sampleTypeIds: [sampleTypeId],
          domain: "CLINICAL",
          orderable: true,
        }),
      });
      const createdText = await created.text();
      if (!created.ok) {
        return { error: `create ${created.status}: ${createdText}` };
      }
      const testId = JSON.parse(createdText).testId as string;
      const optionRows = options.map((value: string, index: number) => ({
        value,
        sortOrder: index + 1,
        normal: index === 1,
      }));
      const configured = await fetch(
        `${catalog}/tests/${testId}/sample-results`,
        {
          method: "PUT",
          credentials: "include",
          headers,
          body: JSON.stringify({
            testId,
            components: [
              {
                code: "RESULT_N",
                label: "Numeric Result",
                displayOrder: 0,
                resultType: "N",
                isPrimary: true,
                showOnReport: true,
                interpretations: [],
                options: [],
              },
              {
                code: "RESULT_D",
                label: "Dictionary Result",
                displayOrder: 1,
                resultType: "D",
                isPrimary: false,
                showOnReport: true,
                interpretations: [],
                options: optionRows,
              },
              {
                code: "RESULT_M",
                label: "Multi-Select Result",
                displayOrder: 2,
                resultType: "M",
                isPrimary: false,
                showOnReport: true,
                interpretations: [],
                options: optionRows,
              },
            ],
          }),
        },
      );
      if (!configured.ok) {
        return {
          error: `sample-results ${configured.status}: ${await configured.text()}`,
        };
      }
      const activated = await fetch(`${catalog}/tests/${testId}/activate`, {
        method: "POST",
        credentials: "include",
        headers,
        body: "{}",
      });
      if (!activated.ok) {
        return {
          error: `activate ${activated.status}: ${await activated.text()}`,
        };
      }
      return { testId };
    },
    {
      catalog: CATALOG,
      name,
      stamp,
      sampleTypeId: SERUM_SAMPLE_TYPE_ID,
      options: [DETECTED, NOT_DETECTED],
    },
  );
  expect(
    result.error,
    "multi-component test setup must succeed",
  ).toBeUndefined();
  return { testId: result.testId as string, name };
}

interface WorklistRow {
  component: string;
  resultType: string;
  resultValue: string;
  resultId: string;
}

/** The worklist rows for one order, as the results page itself reads them. */
async function readWorklist(
  page: Page,
  accessionNumber: string,
): Promise<WorklistRow[]> {
  return page.evaluate(
    async ({ api, accessionNumber }) => {
      const csrf = localStorage.getItem("CSRF") || "";
      const res = await fetch(
        `${api}/rest/LogbookResults?labNumber=${encodeURIComponent(accessionNumber)}&doRange=false&finished=false`,
        { credentials: "include", headers: { "X-CSRF-Token": csrf } },
      );
      const body = await res.json();
      return (body.testResult || []).map(
        (row: {
          testName: string;
          resultType: string;
          resultValue: string;
          resultId: string;
        }) => ({
          component: (row.testName || "").split(" — ")[1] || "",
          resultType: row.resultType,
          resultValue: row.resultValue || "",
          resultId: row.resultId || "",
        }),
      );
    },
    { api: API, accessionNumber },
  );
}

async function esigEnabled(page: Page): Promise<boolean> {
  const res = await page.request.get(`${API}/rest/esig/enabled`);
  const body = await res.json();
  return Boolean(body?.enabled);
}

test.describe("OGC-1186 multi-component result routing", () => {
  let esigWasOn = false;

  test.beforeEach(async ({ page }) => {
    // A signing ceremony is a different flow (esig-result-validation.spec.ts);
    // this spec is about where a value lands, so it runs with signatures off
    // and puts the setting back afterwards.
    esigWasOn = await esigEnabled(page);
    if (esigWasOn) {
      const siteInfo = new SiteInformationPage(page);
      await siteInfo.goto();
      await siteInfo.setBooleanSetting("electronicSignatureEnabled", false);
    }
  });

  test.afterEach(async ({ page }) => {
    if (esigWasOn) {
      const siteInfo = new SiteInformationPage(page);
      await siteInfo.goto();
      await siteInfo.setBooleanSetting("electronicSignatureEnabled", true);
    }
  });

  test("a multi-select value stays on its own component when a sibling offers the same entry", async ({
    page,
  }) => {
    test.setTimeout(90_000);

    const multi = await createMultiComponentTest(page);

    const now = new Date();
    const pad = (n: number) => String(n).padStart(2, "0");
    const accessionNumber = await createSampleOrder(page, {
      labNo: "",
      receivedDate: `${now.getUTCFullYear()}-${pad(now.getUTCMonth() + 1)}-${pad(now.getUTCDate())}`,
      receivedTime: `${pad(now.getUTCHours())}:${pad(now.getUTCMinutes())}`,
      sampleTypeId: SERUM_SAMPLE_TYPE_ID,
      testIds: multi.testId,
    });
    expect(accessionNumber, "the test must be orderable").toBeTruthy();

    const main = page.getByRole("main");
    const rowFor = (component: string) =>
      main
        .locator("tr", { hasText: accessionNumber })
        .filter({ hasText: component })
        .first();

    await test.step("Results Entry shows one row per component", async () => {
      await page.goto(
        `/Results?accessionNumber=${encodeURIComponent(accessionNumber)}`,
        { waitUntil: "domcontentloaded" },
      );
      await expect(rowFor("Numeric Result")).toBeVisible({
        timeout: NAV_TIMEOUT,
      });
      await expect(rowFor("Dictionary Result").locator("select")).toBeVisible();
      await expect(
        rowFor("Multi-Select Result").getByRole("combobox"),
      ).toBeVisible();
    });

    await test.step("Tick one option on the Multi-Select row and save it", async () => {
      const row = rowFor("Multi-Select Result");
      // The Dictionary row's <select> also has an option named "Detected", so
      // the pick is scoped to the open multi-select menu.
      await row.getByRole("combobox").click();
      await page
        .getByRole("listbox")
        .getByRole("option", { name: DETECTED, exact: true })
        .click();
      await page.keyboard.press("Escape");

      const save = row.getByRole("button", { name: /^save$/i });
      await expect(save).toBeVisible({ timeout: UI_TIMEOUT });
      await save.click();
      await expect(row.getByRole("button", { name: /^edit$/i })).toBeVisible({
        timeout: UI_TIMEOUT,
      });
    });

    await test.step("After a reload the value is on the Multi-Select row and the Dictionary row is untouched", async () => {
      await page.reload({ waitUntil: "domcontentloaded" });
      await expect(rowFor("Numeric Result")).toBeVisible({
        timeout: NAV_TIMEOUT,
      });

      const multiRow = rowFor("Multi-Select Result");
      await expect(multiRow.locator(".unifiedResultsReadOnlyValue")).toHaveText(
        DETECTED,
        { timeout: UI_TIMEOUT },
      );
      await expect(
        multiRow.getByRole("button", { name: /^edit$/i }),
      ).toBeVisible();

      // Every component row of an accepted analysis renders read-only, so the
      // sibling is checked for what it shows, not for which control it offers.
      const dictionaryRow = rowFor("Dictionary Result");
      await expect(dictionaryRow).toBeVisible();
      await expect(dictionaryRow).not.toContainText(DETECTED);
      await expect(dictionaryRow).not.toContainText(NOT_DETECTED);
    });

    await test.step("The worklist reads the value back on the Multi-Select component only", async () => {
      const rows = await readWorklist(page, accessionNumber);
      expect(rows, "three component rows").toHaveLength(3);

      const dictionary = rows.find((r) => r.component === "Dictionary Result");
      expect(
        dictionary?.resultType,
        "the dictionary component keeps its type",
      ).toBe("D");
      expect(
        dictionary?.resultId,
        "the dictionary component holds no result",
      ).toBe("");

      const multiSelect = rows.find(
        (r) => r.component === "Multi-Select Result",
      );
      expect(multiSelect?.resultType).toBe("M");
      expect(
        multiSelect?.resultId,
        "the selection is filed on the multi-select component",
      ).not.toBe("");
    });
  });
});
