import { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import { SiteInformationPage } from "../../../fixtures/esig-admin";
import { createEsigSampleOrder } from "../../../helpers/seed-esig-data";
import {
  LONG_TIMEOUT,
  NAV_TIMEOUT,
  UI_TIMEOUT,
} from "../../../helpers/timeouts";

/**
 * OGC-1017: a result released at entry without a validator is recorded with
 * who saved it and the rule that released it, and the auto-validated list shows
 * both. A policy saved from a page that loaded an older version is refused,
 * naming who changed it, and nothing is written. Results use the seeded WBC
 * test (reference range 4 - 10).
 */

const FLAG = "Use the validation policy";
const API = "/api/OpenELIS-Global";
const password = process.env.TEST_PASS || "adminADMIN!";

type LabDefault = {
  trigger: string;
  levels: number;
  levelRoles: { roleId: number }[];
  lastupdated?: string;
};

const csrfHeaders = async (page: Page) => ({
  "X-CSRF-Token": await page.evaluate(() => localStorage.getItem("CSRF") || ""),
});

const labDefault = async (page: Page): Promise<LabDefault> =>
  (await page.request.get(`${API}/rest/validation-policy/default`)).json();

const putLabDefault = async (page: Page, policy: LabDefault) =>
  page.request.put(`${API}/rest/validation-policy/default`, {
    headers: await csrfHeaders(page),
    data: {
      trigger: policy.trigger,
      levels: policy.levels,
      roleIds: policy.levelRoles.map((level) => level.roleId),
      lastupdated: policy.lastupdated,
    },
  });

const openResultConfiguration = async (page: Page) => {
  const menu = new SiteInformationPage(page, "ResultConfigurationMenu");
  await menu.goto();
  return menu;
};

const searchSettings = async (page: Page, text: string) => {
  const search = page.locator("#config-menu-search");
  if (await search.isVisible()) {
    await search.fill(text);
  }
};

const flagValue = async (page: Page) => {
  const menu = await openResultConfiguration(page);
  await searchSettings(page, "useMultiLevelValidation");
  return (await menu.getSettingValue(FLAG)).trim();
};

const setFlag = async (page: Page, on: boolean) => {
  const menu = await openResultConfiguration(page);
  await searchSettings(page, "useMultiLevelValidation");
  await menu.setBooleanSetting(FLAG, on);
};

const esigEnabled = async (page: Page) => {
  const response = await page.request.get(`${API}/rest/esig/enabled`);
  if (!response.ok()) {
    return true;
  }
  const body = (await response.json()) as { enabled?: boolean };
  return body.enabled === true;
};

const signIfAsked = async (page: Page) => {
  if (!(await esigEnabled(page))) {
    return;
  }
  const dialog = page.getByRole("dialog");
  const passwordInput = dialog.locator('input[type="password"]');
  await expect(passwordInput).toBeVisible({ timeout: UI_TIMEOUT });
  const acknowledgement = dialog.locator(
    'label[for="certification-acknowledgement"]',
  );
  if (await acknowledgement.isVisible()) {
    await acknowledgement.click();
    await passwordInput.fill(password);
    await dialog.getByRole("button", { name: /certify|continue/i }).click();
    await expect(acknowledgement).toBeHidden({ timeout: UI_TIMEOUT });
    await expect(passwordInput).toBeVisible({ timeout: UI_TIMEOUT });
  }
  await passwordInput.fill(password);
  await dialog.getByRole("button", { name: /^sign/i }).click();
  await expect(dialog).toBeHidden({ timeout: LONG_TIMEOUT });
};

const enterResult = async (page: Page, accession: string, value: string) => {
  await page.goto(`/Results?accessionNumber=${encodeURIComponent(accession)}`, {
    waitUntil: "domcontentloaded",
  });
  const main = page.getByRole("main");
  await expect(main.locator("tr", { hasText: accession }).first()).toBeVisible({
    timeout: NAV_TIMEOUT,
  });
  const input = main.locator('input[id^="unifiedResultValue-"]').first();
  await expect(input).toBeVisible({ timeout: UI_TIMEOUT });
  await input.fill(value);
  await main
    .getByRole("button", { name: /^save$/i })
    .first()
    .click();
  await signIfAsked(page);
  await expect(
    main.getByRole("button", { name: /^edit$/i }).first(),
  ).toBeVisible({ timeout: LONG_TIMEOUT });
};

const openQueueFor = async (page: Page, accession: string) => {
  await page.goto("/validation?type=order", { waitUntil: "domcontentloaded" });
  const main = page.getByRole("main");
  const search = main.locator("#validationSearch");
  await expect(search).toBeVisible({ timeout: NAV_TIMEOUT });
  await search.fill(accession);
  const loaded = page.waitForResponse(
    (response) =>
      response.url().includes("/rest/AccessionValidation") &&
      response.request().method() === "GET",
    { timeout: LONG_TIMEOUT },
  );
  await main.getByTestId("validation-load").click();
  await loaded;
};

test.describe("OGC-1017 recorded automatic releases and stale policy saves", () => {
  test.describe.configure({ timeout: 300_000 });
  let flagBefore = "false";
  let defaultBefore: LabDefault | undefined;

  test.beforeEach(async ({ page }) => {
    flagBefore = await flagValue(page);
    await setFlag(page, true);
    defaultBefore = await labDefault(page);
  });

  test.afterEach(async ({ page }) => {
    if (defaultBefore) {
      const response = await putLabDefault(page, {
        ...defaultBefore,
        lastupdated: undefined,
      });
      expect(response.ok(), "the lab-wide default is restored").toBeTruthy();
    }
    await setFlag(page, /true/i.test(flagBefore));
  });

  test("a result No review releases shows who saved it and the rule that released it", async ({
    page,
  }) => {
    const current = await labDefault(page);
    const saved = await putLabDefault(page, {
      trigger: "NONE",
      levels: 0,
      levelRoles: [],
      lastupdated: current.lastupdated,
    });
    expect(saved.ok(), "the lab-wide default is No review").toBeTruthy();

    const order = await createEsigSampleOrder(page);
    expect(order, "a sample order is needed").toBeTruthy();
    const accession = order!.accessionNumber;
    await enterResult(page, accession, "15");
    await openQueueFor(page, accession);

    const autoValidated = page.waitForResponse(
      (response) =>
        response.url().includes("/rest/AccessionValidation/auto-validated") &&
        response.request().method() === "GET",
      { timeout: LONG_TIMEOUT },
    );
    await page.locator('label[for="include-auto-validated"]').click();
    await autoValidated;
    const table = page.getByTestId("auto-validated-table");
    await expect(table).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(table.locator("thead")).toContainText("Saved by");
    await expect(table.locator("thead")).toContainText("Released under");
    const row = table.locator("tbody tr").first();
    await expect(row).toContainText("Lab-wide policy: No review");
    await expect(row.locator("td").nth(5)).not.toHaveText("");
  });

  test("a policy saved from an outdated page is refused and names who changed it", async ({
    page,
  }) => {
    const current = await labDefault(page);
    const start = await putLabDefault(page, {
      trigger: "ALL",
      levels: 1,
      levelRoles: current.levelRoles.slice(0, 1),
      lastupdated: current.lastupdated,
    });
    expect(start.ok(), "the lab-wide default starts at All results").toBe(true);

    await openResultConfiguration(page);
    const section = page.getByTestId("validation-policy");
    await expect(
      section.getByRole("radio", { name: "All results", exact: true }),
    ).toBeChecked({ timeout: NAV_TIMEOUT });

    const loaded = await labDefault(page);
    const theirs = await putLabDefault(page, {
      trigger: "NEEDS_REVIEW",
      levels: 1,
      levelRoles: loaded.levelRoles,
      lastupdated: loaded.lastupdated,
    });
    expect(theirs.ok(), "someone else saves the policy").toBeTruthy();

    await section.locator("label", { hasText: /^No review$/ }).click();
    await section.getByRole("button", { name: "Save policy" }).click();

    const stale = page.getByTestId("validation-policy-stale");
    await expect(stale).toContainText("This policy was changed by", {
      timeout: UI_TIMEOUT,
    });
    await expect(stale).toContainText("Nothing was saved.");
    expect((await labDefault(page)).trigger, "their policy stays").toBe(
      "NEEDS_REVIEW",
    );

    await stale.getByRole("button", { name: "Refresh" }).click();
    await expect(
      page.getByTestId("validation-policy").getByRole("radio", {
        name: "Only results that need review",
        exact: true,
      }),
    ).toBeChecked({ timeout: NAV_TIMEOUT });
  });
});
