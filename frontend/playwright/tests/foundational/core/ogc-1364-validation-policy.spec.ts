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

test.describe("OGC-1364 validation policy", () => {
  test.describe.configure({ timeout: 300_000 });
  let flagBefore = "false";
  let policyBefore = "";

  test.beforeEach(async ({ page }) => {
    flagBefore = await flagValue(page);
    await setFlag(page, true);
    await openResultConfiguration(page);
    policyBefore = await checkedPolicy(page);
  });

  test.afterEach(async ({ page }) => {
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
});
