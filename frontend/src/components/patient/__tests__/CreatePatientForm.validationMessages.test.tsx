import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitForElementToBeRemoved } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";

/**
 * OGC-1497: saving an empty patient shows each missing field's message once,
 * on its field, in the user's language.
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
    notificationVisible: false,
    setNotificationVisible: () => {},
    addNotification: () => {},
  }),
  ConfigurationContext: React.createContext({
    configurationProperties: {},
  }),
}));

vi.mock("../../common/CustomNotification", () => ({
  AlertDialog: () => null,
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
  default: ({ invalid, invalidText }) => (
    <div data-testid="custom-date-picker-mock">
      {invalid ? invalidText : null}
    </div>
  ),
}));

import CreatePatientForm from "../CreatePatientForm";
import { ConfigurationContext } from "../../layout/Layout";

const renderForm = (error?: (field: string) => string | null) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <ConfigurationContext.Provider value={{ configurationProperties: {} }}>
        <CreatePatientForm
          showActionsButton={true}
          selectedPatient={{}}
          onClear={() => {}}
          error={error}
        />
      </ConfigurationContext.Provider>
    </IntlProvider>,
  );

describe("CreatePatientForm validation messages (OGC-1497)", () => {
  test("saving an empty patient shows each required message once", async () => {
    renderForm();

    fireEvent.click(screen.getByRole("button", { name: "Save" }));

    expect(
      await screen.findAllByText(messages["patient.validation.sexRequired"]),
    ).toHaveLength(1);
    expect(
      screen.getAllByText(messages["patient.validation.dobOrAgeRequired"]),
    ).toHaveLength(1);
    expect(
      screen.getAllByText(messages["patient.validation.nationalIdRequired"]),
    ).toHaveLength(1);
    expect(screen.queryByText("Sex is Required")).toBeNull();
  });

  test("after a refused save, choosing the sex clears its message at once", async () => {
    renderForm();
    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    expect(
      await screen.findByText(messages["patient.validation.sexRequired"]),
    ).toBeInTheDocument();

    fireEvent.click(screen.getByLabelText(messages["patient.female"]));

    await waitForElementToBeRemoved(() =>
      screen.queryByText(messages["patient.validation.sexRequired"]),
    );
  });

  test("after a refused save, typing an age clears the birth date message", async () => {
    const { container } = renderForm();
    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    expect(
      await screen.findByText(messages["patient.validation.dobOrAgeRequired"]),
    ).toBeInTheDocument();

    fireEvent.change(container.querySelector("#years"), {
      target: { value: "34" },
    });

    await waitForElementToBeRemoved(() =>
      screen.queryByText(messages["patient.validation.dobOrAgeRequired"]),
    );
  });

  test("an order-entry message key is shown translated on its field", () => {
    renderForm((field) =>
      field === "patientProperties.gender"
        ? "patient.validation.sexRequired"
        : null,
    );

    expect(
      screen.getAllByText(messages["patient.validation.sexRequired"]),
    ).toHaveLength(1);
    expect(screen.queryByText("patient.validation.sexRequired")).toBeNull();
  });
});
