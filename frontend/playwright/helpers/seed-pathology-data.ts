import { Page, expect } from "@playwright/test";
import { existingPatientProperties } from "./program-order";

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

/** The dictionary categories the findings pickers read, by category name. */
export const DIAGNOSIS_CATEGORY = "pathologist_conclusions";
export const TECHNIQUE_CATEGORY = "pathology_techniques";

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

/** The reason reopenCase gives when it is not told one. */
const SETUP_REOPEN_REASON = "E2E setup";

/** The microscopic description signOutCase writes when the case has none. */
const SETUP_MICROSCOPY = "Seeded by E2E, microscopic";

/** The narrative conclusion signOutCase writes when the case has none. */
const SETUP_CONCLUSION = "Seeded by E2E, conclusion";

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
 * made here. Pass `existingPatientCaseId`, the id of a pathology case, to place
 * the order for that case's patient instead of a new one. The case that comes
 * back is looked up on the pathology dashboard, so this places pathology
 * orders only; the other benches' orders go through createProgramOrder.
 */
export async function createPathologyCase(
  page: Page,
  target?: PathologyOrderTarget,
  options: { existingPatientCaseId?: string } = {},
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
  const patientProperties = options.existingPatientCaseId
    ? await existingPatientProperties(page, options.existingPatientCaseId)
    : {
        patientPK: "",
        patientUpdateStatus: "ADD",
        firstName: "Pathology",
        lastName: "Casetest",
        gender: "M",
        birthDateForDisplay: "01/01/1990",
        nationalId: uniqueId,
        subjectNumber: uniqueId,
      };
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
    patientProperties,
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
 * A new entry with the given name and local abbreviation (at most ten
 * characters, unique in the laboratory) in one dictionary category, added
 * through the endpoint the Dictionary admin screen posts to and read back
 * through the list the case view loads, so what is returned is what its
 * picker will offer rather than what was posted. `missing` says why the
 * category would be absent on a deployment.
 */
export async function createDictionaryEntry(
  page: Page,
  categoryName: string,
  dictEntry: string,
  localAbbreviation: string,
  missing = "so nothing has created it here.",
): Promise<DictionaryEntry> {
  if (localAbbreviation.length > LOCAL_ABBREVIATION_MAX_LENGTH) {
    throw new Error(
      `The abbreviation "${localAbbreviation}" is longer than ${LOCAL_ABBREVIATION_MAX_LENGTH} characters.`,
    );
  }
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
 * A new entry of this run's own in one dictionary category: a name and an
 * abbreviation no other run uses.
 */
async function addRunEntry(
  page: Page,
  categoryName: string,
  entryPrefix: string,
  missing: string,
): Promise<DictionaryEntry> {
  const runTag = `${Date.now().toString(36)}${Math.floor(Math.random() * 36 * 36).toString(36)}`;
  const localAbbreviation = `E${runTag}`
    .slice(-LOCAL_ABBREVIATION_MAX_LENGTH)
    .toUpperCase();
  return createDictionaryEntry(
    page,
    categoryName,
    `${entryPrefix} ${runTag}`,
    localAbbreviation,
    missing,
  );
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
  /** STAINED or UNSATISFACTORY once staining recorded one, else null. */
  stainStatus?: string | null;
  /** PASS or FAIL once the coverslip was checked, else null. */
  coverslipQc?: string | null;
  active?: boolean;
  location?: string;
}

/** A slide label's check against its block, as the case view reads it. */
export interface SlideLabelVerification {
  verifiedAt?: string;
  verifiedBy?: string;
}

/** Who recorded a slide's stain status or coverslip QC, and when. */
export interface SlideStamp {
  at?: string;
  by?: string;
}

/** The parts of a stored case the stage moves below read and post back. */
export interface StoredPathologyCase {
  status: PathologyStageId;
  nextStage?: PathologyStageId | null;
  previousStage?: PathologyStageId | null;
  /**
   * The stage the findings are written at: Under Pathologist Review where
   * the laboratory uses it, else Ready for Pathologist. A server that does
   * not send it has the findings written at Ready for Pathologist.
   */
  readingStage?: PathologyStageId | null;
  /** Whether the findings can be changed now. */
  findingsOpen?: boolean;
  grossExam?: string | null;
  decalcificationRequired?: boolean;
  microscopyExam?: string | null;
  conclusionText?: string | null;
  reviewNotes?: string | null;
  assignedTechnicianId?: string | null;
  assignedPathologistId?: string | null;
  blocks?: CaseBlock[];
  slides?: CaseSlide[];
  /** Who checked each slide's label against its block and when, by slide id. */
  labelVerifications?: Record<string, SlideLabelVerification | null>;
  /** Who marked each slide's stain status and when, by slide id. */
  stainStamps?: Record<string, SlideStamp | null>;
  /** Who checked each slide's coverslip and when, by slide id. */
  coverslipQcStamps?: Record<string, SlideStamp | null>;
  /** The coverslipping batch written on the case's entry into the stage. */
  coverslipRunReference?: string | null;
  /** Whether the batch can still be written: once, while the case is there. */
  coverslipRunReferenceEditable?: boolean;
  /** The report versions that hold a file, newest first. */
  reports?: CaseReport[];
  /** Whether both descriptions and a conclusion are written. */
  canGenerateReport?: boolean;
  signedOut?: boolean;
  criticalResultFlagged?: boolean;
  /** The case's critical result as the read answers it. */
  criticalResult?: unknown;
  techniques?: { id?: string }[];
  /** The bench requests, newest first. */
  requests?: CaseRequest[];
  conclusions?: { id?: string }[];
}

/** One stored report version as the case read and the report endpoints send it. */
export interface CaseReport {
  id: number;
  version: number;
  type: "DRAFT" | "FINAL" | "EXTERNAL";
  state: "CURRENT" | "SUPERSEDED" | "VOIDED";
  generatedAt?: string;
  generatedBy?: string;
  fileType?: string;
}

/** What a bench request asks for: another block, slide or stain. */
export type RequestKind = "BLOCK" | "SLIDE" | "STAIN";

/** How urgently a bench request is wanted. */
export type RequestPriority = "NORMAL" | "URGENT" | "STAT";

/** Where a bench request stands. */
export type RequestStatus = "OPENED" | "COMPLETED" | "CANCELLED";

/**
 * One bench request as the case view reads it. A request on file from
 * before requests had a kind carries none, nor a target, priority, author or
 * date.
 */
export interface CaseRequest {
  id: number;
  kind?: RequestKind | null;
  targetBlockId?: number | null;
  targetSlideId?: number | null;
  /** The target's designation as printed (A1, A1.1). */
  targetName?: string | null;
  stainId?: string | null;
  stainName?: string | null;
  priority?: RequestPriority | null;
  instruction: string;
  status: RequestStatus;
  cancelReason?: string | null;
  resultBlockId?: number | null;
  resultSlideId?: number | null;
  resultName?: string | null;
  raisedAt?: string | null;
  raisedBy?: string | null;
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

/** How a move treats the bench requests still open on the case. */
export interface MoveOptions {
  /**
   * The open requests the person sending the case on has seen and sends it
   * with, as the case view's Send anyway names them. The server refuses to
   * send a case to the pathologist while any open request is missing here.
   */
  acknowledgedRequestIds?: number[];
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
  options: MoveOptions = {},
): Promise<PathologyStageId> {
  const body =
    direction === "return"
      ? { from, reason }
      : options.acknowledgedRequestIds
        ? { from, acknowledgedRequestIds: options.acknowledgedRequestIds }
        : { from };
  const answer = await apiPost(
    page,
    `${casePath(caseId)}/${direction}`,
    body,
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
  microscopyExam?: string;
  conclusionText?: string;
  reviewNotes?: string;
  /** Flag the case critical (true), clear the flag (false), or keep it (left out). */
  criticalResultFlagged?: boolean;
  /** The pathologist the case is saved with, a user id. */
  assignedPathologistId?: string;
  /** Sign the case out with this save. */
  release?: boolean;
  /** Cut one new cassette. */
  addCassette?: boolean;
  /** The tissue type the new cassette is given. */
  cassetteTissueTypeId?: string;
  /** Cut one new slide (level 1, no stain) under the block with this id. */
  addSlideUnder?: number | string;
  /** The stain to save on existing slides, by slide id. */
  slideStains?: Record<string, string>;
}

/**
 * Save the case through the same endpoint and with the same form the case
 * view's Save draft posts, built from the stored case, with the changes
 * given applied on top. Bench requests are not part of the form: each is
 * raised and changed through its own endpoint (raiseRequest,
 * changeRequestStatus), and a save leaves them as they are.
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
  const slides = (stored.slides ?? []).map((slide) => {
    const stainId = changes.slideStains?.[String(slide.id)];
    return stainId === undefined ? slide : { ...slide, stainId };
  });
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
      assignedPathologistId:
        changes.assignedPathologistId ?? stored.assignedPathologistId,
      blocks,
      slides,
      grossExam: changes.grossExam ?? stored.grossExam,
      decalcificationRequired: stored.decalcificationRequired ?? false,
      microscopyExam: changes.microscopyExam ?? stored.microscopyExam,
      conclusionText: changes.conclusionText ?? stored.conclusionText,
      reviewNotes: changes.reviewNotes ?? stored.reviewNotes,
      release: changes.release ?? false,
      criticalResultFlagged: changes.criticalResultFlagged,
      referToImmunoHistoChemistry: false,
      techniques: (stored.techniques ?? []).map((entry) => entry.id),
      conclusions: (stored.conclusions ?? []).map((entry) => entry.id),
    },
    `Saving case ${caseId}`,
  );
}

/** One bench request to raise, as the new-request row posts it. */
export interface RequestDraft {
  kind: RequestKind;
  /** The block a slide request is cut from. */
  targetBlockId?: number | string;
  /** The slide a stain request is stained on. */
  targetSlideId?: number | string;
  /** The stain a stain request asks for, a Pathology Stains entry id. */
  stainId?: string;
  /** Normal when left out. */
  priority?: RequestPriority;
  instruction: string;
}

/** The answer of both request endpoints: the request and where it stands. */
interface RequestAnswer {
  id?: number;
  status?: RequestStatus;
}

const idOrNull = (value: number | string | undefined) =>
  value === undefined || value === "" ? null : Number(value);

/**
 * Raise a bench request on a case through the endpoint the Review section's
 * Raise request posts to, with the same body. The server takes one at any
 * stage before the case is signed out, stamped with the signed-in user and
 * its own clock. Throws with the server's body when it is refused. Returns
 * the new request's id.
 */
export async function raiseRequest(
  page: Page,
  seeded: Pick<SeededPathologyCase, "pathologySampleId">,
  draft: RequestDraft,
): Promise<number> {
  const caseId = seeded.pathologySampleId;
  const answer = await apiPost(
    page,
    `${casePath(caseId)}/requests`,
    {
      kind: draft.kind,
      targetBlockId: idOrNull(draft.targetBlockId),
      targetSlideId: idOrNull(draft.targetSlideId),
      stainId: draft.stainId ?? null,
      priority: draft.priority ?? "NORMAL",
      instruction: draft.instruction,
    },
    `Raising a ${draft.kind} request on case ${caseId}`,
  );
  const id = (JSON.parse(answer) as RequestAnswer).id;
  if (id == null) {
    throw new Error(
      `Raising a request on case ${caseId} answered no id: ${answer.substring(0, 400)}`,
    );
  }
  return id;
}

/** One status change of a bench request. */
export interface RequestStatusChange {
  status: RequestStatus;
  /**
   * The status the change is made from. Left out, it is read from the case,
   * which is what a page showing the request afresh would send.
   */
  from?: RequestStatus;
  /** Required with CANCELLED. */
  cancelReason?: string;
  /** What fulfilled a completed block request. */
  resultBlockId?: number | string;
  /** What fulfilled a completed slide or stain request. */
  resultSlideId?: number | string;
}

/**
 * Complete, cancel or reopen a bench request through the endpoint the
 * requests table posts to, with the same body: the status the change is made
 * from, the new one, a reason only with a cancellation, and what fulfilled a
 * completed request. Throws with the server's body when it is refused, a
 * request changed elsewhere since `from` included (HTTP 409). Returns the
 * status the request now has.
 */
export async function changeRequestStatus(
  page: Page,
  seeded: Pick<SeededPathologyCase, "pathologySampleId">,
  requestId: number,
  change: RequestStatusChange,
): Promise<RequestStatus> {
  const caseId = seeded.pathologySampleId;
  let from = change.from;
  if (!from) {
    const stored = await readCase(page, caseId);
    const request = (stored.requests ?? []).find(
      (entry) => entry.id === requestId,
    );
    if (!request) {
      throw new Error(`Case ${caseId} holds no request ${requestId}.`);
    }
    from = request.status;
  }
  const answer = await apiPost(
    page,
    `${casePath(caseId)}/requests/${requestId}/status`,
    {
      from,
      status: change.status,
      cancelReason:
        change.status === "CANCELLED" ? (change.cancelReason ?? null) : null,
      resultBlockId:
        change.status === "COMPLETED" ? idOrNull(change.resultBlockId) : null,
      resultSlideId:
        change.status === "COMPLETED" ? idOrNull(change.resultSlideId) : null,
    },
    `Changing request ${requestId} of case ${caseId} from ${from} to ${change.status}`,
  );
  const status = (JSON.parse(answer) as RequestAnswer).status;
  if (!status) {
    throw new Error(
      `Changing request ${requestId} of case ${caseId} answered no status: ${answer.substring(0, 400)}`,
    );
  }
  return status;
}

/** The ids of the requests of a case still open. */
export function openRequestIds(stored: StoredPathologyCase): number[] {
  return (stored.requests ?? [])
    .filter((request) => request.status === "OPENED")
    .map((request) => request.id);
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

/** The slides of a case that are saved and in use. */
function slidesInUse(stored: StoredPathologyCase): CaseSlide[] {
  return (stored.slides ?? []).filter(
    (slide) => slide.id != null && slide.active !== false,
  );
}

/**
 * The stain this worker gives a slide that has none before marking it, added
 * once by ensureStain and reused, so a run adds one setup entry rather than
 * one per case.
 */
let setupStain: Stain | undefined;

/**
 * Whether the laboratory tracks staining, as the authenticated configuration
 * the case view boots with says. Where it does not, no slide is ever marked
 * stained and its coverslip is checked without.
 */
async function stainingTracked(page: Page): Promise<boolean> {
  const config = await apiGet<Record<string, string>>(
    page,
    `${API_PREFIX}/rest/configuration-properties`,
  );
  return (
    String(config.PATHOLOGY_STAGE_STAINING_ENABLED ?? "true")
      .trim()
      .toLowerCase() !== "false"
  );
}

/** Which slides a mark is given to: some by id, or all those it applies to. */
export interface SlideWorkOptions {
  /** Only these slides; by default every slide in use the mark changes. */
  slideIds?: (number | string)[];
}

/**
 * Record what staining found for slides of a case, by slide id, through the
 * endpoint the staining section posts to, exactly as one row's action or a
 * batch does. The server takes it only while the case stands at Staining or
 * Coverslipping, and a slide marked STAINED must carry a stain. Throws with
 * the server's body when the mark is refused.
 */
export async function recordStainStatus(
  page: Page,
  seeded: Pick<SeededPathologyCase, "pathologySampleId">,
  slideIds: (number | string)[],
  status: "STAINED" | "UNSATISFACTORY",
): Promise<void> {
  const caseId = seeded.pathologySampleId;
  await apiPost(
    page,
    `${casePath(caseId)}/slides/stain-status`,
    { slideIds: slideIds.map(Number), status },
    `Marking slides ${slideIds.join(", ")} of case ${caseId} ${status}`,
  );
}

/**
 * Record how the coverslip check of slides of a case went, by slide id,
 * through the endpoint the coverslipping section posts to. The server takes
 * it only while the case stands at Coverslipping. Throws with the server's
 * body when the result is refused.
 */
export async function recordCoverslipQc(
  page: Page,
  seeded: Pick<SeededPathologyCase, "pathologySampleId">,
  slideIds: (number | string)[],
  result: "PASS" | "FAIL",
): Promise<void> {
  const caseId = seeded.pathologySampleId;
  await apiPost(
    page,
    `${casePath(caseId)}/slides/coverslip-qc`,
    { slideIds: slideIds.map(Number), result },
    `Recording coverslip QC ${result} for slides ${slideIds.join(", ")} of case ${caseId}`,
  );
}

/**
 * Mark slides of a case stained through the endpoint the staining section
 * posts to, as the bench does once the stain is on the slide. A slide with no
 * stain saved is first given this worker's own stain through the case save,
 * since the server marks a slide stained only with the stain it carries.
 * By default every slide in use not yet stained is marked; slides already
 * stained are left as they are. The server takes the mark only while the case
 * stands at Staining or Coverslipping. Returns the barcodes of the slides it
 * marked.
 */
export async function stainSlides(
  page: Page,
  seeded: Pick<SeededPathologyCase, "pathologySampleId">,
  options: SlideWorkOptions = {},
): Promise<string[]> {
  const caseId = seeded.pathologySampleId;
  let stored = await readCase(page, caseId);
  const wanted = options.slideIds?.map(String);
  const chosen = () =>
    slidesInUse(stored).filter((slide) =>
      wanted
        ? wanted.includes(String(slide.id))
        : slide.stainStatus !== "STAINED",
    );

  const unstained = chosen().filter((slide) => !slide.stainId);
  if (unstained.length > 0) {
    setupStain ??= await ensureStain(page);
    await saveCaseThroughApi(page, caseId, stored, {
      slideStains: Object.fromEntries(
        unstained.map((slide) => [String(slide.id), setupStain!.id]),
      ),
    });
    stored = await readCase(page, caseId);
  }

  const slides = chosen();
  if (slides.length > 0) {
    await recordStainStatus(
      page,
      seeded,
      slides.map((slide) => slide.id!),
      "STAINED",
    );
  }
  return slides.map((slide) => slide.barcode ?? String(slide.id));
}

/**
 * Pass the coverslip QC of slides of a case through the endpoint the
 * coverslipping section posts to. Where the laboratory tracks staining a
 * slide's coverslip is checked only once it is stained, so any slide that is
 * not is marked stained first through stainSlides. By default every slide in
 * use whose QC has not passed is passed. The server takes the result only
 * while the case stands at Coverslipping. Returns the barcodes of the slides
 * it passed.
 */
export async function passCoverslipQc(
  page: Page,
  seeded: Pick<SeededPathologyCase, "pathologySampleId">,
  options: SlideWorkOptions = {},
): Promise<string[]> {
  const caseId = seeded.pathologySampleId;
  const wanted = options.slideIds?.map(String);
  const chosen = (stored: StoredPathologyCase) =>
    slidesInUse(stored).filter((slide) =>
      wanted ? wanted.includes(String(slide.id)) : slide.coverslipQc !== "PASS",
    );

  let stored = await readCase(page, caseId);
  if (await stainingTracked(page)) {
    const notStained = chosen(stored).filter(
      (slide) => slide.stainStatus !== "STAINED",
    );
    if (notStained.length > 0) {
      await stainSlides(page, seeded, {
        slideIds: notStained.map((slide) => slide.id!),
      });
      stored = await readCase(page, caseId);
    }
  }

  const slides = chosen(stored);
  if (slides.length > 0) {
    await recordCoverslipQc(
      page,
      seeded,
      slides.map((slide) => slide.id!),
      "PASS",
    );
  }
  return slides.map((slide) => slide.barcode ?? String(slide.id));
}

/**
 * Bring a case to a stage for a test's setup, doing the bench's work on the
 * way rather than going round it: the server refuses to send a case on from
 * grossing until it is described and has a cassette, from embedding until
 * every cassette is a block, from microtomy until a slide is cut and every
 * slide's label is checked against its block, from staining until every
 * slide is stained, and from coverslipping until every slide's coverslip
 * passed QC. So a blank description is filled in and a cassette cut through
 * the case save, each outstanding cassette is embedded through its own
 * endpoint, one slide is cut under the first block through the case save and
 * every label checked through verifySlideLabels, every slide is marked
 * stained through stainSlides and passed through passCoverslipQc, exactly as
 * the screen would. Nothing is done at the target stage itself, so a case
 * brought to microtomy arrives there with no slide, and one brought to
 * coverslipping with its slides stained but not yet checked. A target
 * behind the case is reached by returning it, one stage at a time, with a
 * reason.
 *
 * A stage the laboratory does not track is skipped by the server, so asking
 * for one fails, naming it, rather than leaving the case somewhere else. The
 * page has to be on the application, since the calls carry its session; the
 * screen it shows is not read again here, so a caller showing the case opens
 * it afresh afterwards.
 *
 * The review begins (Ready for Pathologist to Under Pathologist Review)
 * only where the case read offers it as the next stage; a server without the
 * review stage offers none, and the case stops at Ready for Pathologist.
 * Completed is not reached by a move: see signOutCase.
 *
 * The server sends a case with open bench requests to the pathologist only
 * when the move names every one of them. By default nothing is named, so
 * such a case is refused there, as the screen refuses it until Send anyway;
 * with `acknowledgeOpenRequests` the move onto Ready for Pathologist names
 * every request open at that moment.
 */
export async function moveCaseToStage(
  page: Page,
  seeded: Pick<SeededPathologyCase, "pathologySampleId">,
  stageId: PathologyStageId,
  {
    acknowledgeOpenRequests = false,
  }: { acknowledgeOpenRequests?: boolean } = {},
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
      const signOut =
        stageId === "COMPLETED"
          ? " A case is signed out by a save that releases it: use signOutCase."
          : "";
      throw new Error(
        `Case ${caseId} stands at ${stored.status}, and the server offers no stage after it, ` +
          `so it cannot be brought to ${stageId}.${signOut}`,
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

    // The marks are taken only while the case stands at these stages, so
    // they are made here, before the case is sent on, never after.
    if (stored.status === "STAINING") {
      await stainSlides(page, seeded);
    }

    if (stored.status === "COVERSLIPPING") {
      await passCoverslipQc(page, seeded);
    }

    const acknowledged =
      acknowledgeOpenRequests && stored.nextStage === "READY_PATHOLOGIST"
        ? { acknowledgedRequestIds: openRequestIds(stored) }
        : {};
    const now = await moveCaseOnce(
      page,
      caseId,
      "advance",
      stored.status,
      undefined,
      acknowledged,
    );
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

/** What signOutCase may be told about the sign-out. */
export interface SignOutOptions {
  /** The pathologist to sign out as, a user id; the case's own when left out. */
  pathologistId?: string;
  /** The microscopic description written when the case has none. */
  microscopyExam?: string;
  /** The narrative conclusion written when the case has no conclusion. */
  conclusionText?: string;
}

/**
 * Sign a case out the way the case view does: bring it to the stage its
 * findings are written at (the case's `readingStage`, Ready for Pathologist
 * when the server sends none), write the gross and microscopic descriptions
 * through a save when either is blank, then save with the release, which
 * lands the case at Completed. No move reaches Completed. A case with no
 * conclusion gets a narrative one with the descriptions. Open bench requests
 * are not closed: a case that holds one is refused, and a spec that needs the
 * sign-out cancels them first. A case already signed out is left as it is. Throws with the server's body when a step is
 * refused.
 */
export async function signOutCase(
  page: Page,
  seeded: Pick<SeededPathologyCase, "pathologySampleId">,
  { pathologistId, microscopyExam, conclusionText }: SignOutOptions = {},
): Promise<void> {
  const caseId = seeded.pathologySampleId;
  const first = await readCase(page, caseId);
  if (first.status === "COMPLETED" && first.findingsOpen !== true) {
    return;
  }
  if (first.status !== "COMPLETED") {
    await moveCaseToStage(
      page,
      seeded,
      first.readingStage ?? "READY_PATHOLOGIST",
    );
  }

  const reading = await readCase(page, caseId);
  const grossBlank = (reading.grossExam ?? "").trim() === "";
  const microscopyBlank = (reading.microscopyExam ?? "").trim() === "";
  const conclusionBlank =
    (reading.conclusions ?? []).length === 0 &&
    (reading.conclusionText ?? "").trim() === "";
  if (grossBlank || microscopyBlank || conclusionBlank) {
    await saveCaseThroughApi(page, caseId, reading, {
      grossExam: grossBlank ? SETUP_DESCRIPTION : undefined,
      microscopyExam: microscopyBlank
        ? (microscopyExam ?? SETUP_MICROSCOPY)
        : undefined,
      conclusionText: conclusionBlank
        ? (conclusionText ?? SETUP_CONCLUSION)
        : undefined,
      assignedPathologistId: pathologistId,
    });
  }

  await saveCaseThroughApi(page, caseId, await readCase(page, caseId), {
    assignedPathologistId: pathologistId,
    release: true,
  });
}

/** A stored file the upload helper sends: a 1x1 PNG unless told otherwise. */
export interface ReportUploadFile {
  /** The media type, one of application/pdf, image/png or image/jpeg. */
  mimeType?: string;
  /** The file's bytes, base64 encoded. */
  base64?: string;
}

const SETUP_UPLOAD_PNG =
  "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";

/**
 * Generate the next report version of a case through the endpoint the Reports
 * tab posts to: a draft before the sign-out, a final after it. Throws with
 * the server's body when it is refused. Returns the new version.
 */
export async function generateReport(
  page: Page,
  caseId: string,
): Promise<CaseReport> {
  const answer = await apiPost(
    page,
    `${casePath(caseId)}/reports/generate`,
    undefined,
    `Generating a report for case ${caseId}`,
  );
  return JSON.parse(answer) as CaseReport;
}

/**
 * Upload a file as an external report version of a case, as the Reports tab
 * posts one. Returns the new version.
 */
export async function uploadReport(
  page: Page,
  caseId: string,
  { mimeType = "image/png", base64 = SETUP_UPLOAD_PNG }: ReportUploadFile = {},
): Promise<CaseReport> {
  const answer = await apiPost(
    page,
    `${casePath(caseId)}/reports`,
    { base64Image: `data:${mimeType};base64,${base64}` },
    `Uploading a report to case ${caseId}`,
  );
  return JSON.parse(answer) as CaseReport;
}

/**
 * Reopen a signed-out case with a reason, naming the stage it is reopened
 * from, through the endpoint the case view posts to. The final report in
 * force is voided and the case goes back to the stage its findings are
 * written at. Throws with the server's body when refused. Returns the stage
 * the case now stands at.
 */
export async function reopenCase(
  page: Page,
  caseId: string,
  from: PathologyStageId,
  reason = SETUP_REOPEN_REASON,
): Promise<PathologyStageId> {
  const answer = await apiPost(
    page,
    `${casePath(caseId)}/reopen`,
    { from, reason },
    `Reopening case ${caseId} from ${from}`,
  );
  const status = (JSON.parse(answer) as { status?: PathologyStageId }).status;
  if (!status) {
    throw new Error(
      `Reopening case ${caseId} from ${from} answered no stage: ${answer.substring(0, 400)}`,
    );
  }
  return status;
}
