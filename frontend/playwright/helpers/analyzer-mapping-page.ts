import { expect, type Page } from "@playwright/test";

/** Saves the mapping edits on the analyzer's mapping page or inside Verify. */
export async function saveMapping(page: Page) {
  await page.getByRole("button", { name: "Save mapping" }).click();
  await page.getByRole("button", { name: "Save changes" }).click();
  await expect(page.getByText("Mappings saved")).toBeVisible();
}

/** Confirms the saved mapping and control recognition as the ones in force. */
export async function confirmMapping(page: Page) {
  const confirm = page.getByRole("button", {
    name: "Confirm mappings and control recognition",
  });
  await expect(confirm).toBeEnabled();
  await confirm.click();
  await expect(
    page.getByText("Mappings and control recognition confirmed"),
  ).toBeVisible();
}

/** Applies the confirmed mapping to the analyzer, which retries what it holds. */
export async function applyMappings(page: Page) {
  await page
    .getByRole("button", { name: "Apply mappings and retry held results" })
    .click();
  await expect(
    page.getByText(
      "Current mappings applied to this analyzer. Eligible held results were retried.",
    ),
  ).toBeVisible();
}

/** Marks one analyzer code "Do not receive" on the mapping page. */
export async function doNotReceive(page: Page, code: string) {
  await page.locator(`label[for="exclude-test-${code}"]`).click();
  await expect(page.locator(`#exclude-test-${code}`)).toBeChecked();
}
