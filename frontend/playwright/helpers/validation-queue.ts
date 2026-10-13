import { expect, type Page } from "@playwright/test";
import { LONG_TIMEOUT, NAV_TIMEOUT, UI_TIMEOUT } from "./timeouts";

const password = process.env.TEST_PASS || "adminADMIN!";

const queueRead = (page: Page) =>
  page.waitForResponse(
    (response) =>
      response.url().includes("/rest/AccessionValidation?") &&
      response.request().method() === "GET",
    { timeout: LONG_TIMEOUT },
  );

/** The Validation queue for one order, as a validator opens it. */
export async function openValidationQueue(page: Page, accession: string) {
  await page.goto("/validation?type=order", { waitUntil: "domcontentloaded" });
  const main = page.getByRole("main");
  const search = main.locator("#validationSearch");
  await expect(search).toBeVisible({ timeout: NAV_TIMEOUT });
  await search.fill(accession);
  const loaded = queueRead(page);
  await main.getByTestId("validation-load").click();
  await loaded;
  await expect(page.getByTestId("release-all-clear")).toBeVisible({
    timeout: LONG_TIMEOUT,
  });
}

/** Completes the e-signature ceremony when the site has it switched on. */
async function signIfAsked(page: Page) {
  const dialog = page.getByRole("dialog");
  const passwordInput = dialog.locator('input[type="password"]');
  if (!(await passwordInput.isVisible())) {
    return;
  }
  const acknowledgement = dialog.locator(
    'label[for="certification-acknowledgement"]',
  );
  if (await acknowledgement.isVisible()) {
    await acknowledgement.click();
  }
  await passwordInput.fill(password);
  await dialog.getByRole("button", { name: /^sign/i }).click();
  await expect(dialog).toBeHidden({ timeout: LONG_TIMEOUT });
}

/** Releases every result in the Clear lane in one action. */
export async function releaseAllClear(page: Page) {
  await page.getByTestId("release-all-clear").click();
  const modal = page.getByTestId("release-all-clear-modal");
  await expect(modal).toBeVisible({ timeout: UI_TIMEOUT });
  const reread = queueRead(page);
  await modal.getByTestId("release-all-clear-sign").getByRole("button").click();
  await signIfAsked(page);
  await reread;
}

/** Opens the review panel of the first result still waiting. */
export async function openFirstWaiting(page: Page) {
  await page
    .getByRole("main")
    .getByRole("button", { name: /expand row/i })
    .first()
    .click();
  await expect(
    page.getByRole("button", { name: "Validate & release" }).first(),
  ).toBeVisible({ timeout: UI_TIMEOUT });
}

/** Releases the result whose review panel is open. */
export async function releaseOpen(page: Page) {
  const reread = queueRead(page);
  await page
    .getByRole("button", { name: "Validate & release" })
    .first()
    .click();
  await signIfAsked(page);
  await reread;
}

/** Opens the first result still waiting and releases it from its review panel. */
export async function validateAndReleaseFirst(page: Page) {
  await openFirstWaiting(page);
  await releaseOpen(page);
}
