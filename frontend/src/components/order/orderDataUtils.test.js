import { describe, expect, it } from "vitest";
import {
  buildLoadedOrderData,
  buildSubmissionMicrobiologyOrderDetail,
  buildSubmissionSampleOrderItems,
  isMicrobiologyOrderReady,
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
        admissionDate: "2026-08-03",
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
      admissionDate: "2026-08-03",
      numberOfSets: 2,
      clinicalHistory: "Persistent fever",
      antibioticExposure: true,
    });
  });

  it("retains microbiology defaults when no draft exists", () => {
    const loaded = buildLoadedOrderData({ labNumber: "20260806-002" });

    expect(loaded.microbiologyOrderDetail).toEqual({
      cultureMethodId: "",
      patientOrigin: "",
      admissionDate: "",
      numberOfSets: "",
      clinicalHistory: "",
      antibioticExposure: false,
    });
  });

  it("restores the transient Microbiology marker from the canonical Program code", () => {
    const loaded = buildLoadedOrderData({
      labNumber: "20260806-003",
      sampleOrderItems: {
        programId: "8",
        programCode: "MICROBIOLOGY",
      },
    });

    expect(loaded.sampleOrderItems.microbiologyProgramId).toBe("8");
  });
});

describe("isMicrobiologyOrderReady", () => {
  it("allows a loaded manual Microbiology order without a default protocol", () => {
    const loaded = buildLoadedOrderData({
      labNumber: "20260806-004",
      sampleOrderItems: {
        programId: "8",
        programCode: "MICROBIOLOGY",
      },
    });

    expect(isMicrobiologyOrderReady(loaded, [])).toBe(true);
  });
});

describe("buildSubmissionMicrobiologyOrderDetail", () => {
  it("keeps an inpatient date and removes the retired notification preference", () => {
    expect(
      buildSubmissionMicrobiologyOrderDetail({
        patientOrigin: "INPATIENT",
        admissionDate: "2026-08-03",
        criticalNotificationPreference: true,
      }),
    ).toEqual({
      patientOrigin: "INPATIENT",
      admissionDate: "2026-08-03",
    });
  });

  it("omits an admission date for an outpatient without erasing form state", () => {
    const detail = {
      patientOrigin: "OUTPATIENT",
      admissionDate: "2026-08-03",
    };

    expect(buildSubmissionMicrobiologyOrderDetail(detail)).toEqual({
      patientOrigin: "OUTPATIENT",
      admissionDate: null,
    });
    expect(detail.admissionDate).toBe("2026-08-03");
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
        microbiologyProgramId: "9",
        microbiologyPreviousProgramId: "1",
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
    expect(serialized).not.toHaveProperty("programCode");
  });
});
