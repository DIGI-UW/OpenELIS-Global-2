import { Page, expect } from "@playwright/test";

import { goToAnalyzerDashboard } from "./analyzer-dashboard";

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

export async function deactivateAnalyzerByName(
  page: Page,
  analyzerName: string,
): Promise<void> {
  if (page.isClosed()) return;

  await goToAnalyzerDashboard(page);
  const row = page
    .locator("tbody tr", {
      hasText: new RegExp(escapeRegExp(analyzerName), "i"),
    })
    .first();
  if ((await row.count()) === 0) return;

  const status = row.locator('[data-testid^="status-badge-"]');
  if ((await status.textContent())?.trim() === "Inactive") return;

  await row.locator('[data-testid^="analyzer-row-overflow-"]').click();
  await page.locator('[data-testid^="analyzer-action-deactivate-"]').click();
  await page.getByRole("button", { name: /Deactivate analyzer$/ }).click();

  await expect(
    page.getByRole("heading", { name: "Deactivate analyzer" }),
  ).not.toBeVisible();
  await expect(status).toHaveText("Inactive");
}
