import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../../languages/en.json";

const { getFromOpenElisServer } = vi.hoisted(() => ({
  getFromOpenElisServer: vi.fn(),
}));
vi.mock("../../../utils/Utils", () => ({ getFromOpenElisServer }));
vi.mock("../../../common/CustomDatePicker", () => ({
  default: () => <div data-testid="date-picker" />,
}));

import EqaAndNoPatientSection from "./EqaAndNoPatientSection";

const renderSection = (sampleOrderItems = {}, options = {}) => {
  const setOrderData = vi.fn();
  const orderData = {
    sampleOrderItems,
    patientProperties: options.patientProperties || {},
  };
  render(
    <IntlProvider locale="en" messages={messages}>
      <EqaAndNoPatientSection
        orderData={orderData}
        setOrderData={setOrderData}
        isReadOnly={false}
        patientRequired={options.patientRequired !== false}
      />
    </IntlProvider>,
  );
  const applied = () => {
    const updater = setOrderData.mock.calls.at(-1)[0];
    return updater(orderData).sampleOrderItems;
  };
  return { setOrderData, applied };
};

describe("EqaAndNoPatientSection", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((_url, cb) => cb([]));
  });

  // OGC-1201 AL/W: EQA used to satisfy the patient gate by creating one
  // shared sentinel patient, so every EQA order pointed at the same
  // 126-year-old man and was evaluated against his reference range.
  it("declares no patient when EQA is ticked, with EQA as the recorded reason", async () => {
    const { applied } = renderSection();

    await userEvent.setup().click(screen.getByLabelText(/EQA\) sample/));

    expect(applied()).toEqual(
      expect.objectContaining({
        isEQASample: true,
        noPatientOverride: true,
        noPatientReasonCode: "EQA",
      }),
    );
  });

  it("suppresses the warning for EQA but still records the reason", () => {
    renderSection({
      isEQASample: true,
      noPatientOverride: true,
      noPatientReasonCode: "EQA",
    });

    expect(
      screen.queryByText(/will not be evaluated against a reference range/),
    ).not.toBeInTheDocument();
  });

  it("warns when an order goes patient-less without EQA", () => {
    renderSection(
      { noPatientOverride: true, noPatientReasonCode: "MANUAL" },
      { patientRequired: false },
    );

    expect(
      screen.getByText(/will not be evaluated against a reference range/),
    ).toBeInTheDocument();
    expect(
      screen.getByLabelText(/Reason for ordering without a patient/),
    ).toBeInTheDocument();
  });

  it("clears the EQA detail and the override when EQA is unticked", async () => {
    const { applied } = renderSection({
      isEQASample: true,
      noPatientOverride: true,
      eqaProgramId: "3",
      eqaProviderSampleId: "PT-1",
    });

    await userEvent.setup().click(screen.getByLabelText(/EQA\) sample/));

    expect(applied()).toEqual(
      expect.objectContaining({
        isEQASample: false,
        noPatientOverride: false,
        eqaProgramId: "",
        eqaProviderSampleId: "",
      }),
    );
  });

  // PatientRequired governs the manual tick only: a site that requires
  // patients for clinical work still runs proficiency testing.
  it("withholds the manual override where the deployment requires a patient", () => {
    renderSection();
    expect(screen.getByLabelText(/This order has no patient/)).toBeDisabled();
  });

  it("offers it under EQA even so", () => {
    renderSection({ isEQASample: true, noPatientOverride: true });
    expect(screen.getByLabelText(/This order has no patient/)).toBeEnabled();
  });

  it("offers it where the deployment does not require a patient", () => {
    renderSection({}, { patientRequired: false });
    expect(screen.getByLabelText(/This order has no patient/)).toBeEnabled();
  });

  it("reveals the EQA detail only when EQA is ticked", () => {
    renderSection({ isEQASample: true });
    expect(screen.getByLabelText(/Provider Sample ID/)).toBeInTheDocument();
  });

  it("keeps the EQA detail hidden otherwise", () => {
    renderSection();
    expect(
      screen.queryByLabelText(/Provider Sample ID/),
    ).not.toBeInTheDocument();
  });

  // Default, not lock: some schemes do use identified specimens.
  it("lets a user attach a patient to an EQA order", () => {
    renderSection(
      { isEQASample: true, noPatientOverride: true },
      { patientProperties: { lastName: "Ada" } },
    );
    expect(screen.getByText(/will not be recorded/)).toBeInTheDocument();
  });
});
