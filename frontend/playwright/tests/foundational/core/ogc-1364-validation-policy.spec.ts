import { Browser, Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import { SiteInformationPage } from "../../../fixtures/esig-admin";
import { createEsigSampleOrder } from "../../../helpers/seed-esig-data";
import {
  LONG_TIMEOUT,
  NAV_TIMEOUT,
  UI_TIMEOUT,
} from "../../../helpers/timeouts";

/**
 * OGC-1364 v1: with "Use the validation policy" on, Result Configuration shows
 * the lab-wide default in place of "validate all results", and that policy
 * decides whether a saved result waits for a validator. Each case orders the
 * seeded WBC test (reference range 4 - 10) and reads the Validation queue for
 * that accession only.
 */

const FLAG = "Use the validation policy";
const password = process.env.TEST_PASS || "adminADMIN!";

const esigEnabled = async (page: Page) => {
  const response = await page.request.get(
    "/api/OpenELIS-Global/rest/esig/enabled",
  );
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

const openResultConfiguration = async (page: Page) => {
  const menu = new SiteInformationPage(page, "ResultConfigurationMenu");
  await menu.goto();
  return menu;
};

const setFlag = async (page: Page, on: boolean) => {
  const menu = await openResultConfiguration(page);
  await page.locator("#config-menu-search").fill("useMultiLevelValidation");
  await menu.setBooleanSetting(FLAG, on);
};

const flagValue = async (page: Page) => {
  const menu = await openResultConfiguration(page);
  await page.locator("#config-menu-search").fill("useMultiLevelValidation");
  return (await menu.getSettingValue(FLAG)).trim();
};

const choosePolicy = async (page: Page, label: string) => {
  const section = page.getByTestId("validation-policy");
  await expect(section).toBeVisible({ timeout: NAV_TIMEOUT });
  const option = section.getByRole("radio", { name: label, exact: true });
  if (await option.isChecked()) {
    return;
  }
  await section.locator("label", { hasText: new RegExp(`^${label}$`) }).click();
  await section.getByRole("button", { name: "Save policy" }).click();
  await expect(section.getByText("Validation policy saved.")).toBeVisible({
    timeout: UI_TIMEOUT,
  });
};

const checkedPolicy = async (page: Page) => {
  const section = page.getByTestId("validation-policy");
  await expect(section).toBeVisible({ timeout: NAV_TIMEOUT });
  for (const label of [
    "All results",
    "Only results that need review",
    "No review",
  ]) {
    if (
      await section.getByRole("radio", { name: label, exact: true }).isChecked()
    ) {
      return label;
    }
  }
  throw new Error("no review trigger is selected");
};

const API = "/api/OpenELIS-Global";
const SEEDED_TEST_ID = "13";

interface LabUnitOverride {
  id: number;
  testSectionId: string;
  labUnitName: string;
  trigger: string;
  levels: number;
  active: boolean;
}

const seededTestLabUnit = async (page: Page) => {
  const response = await page.request.get(
    `${API}/rest/test-catalog/tests/${SEEDED_TEST_ID}/basic-info`,
  );
  expect(response.ok(), "the seeded test's basic info").toBeTruthy();
  const body = (await response.json()) as { labUnitId?: string };
  expect(body.labUnitId, "the seeded test has a lab unit").toBeTruthy();
  return String(body.labUnitId);
};

const overrideFor = async (page: Page, labUnitId: string) => {
  const response = await page.request.get(
    `${API}/rest/validation-policy/lab-units`,
  );
  expect(response.ok(), "the lab unit overrides").toBeTruthy();
  const body = (await response.json()) as { overrides: LabUnitOverride[] };
  return body.overrides.find(
    (override) => override.testSectionId === labUnitId,
  );
};

const csrfHeaders = async (page: Page) => ({
  "X-CSRF-Token": await page.evaluate(() => localStorage.getItem("CSRF") || ""),
});

const setOverrideActive = async (page: Page, id: number, active: boolean) => {
  const response = await page.request.put(
    `${API}/rest/validation-policy/lab-units/${id}/active`,
    { headers: await csrfHeaders(page), data: { active } },
  );
  expect(response.ok(), "the override's state is set").toBeTruthy();
};

/**
 * Puts the seeded unit's override back as it was before the test: its rule and
 * whether it was active, or deactivated when the test added it.
 */
const restoreOverride = async (
  page: Page,
  labUnitId: string,
  before: LabUnitOverride | undefined,
) => {
  if (!labUnitId) {
    return;
  }
  const now = await overrideFor(page, labUnitId);
  if (!now) {
    return;
  }
  if (before) {
    if (!now.active) {
      await setOverrideActive(page, now.id, true);
    }
    const response = await page.request.put(
      `${API}/rest/validation-policy/lab-units/${now.id}`,
      {
        headers: await csrfHeaders(page),
        data: { trigger: before.trigger, levels: before.levels },
      },
    );
    expect(response.ok(), "the override's rule is restored").toBeTruthy();
  }
  const wanted = before ? before.active : false;
  if (!wanted) {
    await setOverrideActive(page, now.id, false);
  }
};

const overrideToNoReview = async (page: Page, labUnitId: string) => {
  await openResultConfiguration(page);
  const overrides = page.getByTestId("validation-overrides");
  await expect(overrides).toBeVisible({ timeout: NAV_TIMEOUT });
  const existing = await overrideFor(page, labUnitId);
  if (existing && !existing.active) {
    await overrides
      .locator('label[for="validation-overrides-show-deactivated"]')
      .click();
    await overrides
      .getByTestId(`validation-override-reactivate-${labUnitId}`)
      .click();
    await expect(overrides.getByText(/reactivated\.$/)).toBeVisible({
      timeout: UI_TIMEOUT,
    });
  }
  if (existing) {
    await overrides
      .getByTestId(`validation-override-edit-${labUnitId}`)
      .click();
  } else {
    await overrides
      .getByRole("button", { name: "Add lab unit override" })
      .click();
  }
  const dialog = page.getByRole("dialog");
  await expect(dialog).toBeVisible({ timeout: UI_TIMEOUT });
  if (!existing) {
    await dialog.getByLabel("Lab unit").selectOption(labUnitId);
  }
  await dialog.locator("label", { hasText: /^No review$/ }).click();
  await dialog.getByRole("button", { name: "Save override" }).click();
  await expect(dialog).toBeHidden({ timeout: UI_TIMEOUT });
  await expect(
    overrides.getByTestId(`validation-override-${labUnitId}`),
  ).toContainText("No review", { timeout: UI_TIMEOUT });
};

type LabDefault = {
  trigger: string;
  levels: number;
  levelRoles: { roleId: number }[];
};

const labDefault = async (page: Page): Promise<LabDefault> =>
  (await page.request.get(`${API}/rest/validation-policy/default`)).json();

const restoreLabDefault = async (page: Page, policy: LabDefault) => {
  const response = await page.request.put(
    `${API}/rest/validation-policy/default`,
    {
      headers: await csrfHeaders(page),
      data: {
        trigger: policy.trigger,
        levels: policy.levels,
        roleIds: policy.levelRoles.map((level) => level.roleId),
      },
    },
  );
  expect(response.ok(), "the lab-wide default is restored").toBeTruthy();
};

const letters = (count: number) =>
  Array.from({ length: count }, () =>
    String.fromCharCode(97 + Math.floor(Math.random() * 26)),
  ).join("");

/** A disposable validator with Validation and Results rights for every unit. */
const createValidator = async (page: Page) => {
  const endpoint = `${API}/rest/UnifiedSystemUser`;
  const form = await (await page.request.get(endpoint)).json();
  const roleId = (name: string) =>
    form.labUnitRoles.find(
      (role: { roleName: string }) => role.roleName === name,
    )?.roleId;
  const username = `pwreview${letters(8)}`;
  const created = await page.request.post(endpoint, {
    headers: await csrfHeaders(page),
    data: {
      userLoginName: username,
      userPassword: password,
      confirmPassword: password,
      userFirstName: "Second",
      userLastName: username,
      expirationDate: form.expirationDate,
      timeout: form.timeout,
      accountActive: "Y",
      accountDisabled: "N",
      accountLocked: "N",
      allowCopyUserRoles: "N",
      selectedRoles: [],
      selectedTestSectionLabUnits: {
        AllLabUnits: [roleId("Results"), roleId("Validation")],
      },
    },
  });
  expect(await created.json()).toEqual({
    forward: "redirect:/UnifiedSystemUser",
  });
  return username;
};

const signInAs = async (
  browser: Browser,
  baseURL: string,
  username: string,
) => {
  const context = await browser.newContext({
    ignoreHTTPSErrors: true,
    baseURL,
    storageState: { cookies: [], origins: [] },
  });
  const page = await context.newPage();
  await page.goto("/login");
  await page.locator("#loginName").fill(username);
  await page.locator("#password").fill(password);
  await page.locator('[data-cy="loginButton"]').click();
  await expect(page).toHaveURL(/\/$/, { timeout: NAV_TIMEOUT });
  return { context, page };
};

test.describe("OGC-1364 validation policy", () => {
  test.describe.configure({ timeout: 300_000 });
  let flagBefore = "false";
  let policyBefore = "";
  let defaultBefore: LabDefault | undefined;
  let labUnitId = "";
  let overrideBefore: LabUnitOverride | undefined;

  test.beforeEach(async ({ page }) => {
    flagBefore = await flagValue(page);
    await setFlag(page, true);
    await openResultConfiguration(page);
    policyBefore = await checkedPolicy(page);
    defaultBefore = await labDefault(page);
    labUnitId = await seededTestLabUnit(page);
    overrideBefore = await overrideFor(page, labUnitId);
    if (overrideBefore?.active) {
      await setOverrideActive(page, overrideBefore.id, false);
    }
  });

  test.afterEach(async ({ page }) => {
    await restoreOverride(page, labUnitId, overrideBefore);
    if (defaultBefore) {
      await restoreLabDefault(page, defaultBefore);
    }
    await openResultConfiguration(page);
    if (policyBefore) {
      await choosePolicy(page, policyBefore);
    }
    await setFlag(page, /true/i.test(flagBefore));
  });

  test("the lab-wide default replaces validate all results and decides what waits", async ({
    page,
  }) => {
    await test.step("The policy is shown and validate all results is not", async () => {
      await openResultConfiguration(page);
      await expect(page.getByTestId("validation-policy")).toBeVisible({
        timeout: NAV_TIMEOUT,
      });
      await page.locator("#config-menu-search").fill("validate all results");
      const names = page.locator("tbody tr td:nth-child(2)");
      await expect(names.filter({ hasText: FLAG })).toHaveCount(1, {
        timeout: UI_TIMEOUT,
      });
      await expect(
        names.filter({ hasText: /^validate all results$/ }),
      ).toHaveCount(0);
      await page.locator("#config-menu-search").fill("");
    });

    await test.step("No review takes the reviews to 0 and is kept", async () => {
      await choosePolicy(page, "No review");
      const section = page.getByTestId("validation-policy");
      await expect(section.getByLabel("Number of reviews")).toHaveValue("0");
      await expect(
        section.getByTestId("validation-policy-summary"),
      ).toContainText("Results that need review: released at entry");
      await openResultConfiguration(page);
      await expect(
        page
          .getByTestId("validation-policy")
          .getByRole("radio", { name: "No review", exact: true }),
      ).toBeChecked({ timeout: UI_TIMEOUT });
    });

    await test.step("Under No review an abnormal result is released at entry", async () => {
      const order = await createEsigSampleOrder(page);
      expect(order, "a sample order is needed").toBeTruthy();
      await enterResult(page, order!.accessionNumber, "15");
      await openQueueFor(page, order!.accessionNumber);
      await expect(
        page.getByTestId("release-all-clear-why-queueEmpty"),
      ).toContainText("Nothing is waiting for validation.", {
        timeout: LONG_TIMEOUT,
      });
    });

    await test.step("Under All results an in-range result waits for a validator", async () => {
      await openResultConfiguration(page);
      await choosePolicy(page, "All results");
      const order = await createEsigSampleOrder(page);
      expect(order, "a sample order is needed").toBeTruthy();
      await enterResult(page, order!.accessionNumber, "6");
      await openQueueFor(page, order!.accessionNumber);
      await expect(page.getByTestId("validation-lane-summary")).toContainText(
        "Clear: 1",
        { timeout: LONG_TIMEOUT },
      );
    });
  });

  test("a lab unit override decides for its unit, and deactivating it hands the unit back", async ({
    page,
  }) => {
    await test.step("The lab-wide default is All results", async () => {
      await openResultConfiguration(page);
      await choosePolicy(page, "All results");
    });

    await test.step("The seeded test's unit is overridden to No review", async () => {
      await overrideToNoReview(page, labUnitId);
      await expect(
        page.getByTestId(`validation-effective-${labUnitId}`),
      ).toContainText("released at entry");
    });

    await test.step("Under the override an abnormal result is released at entry", async () => {
      const order = await createEsigSampleOrder(page);
      expect(order, "a sample order is needed").toBeTruthy();
      await enterResult(page, order!.accessionNumber, "15");
      await openQueueFor(page, order!.accessionNumber);
      await expect(
        page.getByTestId("release-all-clear-why-queueEmpty"),
      ).toContainText("Nothing is waiting for validation.", {
        timeout: LONG_TIMEOUT,
      });
    });

    await test.step("Deactivated, the unit follows All results again", async () => {
      await openResultConfiguration(page);
      const overrides = page.getByTestId("validation-overrides");
      await overrides
        .getByTestId(`validation-override-deactivate-${labUnitId}`)
        .click();
      await expect(overrides.getByText(/deactivated\./)).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      await expect(
        page.getByTestId(`validation-effective-${labUnitId}`),
      ).toContainText("Lab-wide default");
      const order = await createEsigSampleOrder(page);
      expect(order, "a sample order is needed").toBeTruthy();
      await enterResult(page, order!.accessionNumber, "6");
      await openQueueFor(page, order!.accessionNumber);
      await expect(page.getByTestId("validation-lane-summary")).toContainText(
        "Clear: 1",
        { timeout: LONG_TIMEOUT },
      );
    });
  });

  test("with two reviews the first signature keeps the result in review and another validator releases it", async ({
    page,
    browser,
    baseURL,
  }) => {
    let accession = "";

    await test.step("The lab-wide default asks for two reviews", async () => {
      await restoreLabDefault(page, {
        trigger: "ALL",
        levels: 1,
        levelRoles: [],
      });
      await openResultConfiguration(page);
      const section = page.getByTestId("validation-policy");
      await expect(section).toBeVisible({ timeout: NAV_TIMEOUT });
      await section.locator("label", { hasText: /^All results$/ }).click();
      await section.getByLabel("Number of reviews").fill("2");
      await expect(section.getByLabel("Level 2 role")).toHaveValue(/\d+/);
      await expect(
        section.getByTestId("validation-policy-different-people"),
      ).toBeVisible();
      await section.getByRole("button", { name: "Save policy" }).click();
      await expect(section.getByText("Validation policy saved.")).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      await expect(
        section.getByTestId("validation-policy-summary"),
      ).toContainText("reviewed by Validation → Validation (2 reviews)");
    });

    await test.step("Level 1 is signed and the result stays in review", async () => {
      const order = await createEsigSampleOrder(page);
      expect(order, "a sample order is needed").toBeTruthy();
      accession = order!.accessionNumber;
      await enterResult(page, accession, "6");
      await openQueueFor(page, accession);
      const main = page.getByRole("main");
      await expect(main.getByText("Level 1 of 2")).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
      await main.locator('[data-testid^="review-row-"]').first().click();
      await main.getByRole("button", { name: "Sign level 1 of 2" }).click();
      await signIfAsked(page);
      await expect(main.getByText("Level 2 of 2")).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
      await main.locator('[data-testid^="review-row-"]').first().click();
      await expect(main.getByTestId("review-level-blocked")).toContainText(
        "You signed a level of this result.",
      );
      await expect(
        main.getByRole("button", { name: "Validate & release" }),
      ).toBeDisabled();
      await expect(main.getByTestId("review-sign-off-1")).toContainText(
        "Level 1 of 2",
      );
    });

    await test.step("Another validator signs level 2, which releases it", async () => {
      const username = await createValidator(page);
      const second = await signInAs(browser, baseURL!, username);
      try {
        await openQueueFor(second.page, accession);
        const main = second.page.getByRole("main");
        await expect(main.getByText("Level 2 of 2")).toBeVisible({
          timeout: LONG_TIMEOUT,
        });
        await main.getByRole("button", { name: "Expand Row" }).first().click();
        await main.getByRole("button", { name: "Validate & release" }).click();
        await signIfAsked(second.page);
        await openQueueFor(second.page, accession);
        await expect(
          second.page.getByTestId("release-all-clear-why-queueEmpty"),
        ).toContainText("Nothing is waiting for validation.", {
          timeout: LONG_TIMEOUT,
        });
      } finally {
        await second.context.close();
      }
    });
  });
});
