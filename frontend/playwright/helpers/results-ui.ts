import { expect, Locator, Page } from "@playwright/test";
import { LONG_TIMEOUT, SHORT_TIMEOUT, UI_TIMEOUT } from "./timeouts";

/**
 * Mirrors {@link frontend/src/components/utils/Utils.js} `convertAlphaNumLabNumForDisplay`.
 * When site AccessionFormat is ALPHANUM, AnalyzerResults / AccessionResults show this form
 * (e.g. E2E001 → E2-E001). Plain `getByText('E2E001')` then fails in CI while it may pass
 * locally with SiteYearNum formatting.
 */
export function convertAlphaNumLabNumForDisplay(labNumber: string): string {
  if (!labNumber) {
    return labNumber;
  }
  if (labNumber.length > 15) {
    return labNumber;
  }
  const labNumberParts = labNumber.split("-");
  const isAnalysisLabNumber = labNumberParts.length > 1;
  let labNumberForDisplay = labNumberParts[0];
  if (labNumberParts[0].length < 8) {
    labNumberForDisplay = labNumberParts[0].slice(0, 2);
    if (labNumberParts[0].length > 2) {
      labNumberForDisplay =
        labNumberForDisplay + "-" + labNumberParts[0].slice(2);
    }
  } else {
    labNumberForDisplay = labNumberParts[0].slice(0, 2) + "-";
    if (labNumberParts[0].length > 8) {
      labNumberForDisplay =
        labNumberForDisplay +
        labNumberParts[0].slice(2, labNumberParts[0].length - 6) +
        "-";
    }
    labNumberForDisplay =
      labNumberForDisplay +
      labNumberParts[0].slice(
        labNumberParts[0].length - 6,
        labNumberParts[0].length - 3,
      ) +
      "-";
    labNumberForDisplay =
      labNumberForDisplay +
      labNumberParts[0].slice(labNumberParts[0].length - 3);
  }
  if (isAnalysisLabNumber) {
    labNumberForDisplay = labNumberForDisplay + "-" + labNumberParts[1];
  }
  return labNumberForDisplay.toUpperCase();
}

function escapeRegExp(s: string): string {
  return s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

/** Regex matching raw lab number and ALPHANUM display variant (for scoped table locators). */
export function accessionTextRegExp(accession: string): RegExp {
  const raw = accession.trim();
  const alphanum = convertAlphaNumLabNumForDisplay(raw);
  const variants = Array.from(new Set([raw, alphanum].filter(Boolean)));
  return new RegExp(variants.map(escapeRegExp).join("|"));
}

/**
 * Locator for lab/accession text as rendered under either SiteYearNum or ALPHANUM accession format.
 */
export function locatorForAccessionNumber(
  page: Page,
  accession: string,
): Locator {
  return page.getByText(accessionTextRegExp(accession)).first();
}

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
    try {
      // First attempt navigates; retries reload the same page context.
      // page.reload() reuses the existing frame, avoiding the dispatcher
      // object churn from repeated goto() that causes
      // "Object with guid response@… was not bound in the connection".
      if (attempt === 1) {
        await page.goto(url, {
          waitUntil: "load",
          timeout: perAttemptTimeoutMs,
        });
      } else {
        await page.reload({
          waitUntil: "load",
          timeout: perAttemptTimeoutMs,
        });
      }

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
    () => locatorForAccessionNumber(page, visibleText),
    options,
  );
}

/**
 * Verify a result value is visible on the staging page. Checks input fields
 * first (editable numeric results render as <input>), falls back to text nodes
 * (read-only or dictionary results render as plain text).
 */
export async function expectResultVisible(
  resultsRegion: Locator,
  resultValue: string,
): Promise<void> {
  const inputResult = resultsRegion
    .locator(`input[value*="${resultValue}"]`)
    .first();
  try {
    await expect(inputResult).toBeVisible({ timeout: SHORT_TIMEOUT });
    return;
  } catch {
    // Input not found — try text match
  }
  await expect(
    resultsRegion.getByText(resultValue, { exact: false }).first(),
  ).toBeVisible({ timeout: UI_TIMEOUT });
}

export async function openAccessionResultsAndWaitForText(
  page: Page,
  accessionNumber: string,
  visibleText = accessionNumber,
  options?: NavigateUntilVisibleOptions,
) {
  await navigateUntilVisible(
    page,
    accessionResultsUrl(accessionNumber),
    () => locatorForAccessionNumber(page, visibleText),
    options,
  );
}
