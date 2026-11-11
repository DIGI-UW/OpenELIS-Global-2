import { test, expect, Page } from "../../../helpers/test-base";
import { SiteInformationPage } from "../../../fixtures/esig-admin";
import { createSampleOrder } from "../../../helpers/seed-tat-data";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1185 — a test whose result component is a Titer (resultType T) could be
 * created, activated and ordered, but Results Entry rendered no control for
 * it, so the row sat at "Not started" forever.
 *
 * The walk is the ticket's: configure a titer test, order it, open Results
 * Entry, and result it with a dilution ratio. Assertions are on what the
 * technician sees and on what the worklist reads back, never on a status code.
 */

const API = "/api/OpenELIS-Global";
const CATALOG = `${API}/rest/test-catalog`;
const SERUM_SAMPLE_TYPE_ID = "2";

interface TiterTest {
  testId: string;
  name: string;
}

/**
 * Creates an active Serum test whose only component is a titer — the shape the
 * Test Catalogue Editor writes when "Advanced / legacy types → Titer" is chosen.
 * Runs inside the browser so it rides the same session and CSRF token as the UI.
 */
async function createTiterTest(page: Page): Promise<TiterTest> {
  await page.goto("/", { waitUntil: "domcontentloaded" });
  const stamp = Date.now().toString().slice(-8);
  const name = `OGC1185 Titer ${stamp}`;
  const result = await page.evaluate(
    async ({ catalog, name, stamp, sampleTypeId }) => {
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
          code: `T1185${stamp}`,
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
                code: "TITER1",
                label: "Titer",
                displayOrder: 0,
                resultType: "T",
                isPrimary: true,
                showOnReport: true,
                interpretations: [],
                options: [],
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
    { catalog: CATALOG, name, stamp, sampleTypeId: SERUM_SAMPLE_TYPE_ID },
  );
  expect(result.error, "titer test setup must succeed").toBeUndefined();
  return { testId: result.testId as string, name };
}

async function esigEnabled(page: Page): Promise<boolean> {
  const res = await page.request.get(`${API}/rest/esig/enabled`);
  const body = await res.json();
  return Boolean(body?.enabled);
}

test.describe("OGC-1185 titer result entry", () => {
  let esigWasOn = false;

  test.beforeEach(async ({ page }) => {
    // A signing ceremony is a different flow (esig-result-validation.spec.ts);
    // this spec is about the entry control, so it runs with signatures off and
    // puts the setting back afterwards.
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

  test("a titer test can be resulted with a dilution ratio", async ({
    page,
  }) => {
    test.setTimeout(90_000);

    const titer = await createTiterTest(page);

    const now = new Date();
    const pad = (n: number) => String(n).padStart(2, "0");
    const accessionNumber = await createSampleOrder(page, {
      labNo: "",
      receivedDate: `${now.getUTCFullYear()}-${pad(now.getUTCMonth() + 1)}-${pad(now.getUTCDate())}`,
      receivedTime: `${pad(now.getUTCHours())}:${pad(now.getUTCMinutes())}`,
      sampleTypeId: SERUM_SAMPLE_TYPE_ID,
      testIds: titer.testId,
    });
    expect(accessionNumber, "the titer test must be orderable").toBeTruthy();

    await test.step("Results Entry offers a text control for the titer row", async () => {
      await page.goto(
        `/Results?accessionNumber=${encodeURIComponent(accessionNumber)}`,
        { waitUntil: "domcontentloaded" },
      );
      const row = page
        .getByRole("main")
        .locator("tr", { hasText: accessionNumber })
        .first();
      await expect(row).toBeVisible({ timeout: NAV_TIMEOUT });
      await expect(row).toContainText(titer.name);

      const input = row.locator('input[id^="unifiedResultValue-"]');
      await expect(input).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(input).toHaveAttribute("type", "text");
      await expect(input).toHaveAttribute("placeholder", /1:10/);
      await expect(row.locator('input[type="number"]')).toHaveCount(0);
    });

    await test.step("Typing a ratio offers Save, and saving keeps it as typed", async () => {
      const row = page
        .getByRole("main")
        .locator("tr", { hasText: accessionNumber })
        .first();
      const input = row.locator('input[id^="unifiedResultValue-"]');
      await input.fill("1:10");

      const save = row.getByRole("button", { name: /^save$/i });
      await expect(save).toBeVisible({ timeout: UI_TIMEOUT });
      await save.click();

      await expect(row.locator(".unifiedResultsReadOnlyValue")).toHaveText(
        "1:10",
        { timeout: UI_TIMEOUT },
      );
      await expect(row).not.toContainText("Not started");
    });

    await test.step("The stored value survives a reload", async () => {
      await page.reload({ waitUntil: "domcontentloaded" });
      const row = page
        .getByRole("main")
        .locator("tr", { hasText: accessionNumber })
        .first();
      await expect(row).toBeVisible({ timeout: NAV_TIMEOUT });
      await expect(row.locator(".unifiedResultsReadOnlyValue")).toHaveText(
        "1:10",
        { timeout: UI_TIMEOUT },
      );
    });
  });
});
