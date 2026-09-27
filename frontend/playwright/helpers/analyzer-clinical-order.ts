import { randomUUID } from "node:crypto";
import { expect, type Page } from "@playwright/test";
import { csrfToken } from "./api-session";

const API = "/api/OpenELIS-Global/rest";

type MappingRow = {
  rawCode: string;
  loinc: string;
  mappingState: string;
  testId: string | null;
  selectedTest: { name: string; loincCodes: string[] } | null;
  results: Array<{ rawValue: string; mappingState: string }>;
};

type MappingView = { tests: MappingRow[] };

export type AnalyzerClinicalOrder = {
  accession: string;
  profileId: string;
  profileRevision: number;
  sourceCode: string;
  expectedTestName: string;
  expectedLoinc: string;
  specimenName: string;
  expectedMappedValue?: string;
};

export type StockBindingExpectation = Omit<AnalyzerClinicalOrder, "accession">;

async function jsonGet<T>(page: Page, path: string): Promise<T> {
  const response = await page.request.get(`${API}${path}`);
  expect(response.ok(), `GET ${path}: ${response.status()}`).toBeTruthy();
  return (await response.json()) as T;
}

/** Assert the shipped default, without choosing or writing any mapping row. */
export async function stockClinicalBinding(
  page: Page,
  scenario: StockBindingExpectation,
): Promise<string> {
  const mapping = await jsonGet<MappingView>(
    page,
    `/analyzer-types/${scenario.profileId}/mapping?revision=${scenario.profileRevision}`,
  );
  const rows = mapping.tests.filter((row) => row.rawCode === scenario.sourceCode);
  expect(rows, `One shipped ${scenario.sourceCode} row`).toHaveLength(1);
  const row = rows[0];
  expect(row.loinc).toBe(scenario.expectedLoinc);
  expect(row.mappingState, `${scenario.sourceCode} must be mapped by the product`).toBe("BOUND");
  expect(row.testId).toBeTruthy();
  expect(row.selectedTest?.name).toBe(scenario.expectedTestName);
  expect(row.selectedTest?.loincCodes).toContain(scenario.expectedLoinc);
  if (scenario.expectedMappedValue) {
    const values = row.results.filter((result) => result.rawValue === scenario.expectedMappedValue);
    expect(values, `${scenario.sourceCode} ${scenario.expectedMappedValue} row`).toHaveLength(1);
    expect(values[0].mappingState).toBe("BOUND");
  }
  return row.testId!;
}

function nextDateInServerFormat(currentDate: string, isoDate: string): string {
  const parts = currentDate.split(/[/-]/);
  const separator = currentDate.includes("/") ? "/" : "-";
  const [year, month, day] = isoDate.split("-");
  const tomorrow = new Date(`${isoDate}T12:00:00Z`);
  tomorrow.setUTCDate(tomorrow.getUTCDate() + 1);
  const nextYear = String(tomorrow.getUTCFullYear());
  const nextMonth = String(tomorrow.getUTCMonth() + 1).padStart(2, "0");
  const nextDay = String(tomorrow.getUTCDate()).padStart(2, "0");
  if (parts[0] === year) return [nextYear, nextMonth, nextDay].join(separator);
  if (parts[0] === month && month !== day)
    return [nextMonth, nextDay, nextYear].join(separator);
  return [nextDay, nextMonth, nextYear].join(separator);
}

/** Create a synthetic patient, specimen and order via the same validated API as entry. */
export async function createAnalyzerClinicalOrder(
  page: Page,
  scenario: AnalyzerClinicalOrder,
): Promise<{ accession: string; patientLastName: string; testId: string; specimenId: string }> {
  const testId = await stockClinicalBinding(page, scenario);
  const compatibility = await jsonGet<{
    tests: Array<{
      testId: string;
      compatibleSampleTypes: Array<{ id: string; name: string }>;
    }>;
  }>(page, `/test-sample-types?testIds=${encodeURIComponent(testId)}`);
  expect(compatibility.tests).toHaveLength(1);
  expect(compatibility.tests[0].testId).toBe(testId);
  const specimens = compatibility.tests[0].compatibleSampleTypes.filter(
    (type) => type.name === scenario.specimenName,
  );
  expect(specimens, `${scenario.expectedTestName} accepts ${scenario.specimenName}`).toHaveLength(1);
  const specimenId = specimens[0].id;

  const existing = await page.request.get(
    `${API}/order/search?labNumber=${encodeURIComponent(scenario.accession)}`,
  );
  expect(existing.status(), `Fresh fixture accession ${scenario.accession}`).toBe(404);

  const entry = await jsonGet<{ currentDate: string }>(page, "/SamplePatientEntry");
  const serverTime = await jsonGet<{ date: string }>(page, "/server-time");
  const suffix = randomUUID().replaceAll("-", "").slice(0, 12).toUpperCase();
  const patientLastName = `ANALYZER${suffix}`;
  const form = {
    rememberSiteAndRequester: false,
    currentDate: entry.currentDate,
    patientUpdateStatus: "ADD",
    referralItems: [],
    warning: false,
    useReferral: false,
    sampleXML:
      `<?xml version="1.0" encoding="utf-8"?><samples>` +
      `<sample sampleID='${specimenId}' date='' time='' collector='' quantity='' uom='' ` +
      `tests='${testId}' testSectionMap='' testSampleTypeMap='' panels='' rejected='false' ` +
      `rejectReasonId='' initialConditionIds='' storageLocationId='' storageLocationType='' ` +
      `storagePositionCoordinate='' gpsLatitude='' gpsLongitude='' gpsAccuracy='' ` +
      `gpsCaptureMethod='' numOrderLabels='1' numSpecimenLabels='1'/></samples>`,
    patientProperties: {
      patientPK: "",
      patientUpdateStatus: "ADD",
      firstName: "Analyzer",
      lastName: patientLastName,
      gender: "F",
      birthDateForDisplay: "01/01/1990",
      nationalId: suffix,
      subjectNumber: suffix,
    },
    sampleOrderItems: {
      labNo: scenario.accession,
      requestDate: entry.currentDate,
      receivedDateForDisplay: entry.currentDate,
      receivedTime: "09:00",
      nextVisitDate: nextDateInServerFormat(entry.currentDate, serverTime.date),
      priority: "ROUTINE",
      newRequesterName: "Analyzer workflow test",
      referringSiteId: "",
      providerId: "",
      providerPersonId: "",
      programId: "",
      modified: true,
      sampleId: "",
    },
    initialSampleConditionList: [],
    testSectionList: [],
  };
  const response = await page.request.post(`${API}/SamplePatientEntry`, {
    data: form,
    headers: { "X-CSRF-Token": await csrfToken(page) },
  });
  expect(
    response.ok(),
    `POST ${scenario.accession}: ${response.status()} ${(await response.text()).slice(0, 500)}`,
  ).toBeTruthy();

  const order = await jsonGet<{
    labNumber: string;
    patientProperties?: { lastName?: string };
    samples: Array<{ sampleTypeId: string; tests: Array<{ id: string }> }>;
  }>(page, `/order/search?labNumber=${encodeURIComponent(scenario.accession)}`);
  expect(order.labNumber).toBe(scenario.accession);
  expect(order.patientProperties?.lastName).toBe(patientLastName);
  expect(order.samples).toHaveLength(1);
  expect(order.samples[0].sampleTypeId).toBe(specimenId);
  expect(order.samples[0].tests.map((test) => test.id)).toContain(testId);
  return { accession: scenario.accession, patientLastName, testId, specimenId };
}
