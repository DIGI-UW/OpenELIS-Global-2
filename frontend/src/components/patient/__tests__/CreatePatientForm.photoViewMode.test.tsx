import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";

/**
 * An existing patient opens in view mode until the Edit toggle is switched on.
 * The photo tile must follow that mode: in view mode it only offers the
 * read-only viewer, in edit mode it offers the picker. The tile learns the
 * mode through its `disabled` prop, which the form used to derive from the
 * parent's explicit lock alone, so view mode still opened the picker with
 * Confirm, drag-and-drop and the camera all live.
 */

const testState = vi.hoisted(() => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
  selectorProps: [] as Array<{ disabled?: boolean }>,
}));

testState.getFromOpenElisServer.mockImplementation((url, callback) => {
  if (url.includes("SampleEntryAccessionNumberValidation")) {
    callback({ status: true });
    return;
  }
  callback([]);
});

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: testState.getFromOpenElisServer,
  postToOpenElisServerJsonResponse: testState.postToOpenElisServerJsonResponse,
  resolveApiErrorMessage: vi.fn(() => "Save failed"),
}));

vi.mock("../../layout/Layout", () => ({
  NotificationContext: React.createContext({
    notificationVisible: false,
    setNotificationVisible: () => {},
    addNotification: () => {},
  }),
  ConfigurationContext: React.createContext({
    configurationProperties: {
      USE_NEW_ADDRESS_HIERARCHY: "false",
      PATIENT_GPS_CAPTURE_ENABLED: "false",
      DEFAULT_NATIONALITY: "",
      DEFAULT_DATE_LOCALE: "en-US",
      FIRST_NAME_REGEX: "^[A-Za-z\\s'-]+$",
      LAST_NAME_REGEX: "^[A-Za-z\\s'-]+$",
      PATIENT_NATIONAL_ID_REQUIRED: "false",
      PATIENT_ALIAS_ENABLED: "false",
      PHONE_FORMAT: "+261-xx-xxx-xx-xx",
    },
  }),
}));

vi.mock("../../common/CustomNotification", () => ({
  AlertDialog: () => null,
  NotificationKinds: { success: "success", error: "error" },
}));

vi.mock("../AddressSearch", () => ({ default: () => null }));
vi.mock("../photoManagement/uploadPhoto/PatientImageSelector", () => ({
  default: (props: { disabled?: boolean }) => {
    testState.selectorProps.push(props);
    return (
      <div
        data-testid="patient-image-selector-mock"
        data-disabled={String(Boolean(props.disabled))}
      />
    );
  },
}));
vi.mock("../IdentificationDocuments", () => ({
  default: () => <div data-testid="identification-documents-mock" />,
}));
vi.mock("../PatientFormObserver", () => ({ default: () => null }));
vi.mock("../../common/CustomDatePicker", () => ({
  default: ({ value, onChange, id }) => (
    <input
      id={id || "date-picker-default-id"}
      data-testid="dob-input"
      value={value || ""}
      onChange={(e) => onChange(e.target.value)}
    />
  ),
}));

vi.mock("react-router-dom", () => ({
  useHistory: () => ({ push: vi.fn() }),
}));

import CreatePatientForm from "../CreatePatientForm";

const renderForm = (selectedPatient?: Record<string, string>) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <CreatePatientForm
        showActionsButton={true}
        selectedPatient={selectedPatient}
        onClear={() => {}}
      />
    </IntlProvider>,
  );

const selectorDisabled = () =>
  screen
    .getByTestId("patient-image-selector-mock")
    .getAttribute("data-disabled");

beforeEach(() => {
  testState.selectorProps.length = 0;
});

describe("CreatePatientForm — the photo tile follows view and edit mode", () => {
  test("an existing patient opens with the photo tile read-only", () => {
    renderForm({
      patientPK: "EXISTING-1",
      firstName: "Old",
      lastName: "Patient",
      gender: "M",
      birthDateForDisplay: "01/15/1990",
      nationalId: "EX-001",
    });

    expect(selectorDisabled()).toBe("true");
  });

  test("switching Edit on hands the photo tile back to the picker", async () => {
    const user = userEvent.setup();
    renderForm({
      patientPK: "EXISTING-1",
      firstName: "Old",
      lastName: "Patient",
      gender: "M",
      birthDateForDisplay: "01/15/1990",
      nationalId: "EX-001",
    });
    expect(selectorDisabled()).toBe("true");

    await user.click(document.getElementById("patient-edit-toggle")!);

    expect(selectorDisabled()).toBe("false");
  });

  test("a new patient can pick a photo straight away", () => {
    renderForm(undefined);

    expect(selectorDisabled()).toBe("false");
  });
});
