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
  /** Optional API URL to poll before navigating. When provided, the helper
   *  waits for the API to return matching content before loading the page,
   *  eliminating the reload loop entirely. */
  apiPollUrl?: string;
  /** Text(s) to match in resultList accessionNumber fields. When an array,
   *  ALL must be present before navigating (handles multi-sample file imports
   *  where the bridge posts results one accession at a time). */
  apiPollMatch?: string | string[];
};

async function navigateUntilVisible(
  page: Page,
  url: string,
  visibleLocator: () => Locator,
  options?: NavigateUntilVisibleOptions,
) {
  const timeoutMs = options?.timeoutMs ?? LONG_TIMEOUT;
  const perAttemptTimeoutMs = options?.perAttemptTimeoutMs ?? UI_TIMEOUT;

  // When an API poll URL is provided, poll the REST API before navigating.
  // Uses page.request.get() but disposes each response immediately to avoid
  // stale protocol bindings that cause "guid response@... was not bound"
  // on the subsequent page.goto().
  if (options?.apiPollUrl) {
    const matchList = !options?.apiPollMatch
      ? []
      : Array.isArray(options.apiPollMatch)
        ? options.apiPollMatch
        : [options.apiPollMatch];

    await expect
      .poll(
        async () => {
          try {
            const resp = await page.request.get(options.apiPollUrl!, {
              timeout: SHORT_TIMEOUT,
            });
            let ok = false;
            let data = null;
            try {
              ok = resp.ok();
              data = ok ? await resp.json() : null;
            } finally {
              await resp.dispose();
            }
            if (!ok || !data) return false;
            const results = data?.resultList ?? [];
            if (results.length === 0) return false;
            if (matchList.length === 0) return true;
            const accessions = results.map(
              (r: { accessionNumber?: string }) => r.accessionNumber ?? "",
            );
            return matchList.every((m) =>
              accessions.some((a: string) => a.includes(m)),
            );
          } catch {
            return false;
          }
        },
        {
          message: `Waiting for results matching "${options?.apiPollMatch}" at ${options.apiPollUrl}`,
          timeout: timeoutMs,
          intervals: [2_000],
        },
      )
      .toBeTruthy();

    await page.goto(url, {
      waitUntil: "domcontentloaded",
      timeout: perAttemptTimeoutMs,
    });
    await expect(visibleLocator()).toBeVisible({
      timeout: perAttemptTimeoutMs,
    });
    return;
  }

  // Fallback: reload loop for pages without a known API endpoint.
  const attempts = Math.max(1, Math.ceil(timeoutMs / perAttemptTimeoutMs));
  let lastError: unknown;

  for (let attempt = 1; attempt <= attempts; attempt++) {
    try {
      if (attempt === 1) {
        await page.goto(url, {
          waitUntil: "domcontentloaded",
          timeout: perAttemptTimeoutMs,
        });
      } else {
        await page.reload({
          waitUntil: "domcontentloaded",
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
  options?: NavigateUntilVisibleOptions & {
    /** All expected accession numbers — poll waits for ALL before navigating. */
    allExpectedAccessions?: string[];
  },
) {
  const apiUrl = `/api/OpenELIS-Global/rest/AnalyzerResults?type=${encodeURIComponent(analyzerName)}`;
  const pollMatch = options?.allExpectedAccessions ?? visibleText;
  await navigateUntilVisible(
    page,
    analyzerResultsUrl(analyzerName),
    () => locatorForAccessionNumber(page, visibleText),
    { ...options, apiPollUrl: apiUrl, apiPollMatch: pollMatch },
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
  // AccessionResults is called after results are saved — data already exists
  // in the DB. Navigate once with a generous assertion timeout instead of the
  // reload loop, which wastes memory and can trigger OOM browser crashes on CI.
  const timeout = options?.timeoutMs ?? LONG_TIMEOUT;
  await page.goto(accessionResultsUrl(accessionNumber), {
    waitUntil: "domcontentloaded",
    timeout,
  });
  await expect(locatorForAccessionNumber(page, visibleText)).toBeVisible({
    timeout,
  });
}
