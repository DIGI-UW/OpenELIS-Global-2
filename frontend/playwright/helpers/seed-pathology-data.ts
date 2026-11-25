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

/**
 * The dictionary category a slide's stain is chosen from. The application
 * creates it from its shipped dictionary files when it starts, not through a
 * changeset.
 */
const STAIN_CATEGORY = "Pathology Stains";

/** The column a dictionary entry's local abbreviation is stored in. */
const LOCAL_ABBREVIATION_MAX_LENGTH = 10;

/** The bench stages in the order the bench works them, as the server names them. */
export const PATHOLOGY_STAGE_ORDER = [
  "ACCESSIONED",
  "GROSSING",
  "DECALCIFICATION",
  "PROCESSING",
  "EMBEDDING",
  "MICROTOMY",
  "STAINING",
  "COVERSLIPPING",
  "READY_PATHOLOGIST",
  "UNDER_REVIEW",
  "COMPLETED",
] as const;

export type PathologyStageId = (typeof PATHOLOGY_STAGE_ORDER)[number];

/** The description moveCaseToStage writes when it has to finish grossing. */
const SETUP_DESCRIPTION = "Seeded by E2E";

/** The level moveCaseToStage gives the slide it cuts at Microtomy. */
const SETUP_SLIDE_LEVEL = "1";

/** The reason moveCaseToStage gives when it returns a case. */
const SETUP_RETURN_REASON = "E2E setup";

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

/** One dictionary entry a picker on the case view offers: its id and name. */
export interface DictionaryEntry {
  id: string;
  name: string;
}

/** One tissue type a cassette can be given, as the case view offers it. */
export type TissueType = DictionaryEntry;

/** One stain a slide can be given, as the case view offers it. */
export type Stain = DictionaryEntry;

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
export async function apiGet<T>(page: Page, path: string): Promise<T> {
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
export async function apiPost(
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
 * A new entry of this run's own in one dictionary category, added through the
 * endpoint the Dictionary admin screen posts to and read back through the list
 * the case view loads, so what is returned is what its picker will offer
 * rather than what was posted. `missing` says why the category would be
 * absent on a deployment.
 */
async function addRunEntry(
  page: Page,
  categoryName: string,
  entryPrefix: string,
  missing: string,
): Promise<DictionaryEntry> {
  if (!page.url().startsWith("http")) {
    await page.goto("/", { waitUntil: "domcontentloaded" });
  }

  const categories = await apiGet<DictionaryCategoryRow[]>(
    page,
    `${API_PREFIX}/rest/dictionary-categories`,
  );
  const category = categories.find(
    (entry) => entry.categoryName === categoryName,
  );
  if (!category) {
    throw new Error(
      `No dictionary category named ${categoryName} on this deployment, ${missing}`,
    );
  }

  const runTag = `${Date.now().toString(36)}${Math.floor(Math.random() * 36 * 36).toString(36)}`;
  const dictEntry = `${entryPrefix} ${runTag}`;
  const localAbbreviation = `E${runTag}`
    .slice(-LOCAL_ABBREVIATION_MAX_LENGTH)
    .toUpperCase();

  await apiPost(page, `${API_PREFIX}/rest/Dictionary`, {
    id: "",
    selectedDictionaryCategoryId: category.id,
    dictEntry,
    localAbbreviation,
    isActive: "Y",
    dirtyFormFields: "",
  });

  const listPath = `${API_PREFIX}/rest/dictionary/category/${encodeURIComponent(categoryName)}`;
  let added: DictionaryEntry | undefined;
  await expect
    .poll(
      async () => {
        const after = await apiGet<IdValuePair[]>(page, listPath);
        const entry = after.find((item) => item.value === dictEntry);
        added = entry ? { id: String(entry.id), name: entry.value } : undefined;
        return Boolean(added);
      },
      {
        message: `${dictEntry} was saved but ${listPath} does not list it`,
        intervals: [READ_BACK_INTERVAL_MS],
        timeout: READ_BACK_TIMEOUT_MS,
      },
    )
    .toBe(true);

  return added!;
}

/**
 * A tissue type of this run's own, added to the laboratory's tissue category.
 *
 * Each call adds a new entry with a name no other run uses, so a spec picks a
 * tissue it created rather than whichever entry the shared category happens
 * to list first, which another spec or an administrator may retire or rename
 * at any time. The entry is never deleted afterwards, since a cassette that
 * holds it keeps pointing at it; a retired entry stays valid on the cassettes
 * that recorded it.
 */
export async function ensureTissueType(page: Page): Promise<TissueType> {
  return addRunEntry(
    page,
    TISSUE_TYPE_CATEGORY,
    "E2E tissue",
    "so the changeset that creates it has not run here.",
  );
}

/**
 * A stain of this run's own, added to the laboratory's stain category, for
 * the same reasons as ensureTissueType: the shipped stains are the
 * laboratory's to rename or retire, and a slide that recorded one keeps it.
 */
export async function ensureStain(page: Page): Promise<Stain> {
  return addRunEntry(
    page,
    STAIN_CATEGORY,
    "E2E stain",
    "so the application never loaded its shipped dictionary files " +
      "(configuration/backend/dictionaries/pathology-stains.csv).",
  );
}

/** One cassette or block as the case view reads it. */
interface CaseBlock {
  id?: number | string;
  designation?: string;
  barcode?: string;
  cassetteState?: string;
  active?: boolean;
  location?: string;
  tissueTypeId?: string | null;
}

/** One slide as the case view reads it. */
export interface CaseSlide {
  id?: number | string;
  blockId?: number | string | null;
  designation?: string;
  barcode?: string;
  level?: string | null;
  stainId?: string | null;
  active?: boolean;
  location?: string;
}

/** A slide label's check against its block, as the case view reads it. */
export interface SlideLabelVerification {
  verifiedAt?: string;
  verifiedBy?: string;
}

/** The parts of a stored case the stage moves below read and post back. */
export interface StoredPathologyCase {
  status: PathologyStageId;
  nextStage?: PathologyStageId | null;
  previousStage?: PathologyStageId | null;
  grossExam?: string | null;
  decalcificationRequired?: boolean;
  microscopyExam?: string | null;
  conclusionText?: string | null;
  assignedTechnicianId?: string | null;
  assignedPathologistId?: string | null;
  blocks?: CaseBlock[];
  slides?: CaseSlide[];
  /** Who checked each slide's label against its block and when, by slide id. */
  labelVerifications?: Record<string, SlideLabelVerification | null>;
  reports?: unknown[];
  techniques?: { id?: string }[];
  requests?: { id?: string; status?: string }[];
  conclusions?: { id?: string }[];
}

function casePath(caseId: string) {
  return `${API_PREFIX}/rest/pathology/caseView/${caseId}`;
}

/** The case as the server stores it, read the way the case view reads it. */
export async function readCase(
  page: Page,
  caseId: string,
): Promise<StoredPathologyCase> {
  return apiGet<StoredPathologyCase>(page, casePath(caseId));
}

/**
 * Move a case one stage on, or one stage back with a reason, through the
 * endpoints the case view posts to, naming the stage it is moved from. Throws
 * with the server's body when the move is refused. Returns the stage the case
 * now stands at.
 */
export async function moveCaseOnce(
  page: Page,
  caseId: string,
  direction: "advance" | "return",
  from: PathologyStageId,
  reason?: string,
): Promise<PathologyStageId> {
  const answer = await apiPost(
    page,
    `${casePath(caseId)}/${direction}`,
    direction === "return" ? { from, reason } : { from },
    `Moving case ${caseId} (${direction}) from ${from}`,
  );
  const status = (JSON.parse(answer) as { status?: PathologyStageId }).status;
  if (!status) {
    throw new Error(
      `Moving case ${caseId} (${direction}) from ${from} answered no stage: ${answer.substring(0, 400)}`,
    );
  }
  return status;
}

/** The changes saveCaseThroughApi applies on top of the stored case. */
export interface CaseSaveChanges {
  grossExam?: string;
  /** Cut one new cassette. */
  addCassette?: boolean;
  /** The tissue type the new cassette is given. */
  cassetteTissueTypeId?: string;
  /** Cut one new slide (level 1, no stain) under the block with this id. */
  addSlideUnder?: number | string;
}

/**
 * Save the case through the same endpoint and with the same form the case
 * view's Save draft posts, built from the stored case, with the changes
 * given applied on top.
 */
export async function saveCaseThroughApi(
  page: Page,
  caseId: string,
  stored: StoredPathologyCase,
  changes: CaseSaveChanges,
) {
  const blocks = [...(stored.blocks ?? [])];
  if (changes.addCassette) {
    blocks.push({
      location: "",
      tissueTypeId: changes.cassetteTissueTypeId ?? null,
    });
  }
  const slides = [...(stored.slides ?? [])];
  if (changes.addSlideUnder !== undefined) {
    slides.push({
      blockId: changes.addSlideUnder,
      level: SETUP_SLIDE_LEVEL,
      stainId: null,
      location: "",
    });
  }
  await apiPost(
    page,
    casePath(caseId),
    {
      assignedTechnicianId: stored.assignedTechnicianId,
      assignedPathologistId: stored.assignedPathologistId,
      blocks,
      slides,
      reports: stored.reports ?? [],
      grossExam: changes.grossExam ?? stored.grossExam,
      decalcificationRequired: stored.decalcificationRequired ?? false,
      microscopyExam: stored.microscopyExam,
      conclusionText: stored.conclusionText,
      release: false,
      referToImmunoHistoChemistry: false,
      techniques: (stored.techniques ?? []).map((entry) => entry.id),
      requests: (stored.requests ?? []).map((entry) => ({
        value: entry.id,
        status: entry.status,
      })),
      conclusions: (stored.conclusions ?? []).map((entry) => entry.id),
    },
    `Saving case ${caseId}`,
  );
}

/**
 * Check the label of every slide of the case that still waits for it, through
 * the endpoint the microtomy section posts to, with the block's and the
 * slide's barcodes as the server stores them: what a scanner reads off a
 * block and off the slide label printed for it. A slide whose label was
 * already checked, a deactivated slide, and a slide that names no block are
 * left as they are, since none of them holds a case at microtomy. Throws with
 * the server's body when a check is refused or does not match. Returns the
 * barcodes of the slides it checked.
 */
export async function verifySlideLabels(
  page: Page,
  seeded: Pick<SeededPathologyCase, "pathologySampleId">,
): Promise<string[]> {
  const caseId = seeded.pathologySampleId;
  const stored = await readCase(page, caseId);
  const blocks = new Map(
    (stored.blocks ?? []).map((block) => [String(block.id), block]),
  );
  const verified: string[] = [];
  for (const slide of stored.slides ?? []) {
    if (
      slide.id == null ||
      slide.active === false ||
      slide.blockId == null ||
      stored.labelVerifications?.[String(slide.id)]
    ) {
      continue;
    }
    const block = blocks.get(String(slide.blockId));
    if (!block?.barcode || !slide.barcode) {
      throw new Error(
        `Slide ${slide.id} of case ${caseId} or its block carries no barcode to scan.`,
      );
    }
    await apiPost(
      page,
      `${API_PREFIX}/rest/pathology/slide/${slide.id}/verify-label`,
      { block: block.barcode, slide: slide.barcode },
      `Verifying the label of slide ${slide.barcode} of case ${caseId}`,
    );
    verified.push(slide.barcode);
  }
  return verified;
}

/**
 * Bring a case to a stage for a test's setup, doing the bench's work on the
 * way rather than going round it: the server refuses to send a case on from
 * grossing until it is described and has a cassette, from embedding until
 * every cassette is a block, and from microtomy until a slide is cut and
 * every slide's label is checked against its block, so a blank description
 * is filled in and a cassette cut through the case save, each outstanding
 * cassette is embedded through its own endpoint, and one slide is cut under
 * the first block through the case save and every label checked through
 * verifySlideLabels, exactly as the screen would. Nothing is done at the target stage itself, so a case brought
 * to microtomy arrives there with no slide. A target
 * behind the case is reached by returning it, one stage at a time, with a
 * reason.
 *
 * A stage the laboratory does not track is skipped by the server, so asking
 * for one fails, naming it, rather than leaving the case somewhere else. The
 * page has to be on the application, since the calls carry its session; the
 * screen it shows is not read again here, so a caller showing the case opens
 * it afresh afterwards.
 */
export async function moveCaseToStage(
  page: Page,
  seeded: Pick<SeededPathologyCase, "pathologySampleId">,
  stageId: PathologyStageId,
): Promise<void> {
  const caseId = seeded.pathologySampleId;
  const target = PATHOLOGY_STAGE_ORDER.indexOf(stageId);
  const position = (stage: PathologyStageId) =>
    PATHOLOGY_STAGE_ORDER.indexOf(stage);

  for (let step = 0; step <= PATHOLOGY_STAGE_ORDER.length; step += 1) {
    const stored = await readCase(page, caseId);
    const here = position(stored.status);
    if (here === target) {
      return;
    }

    if (here > target) {
      const now = await moveCaseOnce(
        page,
        caseId,
        "return",
        stored.status,
        SETUP_RETURN_REASON,
      );
      if (position(now) < target) {
        throw new Error(
          `Returning case ${caseId} from ${stored.status} went to ${now}, past ${stageId}: ` +
            `${stageId} is not a stage this case can stand at here.`,
        );
      }
      continue;
    }

    if (!stored.nextStage) {
      throw new Error(
        `Case ${caseId} stands at ${stored.status}, and the server offers no stage after it, ` +
          `so it cannot be brought to ${stageId}.`,
      );
    }

    if (stored.status === "GROSSING") {
      const inUse = (stored.blocks ?? []).filter(
        (block) => block.active !== false,
      );
      const described = (stored.grossExam ?? "").trim() !== "";
      if (!described || inUse.length === 0) {
        await saveCaseThroughApi(page, caseId, stored, {
          grossExam: described ? undefined : SETUP_DESCRIPTION,
          addCassette: inUse.length === 0,
        });
      }
    }

    if (stored.status === "EMBEDDING") {
      const outstanding = (stored.blocks ?? []).filter(
        (block) => block.active !== false && block.cassetteState !== "BLOCK",
      );
      for (const block of outstanding) {
        await apiPost(
          page,
          `${API_PREFIX}/rest/pathology/block/${block.id}/embed`,
          {},
          `Embedding cassette ${block.designation} of case ${caseId}`,
        );
      }
    }

    if (stored.status === "MICROTOMY") {
      const slideInUse = (stored.slides ?? []).some(
        (slide) => slide.id != null && slide.active !== false,
      );
      if (!slideInUse) {
        const inUse = (stored.blocks ?? []).filter(
          (block) => block.id != null && block.active !== false,
        );
        // Where the laboratory tracks embedding a slide is cut only from a
        // block; where it does not, no row ever becomes one.
        const parent =
          inUse.find((block) => block.cassetteState === "BLOCK") ?? inUse[0];
        if (!parent) {
          throw new Error(
            `Case ${caseId} stands at MICROTOMY with no block to cut a slide from.`,
          );
        }
        await saveCaseThroughApi(page, caseId, stored, {
          addSlideUnder: parent.id,
        });
      }
      await verifySlideLabels(page, seeded);
    }

    const now = await moveCaseOnce(page, caseId, "advance", stored.status);
    if (position(now) > target) {
      throw new Error(
        `Sending case ${caseId} on from ${stored.status} went to ${now}, past ${stageId}: ` +
          `the laboratory does not track ${stageId}, or the specimen was not flagged for it.`,
      );
    }
  }

  throw new Error(
    `Case ${caseId} did not reach ${stageId} within the stage count.`,
  );
}
