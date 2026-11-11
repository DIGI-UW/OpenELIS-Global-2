import { describe, expect, it } from "vitest";
import {
  buildLoadedOrderData,
  buildSubmissionSampleOrderItems,
} from "./orderDataUtils";

describe("buildLoadedOrderData", () => {
  it("carries the lab number and resets the patient update status", () => {
    const loaded = buildLoadedOrderData({
      labNumber: "20260806-001",
      patientProperties: { patientPK: "12" },
      sampleOrderItems: { programId: "4" },
    });

    expect(loaded.sampleOrderItems.labNo).toBe("20260806-001");
    expect(loaded.patientProperties.patientUpdateStatus).toBe("NO_ACTION");
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
        programCode: "MICROBIOLOGY",
        questionnaire: { id: "client-only" },
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
      programCode: "MICROBIOLOGY",
      domain: "clinical",
    });
    expect(serialized).not.toHaveProperty("domain");
    expect(serialized).not.toHaveProperty("programCode");
  });
});

describe("order override fields on submission", () => {
  // OGC-1201: SampleOrderItem rejects properties it does not declare — these
  // three are declared precisely so the decision travels with the order and
  // is recorded atomically with it.
  it("carries the no-patient decision to the server", () => {
    const submitted = buildSubmissionSampleOrderItems({
      labNo: "LAB-1",
      isEQASample: true,
      eqaProgramId: "3",
      noPatientOverride: true,
      noPatientReasonCode: "EQA",
      noPatientReason: "External quality assessment sample",
    });

    // The server accepts and records the decision in the same request that
    // creates the order, so the declaration has to reach it.
    expect(submitted.noPatientOverride).toBe(true);
    expect(submitted.noPatientReasonCode).toBe("EQA");
    expect(submitted.isEQASample).toBe(true);
    expect(submitted.eqaProgramId).toBe("3");
  });
});
