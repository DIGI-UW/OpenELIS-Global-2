import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../../languages/en.json";
import { ConfigurationContext } from "../../../layout/Layout";
import ClinicalInfoSection from "./ClinicalInfoSection";

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
}));

/**
 * A reopened order fills its dates in after the section has mounted, so the
 * Order Date and Next Visit Date pickers have to show the value the order
 * holds, not the empty one they started with (OGC-1192).
 */
const section = (sampleOrderItems) => (
  <IntlProvider locale="en" messages={messages}>
    <ConfigurationContext.Provider
      value={{ configurationProperties: { DEFAULT_DATE_LOCALE: "fr-FR" } }}
    >
      <ClinicalInfoSection
        orderData={{ sampleOrderItems }}
        setOrderData={vi.fn()}
        isReadOnly={false}
      />
    </ConfigurationContext.Provider>
  </IntlProvider>
);

describe("ClinicalInfoSection dates of a reopened order", () => {
  it("shows the order and next visit dates loaded after mount", () => {
    const { rerender } = render(section({}));

    rerender(
      section({ requestDate: "26/09/2026", nextVisitDate: "15/10/2026" }),
    );

    expect(screen.getByLabelText("Order Date")).toHaveValue("26/09/2026");
    expect(screen.getByLabelText("Next Visit Date")).toHaveValue("15/10/2026");
  });

  it("empties the next visit date when the order no longer has one", () => {
    const { rerender } = render(section({ nextVisitDate: "15/10/2026" }));

    rerender(section({ nextVisitDate: "" }));

    expect(screen.getByLabelText("Next Visit Date")).toHaveValue("");
  });
});

describe("ClinicalInfoSection billing (OGC-1424, FR-B28)", () => {
  const render_ = (configurationProperties, sampleOrderItems = {}) => {
    const setOrderData = vi.fn();
    render(
      <IntlProvider locale="en" messages={messages}>
        <ConfigurationContext.Provider value={{ configurationProperties }}>
          <ClinicalInfoSection
            orderData={{ sampleOrderItems }}
            setOrderData={setOrderData}
            isReadOnly={false}
          />
        </ConfigurationContext.Provider>
      </IntlProvider>,
    );
    return setOrderData;
  };

  it("shows the order-level Payment status only where payment is tracked", () => {
    render_({ TRACK_PATIENT_PAYMENT: "true" });
    expect(screen.getByLabelText("Payment Status")).toBeInTheDocument();
  });

  it("hides Payment status and the billing reference when neither is switched on", () => {
    render_({ TRACK_PATIENT_PAYMENT: "false" });
    expect(screen.queryByLabelText("Payment Status")).toBeNull();
    expect(screen.queryByLabelText("Billing reference")).toBeNull();
  });

  it("shows the billing reference under the laboratory's own label and stores it on the order", () => {
    const setOrderData = render_({
      USE_BILLING_REFERENCE_NUMBER: "true",
      BILLING_REFERENCE_NUMBER_LABEL: "Invoice number",
    });

    fireEvent.change(screen.getByLabelText("Invoice number"), {
      target: { value: "INV-204" },
    });

    const update = setOrderData.mock.calls[0][0];
    expect(
      update({ sampleOrderItems: {} }).sampleOrderItems.billingReferenceNumber,
    ).toBe("INV-204");
  });

  it("falls back to Billing reference when the laboratory set no label", () => {
    render_({ USE_BILLING_REFERENCE_NUMBER: "true" });
    expect(screen.getByLabelText("Billing reference")).toBeInTheDocument();
  });
});
