import { expect, test, Page, Locator } from "../../../helpers/test-base";
import { UI_TIMEOUT, LONG_TIMEOUT } from "../../../helpers/timeouts";

const BILLING_ADDRESS =
  "https://united-nations-development-programme.odoo.com/odoo/accounting";

async function getAdminNav(page: Page): Promise<Locator> {
  const adminNav = page.locator(".adminSideNav");
  await expect(adminNav).toBeVisible({ timeout: LONG_TIMEOUT });
  return adminNav;
}

async function openBillingMenuConfiguration(page: Page) {
  await page.goto("/MasterListsPage", { waitUntil: "domcontentloaded" });

  const adminNav = await getAdminNav(page);
  const menuConfig = adminNav.getByRole("button", {
    name: /Menu Configuration/i,
  });
  await expect(menuConfig).toBeVisible({ timeout: UI_TIMEOUT });
  if ((await menuConfig.getAttribute("aria-expanded")) !== "true") {
    await menuConfig.click();
  }

  await adminNav
    .getByRole("link", { name: /Billing Menu Configuration/i })
    .click();
  await expect(page).toHaveURL(/billingMenuManagement/, {
    timeout: LONG_TIMEOUT,
  });
  await expect(
    page.getByRole("heading", { name: /Billing Menu Management/i }),
  ).toBeVisible({ timeout: UI_TIMEOUT });
}

async function setBillingMenuState(page: Page, active: boolean) {
  await openBillingMenuConfiguration(page);

  await page.locator("#billing_address").fill(BILLING_ADDRESS);
  const activeCheckbox = page.locator("#billing_active");
  const currentlyActive = await activeCheckbox.isChecked();
  if (currentlyActive !== active) {
    await page.getByText("Billing Menu Active", { exact: true }).click();
  }
  await expect(activeCheckbox).toBeChecked({ checked: active });

  const saveResponse = page.waitForResponse(
    (response) =>
      response.url().includes("/rest/menu/menu_billing") &&
      response.request().method() === "POST",
  );
  await page.getByRole("button", { name: /Submit/i }).click();
  const response = await saveResponse;
  expect(response.ok()).toBeTruthy();
  const body = await response.json();
  expect(body.menu.isActive).toBe(active);
  await expect(page.locator("#billing_active")).toBeChecked({
    checked: active,
  });
}

async function expectBillingMenuVisibility(page: Page, visible: boolean) {
  await page.evaluate(() => {
    localStorage.setItem("mainSideNavMode", "lock");
  });

  const adminNav = await getAdminNav(page);
  await adminNav.getByRole("link", { name: /Back to main menu/i }).click();
  await expect(page).toHaveURL(/\/Dashboard$/, { timeout: LONG_TIMEOUT });

  const mainNav = page
    .locator(".cds--side-nav")
    .filter({ hasNotText: "Back to main menu" });
  await expect(mainNav).toBeVisible({ timeout: UI_TIMEOUT });

  const billingMenu = mainNav.locator("#menu_billing");
  if (visible) {
    await expect(billingMenu).toBeVisible({ timeout: UI_TIMEOUT });
  } else {
    await expect(billingMenu).toHaveCount(0, { timeout: UI_TIMEOUT });
  }
}

test.describe("Billing menu configuration", () => {
  test.describe.configure({ mode: "serial" });
  test.setTimeout(90_000);

  test("updates Billing menu visibility in the dynamic main menu", async ({
    page,
  }) => {
    await openBillingMenuConfiguration(page);
    const initiallyActive = await page.locator("#billing_active").isChecked();
    const initialAddress = await page.locator("#billing_address").inputValue();
    let shouldRestore = false;

    try {
      await setBillingMenuState(page, false);
      shouldRestore = true;
      await page.reload({ waitUntil: "domcontentloaded" });
      await expectBillingMenuVisibility(page, false);

      await setBillingMenuState(page, true);
      await page.reload({ waitUntil: "domcontentloaded" });
      await expectBillingMenuVisibility(page, true);
    } finally {
      if (shouldRestore) {
        await openBillingMenuConfiguration(page);
        await page
          .locator("#billing_address")
          .fill(initialAddress || BILLING_ADDRESS);
        const activeCheckbox = page.locator("#billing_active");
        const currentlyActive = await activeCheckbox.isChecked();
        if (currentlyActive !== initiallyActive) {
          await page.getByText("Billing Menu Active", { exact: true }).click();
        }
        const restoreResponse = page.waitForResponse(
          (response) =>
            response.url().includes("/rest/menu/menu_billing") &&
            response.request().method() === "POST",
        );
        await page.getByRole("button", { name: /Submit/i }).click();
        expect((await restoreResponse).ok()).toBeTruthy();
      }
    }
  });
});
