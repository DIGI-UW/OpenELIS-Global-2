import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import { vi } from "vitest";
import messages from "../../../../languages/en.json";

const { getFromOpenElisServer } = vi.hoisted(() => ({
  getFromOpenElisServer: vi.fn(),
}));
vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer,
  postToOpenElisServerFullResponse: vi.fn(),
}));
vi.mock("../../../common/CustomDatePicker", () => ({
  default: ({ value }) => <div data-testid="date-picker">{value}</div>,
}));

const ENROLLMENTS = [{ id: 7, programName: "WHO Malaria", provider: "WHO" }];
const CYCLES = [
  {
    id: 31,
    name: "Malaria 2026-1",
    cycleNumber: 1,
    deadline: "2026-11-30",
    panelCodes: ["MAL-01", "MAL-02"],
  },
];
const serve =
  ({ enrollments = ENROLLMENTS, cycles = CYCLES } = {}) =>
  (url, cb) => {
    if (url.startsWith("/rest/eqa/my-programs?active=true")) cb(enrollments);
    else if (url.includes("/open-cycles")) cb(cycles);
    else cb([]);
  };

import EqaAndNoPatientSection from "./EqaAndNoPatientSection";

const renderSection = (sampleOrderItems = {}, options = {}) => {
  const setOrderData = vi.fn();
  const orderData = {
    sampleOrderItems,
    patientProperties: options.patientProperties || {},
  };
  render(
    <MemoryRouter>
      <IntlProvider locale="en" messages={messages}>
        <EqaAndNoPatientSection
          orderData={orderData}
          setOrderData={setOrderData}
          isReadOnly={false}
          patientRequired={options.patientRequired !== false}
        />
      </IntlProvider>
    </MemoryRouter>,
  );
  const appliedOrder = () => {
    const updater = setOrderData.mock.calls.at(-1)[0];
    return updater(orderData);
  };
  const applied = () => appliedOrder().sampleOrderItems;
  return { setOrderData, applied, appliedOrder };
};

describe("EqaAndNoPatientSection", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation(serve());
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

  // OGC-1483: a proficiency sample never has a patient.
  it("keeps an EQA order patient-less, ticked and locked", () => {
    renderSection({ isEQASample: true, noPatientOverride: true });
    const noPatient = screen.getByLabelText(/This order has no patient/);
    expect(noPatient).toBeChecked();
    expect(noPatient).toBeDisabled();
  });

  it("offers it where the deployment does not require a patient", () => {
    renderSection({}, { patientRequired: false });
    expect(screen.getByLabelText(/This order has no patient/)).toBeEnabled();
  });

  it("reveals the EQA detail only when EQA is ticked", async () => {
    renderSection({ isEQASample: true });
    expect(
      await screen.findByLabelText(/Provider Sample ID/),
    ).toBeInTheDocument();
  });

  it("keeps the EQA detail hidden otherwise", () => {
    renderSection();
    expect(
      screen.queryByLabelText(/Provider Sample ID/),
    ).not.toBeInTheDocument();
  });

  it("ticking EQA drops a patient already on the order", async () => {
    const { appliedOrder } = renderSection(
      {},
      { patientProperties: { lastName: "Ada", nationalId: "N1" } },
    );

    await userEvent.setup().click(screen.getByLabelText(/EQA\) sample/));

    expect(appliedOrder().patientProperties.lastName || "").toBe("");
    expect(appliedOrder().patientProperties.nationalId || "").toBe("");
  });

  // OGC-1483: the scheme is one of this lab's active enrollments.
  it("offers only the lab's active enrollments", async () => {
    renderSection({ isEQASample: true });

    expect(
      await screen.findByRole("option", { name: "WHO Malaria (WHO)" }),
    ).toBeInTheDocument();
    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/eqa/my-programs?active=true",
      expect.any(Function),
    );
    expect(getFromOpenElisServer).not.toHaveBeenCalledWith(
      "/rest/eqa/programs",
      expect.any(Function),
    );
  });

  it("shows a note instead of a scheme list when the lab has no enrollment", () => {
    getFromOpenElisServer.mockImplementation(serve({ enrollments: [] }));
    renderSection({ isEQASample: true });

    expect(
      screen.getByText(messages["order.eqa.noEnrollments.title"]),
    ).toBeInTheDocument();
    expect(
      screen.queryByLabelText(messages["order.eqa.scheme"]),
    ).not.toBeInTheDocument();
    expect(screen.getByLabelText(/Provider Sample ID/)).toBeInTheDocument();
  });

  it("stores the chosen enrollment's id and clears an earlier cycle", async () => {
    const { applied } = renderSection({ isEQASample: true, eqaCycleId: "9" });

    await userEvent
      .setup()
      .selectOptions(
        await screen.findByLabelText(messages["order.eqa.scheme"]),
        "7",
      );

    expect(applied()).toEqual(
      expect.objectContaining({ eqaProgramId: "7", eqaCycleId: "" }),
    );
  });

  it("warns, without blocking, when no cycle is chosen", async () => {
    renderSection({ isEQASample: true, eqaProgramId: "7" });

    expect(
      await screen.findByText(messages["order.eqa.noCycle.warning"]),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: messages["order.eqa.newCycle"] }),
    ).toBeInTheDocument();
  });

  it("a chosen cycle prefills its deadline", async () => {
    const { applied } = renderSection({ isEQASample: true, eqaProgramId: "7" });

    await userEvent
      .setup()
      .selectOptions(
        await screen.findByLabelText(messages["order.eqa.cycle"]),
        "31",
      );

    expect(applied()).toEqual(
      expect.objectContaining({ eqaCycleId: "31", eqaDeadline: "11/30/2026" }),
    );
  });

  it("No cycle drops the deadline the previous cycle filled in", async () => {
    const { applied } = renderSection({
      isEQASample: true,
      eqaProgramId: "7",
      eqaCycleId: "31",
      eqaDeadline: "11/30/2026",
    });
    await screen.findByRole("option", { name: "Malaria 2026-1" });

    await userEvent
      .setup()
      .selectOptions(screen.getByLabelText(messages["order.eqa.cycle"]), "");

    expect(applied()).toEqual(
      expect.objectContaining({ eqaCycleId: "", eqaDeadline: "" }),
    );
  });

  it("warns when the provider sample id matches none of the cycle's panel codes", async () => {
    renderSection({
      isEQASample: true,
      eqaProgramId: "7",
      eqaCycleId: "31",
      eqaProviderSampleId: "MAL-99",
    });

    expect(
      await screen.findByText(/matches none of the cycle's panel codes/),
    ).toBeInTheDocument();
  });
});
