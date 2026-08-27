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
          accessionNumber="UATMICRO001"
          requestingLocation="Medical ward 2"
          orderDetail={{
            clinicalHistory: "Fever and suspected sepsis",
            patientOrigin: "INPATIENT",
            admissionDate: "2026-08-03",
            numberOfSets: 2,
            antibioticExposure: true,
          }}
        />
      </IntlProvider>,
    );

    const clinicalHistory = screen.getByText("Fever and suspected sepsis");
    const patientOrigin = screen.getByText("Inpatient ward");
    expect(
      clinicalHistory.compareDocumentPosition(patientOrigin) &
        Node.DOCUMENT_POSITION_FOLLOWING,
    ).toBeTruthy();
    expect(screen.getByText("2")).toBeInTheDocument();
    expect(screen.getByText("Yes")).toBeInTheDocument();
    expect(screen.getByText(/2026|8\/3/)).toBeInTheDocument();
    expect(screen.getByText(/UATMICRO001/)).toBeInTheDocument();
    expect(screen.getByText(/Medical ward 2/)).toBeInTheDocument();
  });

  it("does not turn uncaptured order context into a negative clinical answer", () => {
    render(
      <IntlProvider locale="en" messages={messages}>
        <CaseInfoSummary orderDetail={null} />
      </IntlProvider>,
    );

    expect(screen.getAllByText("Not provided").length).toBeGreaterThan(1);
  });
});
