import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";

/**
 * The notification dialog is shared through NotificationContext. An order
 * screen renders it once for the whole page, so the patient form embedded in
 * that screen must not render a second one, or every message shows twice.
 * Standing alone (Patient Management) the form is the only host, so it does.
 */

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn((url, callback) => {
    if (typeof callback === "function") {
      callback([]);
    }
  }),
  postToOpenElisServerJsonResponse: vi.fn(),
  resolveApiErrorMessage: vi.fn((err) => String(err)),
}));

vi.mock("../../layout/Layout", () => ({
  NotificationContext: React.createContext({
    notificationVisible: true,
    setNotificationVisible: () => {},
    addNotification: () => {},
  }),
  ConfigurationContext: React.createContext({
    configurationProperties: {},
  }),
}));

vi.mock("../../common/CustomNotification", () => ({
  AlertDialog: () => <div data-testid="alert-dialog" />,
  NotificationKinds: { success: "success", error: "error" },
}));

vi.mock("../AddressSearch", () => ({
  default: () => <div data-testid="address-search-mock" />,
}));

vi.mock("../photoManagement/uploadPhoto/PatientImageSelector", () => ({
  default: () => <div data-testid="patient-image-selector-mock" />,
}));

vi.mock("../IdentificationDocuments", () => ({
  default: () => <div data-testid="identification-documents-mock" />,
}));

vi.mock("../PatientFormObserver", () => ({
  default: () => null,
}));

vi.mock("../../common/CustomDatePicker", () => ({
  default: () => <div data-testid="custom-date-picker-mock" />,
}));

import CreatePatientForm from "../CreatePatientForm";

const renderForm = (extraProps = {}) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <CreatePatientForm
        showActionsButton={false}
        selectedPatient={{}}
        onClear={() => {}}
        {...extraProps}
      />
    </IntlProvider>,
  );

describe("CreatePatientForm notification dialog", () => {
  it("shows the dialog itself when it stands alone", () => {
    renderForm();

    expect(screen.getByTestId("alert-dialog")).toBeInTheDocument();
  });

  it("leaves the dialog to the order screen it is embedded in", () => {
    renderForm({
      orderFormValues: { patientProperties: {} },
      setOrderFormValues: () => {},
    });

    expect(screen.queryByTestId("alert-dialog")).toBeNull();
  });
});
