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
 * Provision fresh accessions via `createSampleOrder` and run the full
 * analysis chain on each (enter results + validate). Returns the
 * generated accession numbers.
 *
 * Works in any environment with foundational fixtures loaded, including
 * the core-mode Playwright CI job that runs specs under
 * `tests/demo/core/`. Unlike `completeAnalysisChains(page,
 * HARNESS_LANE_ACCESSIONS)` it does not require pre-loaded HARN lane
 * fixtures — the spec becomes self-contained.
 *
 * Each accession ends up with `released_date` populated, which is what
 * the TAT Report queries.
 */
export async function createAndCompleteAccessions(
  page: Page,
  count: number,
): Promise<string[]> {
  const accessionNumbers: string[] = [];
  for (let i = 0; i < count; i++) {
    const accession = await createSampleOrder(page, {
      labNo: "",
      receivedDate: "",
      receivedTime: "",
    });
    if (!accession) {
      throw new Error(
        `createAndCompleteAccessions: createSampleOrder returned no accession at index ${i} of ${count}`,
      );
    }
    await completeAnalysisChain(page, accession);
    accessionNumbers.push(accession);
  }
  return accessionNumbers;
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

/* ---------------------------------------------------------------------
 * createSampleOrder — create a fresh sample order from scratch.
 *
 * Consumer: `seed-esig-data.ts` (E-Signature flow tests need a newly
 * created accession, not a fixture one). Kept here rather than in its
 * own file because it's a sibling of the fixture-based seed helpers
 * above and both serve E2E setup.
 * ------------------------------------------------------------------- */

export interface SampleConfig {
  labNo: string;
  receivedDate: string; // "2026-03-15"
  receivedTime: string; // "09:30"
  priority?: "routine" | "stat";
}

/**
 * Create a sample order via `/rest/SamplePatientEntry`.
 *
 * Creates the order, sample, and initial analyses (in NotStarted
 * state). Populates `orderCreated` (enteredDate), `collected`
 * (collectionDate), and `received` (receivedTimestamp) on the
 * analysis.
 *
 * Returns the generated accession number on success, or "" on server
 * error. Mirrors the exact browser payload captured from the React
 * UI's Add Order workflow — only dynamic values are parameterized.
 */
export async function createSampleOrder(
  page: Page,
  config: SampleConfig,
): Promise<string> {
  const { receivedTime, priority } = config;

  // Navigate to Add Order page so the browser has the right session
  // context. The fetch() below runs inside the browser, same as the
  // React UI.
  await page.goto("/SamplePatientEntry", {
    waitUntil: "domcontentloaded",
    timeout: 15_000,
  });

  // Server date format depends on configured locale (e.g. DD/MM/YYYY
  // for fr-FR). Query the active locale's date format from the API.
  const now = new Date();
  const dateFormatRes = await page.request.get(
    `${API_PREFIX}/rest/open-configuration-properties`,
  );
  const configProps = await dateFormatRes.json();
  const dateLocale = configProps?.DEFAULT_DATE_LOCALE || "fr-FR";
  const useMDY = dateLocale.startsWith("en");
  const dd = String(now.getDate()).padStart(2, "0");
  const mm = String(now.getMonth() + 1).padStart(2, "0");
  const yyyy = now.getFullYear();
  const today = useMDY ? `${mm}/${dd}/${yyyy}` : `${dd}/${mm}/${yyyy}`;
  // nextVisitDate must be strictly in the future (@ValidDate(FUTURE) on the form).
  // Using today as in the UI's default behavior triggers HTTP 400. Pick tomorrow.
  const tomorrowDate = new Date(now.getTime() + 24 * 60 * 60 * 1000);
  const tdd = String(tomorrowDate.getDate()).padStart(2, "0");
  const tmm = String(tomorrowDate.getMonth() + 1).padStart(2, "0");
  const tyyyy = tomorrowDate.getFullYear();
  const tomorrow = useMDY ? `${tmm}/${tdd}/${tyyyy}` : `${tdd}/${tmm}/${tyyyy}`;
  const time =
    receivedTime ||
    `${String(now.getHours()).padStart(2, "0")}:${String(now.getMinutes()).padStart(2, "0")}`;
  const uniqueId = String(Date.now());

  // Step 1: generate accession number via the same endpoint the UI uses.
  const genResult = await page.evaluate(async () => {
    const csrf = localStorage.getItem("CSRF") || "";
    const r = await fetch(
      "/api/OpenELIS-Global/rest/SampleEntryGenerateScanProvider",
      {
        credentials: "include",
        headers: { "X-CSRF-Token": csrf },
      },
    );
    return r.text();
  });
  const generatedLabNo = JSON.parse(genResult).body || "";
  if (!generatedLabNo) {
    // eslint-disable-next-line no-console
    console.warn("createSampleOrder: failed to generate accession number");
    return "";
  }

  // Step 2: POST — mirrors the exact browser payload shape.
  const form = {
    rememberSiteAndRequester: false,
    currentDate: null,
    projects: null,
    customNotificationLogic: false,
    patientEmailNotificationTestIds: [],
    patientSMSNotificationTestIds: [],
    providerEmailNotificationTestIds: [],
    providerSMSNotificationTestIds: [],
    patientUpdateStatus: "NO_ACTION",
    referralItems: [],
    referralOrganizations: null,
    referralReasons: null,
    sampleTypes: null,
    sampleXML:
      `<?xml version="1.0" encoding="utf-8"?>` +
      `<samples><sample sampleID='2' date='' time='' ` +
      `collector='' quantity='' uom='' tests='13' testSectionMap='' testSampleTypeMap='' ` +
      `panels='' rejected='false' rejectReasonId='' initialConditionIds='' ` +
      `storageLocationId='' storageLocationType='' storagePositionCoordinate='' ` +
      `gpsLatitude='' gpsLongitude='' gpsAccuracy='' gpsCaptureMethod='' ` +
      `numOrderLabels='1' numSpecimenLabels='1'/></samples>`,
    patientProperties: {
      patientPK: "",
      patientUpdateStatus: "ADD",
      firstName: "Esig",
      lastName: "Testpatient",
      gender: "M",
      birthDateForDisplay: useMDY ? "01/01/1990" : "01/01/1990",
      nationalId: uniqueId,
      subjectNumber: uniqueId,
    },
    patientSearch: null,
    patientEnhancedSearch: null,
    patientClinicalProperties: null,
    sampleOrderItems: {
      newRequesterName: "",
      orderTypes: [],
      orderType: "",
      externalOrderNumber: "",
      labNo: generatedLabNo,
      requestDate: today,
      receivedDateForDisplay: today,
      receivedTime: time,
      nextVisitDate: tomorrow,
      requesterSampleID: "",
      referringPatientNumber: "",
      referringSiteId: "9000100",
      referringSiteDepartmentId: "",
      referringSiteCode: "",
      referringSiteName: "",
      referringSiteDepartmentName: "",
      referringSiteList: [],
      referringSiteDepartmentList: [],
      providersList: [],
      providerId: "9000002",
      providerPersonId: "9000002",
      providerFirstName: "Jim",
      providerLastName: "Jam",
      facilityAddressStreet: "",
      facilityAddressCommune: "",
      facilityPhone: "",
      facilityFax: "",
      paymentOptionSelection: "",
      paymentOptions: [],
      modified: true,
      sampleId: "",
      readOnly: false,
      billingReferenceNumber: "",
      testLocationCode: "",
      otherLocationCode: "",
      testLocationCodeList: [],
      program: "",
      programList: [],
      contactTracingIndexName: "",
      contactTracingIndexRecordNumber: "",
      priorityList: [],
      priority: priority ? priority.toUpperCase() : "ROUTINE",
      programId: "2",
      additionalQuestions: null,
      isEQASample: false,
      eqaProgramId: "",
      eqaProviderOrganizationId: "",
      eqaProviderSampleId: "",
      eqaParticipantId: "",
      eqaDeadline: "",
      eqaPriority: "STANDARD",
    },
    initialSampleConditionList: [],
    sampleNatureList: null,
    testSectionList: [],
    warning: false,
    useReferral: false,
    rejectReasonList: null,
  };

  const result = await page.evaluate(async (formData) => {
    const csrf = localStorage.getItem("CSRF") || "";
    const res = await fetch("/api/OpenELIS-Global/rest/SamplePatientEntry", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-CSRF-Token": csrf,
      },
      credentials: "include",
      body: JSON.stringify(formData),
    });
    const text = await res.text().catch(() => "");
    return { status: res.status, ok: res.ok, text };
  }, form);

  if (!result.ok) {
    // eslint-disable-next-line no-console
    console.warn(
      `createSampleOrder: server returned HTTP ${result.status}: ${result.text.substring(0, 200)}`,
    );
    return "";
  }
  try {
    const responseForm = JSON.parse(result.text);
    const responseLabNo = responseForm?.sampleOrderItems?.labNo || "";
    if (responseLabNo) {
      // eslint-disable-next-line no-console
      console.log(`createSampleOrder: ${responseLabNo}`);
      return responseLabNo;
    }
    // eslint-disable-next-line no-console
    console.warn(
      `createSampleOrder: no labNo in response (HTTP ${result.status})`,
    );
    return "";
  } catch {
    // eslint-disable-next-line no-console
    console.warn(
      `createSampleOrder: non-JSON response (HTTP ${result.status})`,
    );
    return "";
  }
}
