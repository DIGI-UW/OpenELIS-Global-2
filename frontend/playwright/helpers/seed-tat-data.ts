import { Page } from "@playwright/test";

/** API context path — must use /api/OpenELIS-Global prefix so the
 *  JSESSIONID (scoped to that webapp context) is recognized by Tomcat. */
const API_PREFIX = "/api/OpenELIS-Global";

/** Extract CSRF token from the page context's storageState. */
async function getCsrfToken(page: Page): Promise<string> {
  const state = await page.context().storageState();
  for (const origin of state.origins) {
    for (const item of origin.localStorage) {
      if (item.name === "CSRF") return item.value;
    }
  }
  return "";
}

/** Build headers with CSRF token for authenticated API calls. */
async function authHeaders(page: Page): Promise<Record<string, string>> {
  return { "X-CSRF-Token": await getCsrfToken(page) };
}

/**
 * TAT E2E test data seeding helpers.
 *
 * Creates sample orders via the OpenELIS REST API, enters results,
 * and validates them — populating all 6 TAT milestone timestamps.
 *
 * Uses the same authenticated session from auth.setup.ts via page.request.
 *
 * Prerequisite: e2e-foundational-data.sql must be loaded (provides
 * providers, organizations, sample types, tests).
 */

export interface SampleConfig {
  labNo: string;
  receivedDate: string; // "2026-03-15"
  receivedTime: string; // "09:30"
  priority?: "routine" | "stat";
}

/**
 * Create a sample order via /rest/SamplePatientEntry.
 *
 * This creates the order, sample, and initial analyses (in NotStarted state).
 * Populates: orderCreated (enteredDate), collected (collectionDate), received (receivedTimestamp).
 */
export async function createSampleOrder(
  page: Page,
  config: SampleConfig,
): Promise<string> {
  const { labNo, receivedDate, receivedTime, priority } = config;

  // Navigate to Add Order page so browser has the right session context.
  // The fetch() below runs inside the browser, same as the React UI.
  await page.goto("/SamplePatientEntry", {
    waitUntil: "domcontentloaded",
    timeout: 15_000,
  });

  // Exact payload structure captured from Chrome Network tab during successful
  // browser order creation. Only dynamic values are parameterized.
  const now = new Date();
  // Server date format depends on configured locale (e.g. DD/MM/YYYY for fr-FR).
  // Query the active locale's date format from the API.
  const dateFormatRes = await page.request.get(
    "/api/OpenELIS-Global/rest/open-configuration-properties",
  );
  const configProps = await dateFormatRes.json();
  const dateLocale = configProps?.DEFAULT_DATE_LOCALE || "fr-FR";
  const useMDY = dateLocale.startsWith("en");
  const dd = String(now.getDate()).padStart(2, "0");
  const mm = String(now.getMonth() + 1).padStart(2, "0");
  const yyyy = now.getFullYear();
  const today = useMDY ? `${mm}/${dd}/${yyyy}` : `${dd}/${mm}/${yyyy}`;
  const time =
    receivedTime ||
    `${String(now.getHours()).padStart(2, "0")}:${String(now.getMinutes()).padStart(2, "0")}`;
  const uniqueId = String(Date.now());

  // Step 1: Generate accession number via the same endpoint the UI uses
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
    console.warn("createSampleOrder: failed to generate accession number");
    return "";
  }

  // Step 2: POST — mirrors the exact browser payload shape
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
      nextVisitDate: today,
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

  // POST via page.evaluate(fetch) — same path as the React UI:
  // config.serverBaseUrl ("/api/OpenELIS-Global") + "/rest/SamplePatientEntry"
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

  // Extract the auto-generated accession number from the response JSON.
  if (!result.ok) {
    console.warn(
      `createSampleOrder: server returned HTTP ${result.status}: ${result.text.substring(0, 200)}`,
    );
    return "";
  }
  try {
    const responseForm = JSON.parse(result.text);
    const generatedLabNo = responseForm?.sampleOrderItems?.labNo || "";
    if (generatedLabNo) {
      console.log(`createSampleOrder: ${generatedLabNo}`);
      return generatedLabNo;
    }
    console.warn(
      `createSampleOrder: no labNo in response (HTTP ${result.status})`,
    );
    return "";
  } catch {
    console.warn(
      `createSampleOrder: non-JSON response (HTTP ${result.status})`,
    );
    return "";
  }
}

/**
 * Enter results for all analyses on a sample.
 *
 * Fetches analysis IDs via GET /rest/LogbookResults, then POSTs results.
 * Populates: testingStarted (startedDate) and resultEntered (completedDate).
 */
export async function enterResults(
  page: Page,
  accessionNumber: string,
): Promise<void> {
  const headers = await authHeaders(page);

  const getResponse = await page.request.get(
    `${API_PREFIX}/rest/LogbookResults?labNumber=${accessionNumber}`,
    { headers },
  );

  if (!getResponse.ok()) {
    console.warn(
      `enterResults ${accessionNumber}: GET failed ${getResponse.status()}`,
    );
    return;
  }

  const logbookForm = await getResponse.json();
  const testResults = logbookForm?.testResult;

  if (!testResults || testResults.length === 0) {
    console.warn(
      `enterResults ${accessionNumber}: no analyses found in logbook`,
    );
    return;
  }

  const resultPayload = testResults.map(
    (item: {
      accessionNumber: string;
      analysisId: string;
      resultId: string;
      testId: string;
    }) => ({
      accessionNumber: item.accessionNumber || accessionNumber,
      analysisId: item.analysisId,
      resultId: item.resultId || "",
      testId: item.testId,
      resultValue: "NEGATIVE",
      isModified: true,
    }),
  );

  const postResponse = await page.request.post(
    `${API_PREFIX}/rest/LogbookResults`,
    {
      headers,
      data: {
        currentDate: new Date().toISOString().split("T")[0],
        accessionNumber: accessionNumber,
        testResult: resultPayload,
      },
    },
  );

  if (!postResponse.ok()) {
    console.warn(
      `enterResults ${accessionNumber}: POST failed ${postResponse.status()}`,
    );
  }
}

/**
 * Validate/release all analyses on a sample.
 *
 * POSTs to /rest/AccessionValidation with isAccepted=true.
 * Populates: validated (releasedDate).
 */
export async function validateResults(
  page: Page,
  accessionNumber: string,
): Promise<void> {
  const headers = await authHeaders(page);

  const getResponse = await page.request.get(
    `${API_PREFIX}/rest/AccessionValidation?accessionNumber=${accessionNumber}`,
    { headers },
  );

  if (!getResponse.ok()) {
    console.warn(
      `validateResults ${accessionNumber}: GET failed ${getResponse.status()}`,
    );
    return;
  }

  const validationForm = await getResponse.json();
  const resultList = validationForm?.resultList;

  if (!resultList || resultList.length === 0) {
    console.warn(`validateResults ${accessionNumber}: no results to validate`);
    return;
  }

  const acceptPayload = resultList.map(
    (item: {
      accessionNumber: string;
      analysisId: string;
      testId: string;
      resultId: string;
      result: string;
    }) => ({
      accessionNumber: item.accessionNumber || accessionNumber,
      analysisId: item.analysisId,
      testId: item.testId,
      resultId: item.resultId || "",
      result: item.result || "NEGATIVE",
      isAccepted: true,
    }),
  );

  const postResponse = await page.request.post(
    `${API_PREFIX}/rest/AccessionValidation`,
    {
      headers,
      data: {
        currentDate: new Date().toISOString().split("T")[0],
        accessionNumber: accessionNumber,
        resultList: acceptPayload,
      },
    },
  );

  if (!postResponse.ok()) {
    console.warn(
      `validateResults ${accessionNumber}: POST failed ${postResponse.status()}`,
    );
  }
}

/**
 * Create a complete sample with all TAT timestamps populated.
 * Convenience function that runs the full lifecycle.
 */
export async function createCompleteSample(
  page: Page,
  config: SampleConfig,
): Promise<string> {
  const labNo = await createSampleOrder(page, config);
  await enterResults(page, labNo);
  await validateResults(page, labNo);

  return labNo;
}
