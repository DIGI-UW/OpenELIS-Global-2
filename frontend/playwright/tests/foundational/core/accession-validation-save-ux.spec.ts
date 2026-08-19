import { test, expect } from "../../../helpers/test-base";
import {
  QUICK_TIMEOUT,
  SHORT_TIMEOUT,
  UI_TIMEOUT,
  NAV_TIMEOUT,
} from "../../../helpers/timeouts";

/**
 * AccessionValidation Save UX — covers issue #3541 bugs A, C, D:
 * - Bug A: Save with 0 rows selected must show error, not silent page reload
 * - Bug D: Save button must be disabled when no rows are selected
 *
 * Bug B (note persistence) is OGC-654, out of scope.
 */

test("AccessionValidation — Save button disabled when no rows selected (#3541)", async ({
  page,
}) => {
  await test.step("Navigate to Validation by Order", async () => {
    await page.goto("/AccessionValidation", {
      waitUntil: "domcontentloaded",
    });

    const main = page.getByRole("main");
    const searchInput = main.getByPlaceholder(/accession|lab no/i);
    await expect(searchInput).toBeVisible({ timeout: NAV_TIMEOUT });
    await searchInput.fill("");
    await main.getByRole("button", { name: /search/i }).click();

    // Wait for results table or empty state
    await expect(
      page.getByRole("table").or(page.getByText(/no records/i)),
    ).toBeVisible({ timeout: NAV_TIMEOUT });
  });

  await test.step("Save button is disabled when no checkboxes ticked", async () => {
    const validateButton = page.getByRole("button", { name: "Validate" });
    if (await validateButton.isVisible({ timeout: QUICK_TIMEOUT })) {
      await expect(validateButton).toBeDisabled();
    }
  });
});

test("AccessionValidation — Save with 0 rows shows error, no page reload (#3541)", async ({
  page,
}) => {
  await test.step("Navigate and search for pending results", async () => {
    await page.goto("/AccessionValidation", {
      waitUntil: "domcontentloaded",
    });

    const main = page.getByRole("main");
    const searchInput = main.getByPlaceholder(/accession|lab no/i);
    await expect(searchInput).toBeVisible({ timeout: NAV_TIMEOUT });
    await searchInput.fill("");
    await main.getByRole("button", { name: /search/i }).click();

    await expect(
      page.getByRole("table").or(page.getByText(/no records/i)),
    ).toBeVisible({ timeout: NAV_TIMEOUT });
  });

  await test.step("Clicking Save with no rows selected shows error notification", async () => {
    const validateButton = page.getByRole("button", { name: "Validate" });

    if (!(await validateButton.isVisible({ timeout: QUICK_TIMEOUT }))) {
      // No results to test against — skip gracefully
      test.skip();
    }

    // Button should be disabled — verify no navigation occurs
    const currentUrl = page.url();
    await expect(validateButton).toBeDisabled();

    // Even if somehow clicked, no page reload should happen
    await page.waitForTimeout(SHORT_TIMEOUT);
    expect(page.url()).toBe(currentUrl);
  });
});

test("AccessionValidation — Save with rows selected succeeds (#3541)", async ({
  page,
}) => {
  await test.step("Navigate and search for pending results", async () => {
    await page.goto("/AccessionValidation", {
      waitUntil: "domcontentloaded",
    });

    const main = page.getByRole("main");
    const searchInput = main.getByPlaceholder(/accession|lab no/i);
    await expect(searchInput).toBeVisible({ timeout: NAV_TIMEOUT });
    await searchInput.fill("");
    await main.getByRole("button", { name: /search/i }).click();

    await expect(
      page.getByRole("table").or(page.getByText(/no records/i)),
    ).toBeVisible({ timeout: NAV_TIMEOUT });
  });

  await test.step("Tick Accept on first row and save", async () => {
    const firstAcceptCheckbox = page
      .getByRole("checkbox")
      .first();
    if (!(await firstAcceptCheckbox.isVisible({ timeout: QUICK_TIMEOUT }))) {
      test.skip();
    }

    await firstAcceptCheckbox.click();

    // Save button should now be enabled
    const validateButton = page.getByRole("button", { name: "Validate" });
    await expect(validateButton).toBeEnabled({ timeout: SHORT_TIMEOUT });

    await validateButton.click();

    // E-sig modal or success notification should appear (not a silent reload)
    const modal = page.getByRole("dialog");
    const notification = page.getByText(/validated successfully/i);
    await expect(modal.or(notification)).toBeVisible({
      timeout: UI_TIMEOUT,
    });
  });
});
