import { test, expect, Page } from "../../../helpers/test-base";
import {
  SettingsMenu,
  SiteInformationPage,
} from "../../../fixtures/esig-admin";
import { createSampleOrder } from "../../../helpers/seed-tat-data";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1408 — a result save whose request never gets an answer (a dropped
 * connection, a proxy reset, a laptop closed mid-save) must not be shown as
 * saved: the row stays in edit with the typed value and says the result was
 * not saved. A refused value is reported with its accession and the value
 * named, not as "{0}". A save the server answers still closes the editor.
 */

const API = "/api/OpenELIS-Global";
const CATALOG = `${API}/rest/test-catalog`;
const SERUM_SAMPLE_TYPE_ID = "2";
const ESIG_SETTING = "electronicSignatureEnabled";
const SAVE_ROUTE = "**/rest/results-entry/analysis/*/result";
const NOT_SAVED = "The result was not saved because the server did not respond";

const SETTING_MENU: Record<string, SettingsMenu> = {
  [ESIG_SETTING]: "SiteInformationMenu",
};

async function isSettingOn(page: Page, setting: string): Promise<boolean> {
  const menu = new SiteInformationPage(page, SETTING_MENU[setting]);
  await menu.goto();
  const value = await menu.getSettingValue(setting);
  return /true/i.test(value);
}

async function setSetting(page: Page, setting: string, on: boolean) {
  const menu = new SiteInformationPage(page, SETTING_MENU[setting]);
  await menu.goto();
  await menu.setBooleanSetting(setting, on);
}

async function createNumericTest(
  page: Page,
): Promise<{ testId: string; name: string }> {
  await page.goto("/", { waitUntil: "domcontentloaded" });
  const stamp = Date.now().toString().slice(-8);
  const name = `No Answer ${stamp}`;
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
          code: `TNA${stamp}`,
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
                code: "PRIMARY",
                label: name,
                displayOrder: 0,
                resultType: "N",
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
        return { error: `sample-results ${configured.status}` };
      }
      const activated = await fetch(`${catalog}/tests/${testId}/activate`, {
        method: "POST",
        credentials: "include",
        headers,
        body: "{}",
      });
      if (!activated.ok) {
        return { error: `activate ${activated.status}` };
      }
      return { testId };
    },
    { catalog: CATALOG, name, stamp, sampleTypeId: SERUM_SAMPLE_TYPE_ID },
  );
  expect(result.error, "numeric test setup must succeed").toBeUndefined();
  return { testId: result.testId as string, name };
}

async function orderTest(page: Page, testId: string): Promise<string> {
  const now = new Date();
  const pad = (n: number) => String(n).padStart(2, "0");
  return createSampleOrder(page, {
    labNo: "",
    receivedDate: `${now.getUTCFullYear()}-${pad(now.getUTCMonth() + 1)}-${pad(now.getUTCDate())}`,
    receivedTime: `${pad(now.getUTCHours())}:${pad(now.getUTCMinutes())}`,
    sampleTypeId: SERUM_SAMPLE_TYPE_ID,
    testIds: testId,
    referringSiteId: process.env.PW_REFERRING_SITE_ID,
    providerPersonId: process.env.PW_PROVIDER_PERSON_ID,
  });
}

test.describe("Unified Results: a save that gets no answer", () => {
  let esigWasOn = false;

  test.beforeEach(async ({ page }) => {
    esigWasOn = await isSettingOn(page, ESIG_SETTING);
    if (esigWasOn) {
      await setSetting(page, ESIG_SETTING, false);
    }
  });

  test.afterEach(async ({ page }) => {
    if (esigWasOn) {
      await setSetting(page, ESIG_SETTING, true);
    }
  });

  test("keeps the row in edit, names a refused value, and still saves once answered", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const numeric = await createNumericTest(page);
    const accession = await orderTest(page, numeric.testId);
    expect(accession, "an order must exist").toBeTruthy();

    const openRow = async () => {
      await page.goto(
        `/Results?accessionNumber=${encodeURIComponent(accession)}`,
        { waitUntil: "domcontentloaded" },
      );
      const row = page
        .getByRole("main")
        .getByRole("row")
        .filter({ hasText: numeric.name })
        .first();
      await expect(row).toBeVisible({ timeout: NAV_TIMEOUT });
      return row;
    };
    const toast = page.locator(".toastDisplay");

    await test.step("a save that gets no answer is not shown as saved", async () => {
      const row = await openRow();
      const input = row.locator('input[id^="unifiedResultValue-"]');
      await page.route(SAVE_ROUTE, (route) => route.abort("connectionreset"));
      await input.fill("7");
      await row.getByRole("button", { name: /^save$/i }).click();
      await expect(toast).toContainText(NOT_SAVED, { timeout: UI_TIMEOUT });
      await expect(input).toHaveValue("7");
      await expect(row.getByRole("button", { name: /^save$/i })).toBeVisible();
      await expect(row.getByRole("button", { name: /^edit$/i })).toHaveCount(0);
      await page.unroute(SAVE_ROUTE);
    });

    await test.step("and nothing was stored", async () => {
      const row = await openRow();
      await expect(row.locator('input[id^="unifiedResultValue-"]')).toHaveValue(
        "",
      );
      await expect(row.getByRole("button", { name: /^edit$/i })).toHaveCount(0);
    });

    await test.step("text in a numeric result is refused naming the accession and the value", async () => {
      const row = await openRow();
      const input = row.locator('input[id^="unifiedResultValue-"]');
      await input.fill("abc");
      await row.getByRole("button", { name: /^save$/i }).click();
      await expect(toast).toContainText(
        `Errors for accession number ${accession}`,
        { timeout: UI_TIMEOUT },
      );
      await expect(toast).toContainText("abc is not a number");
      await expect(toast).not.toContainText("{0}");
      await expect(input).toHaveValue("abc");
    });

    await test.step("a save the server answers closes the editor and is stored", async () => {
      const row = await openRow();
      const input = row.locator('input[id^="unifiedResultValue-"]');
      await input.fill("7");
      await row.getByRole("button", { name: /^save$/i }).click();
      await expect(row.getByRole("button", { name: /^edit$/i })).toBeVisible({
        timeout: UI_TIMEOUT,
      });

      const saved = await openRow();
      await saved.getByRole("button", { name: /^edit$/i }).click();
      await expect(
        saved.locator('input[id^="unifiedResultValue-"]'),
      ).toHaveValue("7");
    });
  });
});
