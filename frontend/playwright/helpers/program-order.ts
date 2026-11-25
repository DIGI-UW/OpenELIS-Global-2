import type { Page } from "@playwright/test";
import { csrfToken } from "./api-session";

/**
 * Seed an order placed under a program (Histopathology, Immunohistochemistry,
 * Cytology, ...) through `/rest/SamplePatientEntry`, the endpoint Add Order
 * posts to. The program id and its questionnaire response travel in
 * `sampleOrderItems.programId` / `additionalQuestions`, as
 * OrderEntryAdditionalQuestions builds them, and the server creates the
 * program sample (pathology, IHC or cytology case) that the program's
 * dashboard lists.
 *
 * Programs, sample types and tests are looked up by name so the helper follows
 * the stack's catalog instead of hard-coding row ids. The requester and
 * referring site default to the ones the foundational fixtures load.
 */

const API = "/api/OpenELIS-Global";

export interface ProgramOrderConfig {
  /** Program display name as /rest/user-programs lists it. */
  program: string;
  /** Sample type display name as /rest/user-sample-types lists it. */
  sampleType: string;
  /** Test name on that sample type; the first test when omitted. */
  test?: string;
  /** Per-run unique patient name, so the case can be found and told apart. */
  patient: { firstName: string; lastName: string };
  providerPersonId?: string;
  referringSiteId?: string;
}

export interface ProgramOrder {
  labNo: string;
  programId: string;
  testName: string;
}

interface IdValue {
  id: string;
  value: string;
}

interface Questionnaire {
  id: string;
  item?: { linkId: string; definition?: string; text?: string }[];
}

async function getJson<T>(page: Page, path: string): Promise<T> {
  const response = await page.request.get(`${API}${path}`);
  if (response.status() !== 200) {
    throw new Error(
      `GET ${path} returned HTTP ${response.status()}: ${(await response.text()).slice(0, 300)}`,
    );
  }
  return (await response.json()) as T;
}

function byName<T extends IdValue>(list: T[], name: string, what: string): T {
  const found = list.find((entry) => entry.value === name);
  if (!found) {
    throw new Error(
      `${what} "${name}" is not in the stack catalog: ${list.map((e) => e.value).join(", ")}`,
    );
  }
  return found;
}

/** Mirrors OrderEntryAdditionalQuestions.convertQuestionnaireToResponse. */
function emptyResponseFor(questionnaire: Questionnaire | null) {
  if (!questionnaire?.item) return null;
  return {
    resourceType: "QuestionnaireResponse",
    id: "",
    questionnaire: `Questionnaire/${questionnaire.id}`,
    status: "in-progress",
    item: questionnaire.item.map((item) => ({
      linkId: item.linkId,
      definition: item.definition,
      text: item.text,
      answer: [],
    })),
  };
}

/** Server date as the order form expects it (site date locale). */
async function serverDates(page: Page) {
  const [{ date }, config] = await Promise.all([
    getJson<{ date: string }>(page, "/rest/server-time"),
    getJson<{ DEFAULT_DATE_LOCALE?: string }>(
      page,
      "/rest/configuration-properties",
    ),
  ]);
  const monthFirst = (config.DEFAULT_DATE_LOCALE || "fr-FR").startsWith("en");
  const format = (iso: string) => {
    const [y, m, d] = iso.split("-");
    return monthFirst ? `${m}/${d}/${y}` : `${d}/${m}/${y}`;
  };
  const tomorrow = new Date(`${date}T00:00:00Z`);
  tomorrow.setUTCDate(tomorrow.getUTCDate() + 1);
  return {
    today: format(date),
    // nextVisitDate is validated as strictly in the future.
    tomorrow: format(tomorrow.toISOString().slice(0, 10)),
    birthDate: format("1985-04-12"),
  };
}

export async function createProgramOrder(
  page: Page,
  config: ProgramOrderConfig,
): Promise<ProgramOrder> {
  const programs = await getJson<IdValue[]>(page, "/rest/user-programs");
  const program = byName(programs, config.program, "Program");
  const questionnaire = await getJson<Questionnaire | null>(
    page,
    `/rest/program/${program.id}/questionnaire`,
  ).catch(() => null);

  const sampleTypes = await getJson<IdValue[]>(page, "/rest/user-sample-types");
  const sampleType = byName(sampleTypes, config.sampleType, "Sample type");
  const { tests } = await getJson<{ tests: { id: string; name: string }[] }>(
    page,
    `/rest/sample-type-tests?sampleType=${sampleType.id}`,
  );
  const test = config.test
    ? tests.find((t) => t.name === config.test)
    : tests[0];
  if (!test) {
    throw new Error(
      `Sample type "${config.sampleType}" has no test "${config.test ?? "(any)"}"`,
    );
  }

  const { today, tomorrow, birthDate } = await serverDates(page);
  const { body: labNo } = await getJson<{ body: string }>(
    page,
    "/rest/SampleEntryGenerateScanProvider",
  );
  if (!labNo) throw new Error("No accession number was generated");

  const providerPersonId = config.providerPersonId ?? "9000002";
  const referringSiteId = config.referringSiteId ?? "9000100";
  const now = new Date();
  const time = `${String(now.getUTCHours()).padStart(2, "0")}:${String(now.getUTCMinutes()).padStart(2, "0")}`;
  const uniqueId = `${Date.now()}${Math.floor(Math.random() * 1000)}`;

  const form = {
    rememberSiteAndRequester: false,
    customNotificationLogic: false,
    patientEmailNotificationTestIds: [],
    patientSMSNotificationTestIds: [],
    providerEmailNotificationTestIds: [],
    providerSMSNotificationTestIds: [],
    patientUpdateStatus: "NO_ACTION",
    referralItems: [],
    sampleXML:
      `<?xml version="1.0" encoding="utf-8"?>` +
      `<samples><sample sampleID='${sampleType.id}' date='' time='' ` +
      `collector='' quantity='' uom='' tests='${test.id}' testSectionMap='' testSampleTypeMap='' ` +
      `panels='' rejected='false' rejectReasonId='' initialConditionIds='' ` +
      `storageLocationId='' storageLocationType='' storagePositionCoordinate='' ` +
      `gpsLatitude='' gpsLongitude='' gpsAccuracy='' gpsCaptureMethod='' ` +
      `numOrderLabels='1' numSpecimenLabels='1'/></samples>`,
    patientProperties: {
      patientPK: "",
      patientUpdateStatus: "ADD",
      firstName: config.patient.firstName,
      lastName: config.patient.lastName,
      gender: "F",
      birthDateForDisplay: birthDate,
      nationalId: uniqueId,
      subjectNumber: uniqueId,
    },
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
      referringSiteId,
      referringSiteDepartmentId: "",
      referringSiteCode: "",
      referringSiteName: "",
      providerId: providerPersonId,
      providerPersonId,
      providerFirstName: "",
      providerLastName: "",
      paymentOptionSelection: "",
      modified: true,
      sampleId: "",
      readOnly: false,
      billingReferenceNumber: "",
      testLocationCode: "",
      otherLocationCode: "",
      program: "",
      contactTracingIndexName: "",
      contactTracingIndexRecordNumber: "",
      priority: "ROUTINE",
      programId: program.id,
      // Add Order drops the questionnaire itself before posting and sends
      // only the response built from it.
      additionalQuestions: emptyResponseFor(questionnaire),
      isEQASample: false,
      eqaPriority: "STANDARD",
    },
    initialSampleConditionList: [],
    testSectionList: [],
    warning: false,
    useReferral: false,
  };

  const response = await page.request.post(`${API}/rest/SamplePatientEntry`, {
    data: form,
    headers: { "X-CSRF-Token": await csrfToken(page) },
  });
  const text = await response.text();
  if (response.status() !== 200) {
    throw new Error(
      `SamplePatientEntry for ${config.program} returned HTTP ${response.status()}: ${text.slice(0, 400)}`,
    );
  }
  const saved = JSON.parse(text) as { sampleOrderItems?: { labNo?: string } };
  if (saved.sampleOrderItems?.labNo !== labNo) {
    throw new Error(
      `SamplePatientEntry did not echo accession ${labNo}: ${text.slice(0, 300)}`,
    );
  }
  return { labNo, programId: program.id, testName: test.name };
}
