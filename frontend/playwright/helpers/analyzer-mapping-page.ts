import { expect, type Page } from "@playwright/test";

/** Saves the mapping edits on the analyzer's mapping page or inside Verify. */
export async function saveMapping(page: Page) {
  await page.getByRole("button", { name: "Save mapping" }).click();
  await page.getByRole("button", { name: "Save changes" }).click();
  await expect(page.getByText("Mappings saved")).toBeVisible();
}

/** Confirms the saved local mapping decisions. */
export async function confirmMapping(page: Page) {
  const confirm = page.getByRole("button", {
    name: "Confirm mappings",
  });
  await expect(confirm).toBeEnabled();
  await confirm.click();
  await expect(page.getByText("Mappings confirmed")).toBeVisible();
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
  const row = page.getByRole("button").filter({
    has: page.getByText(code, { exact: true }),
  });
  await expect(row).toBeVisible();
  if ((await row.getAttribute("aria-expanded")) === "false") {
    await row.click();
  }
  await page.locator(`label[for="exclude-test-${code}"]`).click();
  await expect(page.locator(`#exclude-test-${code}`)).toBeChecked();
}
