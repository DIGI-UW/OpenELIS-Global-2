import React from "react";
import { render } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../../languages/en.json";

/**
 * OGC-1443: when Sample check needs a reason to release, it names the samples
 * the server does not yet hold as accepted. A rejected sample is out of the
 * count, and so is one already accepted.
 */

const { evaluationsMock } = vi.hoisted(() => ({ evaluationsMock: vi.fn() }));

vi.mock("../../api/sampleAcceptanceApi", () => ({
  getOrderItemEvaluations: (...args) => evaluationsMock(...args),
}));

vi.mock("./SampleAcceptanceChecklist", () => ({
  default: () => null,
  computeTransitMinutes: () => null,
  formatTransit: () => "",
}));

import SampleAcceptanceReview from "./SampleAcceptanceReview";

const urine = (sampleItemId, fields = {}) => ({
  sampleItemId,
  sampleTypeName: "Urine",
  ...fields,
});

describe("SampleAcceptanceReview — samples not accepted yet (OGC-1443)", () => {
  it("names each live sample the server does not hold as accepted", async () => {
    evaluationsMock.mockResolvedValue([
      { sampleItemId: "501", overallStatus: "ACCEPTED", blocked: false },
      { sampleItemId: "502", overallStatus: "PENDING", blocked: false },
    ]);
    const onUnacceptedChange = vi.fn();

    render(
      <IntlProvider locale="en" messages={messages}>
        <SampleAcceptanceReview
          orderId="77"
          labNumber="DEV01260000000001420"
          samples={[
            urine("501"),
            urine("502"),
            urine("503", { sampleRejected: true }),
          ]}
          onBlockedChange={vi.fn()}
          onUnacceptedChange={onUnacceptedChange}
        />
      </IntlProvider>,
    );

    await waitFor(() =>
      expect(onUnacceptedChange).toHaveBeenLastCalledWith([
        "DEV01260000000001420-2 Urine",
      ]),
    );
  });
});
