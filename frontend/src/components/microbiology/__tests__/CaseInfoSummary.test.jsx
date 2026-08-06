import React from "react";
import { render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import CaseInfoSummary from "../CaseInfoSummary";

describe("CaseInfoSummary", () => {
  it("surfaces clinical history before compact order context", () => {
    render(
      <IntlProvider locale="en" messages={messages}>
        <CaseInfoSummary
          orderDetail={{
            clinicalHistory: "Fever and suspected sepsis",
            patientOrigin: "INPATIENT",
            numberOfSets: 2,
            antibioticExposure: true,
            criticalNotificationPreference: false,
          }}
        />
      </IntlProvider>,
    );

    const clinicalHistory = screen.getByText("Fever and suspected sepsis");
    const patientOrigin = screen.getByText("Inpatient");
    expect(
      clinicalHistory.compareDocumentPosition(patientOrigin) &
        Node.DOCUMENT_POSITION_FOLLOWING,
    ).toBeTruthy();
    expect(screen.getByText("2")).toBeInTheDocument();
    expect(screen.getByText("Yes")).toBeInTheDocument();
    expect(screen.getByText("No")).toBeInTheDocument();
  });
});
