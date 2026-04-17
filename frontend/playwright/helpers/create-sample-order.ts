import { Page } from "@playwright/test";

/**
 * Helper for tests that need to create a fresh sample order from scratch
 * via the OpenELIS REST API — as opposed to seeding against existing
 * fixture accessions.
 *
 * Historical note: this lived inline in `seed-tat-data.ts` until that
 * file was refactored to seed against fixture accessions
 * (HARNESS_LANE_ACCESSIONS). `seed-esig-data.ts` is the current
 * consumer; extract to its own module so both helpers are free to
 * evolve independently.
 *
 * All network calls run inside `page.evaluate(fetch)` so they share
 * the browser's authenticated session (JSESSIONID + CSRF in
 * localStorage) established by auth.setup.ts.
 *
 * Prerequisite: the target environment must have the foundational
 * fixture data loaded (providers, organizations, sample types, tests).
 */

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
 * Returns the generated accession number on success, or "" on
 * server error. Mirrors the exact browser payload captured from the
 * React UI's Add Order workflow — only dynamic values are
 * parameterized.
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

  // Server date format depends on configured locale
  // (e.g. DD/MM/YYYY for fr-FR). Query the active locale's date
  // format from the API.
  const now = new Date();
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
    // eslint-disable-next-line no-console
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
