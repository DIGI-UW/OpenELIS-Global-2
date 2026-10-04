import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";

/**
 * A list the form loads (health regions and districts, education, marital
 * status) can fail to arrive: a 500, an expired session, a dropped
 * connection. The request then reports nothing, and the form still renders
 * with empty lists instead of crashing the order page behind it.
 */

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn((url, callback) => {
    if (typeof callback === "function") {
      callback(undefined);
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

// Render the form with a per-test configurationProperties override. We can't
// just mutate the module-scope context default, so we wrap in a Provider —
// React's useContext(ConfigurationContext) picks up the nearest Provider's
// value, overriding createContext's default.
const renderWithConfig = (configurationProperties) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <ConfigurationContext.Provider value={{ configurationProperties }}>
        <CreatePatientForm
          showActionsButton={true}
          selectedPatient={{}}
          onClear={() => {}}
        />
      </ConfigurationContext.Provider>
    </IntlProvider>,
  );

const flush = () => new Promise((r) => setTimeout(r, 0));

describe("CreatePatientForm when its lists fail to load", () => {
  test("renders the form with empty lists instead of crashing", async () => {
    renderWithConfig({
      USE_NEW_ADDRESS_HIERARCHY: "false",
      PATIENT_GPS_CAPTURE_ENABLED: "false",
    });
    await flush();

    expect(document.getElementById("lastName")).not.toBeNull();
    expect(document.getElementById("education")).not.toBeNull();
    expect(document.getElementById("maritialStatus")).not.toBeNull();
    expect(screen.queryByText(/Something went wrong/i)).toBeNull();
  });
});
