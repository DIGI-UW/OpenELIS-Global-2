import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import OrderDetailPanel from "../OrderDetailPanel";
import messages from "../../../languages/en.json";

const renderPanel = (props) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <OrderDetailPanel caseId="case-1" orderDetail={null} {...props} />
    </IntlProvider>,
  );

const patientOrigins = [
  { code: "INPATIENT", label: "Inpatient", whonetCode: "INP" },
  { code: "EMERGENCY", label: "Emergency", whonetCode: "EME" },
];

describe("OrderDetailPanel", () => {
  it("saves captured order detail fields", async () => {
    const user = userEvent.setup();
    const service = {
      getPatientOrigins: vi.fn().mockResolvedValue({
        defaultCode: null,
        options: patientOrigins,
      }),
      saveOrderDetail: vi.fn().mockResolvedValue({
        orderDetail: {
          culturePurpose: "ACTIVE_SCREENING",
          cultureMethodId: "",
          patientOrigin: "EMERGENCY",
          admissionDate: null,
          numberOfSets: 2,
          clinicalHistory: "Fever, suspected sepsis",
          antibioticExposure: true,
        },
      }),
    };

    renderPanel({ service });

    await screen.findByRole("option", { name: "Emergency" });
    expect(screen.getByText("Unspecified")).toBeInTheDocument();
    await user.click(
      screen.getByRole("radio", { name: "Active screening or carriage" }),
    );
    await user.selectOptions(
      screen.getByLabelText("Patient origin"),
      "EMERGENCY",
    );
    await user.type(screen.getByLabelText("Number of sets"), "2");
    await user.type(
      screen.getByLabelText("Clinical history"),
      "Fever, suspected sepsis",
    );
    await user.click(
      screen.getByLabelText(/Patient has recent antibiotic exposure/i),
    );
    await user.click(screen.getByRole("button", { name: "Save order detail" }));

    await waitFor(() =>
      expect(service.saveOrderDetail).toHaveBeenCalledWith("case-1", {
        culturePurpose: "ACTIVE_SCREENING",
        cultureMethodId: "",
        patientOrigin: "EMERGENCY",
        admissionDate: null,
        numberOfSets: 2,
        clinicalHistory: "Fever, suspected sepsis",
        antibioticExposure: true,
      }),
    );
  });

  it("prefills fields from an existing captured order detail", async () => {
    renderPanel({
      orderDetail: {
        culturePurpose: "CLINICAL_DIAGNOSTIC",
        cultureMethodId: "method-1",
        patientOrigin: "INPATIENT",
        admissionDate: "2026-08-03",
        numberOfSets: 3,
        clinicalHistory: "",
        antibioticExposure: true,
      },
      service: {
        getPatientOrigins: vi.fn().mockResolvedValue({
          defaultCode: null,
          options: patientOrigins,
        }),
        saveOrderDetail: vi.fn(),
      },
    });

    await screen.findByRole("option", { name: "Inpatient" });
    expect(screen.getByLabelText("Patient origin")).toHaveValue("INPATIENT");
    expect(
      screen.getByRole("radio", { name: "Clinical diagnosis or treatment" }),
    ).toBeChecked();
    expect(screen.getByLabelText("Number of sets")).toHaveValue(3);
    expect(
      screen.getByLabelText(/Patient has recent antibiotic exposure/i),
    ).toBeChecked();
    expect(screen.getByLabelText("Date of admission")).toHaveValue(
      "08/03/2026",
    );
  });

  it("keeps culture purpose visible but immutable after final release", async () => {
    renderPanel({
      isReadOnly: true,
      orderDetail: { culturePurpose: "ACTIVE_SCREENING" },
      service: {
        getPatientOrigins: vi.fn().mockResolvedValue({ options: [] }),
        saveOrderDetail: vi.fn(),
      },
    });

    expect(
      screen.getByRole("radio", { name: "Active screening or carriage" }),
    ).toBeChecked();
    expect(
      screen.getByRole("radio", { name: "Clinical diagnosis or treatment" }),
    ).toBeDisabled();
    expect(
      screen.queryByRole("button", { name: "Save order detail" }),
    ).not.toBeInTheDocument();
  });
});
