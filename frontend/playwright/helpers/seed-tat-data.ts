import { Page, expect } from "@playwright/test";

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
  const headers = await authHeaders(page);

  // Step 1: GET the preform — same approach as the React Add Order page.
  // This returns the full form shape with default values, display lists, etc.
  const preformRes = await page.request.get(
    `${API_PREFIX}/rest/SamplePatientEntry`,
    { headers },
  );
  if (!preformRes.ok()) {
    console.warn(
      `createSampleOrder ${labNo}: GET preform failed ${preformRes.status()}`,
    );
    return labNo;
  }

  const form = await preformRes.json();

  // Step 2: Find a valid sample type ID from the preform's sampleTypes list
  const sampleTypeId = form.sampleTypes?.[0]?.id || "1";

  // Step 3: Modify the form — same fields the React UI sets
  form.warning = true;
  form.currentDate = form.currentDate; // keep today's date from preform

  // Patient info (minimal — new patient)
  form.patientProperties = form.patientProperties || {};
  form.patientProperties.firstName = "TAT-E2E";
  form.patientProperties.lastName = `Patient-${labNo}`;
  form.patientProperties.birthDateForDisplay = "01/01/1990";
  form.patientProperties.gender = "M";
  form.patientUpdateStatus = "ADD";

  // Build sampleOrderItems from scratch — the preform's version has display-only
  // fields (referringSiteList, isEQASample, readOnly) that don't exist on the
  // SampleOrderItem Java bean. Jackson rejects unknown fields → null object
  // → null labNo → crash. Only include fields that exist on SampleOrderItem.java.
  const seq = String(Date.now()).slice(-6);
  const actualLabNo = `${new Date().getFullYear()}${seq}`;
  form.sampleOrderItems = {
    labNo: actualLabNo,
    receivedDateForDisplay: form.currentDate,
    receivedTime: receivedTime || "09:00",
    modified: true,
    priority: priority ? priority.toUpperCase() : "ROUTINE",
  };

  // Build patientProperties from scratch — same reason (display lists like
  // genders, addressDepartments contain Dictionary objects with extra fields).
  form.patientProperties = {
    firstName: "TAT-E2E",
    lastName: `Patient-${actualLabNo}`,
    birthDateForDisplay: "01/01/1990",
    gender: "M",
  };

  // Sample XML — format from Index.js:710.
  form.sampleXML =
    `<samples>` +
    `<sample sampleID='${sampleTypeId}' date='${form.currentDate}' time='${receivedTime || "09:00"}' ` +
    `collector='' quantity='' uom='' tests='1' testSectionMap='' testSampleTypeMap='' ` +
    `panels='' rejected='false' rejectReasonId='' initialConditionIds='' ` +
    `storageLocationId='' storageLocationType='' storagePositionCoordinate='' ` +
    `gpsLatitude='' gpsLongitude='' gpsAccuracy='' gpsCaptureMethod='' ` +
    `numOrderLabels='1' numSpecimenLabels='1'/>` +
    `</samples>`;

  // Remove display-only top-level lists that have nested objects Jackson can't handle
  delete form.sampleTypes;
  delete form.referralOrganizations;
  delete form.referralReasons;
  delete form.rejectReasonList;
  delete form.sampleNatureList;
  delete form.initialSampleConditionList;
  delete form.testSectionList;
  delete form.patientSearch;
  delete form.patientEnhancedSearch;
  delete form.projects;

  // Step 4: POST the modified form back
  const response = await page.request.post(
    `${API_PREFIX}/rest/SamplePatientEntry`,
    { headers, data: form },
  );

  if (!response.ok()) {
    const text = await response.text().catch(() => "");
    console.warn(
      `createSampleOrder ${actualLabNo}: HTTP ${response.status()} — ${text.slice(0, 200)}`,
    );
  } else {
    console.log(`createSampleOrder: created sample ${actualLabNo}`);
  }

  return actualLabNo;
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
