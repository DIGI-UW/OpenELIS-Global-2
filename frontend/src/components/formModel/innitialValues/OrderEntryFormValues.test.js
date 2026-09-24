import { describe, expect, it } from "vitest";
import { createSampleOrderFormValues } from "./OrderEntryFormValues";
import { getInitialOrderData } from "../../order/OrderContext";

describe("order entry starting values", () => {
  it("gives every new order its own nested state", () => {
    const first = createSampleOrderFormValues();
    first.sampleOrderItems.priorityList = ["carried over"];
    first.sampleOrderItems.requesterSampleID = "LAB-1001";
    first.patientProperties.firstName = "Ana";

    const second = createSampleOrderFormValues();

    expect(second.sampleOrderItems.priorityList).toEqual([]);
    expect(second.sampleOrderItems.requesterSampleID).toBe("");
    expect(second.patientProperties.firstName).toBe("");
  });
});

describe("the order context's starting state", () => {
  it("does not carry one order's entries into the next", () => {
    const first = getInitialOrderData("clinical");
    first.sampleOrderItems.priorityList = ["carried over"];
    first.sampleOrderItems.labNo = "LAB-2002";
    first.patientProperties.firstName = "Ana";

    const second = getInitialOrderData("clinical");

    expect(second.sampleOrderItems.priorityList).toEqual([]);
    expect(second.sampleOrderItems.labNo).toBe("");
    expect(second.patientProperties.firstName).toBe("");
  });
});
