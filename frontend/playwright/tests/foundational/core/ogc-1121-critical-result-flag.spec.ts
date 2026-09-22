import { test, expect, Page } from "../../../helpers/test-base";
import {
  SettingsMenu,
  SiteInformationPage,
} from "../../../fixtures/esig-admin";
import { createSampleOrder } from "../../../helpers/seed-tat-data";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1121 — a result beyond the configured CRITICAL range looked exactly like
 * a merely abnormal one on the legacy Results Entry screen, and the Validation
 * queue showed no cue at all. A technologist must be able to tell 200 (critical)
 * from 120 (abnormal) at a glance, on both screens.
 *
 * Normal 5–100, Critical 2–150: the ticket's own setup.
 */

const API = "/api/OpenELIS-Global";
const CATALOG = `${API}/rest/test-catalog`;
const SERUM_SAMPLE_TYPE_ID = "2";
const UNIFIED_ROUTE_SETTING = "resultsEntryUnifiedRoute";
const ESIG_SETTING = "electronicSignatureEnabled";

interface CriticalTest {
  testId: string;
  name: string;
}

async function createTestWithCriticalRange(page: Page): Promise<CriticalTest> {
  await page.goto("/", { waitUntil: "domcontentloaded" });
  const stamp = Date.now().toString().slice(-8);
  const name = `OGC1121 Crit ${stamp}`;
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
          code: `T1121${stamp}`,
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
        return {
          error: `activate ${activated.status}: ${await activated.text()}`,
        };
      }
      return { testId };
    },
    { catalog: CATALOG, name, stamp, sampleTypeId: SERUM_SAMPLE_TYPE_ID },
  );
  expect(
    result.error,
    "critical-range test setup must succeed",
  ).toBeUndefined();
  return { testId: result.testId as string, name };
}

// The unified-route flag lives in the result configuration domain, the
// e-signature flag in site identity; each is edited on its own admin menu.
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

test.describe("OGC-1121 critical results look critical", () => {
  let unifiedWasOn = false;
  let esigWasOn = false;

  test.beforeEach(async ({ page }) => {
    // The legacy entry screen is only reachable with the unified route off,
    // and a signing ceremony is another spec's concern.
    unifiedWasOn = await isSettingOn(page, UNIFIED_ROUTE_SETTING);
    if (unifiedWasOn) {
      await setSetting(page, UNIFIED_ROUTE_SETTING, false);
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
    if (unifiedWasOn) {
      await setSetting(page, UNIFIED_ROUTE_SETTING, true);
    }
  });

  test("a critical value is marked at entry and in the validation queue, an abnormal one is not", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const critical = await createTestWithCriticalRange(page);
    const criticalAccession = await orderTest(page, critical.testId);
    const abnormalAccession = await orderTest(page, critical.testId);
    expect(
      criticalAccession && abnormalAccession,
      "orders must exist",
    ).toBeTruthy();

    const enterAndSave = async (accession: string, value: string) => {
      await page.goto(
        `/result?type=order&accessionNumber=${encodeURIComponent(accession)}`,
        { waitUntil: "domcontentloaded" },
      );
      const row = page
        .getByRole("main")
        .getByRole("row")
        .filter({ hasText: critical.name })
        .first();
      await expect(row).toBeVisible({ timeout: NAV_TIMEOUT });
      const input = row.locator('input[id^="ResultValue"]');
      await input.fill(value);
      await page
        .getByRole("main")
        .getByRole("button", { name: /^save$/i })
        .first()
        .click();
      await expect(page.getByText(/saved/i).first()).toBeAttached({
        timeout: UI_TIMEOUT,
      });
    };

    await test.step("Enter 200 (critical) and 120 (abnormal) on the legacy screen", async () => {
      await enterAndSave(criticalAccession, "200");
      await enterAndSave(abnormalAccession, "120");
    });

    await test.step("Entry: the critical row carries a Critical tag and a red tint, the abnormal row stays yellow", async () => {
      for (const [accession, expectation] of [
        [criticalAccession, "critical"],
        [abnormalAccession, "abnormal"],
      ] as const) {
        await page.goto(
          `/result?type=order&accessionNumber=${encodeURIComponent(accession)}`,
          { waitUntil: "domcontentloaded" },
        );
        const row = page
          .getByRole("main")
          .getByRole("row")
          .filter({ hasText: critical.name })
          .first();
        await expect(row).toBeVisible({ timeout: NAV_TIMEOUT });
        const input = row.locator('input[id^="ResultValue"]');
        await expect(input).toHaveValue(
          expectation === "critical" ? "200" : "120",
        );
        if (expectation === "critical") {
          await expect(
            row.locator('[data-testid^="critical-flag-"]'),
          ).toBeVisible({
            timeout: UI_TIMEOUT,
          });
          await expect(row).toContainText("Critical");
          await expect(input).toHaveCSS(
            "background-color",
            "rgb(255, 215, 217)",
          );
        } else {
          await expect(
            row.locator('[data-testid^="critical-flag-"]'),
          ).toHaveCount(0);
          await expect(input).toHaveCSS(
            "background-color",
            "rgb(255, 255, 160)",
          );
        }
      }
    });

    await test.step("Validation: the critical row is flagged in its own row, the abnormal row is marked abnormal", async () => {
      for (const [accession, flag] of [
        [criticalAccession, "CRITICAL"],
        [abnormalAccession, "ABNORMAL"],
      ] as const) {
        await page.goto("/validation?type=order", {
          waitUntil: "domcontentloaded",
        });
        const main = page.getByRole("main");
        const searchInput = main.getByPlaceholder(/accession|lab no/i);
        await expect(searchInput).toBeVisible({ timeout: NAV_TIMEOUT });
        await searchInput.fill(accession);
        await main.getByRole("button", { name: /search/i }).click();

        const cell = main
          .locator('[data-testid^="validation-result-"]')
          .first();
        await expect(cell).toBeVisible({ timeout: NAV_TIMEOUT });
        await expect(cell.locator(`[data-testid="flag-${flag}"]`)).toBeVisible({
          timeout: UI_TIMEOUT,
        });
        if (flag === "CRITICAL") {
          await expect(
            main.locator('[data-testid^="check-before-release-"]').first(),
          ).toContainText("Critical");
        } else {
          await expect(
            cell.locator('[data-testid="flag-CRITICAL"]'),
          ).toHaveCount(0);
        }
      }
    });
  });
});
