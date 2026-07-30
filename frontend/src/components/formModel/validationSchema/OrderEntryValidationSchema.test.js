import { describe, expect, test } from "vitest";
import { SampleOrderFormValues } from "../innitialValues/OrderEntryFormValues";
import { createOrderEntryValidationSchema } from "./OrderEntryValidationSchema";

const minimalOrderValues = {
  ...SampleOrderFormValues,
  sampleXML: "<sample/>",
  patientProperties: {
    ...SampleOrderFormValues.patientProperties,
    firstName: "Test",
    lastName: "Patient",
    nationalId: "",
    gender: "M",
    birthDateForDisplay: "01/01/1990",
    email: "",
  },
  sampleOrderItems: {
    ...SampleOrderFormValues.sampleOrderItems,
    labNo: "TEST-ORDER-1",
    referringSiteName: "Central Lab",
    providerFirstName: "Provider",
    providerLastName: "User",
  },
};

const fullOrderValues = {
  ...minimalOrderValues,
  patientEmailNotificationTestIds: ["1"],
  providerEmailNotificationTestIds: ["2"],
  patientProperties: {
    ...minimalOrderValues.patientProperties,
    nationalId: "NAT-12345",
    subjectNumber: "SUB-12345",
    aka: "Alias",
    mothersName: "Mother",
    streetAddress: "123 Main",
    city: "Antananarivo",
    commune: "Commune",
    occupation: "Clinician",
    primaryPhone: "+261 37 12 345 67",
    email: "patient@example.org",
    healthRegion: "Analamanga",
    education: "Secondary",
    maritialStatus: "Single",
    nationality: "Malagasy",
    healthDistrict: "District",
    otherNationality: "",
    patientContact: {
      person: {
        firstName: "Contact",
        lastName: "Person",
        primaryPhone: "+261 38 99 888 77",
        email: "contact@example.org",
      },
    },
    addressHierarchy_0: "19",
    addressHierarchy_1: "20",
    addressHierarchy_2: "21",
    addressHierarchy_3: "Fokontany",
    addressHierarchy_4: "Hamlet",
  },
  sampleOrderItems: {
    ...minimalOrderValues.sampleOrderItems,
    requestDate: "01/02/2026",
    receivedDateForDisplay: "01/02/2026",
    receivedTime: "09:30",
    requesterSampleID: "REQ-1",
    referringPatientNumber: "REF-PAT-1",
    referringSiteId: "9000100",
    referringSiteDepartmentId: "9000200",
    referringSiteCode: "SITE",
    referringSiteDepartmentName: "Dept",
    providerId: "9000002",
    providerPersonId: "9000002",
    providerWorkPhone: "+261 37 22 333 44",
    providerFax: "123",
    providerEmail: "provider@example.org",
    facilityAddressStreet: "Facility Street",
    facilityAddressCommune: "Facility Commune",
    facilityPhone: "+261 38 22 333 44",
    facilityFax: "456",
    paymentOptionSelection: "PAID",
    billingReferenceNumber: "BILL-1",
    testLocationCode: "LAB",
    otherLocationCode: "",
    program: "Routine Testing",
    priority: "ROUTINE",
    programId: "2",
    consentGiven: true,
    consentFormReference: "CONSENT-1",
    consentRecordedAt: "2026-01-02",
    consentRecordedBy: "admin",
  },
};

describe("createOrderEntryValidationSchema", () => {
  test("accepts minimal order form without patient national ID when PATIENT_NATIONAL_ID_REQUIRED is false", async () => {
    const schema = createOrderEntryValidationSchema({
      PATIENT_NATIONAL_ID_REQUIRED: "false",
    });

    await expect(schema.isValid(minimalOrderValues)).resolves.toBe(true);
  });

  test("accepts full order form with patient national ID and optional patient details", async () => {
    const schema = createOrderEntryValidationSchema({
      PATIENT_NATIONAL_ID_REQUIRED: "false",
    });

    await expect(schema.isValid(fullOrderValues)).resolves.toBe(true);
  });

  test("requires patient national ID by default to preserve existing behavior", async () => {
    const schema = createOrderEntryValidationSchema({});

    await expect(schema.isValid(minimalOrderValues)).resolves.toBe(false);
  });
});
