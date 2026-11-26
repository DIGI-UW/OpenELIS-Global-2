import { test, expect } from "../../../helpers/test-base";
import type { Page, Locator } from "@playwright/test";
import { StorageManagement } from "../../../fixtures/storage-management";
import { chooseCarbonOption } from "../../../helpers/carbon-select";
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
): Promise<{ row: Locator; sampleItemId: string; accession: string }> {
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
      const accession = (await row.locator("td").nth(1).innerText()).trim();
      return { row, sampleItemId, accession };
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
    const storage = new StorageManagement(page);

    await test.step("load the Sample Items tab", async () => {
      await page.goto("/Storage/sample-items", {
        waitUntil: "domcontentloaded",
      });
      // Sample Items is a tab of the Storage Management container now.
      await storage.expectContainer();
      await storage.expectTabSelected("Sample Items");
    });

    const {
      row: sampleRow,
      sampleItemId,
      accession,
    } = await pickDisposableSample(page);

    await test.step("open Dispose modal from row actions", async () => {
      await sampleRow
        .locator('[data-testid="sample-actions-overflow-menu"]')
        .click();
      await page.getByRole("menuitem", { name: "Dispose" }).click();
      await expect(page.locator('[data-testid="dispose-modal"]')).toBeVisible();
    });

    const dialog = page.locator('[data-testid="dispose-modal"]');

    await test.step("fill disposal reason and method", async () => {
      await chooseCarbonOption(
        dialog.getByRole("combobox", { name: "Disposal Reason" }),
        /testing complete/i,
      );
      await chooseCarbonOption(
        dialog.getByRole("combobox", { name: "Disposal Method" }),
        /incineration/i,
      );
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

    await test.step("the order audit trail shows the disposal", async () => {
      await page.goto("/qa/qms/audit-trail?type=order", {
        waitUntil: "domcontentloaded",
      });
      await page.getByRole("textbox", { name: "Lab No" }).fill(accession);
      const report = page.waitForResponse((response) =>
        response.url().includes("/rest/AuditTrailReport"),
      );
      await page.getByRole("button", { name: "View Report" }).click();
      await report;
      const disposalRow = page
        .getByRole("table", { name: "Patient Results" })
        .getByRole("row")
        .filter({
          has: page.getByRole("cell", { name: "Status", exact: true }),
        })
        .filter({
          has: page.getByRole("cell", { name: "Disposed", exact: true }),
        });
      await expect(disposalRow.first()).toBeVisible();
    });
  });
});
