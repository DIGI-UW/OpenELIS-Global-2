import React, { useEffect } from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../languages/en.json";

// An incoming (electronic) order opened from Order > Incoming Orders loads the
// referral once. A second load used to replace the whole order with a copy
// taken before the patient arrived, so the patient's required fields were
// empty and Submit stayed disabled while the form still showed the patient.

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

// Stands in for PatientInfo: once the order names a patient (guid), it loads
// the patient into the order, as the real component does after its search.
vi.mock("./PatientInfo", () => ({
  default: ({ orderFormValues, setOrderFormValues }) => {
    const guid = orderFormValues.patientProperties?.guid;
    useEffect(() => {
      if (guid) {
        setOrderFormValues((prev) => ({
          ...prev,
          patientProperties: {
            ...prev.patientProperties,
            lastName: "Okello",
            nationalId: "NIN-5040",
          },
        }));
      }
    }, [guid]);
    return (
      <div data-testid="order-patient">
        {orderFormValues.patientProperties?.lastName || "no patient"}
      </div>
    );
  },
}));
vi.mock("./AddSample", () => ({ default: () => <div /> }));
vi.mock("./AddOrder", () => ({ default: () => <div /> }));
vi.mock("./OrderEntryAdditionalQuestions", () => ({ default: () => <div /> }));
vi.mock("./OrderSuccessMessage", () => ({ default: () => <div /> }));
vi.mock("../eqa/EQASampleEntry", () => ({ default: () => <div /> }));
vi.mock("../eqa/EQAOrderForm", () => ({ default: () => <div /> }));
vi.mock("../common/PageBreadCrumb", () => ({ default: () => <div /> }));

import Index from "./Index";

const referral = () => ({
  fieldmessage: {
    message: "valid",
    formfield: {
      order: {
        patient: { guid: "guid-5040" },
        sampleTypes: "",
      },
    },
  },
});

const providerCalls = () =>
  global.fetch.mock.calls.filter(([url]) =>
    String(url).includes("LabOrderSearchProvider"),
  );

describe("incoming order prefill", () => {
  it("loads the referral once and keeps the patient it brought", async () => {
    window.history.pushState({}, "", "/SamplePatientEntry?ID=EORD5040");
    global.fetch = vi.fn(
      () =>
        new Promise((resolve) =>
          setTimeout(
            () => resolve({ json: () => Promise.resolve(referral()) }),
            20,
          ),
        ),
    );

    render(
      <MemoryRouter>
        <IntlProvider locale="en" messages={messages}>
          <Index />
        </IntlProvider>
      </MemoryRouter>,
    );

    expect(await screen.findByText("Okello")).toBeInTheDocument();
    await new Promise((resolve) => setTimeout(resolve, 200));

    expect(providerCalls()).toHaveLength(1);
    await waitFor(() =>
      expect(screen.getByTestId("order-patient")).toHaveTextContent("Okello"),
    );
  });
});
