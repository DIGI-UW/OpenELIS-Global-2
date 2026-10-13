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

  it("labels each row with the lab number and position the release reason uses", async () => {
    evaluationsMock.mockResolvedValue([]);

    const { findByText } = render(
      <IntlProvider locale="en" messages={messages}>
        <SampleAcceptanceReview
          orderId="79"
          labNumber="DEV01260000000001422"
          samples={[urine("701"), urine("702")]}
          onBlockedChange={vi.fn()}
        />
      </IntlProvider>,
    );

    expect(await findByText("DEV01260000000001422-1")).toBeTruthy();
    expect(await findByText("DEV01260000000001422-2")).toBeTruthy();
  });

  it("names nothing until the server has reported, so no sample shows as not accepted while loading", () => {
    evaluationsMock.mockReturnValue(new Promise(() => {}));
    const onUnacceptedChange = vi.fn();

    render(
      <IntlProvider locale="en" messages={messages}>
        <SampleAcceptanceReview
          orderId="78"
          labNumber="DEV01260000000001421"
          samples={[urine("601"), urine("602")]}
          onBlockedChange={vi.fn()}
          onUnacceptedChange={onUnacceptedChange}
        />
      </IntlProvider>,
    );

    expect(onUnacceptedChange).not.toHaveBeenCalledWith(
      expect.arrayContaining(["DEV01260000000001421-1 Urine"]),
    );
  });
});
