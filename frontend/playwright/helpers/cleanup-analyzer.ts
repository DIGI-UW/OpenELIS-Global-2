import { Locator, Page } from "@playwright/test";
import { findAnalyzerRow, goToAnalyzerDashboard } from "./analyzer-dashboard";

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

export async function cleanupAnalyzerByName(page: Page, analyzerName: string) {
  if (page.isClosed()) return;
  try {
    await goToAnalyzerDashboard(page);
    const row = page.locator("tbody tr", {
      hasText: new RegExp(escapeRegExp(analyzerName), "i"),
    });
    if ((await row.count()) === 0) {
      return;
    }
    await row
      .first()
      .locator('[data-testid^="analyzer-row-overflow-"]')
      .click();
    const deleteAction = page
      .locator('[data-testid*="analyzer-action-delete"]')
      .first();
    await deleteAction.click();
    const confirmButton = page
      .getByRole("button", { name: /delete|confirm/i })
      .last();
    await confirmButton.click();
  } catch (e) {
    // Cleanup is best-effort — don't mask the actual test failure
    console.warn(`Cleanup failed for "${analyzerName}": ${e}`);
  }
}

export async function cleanupAnalyzerByExactName(
  page: Page,
  analyzerName: string,
) {
  await goToAnalyzerDashboard(page);
  const row = await findAnalyzerRow(page, analyzerName);
  await row.first().locator('[data-testid^="analyzer-row-overflow-"]').click();
  const deleteAction = page
    .locator('[data-testid*="analyzer-action-delete"]')
    .first();
  await deleteAction.click();
  const confirmButton = page
    .getByRole("button", { name: /delete|confirm/i })
    .last();
  await confirmButton.click();
}

export async function cleanupAnalyzersMatching(
  page: Page,
  matcher: RegExp,
  maxDeletes = 20,
) {
  let deletedCount = 0;

  while (deletedCount < maxDeletes) {
    await goToAnalyzerDashboard(page);
    const rows = page.locator("tbody tr");
    const rowCount = await rows.count();

    let matchingRow: Locator | undefined;
    for (let index = 0; index < rowCount; index++) {
      const row = rows.nth(index);
      const text = ((await row.textContent()) || "").trim();
      if (matcher.test(text)) {
        matchingRow = row;
        matcher.lastIndex = 0;
        break;
      }
      matcher.lastIndex = 0;
    }

    if (!matchingRow) {
      return deletedCount;
    }

    await matchingRow
      .locator('[data-testid^="analyzer-row-overflow-"]')
      .first()
      .click();
    const deleteAction = page
      .locator('[data-testid*="analyzer-action-delete"]')
      .first();
    await deleteAction.click();
    const confirmButton = page
      .getByRole("button", { name: /delete|confirm/i })
      .last();
    await confirmButton.click();

    deletedCount += 1;
  }

  return deletedCount;
}
