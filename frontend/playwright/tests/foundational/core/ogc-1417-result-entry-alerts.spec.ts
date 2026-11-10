import { test, expect, Page } from "../../../helpers/test-base";
import {
  SettingsMenu,
  SiteInformationPage,
} from "../../../fixtures/esig-admin";
import { createSampleOrder } from "../../../helpers/seed-tat-data";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1417 — on the unified Results page a critical value is acknowledged
 * before it is saved, with the custom message from Result Configuration shown
 * as entered, and a value outside the valid range is questioned on leaving
 * the field when Result Configuration asks for it. The acknowledgement shows
 * in the result's history, and a saved critical result offers the callback
 * log.
 *
 * Normal 20–80, Valid 0–100, Critical < 10 or > 90: the QA setup on the ticket.
 */

const API = "/api/OpenELIS-Global";
const CATALOG = `${API}/rest/test-catalog`;
const SERUM_SAMPLE_TYPE_ID = "2";
const CUSTOM_MESSAGE =
  "OGC-1417 check: call the clinician now and record who you told";

const SETTING_MENU: Record<string, SettingsMenu> = {
  alertWhenInvalidResult: "ResultConfigurationMenu",
  customCriticalMessage: "ResultConfigurationMenu",
  electronicSignatureEnabled: "SiteInformationMenu",
};

async function settingValue(page: Page, setting: string): Promise<string> {
  const menu = new SiteInformationPage(page, SETTING_MENU[setting]);
  await menu.goto();
  return (await menu.getSettingValue(setting)).trim();
}

async function setBoolean(page: Page, setting: string, on: boolean) {
  const menu = new SiteInformationPage(page, SETTING_MENU[setting]);
  await menu.goto();
  await menu.setBooleanSetting(setting, on);
}

async function setText(page: Page, setting: string, value: string) {
  const menu = new SiteInformationPage(page, SETTING_MENU[setting]);
  await menu.goto();
  await menu.setTextSetting(setting, value);
}

async function createUricAcidTest(page: Page): Promise<{
  testId: string;
  name: string;
}> {
  await page.goto("/", { waitUntil: "domcontentloaded" });
  const stamp = Date.now().toString().slice(-8);
  const name = `Uric Acid 1417 ${stamp}`;
  const result = await page.evaluate(
    async ({ catalog, name, stamp, sampleTypeId }) => {
      const headers = {
        "Content-Type": "application/json",
        "X-CSRF-Token": localStorage.getItem("CSRF") || "",
      };
      const call = (url: string, method: string, body?: unknown) =>
        fetch(url, {
          method,
          credentials: "include",
          headers,
          body: body === undefined ? undefined : JSON.stringify(body),
        });
      const labUnits = await (await call(`${catalog}/lab-units`, "GET")).json();
      const created = await call(`${catalog}/tests`, "POST", {
        name,
        reportingName: name,
        code: `UA${stamp}`,
        labUnitId: Array.isArray(labUnits) && labUnits[0]?.id,
        sampleTypeIds: [sampleTypeId],
        domain: "CLINICAL",
        orderable: true,
      });
      if (!created.ok) {
        return { error: `create ${created.status}` };
      }
      const testId = (await created.json()).testId as string;
      const configured = await call(
        `${catalog}/tests/${testId}/sample-results`,
        "PUT",
        {
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
        },
      );
      if (!configured.ok) {
        return { error: `sample-results ${configured.status}` };
      }
      const components = await configured.json();
      const ranges = await call(`${catalog}/tests/${testId}/ranges`, "PUT", {
        testId,
        ranges: [
          {
            componentId: components.components[0].id,
            gender: null,
            minAge: 0,
            maxAge: null,
            lowNormal: 20,
            highNormal: 80,
            lowCritical: 10,
            highCritical: 90,
            lowValid: 0,
            highValid: 100,
          },
        ],
      });
      if (!ranges.ok) {
        return { error: `ranges ${ranges.status}` };
      }
      const activated = await call(
        `${catalog}/tests/${testId}/activate`,
        "POST",
        {},
      );
      return activated.ok
        ? { testId }
        : { error: `activate ${activated.status}` };
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

async function openRow(page: Page, accession: string, testName: string) {
  await page.goto(`/Results?accessionNumber=${encodeURIComponent(accession)}`, {
    waitUntil: "domcontentloaded",
  });
  const row = page
    .getByRole("main")
    .getByRole("row")
    .filter({ hasText: testName })
    .first();
  await expect(row).toBeVisible({ timeout: NAV_TIMEOUT });
  return row;
}

test.describe("OGC-1417 — critical and invalid results on the unified page", () => {
  test.describe.configure({ timeout: 300_000 });
  const before: Record<string, string> = {};

  test.beforeEach(async ({ page }) => {
    for (const setting of Object.keys(SETTING_MENU)) {
      before[setting] = await settingValue(page, setting);
    }
    if (/true/i.test(before.electronicSignatureEnabled)) {
      await setBoolean(page, "electronicSignatureEnabled", false);
    }
    await setBoolean(page, "alertWhenInvalidResult", true);
    await setText(page, "customCriticalMessage", CUSTOM_MESSAGE);
  });

  test.afterEach(async ({ page }) => {
    await setText(page, "customCriticalMessage", before.customCriticalMessage);
    await setBoolean(
      page,
      "alertWhenInvalidResult",
      /true/i.test(before.alertWhenInvalidResult),
    );
    if (/true/i.test(before.electronicSignatureEnabled)) {
      await setBoolean(page, "electronicSignatureEnabled", true);
    }
  });

  test("a critical value is acknowledged with the custom message, recorded in history, and offers the callback", async ({
    page,
  }) => {
    const uricAcid = await createUricAcidTest(page);
    const accession = await orderTest(page, uricAcid.testId);
    const row = await openRow(page, accession, uricAcid.name);
    const input = row.locator('input[id^="unifiedResultValue-"]');
    const modal = page.locator(".cds--modal.is-visible");

    await test.step("Save opens the critical pop-up, nothing is sent", async () => {
      await input.fill("95");
      await row.getByRole("button", { name: /^save$/i }).click();
      await expect(
        page.getByTestId("result-alert-critical-message"),
      ).toHaveText(CUSTOM_MESSAGE, { timeout: UI_TIMEOUT });
      await modal.getByRole("button", { name: "Correct the value" }).click();
      await expect(page.getByTestId("result-alert-modal")).toHaveCount(0);
      await expect(input).toHaveValue("95");
    });

    await test.step("acknowledging saves the value", async () => {
      await row.getByRole("button", { name: /^save$/i }).click();
      await modal.getByRole("button", { name: /Acknowledge and save/ }).click();
      await expect(row.getByRole("button", { name: /^edit$/i })).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      await expect(row.locator('[data-testid="flag-CRITICAL"]')).toBeVisible();
    });

    await test.step("history names the acknowledgement and the callback can be logged", async () => {
      await row.getByRole("button", { name: /expand row/i }).click();
      const main = page.getByRole("main");
      await expect(main.getByTestId("unified-log-callback-button")).toBeVisible(
        { timeout: UI_TIMEOUT },
      );
      const history = main
        .getByTestId("ref-section-history")
        .locator("button.unifiedRefSectionHeader");
      if ((await history.getAttribute("aria-expanded")) !== "true") {
        await history.click();
      }
      await expect(main.getByText("Critical acknowledged")).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      await expect(
        main.getByText(CUSTOM_MESSAGE, { exact: false }),
      ).toBeVisible();
    });
  });

  test("a value outside the valid range is questioned on leaving the field and saved confirmed", async ({
    page,
  }) => {
    const uricAcid = await createUricAcidTest(page);
    const accession = await orderTest(page, uricAcid.testId);
    const row = await openRow(page, accession, uricAcid.name);
    const input = row.locator('input[id^="unifiedResultValue-"]');
    const modal = page.locator(".cds--modal.is-visible");

    await input.fill("150");
    await input.blur();
    await expect(page.getByTestId("result-alert-item")).toContainText(
      "(0 to 100)",
      { timeout: UI_TIMEOUT },
    );
    await modal.getByRole("button", { name: "Keep this value" }).click();
    await row.getByRole("button", { name: /^save$/i }).click();
    // 150 is past the critical bound too: kept as a value, it still owes its
    // critical acknowledgement, and only that is asked at Save
    await expect(page.getByTestId("result-alert-item")).toHaveCount(1, {
      timeout: UI_TIMEOUT,
    });
    await expect(page.getByTestId("result-alert-critical-message")).toHaveText(
      CUSTOM_MESSAGE,
    );
    await modal.getByRole("button", { name: /Acknowledge and save/ }).click();
    await expect(row.getByRole("button", { name: /^edit$/i })).toBeVisible({
      timeout: UI_TIMEOUT,
    });
    await expect(row.locator('[data-testid="flag-INVALID"]')).toBeVisible();
  });
});
