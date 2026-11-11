import { expect, type Locator, type Page } from "@playwright/test";

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

/** Opens Analyzer Types and waits for its table. */
export async function openAnalyzerTypes(page: Page, query = "") {
  await page.goto(`/analyzers/types${query}`, {
    waitUntil: "domcontentloaded",
  });
  await expect(
    page.getByRole("heading", { level: 1, name: "Analyzer Types" }),
  ).toBeVisible();
  await expect(page.getByRole("table")).toBeVisible();
}

/** The row of one type, by its display name. */
export function analyzerTypeRow(page: Page, name: string): Locator {
  return page.getByRole("row", {
    name: new RegExp(escapeRegExp(name), "i"),
  });
}

/** Picks an action from a type's row menu. */
export async function openRowAction(page: Page, name: string, action: string) {
  await page
    .getByRole("button", { name: `Actions for ${name}`, exact: true })
    .click();
  await page.getByRole("menuitem", { name: action, exact: true }).click();
}
