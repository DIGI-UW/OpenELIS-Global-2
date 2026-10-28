import { Page, expect } from "@playwright/test";

/**
 * Creates an anatomic-pathology case on whatever stack the run is pointed at.
 *
 * Every id the order needs is discovered from the running deployment rather
 * than written into this file, because the ids a pathology order depends on
 * (the pathology programme, a sample type that offers the histopathology
 * examination, that test) are seed data a deployment is free to renumber, and
 * a dev stack is not loaded with the same fixtures as CI. A hardcoded id that
 * happens to belong to something else produces an order that saves and a case
 * that never appears, which is far harder to read than a discovery that fails
 * naming what it could not find.
 *
 * The order save is what creates the pathology case, but it answers with the
 * sample order, not with the case: the pathology_sample row is inserted as a
 * side effect of the programme being a pathology programme. So the case's own
 * id is read back afterwards from the pathology dashboard, which is the only
 * endpoint that maps an accession number to a pathology case id.
 *
 * All network calls run inside the browser through page.evaluate so they carry
 * the same session and CSRF token as the React application.
 */

const API_PREFIX = "/api/OpenELIS-Global";

/** The test every pathology case in this seed is ordered against. */
const PATHOLOGY_TEST_NAME = "Histopathology examination";

/**
 * How long a read-back is given to show what a save just wrote: the case on
 * the dashboard, or a dictionary entry in its category's list.
 */
const READ_BACK_TIMEOUT_MS = 10_000;
const READ_BACK_INTERVAL_MS = 500;

/** The dictionary category a cassette's tissue type is chosen from. */
const TISSUE_TYPE_CATEGORY = "pathology_tissue_types";

/** The entry ensureTissueType adds when a deployment has none of its own. */
const SEEDED_TISSUE_TYPE = {
  dictEntry: "E2E skin",
  localAbbreviation: "E2ESKIN",
};

interface IdValuePair {
  id: string;
  value: string;
}

interface SampleTypeTests {
  tests?: { id: string; name: string }[];
}

interface DashboardEntry {
  labNumber?: string;
  pathologySampleId?: number | string;
  status?: string;
}

/** The ids one pathology order is placed against, and their display names. */
export interface PathologyOrderTarget {
  programId: string;
  programName: string;
  sampleTypeId: string;
  sampleTypeName: string;
  testId: string;
  testName: string;
  /** Empty when the deployment has no organization usable as a referring site. */
  referringSiteId: string;
  /** Empty when the deployment has no provider on file. */
  providerId: string;
  /** The person behind that provider; empty whenever providerId is. */
  providerPersonId: string;
}

/** One tissue type a cassette can be given, as the case view offers it. */
export interface TissueType {
  id: string;
  name: string;
}

interface DictionaryCategoryRow {
  id: string;
  categoryName?: string;
}

/** One seeded case: the accession it was ordered under, and its case id. */
export interface SeededPathologyCase {
  accessionNumber: string;
  pathologySampleId: string;
}

/**
 * GET through the browser session. Throws on any non-2xx with the body, so a
 * seed that cannot see the data it needs says which call failed and why
 * instead of failing later on an empty list.
 */
async function apiGet<T>(page: Page, path: string): Promise<T> {
  const result = await page.evaluate(async (p) => {
    const csrf = localStorage.getItem("CSRF") || "";
    const res = await fetch(p, {
      credentials: "include",
      headers: { Accept: "application/json", "X-CSRF-Token": csrf },
    });
    return { status: res.status, text: await res.text() };
  }, path);

  if (result.status < 200 || result.status >= 300) {
    throw new Error(
      `GET ${path} failed: HTTP ${result.status}: ${result.text.substring(0, 400)}`,
    );
  }
  try {
    return JSON.parse(result.text) as T;
  } catch {
    throw new Error(
      `GET ${path} returned non-JSON: ${result.text.substring(0, 400)}`,
    );
  }
}

/**
 * POST JSON through the browser session, the counterpart of apiGet, and return
 * the response body as text. Throws on any non-2xx with the body, so a seed
 * the server refuses says why; `action` names what was being done.
 */
async function apiPost(
  page: Page,
  path: string,
  body: unknown,
  action = `POST ${path}`,
): Promise<string> {
  const result = await page.evaluate(
    async ({ p, data }) => {
      const csrf = localStorage.getItem("CSRF") || "";
      const res = await fetch(p, {
        method: "POST",
        credentials: "include",
        headers: {
          Accept: "application/json",
          "Content-Type": "application/json",
          "X-CSRF-Token": csrf,
        },
        body: JSON.stringify(data),
      });
      return { status: res.status, text: await res.text() };
    },
    { p: path, data: body },
  );

  if (result.status < 200 || result.status >= 300) {
    throw new Error(
      `${action} failed: HTTP ${result.status}: ${result.text.substring(0, 600)}`,
    );
  }
  return result.text;
}

/**
 * The pathology programme, chosen by the same rule the server applies when it
 * decides an order has created a pathology case: the programme's name contains
 * "pathology", whatever else it is called. Matching on the displayed name
 * rather than on a fixed id is what lets this run against a deployment that
 * calls the programme Histopathology and one that calls it Pathology alike.
 */
function pickPathologyProgram(programs: IdValuePair[]): IdValuePair {
  const program = programs.find((entry) =>
    (entry.value || "").toLowerCase().includes("pathology"),
  );
  if (!program) {
    throw new Error(
      "No pathology programme on this deployment. /rest/user-programs offered: " +
        programs.map((entry) => entry.value).join(", "),
    );
  }
  return program;
}

/**
 * Discover the programme, sample type and test one pathology order needs, plus
 * a referring site and provider if the deployment has any.
 *
 * The sample type is found by asking each type what it offers until one offers
 * the histopathology examination, rather than by name: several types carry
 * pathology-sounding names and only the mapping says which of them the test is
 * actually orderable on.
 */
export async function discoverPathologyOrderTarget(
  page: Page,
): Promise<PathologyOrderTarget> {
  const programs = await apiGet<IdValuePair[]>(
    page,
    `${API_PREFIX}/rest/user-programs`,
  );
  const program = pickPathologyProgram(programs);

  const sampleTypes = await apiGet<IdValuePair[]>(
    page,
    `${API_PREFIX}/rest/user-sample-types`,
  );

  for (const sampleType of sampleTypes) {
    const offered = await apiGet<SampleTypeTests>(
      page,
      `${API_PREFIX}/rest/sample-type-tests?sampleType=${sampleType.id}`,
    );
    const test = (offered.tests ?? []).find(
      (entry) => entry.name === PATHOLOGY_TEST_NAME,
    );
    if (test) {
      return {
        programId: program.id,
        programName: program.value,
        sampleTypeId: sampleType.id,
        sampleTypeName: sampleType.value,
        testId: test.id,
        testName: test.name,
        referringSiteId: await firstReferringSiteId(page),
        ...(await firstProvider(page)),
      };
    }
  }

  throw new Error(
    `No sample type on this deployment offers "${PATHOLOGY_TEST_NAME}". ` +
      `Types checked: ${sampleTypes.map((entry) => entry.value).join(", ")}`,
  );
}

/**
 * The first organization the site search offers, or "" when it offers none.
 *
 * An id that does not resolve to an organization makes the save fail while it
 * reads that organization's contact details back, so a made-up id is worse
 * than none: with no id and no site name the server never looks an
 * organization up at all, and the order is saved without a referring site.
 */
async function firstReferringSiteId(page: Page): Promise<string> {
  const found = await apiGet<{ organizations?: { id?: string }[] }>(
    page,
    `${API_PREFIX}/rest/organization/search?search=`,
  );
  return (found.organizations ?? [])[0]?.id ?? "";
}

/**
 * The first provider on file that has a person behind it, as the two ids the
 * order form carries, or both "" when the deployment has none.
 *
 * A provider and its person are separate rows with separate ids, and the save
 * looks the person up by providerPersonId, so both ids are taken from the same
 * search result exactly as the order screen sends them.
 */
async function firstProvider(
  page: Page,
): Promise<{ providerId: string; providerPersonId: string }> {
  const found = await apiGet<{
    providers?: { id?: string; personId?: string }[];
  }>(page, `${API_PREFIX}/rest/provider/search?search=`);
  const provider = (found.providers ?? []).find(
    (entry) => entry.id && entry.personId,
  );
  return {
    providerId: provider?.id ?? "",
    providerPersonId: provider?.personId ?? "",
  };
}

/**
 * The day after a date written in the deployment's own order, in that same
 * order. The three parts are already a calendar day, so the arithmetic is done
 * in UTC: any other zone could move the date the server just reported.
 */
function nextDay(date: string, useMDY: boolean): string {
  const parts = date.split("/").map((part) => Number(part));
  if (parts.length !== 3 || parts.some((part) => !Number.isFinite(part))) {
    throw new Error(
      `The server reported its date as "${date}", which is neither ` +
        `MM/DD/YYYY nor DD/MM/YYYY, so this seed cannot work out tomorrow.`,
    );
  }

  const [day, month, year] = useMDY
    ? [parts[1], parts[0], parts[2]]
    : [parts[0], parts[1], parts[2]];
  const next = new Date(Date.UTC(year, month - 1, day + 1));
  const dd = String(next.getUTCDate()).padStart(2, "0");
  const mm = String(next.getUTCMonth() + 1).padStart(2, "0");
  const yyyy = next.getUTCFullYear();
  return useMDY ? `${mm}/${dd}/${yyyy}` : `${dd}/${mm}/${yyyy}`;
}

/**
 * Today, tomorrow and the time now, all read from the deployment rather than
 * from the machine the run is started on.
 *
 * The two are not in the same timezone: the dev stack keeps the laboratory's
 * own zone, and the next visit date is validated as strictly after the
 * server's today, so a date taken from the runner's clock is refused with HTTP
 * 400 for part of every day. The open configuration endpoint already reports
 * the server's own date and time, formatted the way that deployment formats
 * dates, so those are used as they come and tomorrow is derived from them.
 */
function orderDates(configProps: Record<string, string>) {
  const today = configProps?.currentDateAsText || "";
  const time = configProps?.currentTimeAsText || "";
  if (!today || !time) {
    throw new Error(
      "open-configuration-properties reported no currentDateAsText or " +
        "currentTimeAsText, so this seed has no server clock to order against.",
    );
  }

  const useMDY = (configProps?.DEFAULT_DATE_LOCALE || "fr-FR").startsWith("en");
  return { today, tomorrow: nextDay(today, useMDY), time };
}

/**
 * Order one histopathology examination on a new patient, and return the
 * accession number and the id of the pathology case it created.
 *
 * Pass a target to reuse a discovery already made; leave it out and one is
 * made here.
 */
export async function createPathologyCase(
  page: Page,
  target?: PathologyOrderTarget,
): Promise<SeededPathologyCase> {
  // The calls below run inside the browser so they carry the application's
  // own session, which means the page has to be on the application. One that
  // already is, is left where it is: the order is placed through the API, so
  // no particular screen is needed and reloading a heavy one only costs time.
  if (!page.url().startsWith("http")) {
    await page.goto("/", { waitUntil: "domcontentloaded" });
  }

  const order = target ?? (await discoverPathologyOrderTarget(page));

  const configProps = await apiGet<Record<string, string>>(
    page,
    `${API_PREFIX}/rest/open-configuration-properties`,
  );
  const { today, tomorrow, time } = orderDates(configProps);

  const generated = await apiGet<{ body?: string }>(
    page,
    `${API_PREFIX}/rest/SampleEntryGenerateScanProvider`,
  );
  const labNo = generated?.body ?? "";
  if (!labNo) {
    throw new Error(
      "SampleEntryGenerateScanProvider returned no accession number",
    );
  }

  const uniqueId = String(Date.now());
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
      `<samples><sample sampleID='${order.sampleTypeId}' date='' time='' ` +
      `collector='' quantity='' uom='' tests='${order.testId}' testSectionMap='' testSampleTypeMap='' ` +
      `panels='' rejected='false' rejectReasonId='' initialConditionIds='' ` +
      `storageLocationId='' storageLocationType='' storagePositionCoordinate='' ` +
      `gpsLatitude='' gpsLongitude='' gpsAccuracy='' gpsCaptureMethod='' ` +
      `numOrderLabels='1' numSpecimenLabels='1'/></samples>`,
    patientProperties: {
      patientPK: "",
      patientUpdateStatus: "ADD",
      firstName: "Pathology",
      lastName: "Casetest",
      gender: "M",
      birthDateForDisplay: "01/01/1990",
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
      labNo,
      requestDate: today,
      receivedDateForDisplay: today,
      receivedTime: time,
      nextVisitDate: tomorrow,
      requesterSampleID: "",
      referringPatientNumber: "",
      referringSiteId: order.referringSiteId,
      referringSiteDepartmentId: "",
      referringSiteCode: "",
      referringSiteName: "",
      referringSiteDepartmentName: "",
      referringSiteList: [],
      referringSiteDepartmentList: [],
      providersList: [],
      providerId: order.providerId,
      providerPersonId: order.providerPersonId,
      providerFirstName: "",
      providerLastName: "",
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
      priority: "ROUTINE",
      programId: order.programId,
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

  const saved = await apiPost(
    page,
    `${API_PREFIX}/rest/SamplePatientEntry`,
    form,
    `Ordering ${order.testName} on ${order.sampleTypeName} for programme ${order.programName}`,
  );

  const accessionNumber =
    (JSON.parse(saved)?.sampleOrderItems?.labNo as string) || "";
  if (!accessionNumber) {
    throw new Error(
      `Order saved but answered with no accession number: ${saved.substring(0, 400)}`,
    );
  }

  return {
    accessionNumber,
    pathologySampleId: await findCaseId(page, accessionNumber),
  };
}

/**
 * The pathology case id for an accession, read from the dashboard.
 *
 * Polled rather than read once: the case is inserted in the same transaction
 * as the order, but the dashboard is a separate request and a stack under load
 * can answer it before the order's own response has been written out.
 */
async function findCaseId(
  page: Page,
  accessionNumber: string,
): Promise<string> {
  let caseId = "";

  await expect
    .poll(
      async () => {
        // The dashboard answers a page, `{ items, paging, totalItems }`, the
        // shape PathologyDashboard.jsx reads.
        const response = await apiGet<{ items?: DashboardEntry[] }>(
          page,
          `${API_PREFIX}/rest/pathology/dashboard?statuses=ACCESSIONED&searchTerm=${encodeURIComponent(accessionNumber)}`,
        );
        const entry = (response?.items ?? []).find(
          (item) => item.labNumber === accessionNumber,
        );
        if (entry?.pathologySampleId != null) {
          caseId = String(entry.pathologySampleId);
        }
        return caseId;
      },
      {
        message:
          `The pathology dashboard never listed a case at ACCESSIONED for ${accessionNumber}. ` +
          `The order saved, so either the programme it was ordered under is not a pathology ` +
          `programme or the case was created at some other stage.`,
        intervals: [READ_BACK_INTERVAL_MS],
        timeout: READ_BACK_TIMEOUT_MS,
      },
    )
    .not.toBe("");

  return caseId;
}

/**
 * The first active tissue type on the deployment, adding one when the
 * category holds none.
 *
 * The category ships empty, because each laboratory names its own tissues, so
 * a fresh stack offers nothing to pick and the picker is disabled. An entry a
 * deployment already has is used as it is; one is added only to an empty
 * category, through the endpoint the Dictionary admin screen posts to, and is
 * never deleted afterwards, since a cassette that holds it keeps pointing at
 * it. The name returned is the one the case view's picker displays.
 */
export async function ensureTissueType(page: Page): Promise<TissueType> {
  if (!page.url().startsWith("http")) {
    await page.goto("/", { waitUntil: "domcontentloaded" });
  }

  const listPath = `${API_PREFIX}/rest/dictionary/category/${TISSUE_TYPE_CATEGORY}`;
  const listed = await apiGet<IdValuePair[]>(page, listPath);
  if (listed.length > 0) {
    return { id: String(listed[0].id), name: listed[0].value };
  }

  const categories = await apiGet<DictionaryCategoryRow[]>(
    page,
    `${API_PREFIX}/rest/dictionary-categories`,
  );
  const category = categories.find(
    (entry) => entry.categoryName === TISSUE_TYPE_CATEGORY,
  );
  if (!category) {
    throw new Error(
      `No dictionary category named ${TISSUE_TYPE_CATEGORY} on this deployment, ` +
        "so the changeset that creates it has not run here.",
    );
  }

  await apiPost(page, `${API_PREFIX}/rest/Dictionary`, {
    id: "",
    selectedDictionaryCategoryId: category.id,
    dictEntry: SEEDED_TISSUE_TYPE.dictEntry,
    localAbbreviation: SEEDED_TISSUE_TYPE.localAbbreviation,
    isActive: "Y",
    dirtyFormFields: "",
  });

  // Read back through the same list the case view loads, so what is returned
  // is what the picker will offer rather than what was posted.
  let added: TissueType | undefined;
  await expect
    .poll(
      async () => {
        const after = await apiGet<IdValuePair[]>(page, listPath);
        const entry = after.find(
          (item) => item.value === SEEDED_TISSUE_TYPE.dictEntry,
        );
        added = entry ? { id: String(entry.id), name: entry.value } : undefined;
        return Boolean(added);
      },
      {
        message: `${SEEDED_TISSUE_TYPE.dictEntry} was saved but ${listPath} does not list it`,
        intervals: [READ_BACK_INTERVAL_MS],
        timeout: READ_BACK_TIMEOUT_MS,
      },
    )
    .toBe(true);

  return added!;
}
