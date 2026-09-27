import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../languages/en.json";

// ---------------------------------------------------------------------------
// OGC-1145 FR-8 — the e-order accession sample-type chooser. When the
// LabOrderSearchProvider can't bind an orderable to one specimen (multi-type
// test, no specimen coding) it emits <crosstest> entries; the order form must
// surface a chooser instead of first-matching, and resolving one files the
// test under the chosen sample type.
// ---------------------------------------------------------------------------

const { utilsMock } = vi.hoisted(() => ({
  utilsMock: {
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerFormData: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
    resolveApiErrorMessage: vi.fn(),
  },
}));
vi.mock("../utils/Utils", () => utilsMock);

vi.mock("../layout/Layout", () => ({
  NotificationContext: React.createContext({
    notificationVisible: false,
    setNotificationVisible: vi.fn(),
    addNotification: vi.fn(),
  }),
  ConfigurationContext: React.createContext({
    configurationProperties: {
      ACCEPT_EXTERNAL_ORDERS: "true",
      AUTOFILL_COLLECTION_DATE: "false",
    },
  }),
}));

vi.mock("../common/CustomNotification", () => ({
  AlertDialog: () => <div />,
  NotificationKinds: { success: "success", error: "error" },
}));

// Heavy children irrelevant to the chooser wiring.
vi.mock("./PatientInfo", () => ({ default: () => <div /> }));
vi.mock("./AddSample", () => ({ default: () => <div /> }));
vi.mock("./AddOrder", () => ({ default: () => <div /> }));
vi.mock("./OrderEntryAdditionalQuestions", () => ({ default: () => <div /> }));
vi.mock("./OrderSuccessMessage", () => ({ default: () => <div /> }));
vi.mock("../eqa/EQASampleEntry", () => ({ default: () => <div /> }));
vi.mock("../eqa/EQAOrderForm", () => ({ default: () => <div /> }));
vi.mock("../common/PageBreadCrumb", () => ({ default: () => <div /> }));

import Index from "./Index";

// The provider's JSON-ified XML for an order whose only orderable is a
// multi-specimen test with no specimen coding: no pre-bound sampleTypes,
// one <crosstest> carrying the candidate types.
const crossTestOrder = () => ({
  fieldmessage: {
    message: "valid",
    formfield: {
      order: {
        patient: { guid: "guid-1145" },
        sampleTypes: "",
        crosstest: {
          name: "COVID-19 PCR",
          crosssampletypes: {
            crosssampletype: [
              { id: "2", name: "Serum", testid: "300" },
              { id: "30", name: "Respiratory Swab", testid: "300" },
            ],
          },
        },
      },
    },
  },
});

const renderWithOrder = () => {
  window.history.pushState({}, "", "/SamplePatientEntry?ID=EORD1145X1");
  global.fetch = vi.fn(() =>
    Promise.resolve({ json: () => Promise.resolve(crossTestOrder()) }),
  );
  return render(
    <MemoryRouter>
      <IntlProvider locale="en" messages={messages}>
        <Index />
      </IntlProvider>
    </MemoryRouter>,
  );
};

describe("e-order awaiting-specimen chooser (OGC-1145 FR-8)", () => {
  it("surfaces the chooser with the candidate sample types instead of first-matching", async () => {
    renderWithOrder();

    const chooser = await screen.findByTestId("awaiting-specimen-chooser");
    expect(chooser).toBeInTheDocument();
    expect(
      screen.getByText(messages["notice.testCatalog.intake.awaitingSpecimen"]),
    ).toBeInTheDocument();

    const select = screen.getByLabelText("COVID-19 PCR");
    const options = Array.from(select.querySelectorAll("option")).map(
      (o) => o.textContent,
    );
    expect(options).toEqual(
      expect.arrayContaining(["Serum", "Respiratory Swab"]),
    );
  });

  it("resolving the chooser files the test and clears the hold", async () => {
    renderWithOrder();
    const select = await screen.findByLabelText("COVID-19 PCR");

    fireEvent.change(select, { target: { value: "2" } });

    await waitFor(() =>
      expect(
        screen.queryByTestId("awaiting-specimen-chooser"),
      ).not.toBeInTheDocument(),
    );
  });
});
