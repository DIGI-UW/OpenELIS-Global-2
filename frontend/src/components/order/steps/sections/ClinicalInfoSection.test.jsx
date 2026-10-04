import React from "react";
import { render, screen } from "@testing-library/react";
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
