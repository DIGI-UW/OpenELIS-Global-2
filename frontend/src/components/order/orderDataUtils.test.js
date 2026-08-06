import { describe, expect, it } from "vitest";
import { buildLoadedOrderData } from "./orderDataUtils";

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
});
