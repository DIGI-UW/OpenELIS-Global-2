import { test, expect } from "../../../helpers/test-base";
import type { Page, Locator } from "@playwright/test";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

/**
 * Storage Dispose.
 *
 * Precondition: /Storage/sample-items must list at least one non-disposed
 * sample item. If the table is empty or every row is already disposed,
 * the test fails loudly with an actionable message rather than skipping
 * — silent skips mask real data/flow regressions.
 *
 * Selector strategy follows .specify/guides/playwright-best-practices.md:
 *   - Dialog lookups via getByRole
 *   - Carbon Checkbox: click the <label>, never .check() the hidden <input>
 *   - data-testid is retained only where the project already anchors
 *     stable hooks (overflow menu, modal container)
 */

async function pickDisposableSample(
  page: Page,
): Promise<{ row: Locator; sampleItemId: string }> {
  // Auto-retrying wait: `toBeVisible` polls until rows hydrate.
  // `locator.count()` is a one-shot snapshot (non-retrying) and must
  // only run AFTER the DOM stabilizes — otherwise flakes on cold
  // runners where the row XHR arrives after first paint.
  const rows = page.locator("tbody tr");
  await expect(
    rows.first(),
    "Sample Items table should contain at least one row — " +
      "seed sample items before exercising the disposal flow.",
  ).toBeVisible({ timeout: LONG_TIMEOUT });
  const rowCount = await rows.count();

  for (let i = 0; i < rowCount; i += 1) {
    const row = rows.nth(i);
    const rowText = (await row.textContent()) ?? "";
    if (!/Disposed/i.test(rowText)) {
      const sampleItemId = (await row.locator("td").first().innerText()).trim();
      return { row, sampleItemId };
    }
  }

  throw new Error(
    "No non-disposed sample row available — all rows are already " +
      "disposed. Reset or extend fixtures to include at least one " +
      "active sample item.",
  );
}

test.describe("Storage Dispose", () => {
  test("dispose sample item from overflow menu", async ({ page }) => {
    await test.step("load Sample Items listing", async () => {
      await page.goto("/Storage/sample-items", {
        waitUntil: "domcontentloaded",
      });
      await expect(
        page.getByRole("heading", { name: "Sample Items", exact: true }),
      ).toBeVisible();
    });

    const { row: sampleRow, sampleItemId } = await pickDisposableSample(page);

    await test.step("open Dispose modal from row actions", async () => {
      await sampleRow
        .locator('[data-testid="sample-actions-overflow-menu"]')
        .click();
      await page.getByRole("menuitem", { name: "Dispose" }).click();
      await expect(page.locator('[data-testid="dispose-modal"]')).toBeVisible();
    });

    const dialog = page.locator('[data-testid="dispose-modal"]');

    await test.step("fill disposal reason and method", async () => {
      const reasonScope = dialog.locator("#disposal-reason");
      await reasonScope.locator("button.cds--list-box__field").click();
      await reasonScope
        .getByRole("option", { name: /testing complete/i })
        .click();

      const methodScope = dialog.locator("#disposal-method");
      await methodScope.locator("button.cds--list-box__field").click();
      await methodScope.getByRole("option", { name: /incineration/i }).click();
    });

    await test.step("acknowledge confirmation and submit", async () => {
      // Carbon Checkbox hides the <input>; click the associated <label>.
      await dialog.locator('label[for="disposal-confirmation"]').click();
      await dialog.getByRole("button", { name: /confirm disposal/i }).click();
      await expect(dialog).toBeHidden();
    });

    await test.step("row reflects disposed status", async () => {
      const disposedRow = page.locator("tbody tr", {
        hasText: sampleItemId,
      });
      await expect(disposedRow).toBeVisible();
      await expect(disposedRow.getByText(/disposed/i)).toBeVisible();
    });
  });
});
