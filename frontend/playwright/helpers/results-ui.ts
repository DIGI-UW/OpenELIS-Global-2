import { expect, Locator, Page } from "@playwright/test";
import { LONG_TIMEOUT, UI_TIMEOUT } from "./timeouts";

type NavigateUntilVisibleOptions = {
  timeoutMs?: number;
  perAttemptTimeoutMs?: number;
};

async function navigateUntilVisible(
  page: Page,
  url: string,
  visibleLocator: () => Locator,
  options?: NavigateUntilVisibleOptions,
) {
  const timeoutMs = options?.timeoutMs ?? LONG_TIMEOUT;
  const perAttemptTimeoutMs = options?.perAttemptTimeoutMs ?? UI_TIMEOUT;
  const attempts = Math.max(1, Math.ceil(timeoutMs / perAttemptTimeoutMs));

  let lastError: unknown;

  for (let attempt = 1; attempt <= attempts; attempt++) {
    await page.goto(url, { waitUntil: "domcontentloaded" });

    try {
      await expect(visibleLocator()).toBeVisible({
        timeout: perAttemptTimeoutMs,
      });
      return;
    } catch (error) {
      lastError = error;
    }
  }

  throw lastError instanceof Error
    ? lastError
    : new Error(`Timed out waiting for visible content at ${url}`);
}

export function analyzerResultsUrl(analyzerName: string): string {
  return `AnalyzerResults?type=${encodeURIComponent(analyzerName)}`;
}

export function accessionResultsUrl(accessionNumber: string): string {
  return `AccessionResults?accessionNumber=${encodeURIComponent(accessionNumber)}`;
}

export async function openAnalyzerResultsAndWaitForText(
  page: Page,
  analyzerName: string,
  visibleText: string,
  options?: NavigateUntilVisibleOptions,
) {
  await navigateUntilVisible(
    page,
    analyzerResultsUrl(analyzerName),
    () => page.getByText(visibleText, { exact: false }).first(),
    options,
  );
}

export async function openAccessionResultsAndWaitForText(
  page: Page,
  accessionNumber: string,
  visibleText = accessionNumber,
  options?: NavigateUntilVisibleOptions,
) {
  const timeoutMs = options?.timeoutMs ?? LONG_TIMEOUT;
  const perAttemptTimeoutMs = options?.perAttemptTimeoutMs ?? UI_TIMEOUT;
  const attempts = Math.max(1, Math.ceil(timeoutMs / perAttemptTimeoutMs));

  let lastError: unknown;

  for (let attempt = 1; attempt <= attempts; attempt++) {
    await page.goto("AccessionResults", { waitUntil: "domcontentloaded" });

    const accessionInput = page
      .locator('input[name="accessionNumber"]')
      .first();
    await expect(accessionInput).toBeVisible({ timeout: perAttemptTimeoutMs });
    await accessionInput.fill(accessionNumber);

    const searchButton = page.getByRole("button", { name: "Search" }).first();
    await expect(searchButton).toBeVisible({ timeout: perAttemptTimeoutMs });
    await searchButton.click();

    try {
      await expect(
        page.getByText(visibleText, { exact: false }).first(),
      ).toBeVisible({
        timeout: perAttemptTimeoutMs,
      });
      return;
    } catch (error) {
      lastError = error;
    }
  }

  throw lastError instanceof Error
    ? lastError
    : new Error(
        `Timed out waiting for visible content in AccessionResults for ${accessionNumber}`,
      );
}
