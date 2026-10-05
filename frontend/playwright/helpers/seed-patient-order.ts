import { expect, type Page } from "@playwright/test";
import { csrfToken } from "./api-session";

/**
 * Patients and orders seeded through the endpoints the Add/Modify Patient and
 * Add Order screens post to, for specs that drive a later screen against them.
 */

const API = "/api/OpenELIS-Global/rest";

/** Random uppercase letters; the site's name validators reject digits. */
export function letters(length: number): string {
  return Array.from({ length }, () =>
    String.fromCharCode(65 + Math.floor(Math.random() * 26)),
  ).join("");
}

export interface SeededPatient {
  patientPK: string;
  firstName: string;
  lastName: string;
  nationalId: string;
  gender: "M" | "F";
  /** As the server stores and displays it. */
  birthDate: string;
}

export interface PatientSeed {
  firstName?: string;
  lastName?: string;
  gender?: "M" | "F";
  /** In the server's display format; pick day == month to stay locale-proof. */
  birthDate?: string;
}

export async function seedPatient(
  page: Page,
  seed: PatientSeed = {},
): Promise<SeededPatient> {
  // No shared prefix: the name search is fuzzy (trigram), so seeded names
  // sharing a stem would match one another.
  const patient = {
    firstName: seed.firstName ?? letters(10),
    lastName: seed.lastName ?? letters(12),
    nationalId: `SPO${Date.now()}${letters(4)}`,
    gender: seed.gender ?? "F",
    birthDate: seed.birthDate ?? "05/05/1985",
  };
  const response = await page.request.post(`${API}/PatientManagement`, {
    headers: { "X-CSRF-Token": await csrfToken(page) },
    data: {
      patientPK: "",
      patientUpdateStatus: "ADD",
      firstName: patient.firstName,
      lastName: patient.lastName,
      nationalId: patient.nationalId,
      subjectNumber: "",
      gender: patient.gender,
      birthDateForDisplay: patient.birthDate,
      idDocuments: [],
      // PatientUtil#setSystemUserID fails without a contact person.
      patientContact: {
        person: { firstName: "", lastName: "", primaryPhone: "", email: "" },
      },
    },
  });
  const body = await response.text();
  expect(response.status(), `seed patient: ${body}`).toBe(200);
  const patientPK = String(JSON.parse(body).patientId ?? "");
  expect(patientPK, `seed patient returned an id: ${body}`).not.toBe("");
  return { ...patient, patientPK };
}

type IdValue = { id: string; value: string };

export interface OrderSeed {
  sampleTypeId?: string;
  testIds?: string[];
}

/** A new order on an existing patient; returns its lab number. */
export async function seedOrderForPatient(
  page: Page,
  patient: SeededPatient,
  { sampleTypeId = "2", testIds = ["4"] }: OrderSeed = {},
): Promise<string> {
  const headers = { "X-CSRF-Token": await csrfToken(page) };
  const entry = await page.request.get(`${API}/SamplePatientEntry`);
  expect(entry.status(), "load Add Order form").toBe(200);
  const form = (await entry.json()) as {
    currentDate: string;
    sampleOrderItems: {
      referringSiteList: IdValue[];
      providersList: IdValue[];
    };
  };
  const site = form.sampleOrderItems.referringSiteList.find(
    (s) => s.value === "279 - CAMES MAN",
  );
  const provider = form.sampleOrderItems.providersList.find(
    (p) => p.value === "Prime, Optimus",
  );
  expect(site, "base referring site CAMES MAN").toBeTruthy();
  expect(provider, "base provider Optimus Prime").toBeTruthy();

  const generated = await page.request.get(
    `${API}/SampleEntryGenerateScanProvider`,
    { headers },
  );
  const labNo = ((await generated.json()) as { body?: string }).body ?? "";
  expect(labNo, "generated lab number").not.toBe("");

  const response = await page.request.post(`${API}/SamplePatientEntry`, {
    headers,
    data: {
      rememberSiteAndRequester: false,
      currentDate: form.currentDate,
      patientUpdateStatus: "NO_ACTION",
      referralItems: [],
      warning: false,
      useReferral: false,
      sampleXML:
        `<?xml version="1.0" encoding="utf-8"?><samples>` +
        `<sample sampleID='${sampleTypeId}' date='' time='' collector='' quantity='' uom='' ` +
        `tests='${testIds.join(",")}' testSectionMap='' testSampleTypeMap='' panels='' ` +
        `rejected='false' rejectReasonId='' initialConditionIds='' storageLocationId='' ` +
        `storageLocationType='' storagePositionCoordinate='' gpsLatitude='' gpsLongitude='' ` +
        `gpsAccuracy='' gpsCaptureMethod='' numOrderLabels='1' numSpecimenLabels='1'/></samples>`,
      patientProperties: {
        patientPK: patient.patientPK,
        patientUpdateStatus: "NO_ACTION",
        firstName: patient.firstName,
        lastName: patient.lastName,
        nationalId: patient.nationalId,
        gender: patient.gender,
        birthDateForDisplay: patient.birthDate,
      },
      sampleOrderItems: {
        labNo,
        requestDate: form.currentDate,
        receivedDateForDisplay: form.currentDate,
        receivedTime: "09:00",
        nextVisitDate: "",
        priority: "ROUTINE",
        referringSiteId: site!.id,
        providerId: provider!.id,
        providerPersonId: provider!.id,
        programId: "",
        modified: true,
        sampleId: "",
      },
      initialSampleConditionList: [],
      testSectionList: [],
    },
  });
  expect(
    response.status(),
    `seed order ${labNo}: ${(await response.text()).slice(0, 400)}`,
  ).toBe(200);

  const order = await page.request.get(
    `${API}/order/search?labNumber=${encodeURIComponent(labNo)}`,
  );
  expect(order.status(), `order ${labNo} saved`).toBe(200);
  const saved = (await order.json()) as {
    patientProperties?: { patientPK?: string };
  };
  expect(saved.patientProperties?.patientPK, `order ${labNo} patient`).toBe(
    patient.patientPK,
  );
  return labNo;
}
