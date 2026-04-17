import { Page } from "@playwright/test";

/**
 * TAT E2E test data seeding helpers.
 *
 * Runs the real result-entry → validation chain on existing fixture
 * sample accessions so the TAT Report has `analysis.released_date`
 * populated. Mirrors the React UI save pattern exactly
 * (SearchResultForm.js:1918-1933) — same endpoint, same payload shape.
 *
 * Prerequisite: fixture samples already exist (loaded by
 * `load-test-fixtures.sh` via `reset-env.sh --full-reset`). Callers
 * pass accessions that already have an analysis in a status that shows
 * up in /rest/LogbookResults (e.g. NotStarted).
 *
 * All network calls use `page.evaluate(fetch)` so they share the
 * browser's authenticated session (JSESSIONID + CSRF in localStorage).
 */

const API_PREFIX = "/api/OpenELIS-Global";

async function apiCall<T = unknown>(
  page: Page,
  path: string,
  init: { method?: "GET" | "POST"; body?: unknown } = {},
): Promise<T> {
  const result = await page.evaluate(
    async ({ p, m, b }) => {
      const csrf = localStorage.getItem("CSRF") || "";
      const res = await fetch(p, {
        method: m,
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
          Accept: "application/json",
          "X-CSRF-Token": csrf,
        },
        body: b ? JSON.stringify(b) : undefined,
      });
      const text = await res.text().catch(() => "");
      return { status: res.status, text };
    },
    { p: path, m: init.method ?? "GET", b: init.body },
  );
  if (result.status < 200 || result.status >= 300) {
    throw new Error(
      `API ${init.method ?? "GET"} ${path} failed: HTTP ${result.status}: ${result.text.substring(0, 400)}`,
    );
  }
  try {
    return JSON.parse(result.text) as T;
  } catch {
    throw new Error(
      `API ${init.method ?? "GET"} ${path} returned non-JSON: ${result.text.substring(0, 400)}`,
    );
  }
}

interface LogbookItem {
  resultType?: string;
  defaultResultValue?: string;
  dictionaryResults?: { id: string; value: string }[];
  shadowResultValue?: string;
  reportable?: string | boolean;
}

interface LogbookResponse {
  testResult?: LogbookItem[];
}

interface ValidationResponse {
  resultList?: { isAccepted?: string | boolean }[];
}

/**
 * Pick a valid result value for the given item by type.
 * Dictionary/multi-select tests expect a dictionary row ID (not a
 * numeric literal); numeric/text accept the string fallback.
 */
function valueForItem(item: LogbookItem, numericFallback: string): string {
  const t = (item.resultType || "").toUpperCase();
  if (t === "D" || t === "M") {
    return (
      item.defaultResultValue ||
      item.dictionaryResults?.[0]?.id ||
      numericFallback
    );
  }
  return numericFallback;
}

/**
 * Enter results for all analyses on an EXISTING accession.
 *
 * Mirrors the React UI save pattern at `SearchResultForm.js:1918-1933`:
 * fetch the logbook view, mutate the in-memory state (coerce
 * `reportable` Y/N → boolean, delete the nested `result` object, set
 * `shadowResultValue` — the field the user's input is bound to), POST
 * the whole thing back unchanged otherwise.
 *
 * Why `shadowResultValue` matters: the backend routes items by
 * `ResultUtil.areResults()` (ResultUtil.java:206-212), which gates on
 * `shadowResultValue`. If blank, the item is shunted to
 * `analysisOnlyChangeResults` and no Result row is created — which is
 * why the prior helper (sending `resultValue` only) persisted nothing
 * despite returning HTTP 200.
 */
export async function enterResults(
  page: Page,
  accessionNumber: string,
  numericFallback = "5.5",
): Promise<void> {
  const logbook = await apiCall<LogbookResponse>(
    page,
    `${API_PREFIX}/rest/LogbookResults?labNumber=${accessionNumber}`,
  );

  if ((logbook.testResult ?? []).length === 0) {
    throw new Error(
      `enterResults(${accessionNumber}): LogbookResults returned 0 analyses. ` +
        `Confirm the accession exists, has at least one analysis, and is in an allowable status (NotStarted/Entered).`,
    );
  }

  const body = JSON.parse(JSON.stringify(logbook)) as LogbookResponse;
  for (const item of body.testResult ?? []) {
    const m = item as Record<string, unknown>;
    const value = valueForItem(item, numericFallback);
    m.reportable = m.reportable === "N" ? false : true;
    // Set BOTH resultValue and shadowResultValue. Jackson calls setters in
    // property order on deserialization, and TestResultItem.setResultValue
    // (line 571-574) cascades via `setShadowResultValue(results)`. If we set
    // only `shadowResultValue`, the GET-echoed `resultValue: ""` runs its
    // setter afterward and clobbers shadowResultValue back to "". Setting
    // both fields to the same value makes the cascade a no-op.
    m.resultValue = value;
    m.shadowResultValue = value;
    // Required: ResultsUpdateDataSet.isUpdated (line 123) gates on
    // item.getIsModified() AND areResults(). In the UI, React's onChange
    // handler sets isModified=true on rows the user edits; our automated
    // seed must do the same explicitly.
    m.isModified = true;
    delete m.result;
  }

  await apiCall(page, `${API_PREFIX}/rest/LogbookResults`, {
    method: "POST",
    body,
  });
}

/**
 * Accept/release all results on an EXISTING accession.
 *
 * Mirrors the UI AccessionValidation save: GET echo back with
 * `isAccepted` flipped to true. After this returns,
 * `analysis.released_date` is populated.
 */
export async function validateResults(
  page: Page,
  accessionNumber: string,
): Promise<void> {
  const validation = await apiCall<ValidationResponse>(
    page,
    `${API_PREFIX}/rest/AccessionValidation?accessionNumber=${accessionNumber}`,
  );

  if ((validation.resultList ?? []).length === 0) {
    throw new Error(
      `validateResults(${accessionNumber}): AccessionValidation returned 0 results. ` +
        `Ensure enterResults() was called first and persisted.`,
    );
  }

  const body = JSON.parse(JSON.stringify(validation)) as ValidationResponse;
  for (const item of body.resultList ?? []) {
    item.isAccepted = true;
  }

  await apiCall(page, `${API_PREFIX}/rest/AccessionValidation`, {
    method: "POST",
    body,
  });
}

/**
 * Complete the analysis chain on an EXISTING accession: result entry
 * + validation. Populates `started_date`, `completed_date`, and
 * `released_date` on the analysis.
 */
export async function completeAnalysisChain(
  page: Page,
  accessionNumber: string,
): Promise<void> {
  await enterResults(page, accessionNumber);
  await validateResults(page, accessionNumber);
}

export async function completeAnalysisChains(
  page: Page,
  accessionNumbers: string[],
): Promise<void> {
  for (const accession of accessionNumbers) {
    await completeAnalysisChain(page, accession);
  }
}

/**
 * Fixture accessions seeded by `analyzer-harness-lane-data.sql`.
 * 13 samples, each with 1 analysis in NotStarted status.
 */
export const HARNESS_LANE_ACCESSIONS = [
  "DEV01261000000000001",
  "DEV01262000000000001",
  "DEV01262000000000002",
  "DEV01262000000000003",
  "DEV01262000000000004",
  "DEV01262000000000005",
  "DEV01262000000000007",
  "DEV01262100000000001",
  "DEV01262100000000002",
  "DEV01262100000000005",
  "DEV01263000000000001",
  "DEV01263000000000002",
  "DEV01263000000000003",
];
