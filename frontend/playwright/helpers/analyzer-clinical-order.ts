import { randomUUID } from "node:crypto";
import { expect, type Page } from "@playwright/test";
import { csrfToken } from "./api-session";

const API = "/api/OpenELIS-Global/rest";

async function jsonGet<T>(page: Page, path: string): Promise<T> {
  const response = await page.request.get(`${API}${path}`);
  expect(response.ok(), `GET ${path}: ${response.status()}`).toBeTruthy();
  return (await response.json()) as T;
}

/** A synthetic patient with one specimen and these catalog tests ordered. */
export async function createClinicalOrder(
  page: Page,
  {
    testIds,
    specimenName,
    accession: existingAccession,
  }: { testIds: string[]; specimenName: string; accession?: string },
): Promise<{
  accession: string;
  patientLastName: string;
  testId: string;
  specimenId: string;
}> {
  const compatibility = await jsonGet<{
    tests: Array<{
      testId: string;
      compatibleSampleTypes: Array<{ id: string; name: string }>;
    }>;
  }>(
    page,
    `/test-sample-types?testIds=${encodeURIComponent(testIds.join(","))}`,
  );
  expect(compatibility.tests).toHaveLength(testIds.length);
  const specimenIds = testIds.map((testId) => {
    const compatible = compatibility.tests.find(
      (test) => test.testId === testId,
    );
    expect(compatible, `Compatibility for test ${testId}`).toBeDefined();
    const specimens = compatible!.compatibleSampleTypes.filter(
      (type) => type.name === specimenName,
    );
    expect(specimens, `${testId} accepts ${specimenName}`).toHaveLength(1);
    return specimens[0].id;
  });
  expect(new Set(specimenIds).size).toBe(1);
  const specimenId = specimenIds[0];

  let accession = existingAccession;
  if (!accession) {
    const generated = await page.request.get(
      `${API}/SampleEntryGenerateScanProvider`,
      {
        headers: { "X-CSRF-Token": await csrfToken(page) },
      },
    );
    expect(
      generated.ok(),
      `Generate analyzer order accession: ${generated.status()}`,
    ).toBeTruthy();
    accession = ((await generated.json()) as { body?: string }).body;
    expect(accession, "OpenELIS generated an accession").toBeTruthy();
  }

  const existing = await page.request.get(
    `${API}/order/search?labNumber=${encodeURIComponent(accession!)}`,
  );
  expect(existing.status(), `Fresh fixture accession ${accession}`).toBe(404);

  const entry = await jsonGet<{ currentDate: string }>(
    page,
    "/SamplePatientEntry",
  );
  const suffix = randomUUID().replaceAll("-", "").slice(0, 12).toUpperCase();
  const nameSuffix = Array.from(suffix, (digit) =>
    String.fromCharCode(65 + Number.parseInt(digit, 16)),
  ).join("");
  const patientLastName = `ANALYZER${nameSuffix}`;
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
      `tests='${testIds.join(",")}' testSectionMap='' testSampleTypeMap='' panels='' rejected='false' ` +
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
      labNo: accession,
      requestDate: entry.currentDate,
      receivedDateForDisplay: entry.currentDate,
      receivedTime: "09:00",
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
    `POST ${accession}: ${response.status()} ${(await response.text()).slice(0, 500)}`,
  ).toBeTruthy();

  const order = await jsonGet<{
    labNumber: string;
    patientProperties?: { lastName?: string };
    samples: Array<{ sampleTypeId: string; tests: Array<{ id: string }> }>;
  }>(page, `/order/search?labNumber=${encodeURIComponent(accession!)}`);
  expect(order.labNumber).toBe(accession);
  expect(order.patientProperties?.lastName).toBe(patientLastName);
  expect(order.samples).toHaveLength(1);
  expect(order.samples[0].sampleTypeId).toBe(specimenId);
  expect(order.samples[0].tests.map((test) => test.id)).toEqual(
    expect.arrayContaining(testIds),
  );
  return {
    accession: accession!,
    patientLastName,
    testId: testIds[0],
    specimenId,
  };
}
