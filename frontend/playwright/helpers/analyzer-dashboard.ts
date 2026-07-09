import { expect, Locator, Page, TestInfo } from "@playwright/test";
import { LONG_TIMEOUT, UI_TIMEOUT } from "./timeouts";
import { videoPause } from "./video-pause";

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

export async function goToAnalyzerDashboard(
  page: Page,
  testInfo?: TestInfo,
): Promise<void> {
  await page.goto("analyzers", { waitUntil: "domcontentloaded" });
  await expect(page.locator('[data-testid="analyzers-list"]')).toBeVisible({
    timeout: LONG_TIMEOUT,
  });
  if (testInfo) {
    await videoPause(page, 1_000, testInfo);
  }
}

export async function findAnalyzerRow(
  page: Page,
  name: string,
  testInfo?: TestInfo,
): Promise<Locator> {
  const searchInput = page.locator('[data-testid="analyzer-search-input"]');
  await searchInput.fill(name);
  if (testInfo) {
    await videoPause(page, 1_000, testInfo);
  }
  const row = page.locator("tbody tr", {
    hasText: new RegExp(escapeRegExp(name), "i"),
  });
  await expect(row.first()).toBeVisible({ timeout: UI_TIMEOUT });
  return row;
}
