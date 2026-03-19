import { Page } from "@playwright/test";
import { findAnalyzerRow, goToAnalyzerDashboard } from "./analyzer-dashboard";

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

export async function cleanupAnalyzerByName(page: Page, analyzerName: string) {
  await goToAnalyzerDashboard(page);
  const row = page.locator("tbody tr", {
    hasText: new RegExp(escapeRegExp(analyzerName), "i"),
  });
  if ((await row.count()) === 0) {
    return;
  }
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
