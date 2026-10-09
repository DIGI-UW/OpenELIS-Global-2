import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";

/**
 * OGC-1240: a required field tells assistive technology it is required, not
 * only sighted users through the asterisk, and it follows the same setting as
 * the asterisk. A National ID the server refused is shown on the field itself.
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
  default: () => <div data-testid="custom-date-picker-mock" />,
}));

import CreatePatientForm from "../CreatePatientForm";
import { ConfigurationContext } from "../../layout/Layout";

const renderForm = (
  configurationProperties: Record<string, string>,
  error?: (field: string) => string | null,
) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <ConfigurationContext.Provider value={{ configurationProperties }}>
        <CreatePatientForm
          showActionsButton={false}
          selectedPatient={{}}
          onClear={() => {}}
          error={error}
        />
      </ConfigurationContext.Provider>
    </IntlProvider>,
  );

describe("CreatePatientForm required fields (OGC-1240)", () => {
  test("National ID is announced as required, and its asterisk is hidden from screen readers", () => {
    renderForm({});

    const nationalId = screen.getByRole("textbox", { name: /National ID/ });
    expect(nationalId).toHaveAttribute("aria-required", "true");
    expect(
      nationalId.closest("div.cds--form-item")?.querySelector(".requiredlabel"),
    ).toHaveAttribute("aria-hidden", "true");
  });

  test("National ID is neither marked nor announced when the deployment turns it off", () => {
    const { container } = renderForm({ PATIENT_NATIONAL_ID_REQUIRED: "false" });

    const nationalId = container.querySelector("#nationalId");
    expect(nationalId).not.toHaveAttribute("aria-required");
    expect(
      nationalId
        ?.closest("div.cds--form-item")
        ?.querySelector(".requiredlabel"),
    ).toBeNull();
  });

  test("a National ID the server refused is shown on the field", () => {
    const { container } = renderForm({}, (field) =>
      field === "patientProperties.nationalId" ? "Cannot be blank" : null,
    );

    expect(container.querySelector("#nationalId")).toHaveAttribute(
      "aria-invalid",
      "true",
    );
    expect(screen.getByText("Cannot be blank")).toBeInTheDocument();
  });
});
