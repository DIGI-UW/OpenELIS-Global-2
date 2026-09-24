import { test, expect, Page } from "../../../helpers/test-base";
import {
  SettingsMenu,
  SiteInformationPage,
} from "../../../fixtures/esig-admin";
import { createSampleOrder } from "../../../helpers/seed-tat-data";
import {
  NAV_TIMEOUT,
  QUICK_TIMEOUT,
  UI_TIMEOUT,
} from "../../../helpers/timeouts";

/**
 * The unified Results screen judges a numeric value as it is typed, the way
 * the legacy screen does: the flag chip and the accent follow the value
 * before Save, on a fresh entry and on an edit, and the server agrees once
 * the value is saved. The expanded row also names the kind of sample the
 * result belongs to, as the legacy Sample Kind column does.
 *
 * Normal 5–100, Critical 2–150, Valid 0–1000: the OGC-1121 setup.
 */

const API = "/api/OpenELIS-Global";
const CATALOG = `${API}/rest/test-catalog`;
const SERUM_SAMPLE_TYPE_ID = "2";
const UNIFIED_ROUTE_SETTING = "resultsEntryUnifiedRoute";
const ESIG_SETTING = "electronicSignatureEnabled";

const SETTING_MENU: Record<string, SettingsMenu> = {
  [UNIFIED_ROUTE_SETTING]: "ResultConfigurationMenu",
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

async function createRangedTest(
  page: Page,
): Promise<{ testId: string; name: string }> {
  await page.goto("/", { waitUntil: "domcontentloaded" });
  const stamp = Date.now().toString().slice(-8);
  const name = `Live Flag ${stamp}`;
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
          code: `TLF${stamp}`,
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
      const components = await configured.json();
      if (!configured.ok) {
        return { error: `sample-results ${configured.status}` };
      }
      const ranges = await fetch(`${catalog}/tests/${testId}/ranges`, {
        method: "PUT",
        credentials: "include",
        headers,
        body: JSON.stringify({
          testId,
          ranges: [
            {
              componentId: components.components[0].id,
              gender: null,
              minAge: 0,
              maxAge: null,
              lowNormal: 5,
              highNormal: 100,
              lowCritical: 2,
              highCritical: 150,
              lowValid: 0,
              highValid: 1000,
            },
          ],
        }),
      });
      if (!ranges.ok) {
        return { error: `ranges ${ranges.status}: ${await ranges.text()}` };
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
  expect(result.error, "ranged test setup must succeed").toBeUndefined();
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
  });
}

test.describe("Unified Results judges a value as it is typed", () => {
  let unifiedWasOn = true;
  let esigWasOn = false;

  test.beforeEach(async ({ page }) => {
    unifiedWasOn = await isSettingOn(page, UNIFIED_ROUTE_SETTING);
    if (!unifiedWasOn) {
      await setSetting(page, UNIFIED_ROUTE_SETTING, true);
    }
    esigWasOn = await isSettingOn(page, ESIG_SETTING);
    if (esigWasOn) {
      await setSetting(page, ESIG_SETTING, false);
    }
  });

  test.afterEach(async ({ page }) => {
    if (esigWasOn) {
      await setSetting(page, ESIG_SETTING, true);
    }
    if (!unifiedWasOn) {
      await setSetting(page, UNIFIED_ROUTE_SETTING, false);
    }
  });

  test("the flag follows the typed value before Save, survives Save, and the row names its sample kind", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const ranged = await createRangedTest(page);
    const accession = await orderTest(page, ranged.testId);
    expect(accession, "an order must exist").toBeTruthy();

    await page.goto(
      `/Results?accessionNumber=${encodeURIComponent(accession)}`,
      { waitUntil: "domcontentloaded" },
    );
    const main = page.getByRole("main");
    const row = main.getByRole("row").filter({ hasText: ranged.name }).first();
    await expect(row).toBeVisible({ timeout: NAV_TIMEOUT });
    const input = row.locator('input[id^="unifiedResultValue-"]');
    const chip = (flag: string) => row.locator(`[data-testid="flag-${flag}"]`);

    await test.step("no value, no flag", async () => {
      await expect(row.locator('[data-testid^="flag-"]')).toHaveCount(0, {
        timeout: QUICK_TIMEOUT,
      });
    });

    await test.step("50 is normal, 120 abnormal, 200 critical, 2000 invalid, with nothing saved", async () => {
      for (const [value, flag] of [
        ["50", "NORMAL"],
        ["120", "ABNORMAL"],
        ["200", "CRITICAL"],
        ["2000", "INVALID"],
        ["50", "NORMAL"],
      ] as const) {
        await input.fill(value);
        await expect(chip(flag)).toBeVisible({ timeout: UI_TIMEOUT });
        await expect(row.locator('[data-testid^="flag-"]')).toHaveCount(1);
      }
      await input.fill("");
      await expect(row.locator('[data-testid^="flag-"]')).toHaveCount(0, {
        timeout: UI_TIMEOUT,
      });
    });

    await test.step("the expanded row names a client sample", async () => {
      await row.getByRole("button", { name: /expand/i }).click();
      const kind = main.locator('[data-testid^="sample-kind-"]').first();
      await expect(kind).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(kind).toContainText("Sample Kind");
      await expect(kind).toContainText("Client sample");
    });

    await test.step("saving an abnormal value keeps the flag the server computes", async () => {
      await input.fill("120");
      await expect(chip("ABNORMAL")).toBeVisible({ timeout: UI_TIMEOUT });
      await row.getByRole("button", { name: /^save$/i }).click();
      await expect(row.getByRole("button", { name: /^edit$/i })).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      await expect(chip("ABNORMAL")).toBeVisible({ timeout: UI_TIMEOUT });

      await page.reload({ waitUntil: "domcontentloaded" });
      const saved = page
        .getByRole("main")
        .getByRole("row")
        .filter({ hasText: ranged.name })
        .first();
      await expect(saved).toBeVisible({ timeout: NAV_TIMEOUT });
      await expect(saved.locator('[data-testid="flag-ABNORMAL"]')).toBeVisible({
        timeout: UI_TIMEOUT,
      });
    });

    await test.step("editing the saved value re-judges it before Save", async () => {
      const saved = page
        .getByRole("main")
        .getByRole("row")
        .filter({ hasText: ranged.name })
        .first();
      await saved.getByRole("button", { name: /^edit$/i }).click();
      const editInput = saved.locator('input[id^="unifiedResultValue-"]');
      await editInput.fill("200");
      await expect(saved.locator('[data-testid="flag-CRITICAL"]')).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      await editInput.fill("50");
      await expect(saved.locator('[data-testid="flag-NORMAL"]')).toBeVisible({
        timeout: UI_TIMEOUT,
      });
    });
  });
});
