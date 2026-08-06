import { describe, expect, it } from "vitest";
import {
  buildLoadedOrderData,
  buildSubmissionSampleOrderItems,
} from "./orderDataUtils";

describe("buildLoadedOrderData", () => {
  it("restores durable microbiology detail after the order-entry reload", () => {
    const loaded = buildLoadedOrderData({
      labNumber: "20260806-001",
      patientProperties: { patientPK: "12" },
      sampleOrderItems: { programId: "4" },
      microbiologyOrderDetail: {
        cultureMethodId: "17",
        patientOrigin: "INPATIENT",
        numberOfSets: 2,
        clinicalHistory: "Persistent fever",
        antibioticExposure: true,
        criticalNotificationPreference: false,
      },
    });

    expect(loaded.sampleOrderItems.labNo).toBe("20260806-001");
    expect(loaded.patientProperties.patientUpdateStatus).toBe("NO_ACTION");
    expect(loaded.microbiologyOrderDetail).toEqual({
      cultureMethodId: "17",
      patientOrigin: "INPATIENT",
      numberOfSets: 2,
      clinicalHistory: "Persistent fever",
      antibioticExposure: true,
      criticalNotificationPreference: false,
    });
  });

  it("retains microbiology defaults when no draft exists", () => {
    const loaded = buildLoadedOrderData({ labNumber: "20260806-002" });

    expect(loaded.microbiologyOrderDetail).toEqual({
      cultureMethodId: "",
      patientOrigin: "",
      numberOfSets: "",
      clinicalHistory: "",
      antibioticExposure: false,
      criticalNotificationPreference: null,
    });
  });

  it("preserves loaded reference lists and environmental state", () => {
    const sampleTypes = [{ id: "1", value: "Blood" }];
    const loaded = buildLoadedOrderData(
      {
        labNumber: "20260806-003",
        sampleOrderItems: {
          environmentalFields: { site: "updated" },
        },
      },
      {
        sampleTypes,
        testSectionList: [{ id: "2", value: "Microbiology" }],
        rejectReasonList: [{ id: "3", value: "Leaking" }],
        referralOrganizations: [{ id: "4", value: "Reference lab" }],
        referralReasons: [{ id: "5", value: "Confirmatory testing" }],
        sampleOrderItems: {
          environmentalFields: { district: "North", site: "original" },
        },
      },
    );

    expect(loaded.sampleTypes).toBe(sampleTypes);
    expect(loaded.referralOrganizations).toHaveLength(1);
    expect(loaded.sampleOrderItems.environmentalFields).toEqual({
      district: "North",
      site: "updated",
    });
  });
});

describe("buildSubmissionSampleOrderItems", () => {
  it("keeps server fields and removes client-only program state", () => {
    expect(
      buildSubmissionSampleOrderItems({
        labNo: "20260806-003",
        programId: "9",
        program: "Microbiology",
        questionnaire: { id: "client-only" },
        microbiologyProgramId: "9",
        microbiologyPreviousProgramId: "1",
        domain: "clinical",
        priorityList: [{ id: "1" }],
      }),
    ).toEqual(
      expect.objectContaining({
        labNo: "20260806-003",
        programId: "9",
        priorityList: [],
      }),
    );

    const serialized = buildSubmissionSampleOrderItems({
      microbiologyProgramId: "9",
      microbiologyPreviousProgramId: "1",
    });
    expect(serialized).not.toHaveProperty("microbiologyProgramId");
    expect(serialized).not.toHaveProperty("microbiologyPreviousProgramId");
    expect(serialized).not.toHaveProperty("domain");
  });
});
